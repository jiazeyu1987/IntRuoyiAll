package cn.iocoder.yudao.module.system.service.configpackage;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackagePrecheckRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(SystemConfigPackageServiceImpl.class)
class SystemConfigPackageServiceImplTest extends BaseDbUnitTest {

    private static final List<String> AVAILABLE_COMPONENTS = List.of(
            "system/config-package/index",
            "system/user/index"
    );
    private static final List<String> REQUIRED_SHEETS = List.of(
            "manifest", "menus", "roles", "role_menu", "users", "user_role",
            "dept", "post", "user_post", "dict_type", "dict_data", "tenant_package"
    );

    @Resource
    private SystemConfigPackageServiceImpl service;

    @Resource
    private MenuMapper menuMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initJdbcTemplate() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void exportPackageShouldContainRequiredSheetsAndNoPasswordHashByDefault() throws Exception {
        seedConfigPackageMenu("配置包中心");
        seedRole(10L, "配置管理员");
        seedRoleMenu(100L, 10L, 9000L);
        seedUser(20L, "operator", "source-password-hash");
        jdbcTemplate.update("""
                INSERT INTO system_user_role
                (id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
                VALUES (30, 20, 10, '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, FALSE, 1)
                """);

        byte[] workbookBytes = service.exportPackage();

        assertEquals(REQUIRED_SHEETS, readSheetNames(workbookBytes));
        Map<String, String> manifest = readManifest(workbookBytes);
        assertEquals("1", manifest.get("formatVersion"));
        assertEquals("1", manifest.get("sourceTenantId"));
        assertEquals("preserve_existing_passwords", manifest.get("passwordPolicy"));
        assertFalse(readHeader(workbookBytes, "users").contains("密码摘要"));
    }

    @Test
    void exportPackageShouldNotExposePasswordHashOption() {
        assertThrows(NoSuchMethodException.class,
                () -> SystemConfigPackageService.class.getMethod("exportPackage", Boolean.class));
    }

    @Test
    void precheckShouldReportInvalidFrontendComponentAndNotWriteDatabase() throws Exception {
        seedConfigPackageMenu("配置包中心");
        seedRole(10L, "配置管理员");
        seedUser(20L, "operator", "source-password-hash");
        byte[] exported = service.exportPackage();
        byte[] invalidPackage = replaceFirstMenuComponent(exported, "system/missing/index");

        SystemConfigPackagePrecheckRespVO result = service.precheck(invalidPackage, AVAILABLE_COMPONENTS);

        assertFalse(result.getValid());
        assertTrue(result.getBlockingErrors().stream()
                .anyMatch(error -> error.contains("system/missing/index")));
        assertEquals("配置包中心", menuMapper.selectById(9000L).getName());
    }

    @Test
    void precheckShouldAcceptPackageHashFromFreshExportWhenValuesContainTrimmedWhitespace() throws Exception {
        seedConfigPackageMenu("配置包中心 ");
        seedRole(10L, "配置管理员");
        seedUser(20L, "operator", "source-password-hash");

        byte[] exported = service.exportPackage();
        SystemConfigPackagePrecheckRespVO result = service.precheck(exported, AVAILABLE_COMPONENTS);

        assertTrue(result.getBlockingErrors().stream()
                .noneMatch(error -> error.contains("packageSha256 不匹配")));
    }

    @Test
    void precheckShouldAllowPackageFromDifferentTenant() throws Exception {
        seedConfigPackageMenu("配置包中心");
        seedRole(10L, "配置管理员");
        seedUser(20L, "operator", "source-password-hash");
        byte[] exported = service.exportPackage();

        try {
            TenantContextHolder.setTenantId(2L);
            SystemConfigPackagePrecheckRespVO result = service.precheck(exported, AVAILABLE_COMPONENTS);

            assertTrue(result.getValid());
            assertTrue(result.getBlockingErrors().isEmpty());
        } finally {
            TenantContextHolder.setTenantId(1L);
        }
    }

