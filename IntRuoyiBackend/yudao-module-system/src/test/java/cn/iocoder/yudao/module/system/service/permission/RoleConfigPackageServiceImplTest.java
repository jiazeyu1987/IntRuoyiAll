package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Import(RoleConfigPackageServiceImpl.class)
class RoleConfigPackageServiceImplTest extends BaseDbUnitTest {

    @Resource
    private RoleConfigPackageServiceImpl roleConfigPackageService;

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleCategoryMapper roleCategoryMapper;

    @Resource
    private MenuMapper menuMapper;

    @MockitoBean
    private PermissionService permissionService;

    @Test
    void importPackage_shouldReturnBusinessErrorWhenJsonInvalid() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> roleConfigPackageService.importPackage("not-json".getBytes()));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID.getCode(), exception.getCode());
        assertEquals("配置包内容非法，原因：权限角色配置包 JSON 非法", exception.getMessage());
    }

    @Test
    void importPackage_shouldReturnBusinessErrorWhenVersionUnsupported() {
        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("1");
        payload.setRoles(List.of());

        assertServiceException(() -> roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload)),
                ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED, "1");
    }

    @Test
    void importPackage_shouldReturnBusinessErrorWhenRoleCodeMissing() {
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setName("排产员");
        item.setSort(1);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setMenuKeys(List.of());

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("menu", "菜单")));
        payload.setRoles(List.of(item));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload)));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID.getCode(), exception.getCode());
        assertEquals("配置包内容非法，原因：权限角色配置包缺少 role code", exception.getMessage());
    }

    private void insertButtonMenu(String name, String permission) {
        MenuDO menu = new MenuDO();
        menu.setName(name);
        menu.setPermission(permission);
        menu.setType(MenuTypeEnum.BUTTON.getType());
        menu.setSort(1);
        menu.setParentId(0L);
        menu.setPath("");
        menu.setComponent("");
        menu.setComponentName("");
        menu.setStatus(CommonStatusEnum.ENABLE.getStatus());
        menu.setVisible(Boolean.TRUE);
        menu.setKeepAlive(Boolean.FALSE);
        menu.setAlwaysShow(Boolean.FALSE);
        menuMapper.insert(menu);
    }

    @Test
    void importPackage_shouldAllowSystemRoleRoundTripWithoutDataScopeMutation() {
        insertButtonMenu("角色查询", "system:role:query");
        insertButtonMenu("角色更新", "system:role:update");
        MenuDO queryMenu = menuMapper.selectListByPermission("system:role:query").get(0);
        MenuDO updateMenu = menuMapper.selectListByPermission("system:role:update").get(0);

        RoleDO existing = new RoleDO();
        existing.setName("租户管理员");
        existing.setCode("tenant_admin");
        existing.setSort(0);
        existing.setStatus(CommonStatusEnum.ENABLE.getStatus());
        existing.setType(RoleTypeEnum.SYSTEM.getType());
        existing.setRemark("系统自动生成");
        existing.setCategoryId(insertCategory("menu", "菜单"));
        existing.setDataScope(DataScopeEnum.ALL.getScope());
        existing.setDataScopeDeptIds(null);
        roleMapper.insert(existing);

        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("tenant_admin");
        item.setName("租户管理员-导入");
        item.setSort(7);
        item.setStatus(CommonStatusEnum.DISABLE.getStatus());
        item.setType(RoleTypeEnum.SYSTEM.getType());
        item.setRemark("导入回放");
        item.setCategoryCode("menu");
        item.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope());
        item.setDataScopeDeptIds(Set.of(100L, 200L));
        item.setMenuKeys(List.of("permission:system:role:query", "permission:system:role:update"));

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("menu", "菜单")));
        payload.setRoles(List.of(item));

        roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload));

        RoleDO dbRole = roleMapper.selectById(existing.getId());
        assertEquals("租户管理员-导入", dbRole.getName());
        assertEquals(7, dbRole.getSort());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), dbRole.getStatus());
        assertEquals("导入回放", dbRole.getRemark());
        assertEquals(existing.getCategoryId(), dbRole.getCategoryId());
        assertEquals(DataScopeEnum.DEPT_CUSTOM.getScope(), dbRole.getDataScope());
        assertEquals(Set.of(100L, 200L), dbRole.getDataScopeDeptIds());
        verify(permissionService).assignRoleMenu(existing.getId(), Set.of(queryMenu.getId(), updateMenu.getId()));
        verify(permissionService, never()).assignRoleDataScope(existing.getId(), dbRole.getDataScope(), dbRole.getDataScopeDeptIds());
    }

    @Test
    void importPackage_shouldCreateMissingSystemRoleWithoutCallingDataScopeMutation() {
        insertButtonMenu("角色创建", "system:role:create");
        insertButtonMenu("角色更新", "system:role:update");
        MenuDO createMenu = menuMapper.selectListByPermission("system:role:create").get(0);
        MenuDO updateMenu = menuMapper.selectListByPermission("system:role:update").get(0);

        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("tenant_admin");
        item.setName("租户管理员");
        item.setSort(9);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setType(RoleTypeEnum.SYSTEM.getType());
        item.setRemark("跨环境导入");
        item.setCategoryCode("menu");
        item.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope());
        item.setDataScopeDeptIds(Set.of(300L, 400L));
        item.setMenuKeys(List.of("permission:system:role:create", "permission:system:role:update"));
        Long categoryId = insertCategory("menu", "菜单");

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("menu", "菜单")));
        payload.setRoles(List.of(item));

        roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload));

        RoleDO dbRole = roleMapper.selectByCode("tenant_admin");
        assertEquals("租户管理员", dbRole.getName());
        assertEquals(9, dbRole.getSort());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), dbRole.getStatus());
        assertEquals(RoleTypeEnum.SYSTEM.getType(), dbRole.getType());
        assertEquals("跨环境导入", dbRole.getRemark());
        assertEquals(categoryId, dbRole.getCategoryId());
        assertEquals(DataScopeEnum.DEPT_CUSTOM.getScope(), dbRole.getDataScope());
        assertEquals(Set.of(300L, 400L), dbRole.getDataScopeDeptIds());
        verify(permissionService).assignRoleMenu(dbRole.getId(), Set.of(createMenu.getId(), updateMenu.getId()));
        verify(permissionService, never()).assignRoleDataScope(dbRole.getId(), dbRole.getDataScope(), dbRole.getDataScopeDeptIds());
    }

    @Test
    void exportPackage_shouldIncludeStableMenuKeys() {
        MenuDO menu = new MenuDO();
        menu.setName("角色查询");
        menu.setPermission("system:role:query");
        menu.setType(MenuTypeEnum.BUTTON.getType());
        menu.setSort(1);
        menu.setParentId(0L);
        menu.setPath("");
        menu.setComponent("");
        menu.setComponentName("");
        menu.setStatus(CommonStatusEnum.ENABLE.getStatus());
        menu.setVisible(Boolean.TRUE);
        menu.setKeepAlive(Boolean.FALSE);
        menu.setAlwaysShow(Boolean.FALSE);
        menuMapper.insert(menu);

        RoleDO role = new RoleDO();
        role.setName("排产员");
        role.setCode("scheduler");
        role.setSort(1);
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        role.setType(RoleTypeEnum.CUSTOM.getType());
        role.setRemark("导出测试");
        role.setCategoryId(insertCategory("scheduling", "排产"));
        role.setDataScope(DataScopeEnum.ALL.getScope());
        roleMapper.insert(role);

        reset(permissionService);
        org.mockito.Mockito.when(permissionService.getRoleMenuListByRoleId(role.getId())).thenReturn(Set.of(menu.getId()));

        byte[] data = roleConfigPackageService.exportPackage();
        RoleConfigPackageServiceImpl.RoleConfigPackage payload = JsonUtils.parseObject(data, RoleConfigPackageServiceImpl.RoleConfigPackage.class);
        RoleConfigPackageServiceImpl.RoleConfigItem item = payload.getRoles().stream()
                .filter(current -> "scheduler".equals(current.getCode()))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of("permission:system:role:query"), item.getMenuKeys());
        assertEquals("scheduling", item.getCategoryCode());
        assertEquals("3", payload.getPackageVersion());
        assertEquals("scheduling", payload.getCategories().get(0).getCode());
    }

    @Test
    void importPackage_shouldFailFastWhenMenuKeyMissingInTargetEnv() {
        insertCategory("menu", "菜单");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("scheduler");
        item.setName("排产员");
        item.setSort(8);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setType(RoleTypeEnum.CUSTOM.getType());
        item.setRemark("跨环境菜单缺失");
        item.setCategoryCode("menu");
        item.setDataScope(DataScopeEnum.ALL.getScope());
        item.setMenuKeys(List.of("permission:system:role:query"));

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("menu", "菜单")));
        payload.setRoles(List.of(item));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload)));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), exception.getCode());
        assertEquals("配置包引用缺失，原因：角色【scheduler】引用的菜单标识【permission:system:role:query】在目标环境不存在", exception.getMessage());
    }

    @Test
    void importPackage_shouldResolveMenuByStableKeyInsteadOfSourceMenuId() {
        MenuDO targetMenu = new MenuDO();
        targetMenu.setName("角色查询");
        targetMenu.setPermission("system:role:query");
        targetMenu.setType(MenuTypeEnum.BUTTON.getType());
        targetMenu.setSort(1);
        targetMenu.setParentId(0L);
        targetMenu.setPath("");
        targetMenu.setComponent("");
        targetMenu.setComponentName("");
        targetMenu.setStatus(CommonStatusEnum.ENABLE.getStatus());
        targetMenu.setVisible(Boolean.TRUE);
        targetMenu.setKeepAlive(Boolean.FALSE);
        targetMenu.setAlwaysShow(Boolean.FALSE);
        menuMapper.insert(targetMenu);

        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("scheduler");
        item.setName("排产员");
        item.setSort(3);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setType(RoleTypeEnum.CUSTOM.getType());
        item.setRemark("业务键映射");
        item.setCategoryCode("menu");
        item.setDataScope(DataScopeEnum.ALL.getScope());
        item.setMenuKeys(List.of("permission:system:role:query"));
        Long categoryId = insertCategory("menu", "菜单");

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("menu", "菜单")));
        payload.setRoles(List.of(item));

        roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload));

        RoleDO dbRole = roleMapper.selectByCode("scheduler");
        assertEquals(categoryId, dbRole.getCategoryId());
        verify(permissionService).assignRoleMenu(dbRole.getId(), Set.of(targetMenu.getId()));
    }

    @Test
    void importPackage_shouldFailFastWhenCategoryMissingInTargetEnv() {
        insertCategory("menu", "菜单");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("scheduler");
        item.setName("排产员");
        item.setSort(8);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setType(RoleTypeEnum.CUSTOM.getType());
        item.setCategoryCode("missing-category");
        item.setDataScope(DataScopeEnum.ALL.getScope());
        item.setMenuKeys(List.of());

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("menu", "菜单")));
        payload.setRoles(List.of(item));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload)));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), exception.getCode());
        assertEquals("配置包引用缺失，原因：角色【scheduler】引用的分类标识【missing-category】在目标环境不存在", exception.getMessage());
    }

    @Test
    void importPackage_shouldFailFastWhenPackageCategoryMissingInTargetEnv() {
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("scheduler");
        item.setName("排产员");
        item.setSort(8);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        item.setType(RoleTypeEnum.CUSTOM.getType());
        item.setCategoryCode("scheduling");
        item.setDataScope(DataScopeEnum.ALL.getScope());
        item.setMenuKeys(List.of());

        RoleConfigPackageServiceImpl.RoleConfigPackage payload = new RoleConfigPackageServiceImpl.RoleConfigPackage();
        payload.setPackageVersion("3");
        payload.setCategories(List.of(categoryItem("scheduling", "排产")));
        payload.setRoles(List.of(item));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> roleConfigPackageService.importPackage(JsonUtils.toJsonByte(payload)));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), exception.getCode());
        assertEquals("配置包引用缺失，原因：分类标识【scheduling】在目标环境不存在", exception.getMessage());
    }

    private Long insertCategory(String code, String name) {
        RoleCategoryDO category = new RoleCategoryDO();
        category.setCode(code);
        category.setName(name);
        category.setSort(1);
        category.setStatus(CommonStatusEnum.ENABLE.getStatus());
        roleCategoryMapper.insert(category);
        return category.getId();
    }

    private RoleConfigPackageServiceImpl.RoleCategoryConfigItem categoryItem(String code, String name) {
        RoleConfigPackageServiceImpl.RoleCategoryConfigItem item = new RoleConfigPackageServiceImpl.RoleCategoryConfigItem();
        item.setCode(code);
        item.setName(name);
        item.setSort(1);
        item.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return item;
    }
}