    @Test
    void exportPackageShouldIncludePostsStillReferencedByUsersAndUserPosts() throws Exception {
        seedConfigPackageMenu("配置包中心");
        seedRole(10L, "配置管理员");
        seedUser(20L, "operator", "source-password-hash");
        seedPost(2L, "项目经理", "SE", true);
        seedPost(8L, "车间主任", "WORKSHOP_DIRECTOR", true);
        jdbcTemplate.update("UPDATE system_users SET post_ids = '[2,8]' WHERE id = 20");
        jdbcTemplate.update("""
                INSERT INTO system_user_post
                (id, user_id, post_id, creator, create_time, updater, update_time, deleted, tenant_id)
                VALUES (30, 20, 2, '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, FALSE, 1),
                       (31, 20, 8, '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, FALSE, 1)
                """);

        byte[] exported = service.exportPackage();
        List<LinkedHashMap<String, String>> postRows = readSheetRows(exported, "post");

        assertEquals(List.of("2", "8"), postRows.stream().map(row -> row.get("岗位ID")).toList());

        try {
            TenantContextHolder.setTenantId(122L);
            seedSystemParentMenu();
            SystemConfigPackagePrecheckRespVO result = service.precheck(exported, AVAILABLE_COMPONENTS);

            assertTrue(result.getValid());
            assertTrue(result.getBlockingErrors().isEmpty());
        } finally {
            TenantContextHolder.setTenantId(1L);
        }
    }

    @Test
    void importPackageShouldReplaceConfigAndPreserveExistingPasswordWhenHashIsNotExported() throws Exception {
        seedConfigPackageMenu("配置包中心");
        seedRole(10L, "配置管理员");
        seedRoleMenu(100L, 10L, 9000L);
        seedUser(20L, "operator", "source-password-hash");
        byte[] desiredPackage = service.exportPackage();

        jdbcTemplate.update("DELETE FROM system_role_menu WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM system_role WHERE tenant_id = 1");
        jdbcTemplate.update("DELETE FROM system_menu");
        jdbcTemplate.update("DELETE FROM system_users WHERE tenant_id = 1");
        seedMenu(9000L, "配置包中心-已漂移", "system/config-package/index");
        seedRole(10L, "配置管理员-已漂移");
        seedRole(11L, "多余角色");
        seedUser(20L, "operator", "target-password-hash");

        SystemConfigPackagePrecheckRespVO precheck = service.precheck(desiredPackage, AVAILABLE_COMPONENTS);
        assertTrue(precheck.getValid());

        service.importPackage(desiredPackage, true, precheck.getTargetSnapshotSha256(), AVAILABLE_COMPONENTS);

        assertEquals("配置包中心", menuMapper.selectById(9000L).getName());
        assertEquals("配置管理员", roleMapper.selectById(10L).getName());
        assertEquals(1L, roleMapper.selectCount());
        assertEquals("target-password-hash", userMapper.selectById(20L).getPassword());
        List<RoleMenuDO> roleMenus = roleMenuMapper.selectListByRoleId(10L);
        assertEquals(1, roleMenus.size());
        assertEquals(9000L, roleMenus.get(0).getMenuId());
    }

    @Test
    void importPackageShouldAllowCrossTenantNewUserWhenPasswordHashesArePreserved() throws Exception {
        seedConfigPackageMenu("配置包中心");
        seedRole(10L, "配置管理员");
        seedRoleMenu(100L, 10L, 9000L);
        seedUser(20L, "operator", "source-password-hash");
        jdbcTemplate.update("""
                INSERT INTO system_user_role
                (id, user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
                VALUES (30, 20, 10, '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, FALSE, 1)
                """);
        byte[] desiredPackage = service.exportPackage();

        try {
            TenantContextHolder.setTenantId(122L);
            seedSystemParentMenu();
            SystemConfigPackagePrecheckRespVO precheck = service.precheck(desiredPackage, AVAILABLE_COMPONENTS);
            assertTrue(precheck.getValid());

            service.importPackage(desiredPackage, true, precheck.getTargetSnapshotSha256(), AVAILABLE_COMPONENTS);

            AdminUserDO importedUser = userMapper.selectListByUsernames(List.of("operator")).stream()
                    .filter(user -> Long.valueOf(122L).equals(user.getTenantId()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(importedUser);
            assertEquals(122L, importedUser.getTenantId());
            assertEquals("operator", importedUser.getUsername());
            assertFalse("source-password-hash".equals(importedUser.getPassword()));
        } finally {
            TenantContextHolder.setTenantId(1L);
        }
    }

    private void seedConfigPackageMenu(String name) {
        seedSystemParentMenu();
        seedMenu(9000L, name, "system/config-package/index");
    }

    private void seedSystemParentMenu() {
        if (menuMapper.selectById(1L) != null) {
            return;
        }
        MenuDO menu = new MenuDO();
        menu.setId(1L);
        menu.setName("系统管理");
        menu.setPermission("");
        menu.setType(MenuTypeEnum.DIR.getType());
        menu.setSort(1);
        menu.setParentId(0L);
        menu.setPath("/system");
        menu.setIcon("ep:setting");
        menu.setComponent("");
        menu.setComponentName("");
        menu.setStatus(CommonStatusEnum.ENABLE.getStatus());
        menu.setVisible(true);
        menu.setKeepAlive(true);
        menu.setAlwaysShow(true);
        menu.setDeleted(false);
        menuMapper.insert(menu);
    }

    private void seedMenu(Long id, String name, String component) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setName(name);
        menu.setPermission("system:config-package:query");
        menu.setType(MenuTypeEnum.MENU.getType());
        menu.setSort(99);
        menu.setParentId(1L);
        menu.setPath("config-package");
        menu.setIcon("ep:document-copy");
        menu.setComponent(component);
        menu.setComponentName("SystemConfigPackage");
        menu.setStatus(CommonStatusEnum.ENABLE.getStatus());
        menu.setVisible(true);
        menu.setKeepAlive(true);
        menu.setAlwaysShow(true);
        menu.setDeleted(false);
        menuMapper.insert(menu);
    }

    private void seedRole(Long id, String name) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setName(name);
        role.setCode("config_admin_" + id);
        role.setSort(1);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setRemark("");
        role.setDataScope(DataScopeEnum.ALL.getScope());
        role.setTenantId(1L);
        role.setDeleted(false);
        roleMapper.insert(role);
    }

    private void seedRoleMenu(Long id, Long roleId, Long menuId) {
        RoleMenuDO roleMenu = new RoleMenuDO();
        roleMenu.setId(id);
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        roleMenu.setTenantId(1L);
        roleMenu.setDeleted(false);
        roleMenuMapper.insert(roleMenu);
    }

    private void seedUser(Long id, String username, String password) {
        AdminUserDO user = new AdminUserDO();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(username);
        user.setRemark("");
        user.setEmail(username + "@example.com");
        user.setMobile("13800138000");
        user.setSex(1);
        user.setAvatar("");
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        user.setTenantId(1L);
        user.setDeleted(false);
        userMapper.insert(user);
    }

    private void seedPost(Long id, String name, String code, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO system_post
                (id, code, name, sort, status, remark, creator, create_time, updater, update_time, deleted, tenant_id)
                VALUES (?, ?, ?, 1, ?, '', '', CURRENT_TIMESTAMP, '', CURRENT_TIMESTAMP, ?, 1)
                """, id, code, name, CommonStatusEnum.ENABLE.getStatus(), deleted);
    }

    private static List<String> readSheetNames(byte[] bytes) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                names.add(workbook.getSheetName(index));
            }
            return names;
        }
    }

    private static Map<String, String> readManifest(byte[] bytes) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("manifest");
            assertNotNull(sheet);
            java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String key = formatter.formatCellValue(row.getCell(0));
                String value = formatter.formatCellValue(row.getCell(1));
                if (!key.isBlank()) {
                    values.put(key, value);
                }
            }
            return values;
        }
    }

    private static List<String> readHeader(byte[] bytes, String sheetName) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheet(sheetName).getRow(0);
            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            java.util.ArrayList<String> headers = new java.util.ArrayList<>();
            for (int index = 0; index < row.getLastCellNum(); index++) {
                headers.add(formatter.formatCellValue(row.getCell(index)));
            }
            return headers;
        }
    }

    private static List<LinkedHashMap<String, String>> readSheetRows(byte[] bytes, String sheetName) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet(sheetName);
            Row headerRow = sheet.getRow(0);
            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            List<String> headers = new ArrayList<>();
            for (int index = 0; index < headerRow.getLastCellNum(); index++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(index)));
            }
            List<LinkedHashMap<String, String>> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                LinkedHashMap<String, String> values = new LinkedHashMap<>();
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    values.put(headers.get(cellIndex), formatter.formatCellValue(row.getCell(cellIndex)));
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private static byte[] replaceFirstMenuComponent(byte[] bytes, String component) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("menus");
            Row header = sheet.getRow(0);
            int componentColumn = -1;
            for (int index = 0; index < header.getLastCellNum(); index++) {
                if ("组件路径".equals(header.getCell(index).getStringCellValue())) {
                    componentColumn = index;
                    break;
                }
            }
            assertTrue(componentColumn >= 0);
            sheet.getRow(1).getCell(componentColumn).setCellValue(component);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
