package cn.iocoder.yudao.module.system.service.configpackage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackageImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.configpackage.vo.SystemConfigPackagePrecheckRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictDataDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictTypeDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dict.DictDataMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dict.DictTypeMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantPackageMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONFIRM_REQUIRED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FILE_EMPTY;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_MANIFEST_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_PASSWORD_POLICY_MISMATCH;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_SHEET_MISSING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_SNAPSHOT_MISMATCH;

@Service
public class SystemConfigPackageServiceImpl implements SystemConfigPackageService {

    private static final String FORMAT_VERSION = "1";
    private static final String PASSWORD_POLICY_PRESERVE = "preserve_existing_passwords";
    private static final String PASSWORD_POLICY_RESTORE = "restore_password_hashes";
    private static final String SHEET_MANIFEST = "manifest";
    private static final String SHEET_MENUS = "menus";
    private static final String SHEET_ROLES = "roles";
    private static final String SHEET_ROLE_MENU = "role_menu";
    private static final String SHEET_USERS = "users";
    private static final String SHEET_USER_ROLE = "user_role";
    private static final String SHEET_DEPT = "dept";
    private static final String SHEET_POST = "post";
    private static final String SHEET_USER_POST = "user_post";
    private static final String SHEET_DICT_TYPE = "dict_type";
    private static final String SHEET_DICT_DATA = "dict_data";
    private static final String SHEET_TENANT_PACKAGE = "tenant_package";
    private static final String HEADER_PASSWORD_HASH = "密码摘要";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<String> REQUIRED_SHEETS = List.of(
            SHEET_MANIFEST, SHEET_MENUS, SHEET_ROLES, SHEET_ROLE_MENU, SHEET_USERS, SHEET_USER_ROLE,
            SHEET_DEPT, SHEET_POST, SHEET_USER_POST, SHEET_DICT_TYPE, SHEET_DICT_DATA, SHEET_TENANT_PACKAGE);
    private static final Map<String, List<String>> BASE_HEADERS = Map.ofEntries(
            Map.entry(SHEET_MENUS, List.of("菜单ID", "菜单名称", "权限标识", "菜单类型", "显示顺序", "父菜单ID", "路由地址",
                    "菜单图标", "组件路径", "组件名", "状态", "是否可见", "是否缓存", "是否总是显示", "创建者",
                    "创建时间", "更新者", "更新时间", "是否删除")),
            Map.entry(SHEET_ROLES, List.of("角色ID", "角色名称", "角色标识", "角色排序", "角色状态", "角色类型",
                    "备注", "数据范围", "数据范围部门ID", "租户ID", "创建者", "创建时间", "更新者", "更新时间", "是否删除")),
            Map.entry(SHEET_ROLE_MENU, List.of("ID", "角色ID", "菜单ID", "租户ID", "创建者", "创建时间", "更新者",
                    "更新时间", "是否删除")),
            Map.entry(SHEET_USER_ROLE, List.of("ID", "用户ID", "角色ID", "租户ID", "创建者", "创建时间", "更新者",
                    "更新时间", "是否删除")),
            Map.entry(SHEET_DEPT, List.of("部门ID", "部门名称", "父部门ID", "显示顺序", "负责人用户ID", "联系电话",
                    "邮箱", "状态", "租户ID", "创建者", "创建时间", "更新者", "更新时间", "是否删除")),
            Map.entry(SHEET_POST, List.of("岗位ID", "岗位名称", "岗位编码", "岗位排序", "状态", "备注", "租户ID",
                    "创建者", "创建时间", "更新者", "更新时间", "是否删除")),
            Map.entry(SHEET_USER_POST, List.of("ID", "用户ID", "岗位ID", "租户ID", "创建者", "创建时间", "更新者",
                    "更新时间", "是否删除")),
            Map.entry(SHEET_DICT_TYPE, List.of("字典ID", "字典名称", "字典类型", "状态", "备注", "删除时间",
                    "创建者", "创建时间", "更新者", "更新时间", "是否删除")),
            Map.entry(SHEET_DICT_DATA, List.of("字典数据ID", "字典排序", "字典标签", "字典值", "字典类型", "状态",
                    "颜色类型", "CSS 样式", "备注", "创建者", "创建时间", "更新者", "更新时间", "是否删除")),
            Map.entry(SHEET_TENANT_PACKAGE, List.of("套餐ID", "套餐名称", "状态", "备注", "菜单ID集合", "创建者",
                    "创建时间", "更新者", "更新时间", "是否删除"))
    );

    @Resource
    private MenuMapper menuMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private TenantPackageMapper tenantPackageMapper;
    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initJdbcTemplate() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public byte[] exportPackage() {
        Snapshot snapshot = loadCurrentSnapshot(false);
        String packageSha256 = hashSnapshot(snapshot.sheets(), false);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeManifest(workbook, PASSWORD_POLICY_PRESERVE, packageSha256);
            for (String sheetName : dataSheetNames()) {
                writeSheet(workbook, sheetName, headers(sheetName, false), snapshot.sheets().get(sheetName));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("CONFIG_PACKAGE_EXPORT_FAILED", exception);
        }
    }

    @Override
    public SystemConfigPackagePrecheckRespVO precheck(byte[] content, Collection<String> availableComponents) {
        requireContent(content);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        PackageWorkbook pkg = readPackage(content, errors);
        String passwordPolicy = pkg.passwordPolicy();
        boolean packageIncludesPasswordHash = PASSWORD_POLICY_RESTORE.equals(passwordPolicy);
        if (!PASSWORD_POLICY_PRESERVE.equals(passwordPolicy)) {
            errors.add("passwordPolicy 只能是 " + PASSWORD_POLICY_PRESERVE + "；禁止导入用户密码摘要");
        }
        Long sourceTenantId = validateSourceTenant(pkg, errors);
        if (!errors.isEmpty()) {
            String targetHash = hashSnapshot(loadCurrentSnapshot(true).sheets(), true);
            return new SystemConfigPackagePrecheckRespVO()
                    .setValid(false)
                    .setPackageSha256(pkg.packageSha256())
                    .setTargetSnapshotSha256(targetHash)
                    .setBlockingErrors(errors)
                    .setWarnings(warnings)
                    .setSheetDiffs(List.of());
        }

        validatePackageHash(pkg, packageIncludesPasswordHash, errors);
        validateFrontendComponents(pkg, availableComponents, errors);
        validateReferences(pkg, errors);
        validatePasswordPolicy(pkg, packageIncludesPasswordHash, sourceTenantId, errors);

        Snapshot current = loadCurrentSnapshot(true);
        String targetHash = hashSnapshot(current.sheets(), true);
        List<SystemConfigPackagePrecheckRespVO.SheetDiff> diffs = buildDiffs(pkg.sheets(), current.sheets());
        return new SystemConfigPackagePrecheckRespVO()
                .setValid(errors.isEmpty())
                .setPackageSha256(pkg.packageSha256())
                .setTargetSnapshotSha256(targetHash)
                .setBlockingErrors(errors)
                .setWarnings(warnings)
                .setSheetDiffs(diffs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemConfigPackageImportRespVO importPackage(byte[] content, Boolean confirmed,
                                                         String targetSnapshotSha256,
                                                         Collection<String> availableComponents) {
        if (!Boolean.TRUE.equals(confirmed)) {
            throw exception(CONFIG_PACKAGE_CONFIRM_REQUIRED);
        }
        SystemConfigPackagePrecheckRespVO precheck = precheck(content, availableComponents);
        if (!Boolean.TRUE.equals(precheck.getValid())) {
            throw exception(CONFIG_PACKAGE_REFERENCE_MISSING, String.join("；", precheck.getBlockingErrors()));
        }
        String currentSnapshotSha256 = hashSnapshot(loadCurrentSnapshot(true).sheets(), true);
        if (!StrUtil.equals(currentSnapshotSha256, targetSnapshotSha256)
                || !StrUtil.equals(currentSnapshotSha256, precheck.getTargetSnapshotSha256())) {
            throw exception(CONFIG_PACKAGE_SNAPSHOT_MISMATCH);
        }

        List<String> errors = new ArrayList<>();
        PackageWorkbook pkg = readPackage(content, errors);
        if (CollUtil.isNotEmpty(errors)) {
            throw exception(CONFIG_PACKAGE_REFERENCE_MISSING, String.join("；", errors));
        }
        boolean restorePasswordHash = PASSWORD_POLICY_RESTORE.equals(pkg.passwordPolicy());
        Long sourceTenantId = parseLong(pkg.sourceTenantId());
        Long targetTenantId = TenantContextHolder.getRequiredTenantId();
        if (Objects.equals(sourceTenantId, targetTenantId)) {
            Map<Long, AdminUserDO> currentUsers = userMapper.selectList().stream()
                    .collect(Collectors.toMap(AdminUserDO::getId, Function.identity(), (first, second) -> first));
            replaceAllConfig(pkg, restorePasswordHash, currentUsers);
        } else {
            replaceCrossTenantConfig(pkg, restorePasswordHash);
        }
        Map<String, Integer> counts = dataSheetNames().stream().collect(Collectors.toMap(
                Function.identity(),
                sheet -> pkg.sheets().getOrDefault(sheet, List.of()).size(),
                (first, second) -> first,
                LinkedHashMap::new));
        return new SystemConfigPackageImportRespVO()
                .setRestored(true)
                .setTargetSnapshotSha256(currentSnapshotSha256)
                .setRestoredCounts(counts);
    }

    private Snapshot loadCurrentSnapshot(boolean includePasswordHash) {
        Map<String, List<LinkedHashMap<String, String>>> sheets = new LinkedHashMap<>();
        sheets.put(SHEET_MENUS, menuMapper.selectList().stream()
                .sorted(Comparator.comparing(MenuDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::menuRow).toList());
        sheets.put(SHEET_ROLES, roleMapper.selectList().stream()
                .sorted(Comparator.comparing(RoleDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::roleRow).toList());
        sheets.put(SHEET_ROLE_MENU, roleMenuMapper.selectList().stream()
                .sorted(Comparator.comparing(RoleMenuDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::roleMenuRow).toList());
        sheets.put(SHEET_USERS, userMapper.selectList().stream()
                .sorted(Comparator.comparing(AdminUserDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(user -> userRow(user, includePasswordHash)).toList());
        sheets.put(SHEET_USER_ROLE, selectUserRoleRows());
        sheets.put(SHEET_DEPT, deptMapper.selectList().stream()
                .sorted(Comparator.comparing(DeptDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::deptRow).toList());
        sheets.put(SHEET_POST, selectPostRows());
        sheets.put(SHEET_USER_POST, selectUserPostRows());
        sheets.put(SHEET_DICT_TYPE, dictTypeMapper.selectList().stream()
                .sorted(Comparator.comparing(DictTypeDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::dictTypeRow).toList());
        sheets.put(SHEET_DICT_DATA, dictDataMapper.selectList().stream()
                .sorted(Comparator.comparing(DictDataDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::dictDataRow).toList());
        sheets.put(SHEET_TENANT_PACKAGE, tenantPackageMapper.selectList().stream()
                .sorted(Comparator.comparing(TenantPackageDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::tenantPackageRow).toList());
        return new Snapshot(sheets);
    }

    private List<LinkedHashMap<String, String>> selectPostRows() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return jdbcTemplate.query("""
                SELECT id, name, code, sort, status, remark, tenant_id, creator, create_time, updater, update_time, deleted
                FROM system_post
                WHERE tenant_id = ?
                  AND (
                    deleted = FALSE
                    OR EXISTS (
                        SELECT 1
                        FROM system_user_post up
                        WHERE up.tenant_id = system_post.tenant_id
                          AND up.post_id = system_post.id
                          AND up.deleted = FALSE
                    )
                  )
                ORDER BY id
                """, (rs, rowNum) -> row(
                "岗位ID", rs.getLong("id"),
                "岗位名称", rs.getString("name"),
                "岗位编码", rs.getString("code"),
                "岗位排序", rs.getObject("sort"),
                "状态", rs.getObject("status"),
                "备注", rs.getString("remark"),
                "租户ID", rs.getObject("tenant_id"),
                "创建者", rs.getString("creator"),
                "创建时间", rs.getTimestamp("create_time"),
                "更新者", rs.getString("updater"),
                "更新时间", rs.getTimestamp("update_time"),
                "是否删除", rs.getObject("deleted")
        ), tenantId);
    }

    private List<LinkedHashMap<String, String>> selectUserRoleRows() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return jdbcTemplate.query("""
                SELECT id, user_id, role_id, tenant_id, creator, create_time, updater, update_time, deleted
                FROM system_user_role
                WHERE tenant_id = ? AND (deleted = FALSE OR deleted IS NULL)
                ORDER BY id
                """, (rs, rowNum) -> relationRow(rs, "角色ID", "role_id"), tenantId);
    }

    private List<LinkedHashMap<String, String>> selectUserPostRows() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return jdbcTemplate.query("""
                SELECT id, user_id, post_id, tenant_id, creator, create_time, updater, update_time, deleted
                FROM system_user_post
                WHERE tenant_id = ? AND deleted = FALSE
                ORDER BY id
                """, (rs, rowNum) -> relationRow(rs, "岗位ID", "post_id"), tenantId);
    }

    private LinkedHashMap<String, String> relationRow(ResultSet rs, String targetHeader, String targetColumn) throws java.sql.SQLException {
        return row(
                "ID", rs.getLong("id"),
                "用户ID", rs.getLong("user_id"),
                targetHeader, rs.getLong(targetColumn),
                "租户ID", rs.getObject("tenant_id"),
                "创建者", rs.getString("creator"),
                "创建时间", rs.getTimestamp("create_time"),
                "更新者", rs.getString("updater"),
                "更新时间", rs.getTimestamp("update_time"),
                "是否删除", rs.getObject("deleted")
        );
    }

    private LinkedHashMap<String, String> menuRow(MenuDO menu) {
        return row("菜单ID", menu.getId(), "菜单名称", menu.getName(), "权限标识", menu.getPermission(),
                "菜单类型", menu.getType(), "显示顺序", menu.getSort(), "父菜单ID", menu.getParentId(),
                "路由地址", menu.getPath(), "菜单图标", menu.getIcon(), "组件路径", menu.getComponent(),
                "组件名", menu.getComponentName(), "状态", menu.getStatus(), "是否可见", menu.getVisible(),
                "是否缓存", menu.getKeepAlive(), "是否总是显示", menu.getAlwaysShow(), "创建者", menu.getCreator(),
                "创建时间", menu.getCreateTime(), "更新者", menu.getUpdater(), "更新时间", menu.getUpdateTime(),
                "是否删除", menu.getDeleted());
    }

    private LinkedHashMap<String, String> roleRow(RoleDO role) {
        return row("角色ID", role.getId(), "角色名称", role.getName(), "角色标识", role.getCode(),
                "角色排序", role.getSort(), "角色状态", role.getStatus(), "角色类型", role.getType(),
                "备注", role.getRemark(), "数据范围", role.getDataScope(),
                "数据范围部门ID", joinLongs(role.getDataScopeDeptIds()), "租户ID", role.getTenantId(),
                "创建者", role.getCreator(), "创建时间", role.getCreateTime(), "更新者", role.getUpdater(),
                "更新时间", role.getUpdateTime(), "是否删除", role.getDeleted());
    }

    private LinkedHashMap<String, String> roleMenuRow(RoleMenuDO roleMenu) {
        return row("ID", roleMenu.getId(), "角色ID", roleMenu.getRoleId(), "菜单ID", roleMenu.getMenuId(),
                "租户ID", roleMenu.getTenantId(), "创建者", roleMenu.getCreator(),
                "创建时间", roleMenu.getCreateTime(), "更新者", roleMenu.getUpdater(),
                "更新时间", roleMenu.getUpdateTime(), "是否删除", roleMenu.getDeleted());
    }

    private LinkedHashMap<String, String> userRow(AdminUserDO user, boolean includePasswordHash) {
        LinkedHashMap<String, String> row = row("用户ID", user.getId(), "用户账号", user.getUsername());
        if (includePasswordHash) {
            row.put(HEADER_PASSWORD_HASH, value(user.getPassword()));
        }
        row.putAll(row("密码更新时间", user.getPasswordUpdateTime(), "用户昵称", user.getNickname(),
                "备注", user.getRemark(), "部门ID", user.getDeptId(), "岗位ID集合", joinLongs(user.getPostIds()),
                "用户邮箱", user.getEmail(), "手机号码", user.getMobile(), "用户性别", user.getSex(),
                "用户头像", user.getAvatar(), "状态", user.getStatus(), "最后登录IP", user.getLoginIp(),
                "最后登录时间", user.getLoginDate(), "租户ID", user.getTenantId(), "创建者", user.getCreator(),
                "创建时间", user.getCreateTime(), "更新者", user.getUpdater(), "更新时间", user.getUpdateTime(),
                "是否删除", user.getDeleted()));
        return row;
    }

    private LinkedHashMap<String, String> deptRow(DeptDO dept) {
        return row("部门ID", dept.getId(), "部门名称", dept.getName(), "父部门ID", dept.getParentId(),
                "显示顺序", dept.getSort(), "负责人用户ID", dept.getLeaderUserId(), "联系电话", dept.getPhone(),
                "邮箱", dept.getEmail(), "状态", dept.getStatus(), "租户ID", dept.getTenantId(),
                "创建者", dept.getCreator(), "创建时间", dept.getCreateTime(), "更新者", dept.getUpdater(),
                "更新时间", dept.getUpdateTime(), "是否删除", dept.getDeleted());
    }

    private LinkedHashMap<String, String> dictTypeRow(DictTypeDO dictType) {
        return row("字典ID", dictType.getId(), "字典名称", dictType.getName(), "字典类型", dictType.getType(),
                "状态", dictType.getStatus(), "备注", dictType.getRemark(), "删除时间", dictType.getDeletedTime(),
                "创建者", dictType.getCreator(), "创建时间", dictType.getCreateTime(), "更新者", dictType.getUpdater(),
                "更新时间", dictType.getUpdateTime(), "是否删除", dictType.getDeleted());
    }

    private LinkedHashMap<String, String> dictDataRow(DictDataDO dictData) {
        return row("字典数据ID", dictData.getId(), "字典排序", dictData.getSort(), "字典标签", dictData.getLabel(),
                "字典值", dictData.getValue(), "字典类型", dictData.getDictType(), "状态", dictData.getStatus(),
                "颜色类型", dictData.getColorType(), "CSS 样式", dictData.getCssClass(), "备注", dictData.getRemark(),
                "创建者", dictData.getCreator(), "创建时间", dictData.getCreateTime(), "更新者", dictData.getUpdater(),
                "更新时间", dictData.getUpdateTime(), "是否删除", dictData.getDeleted());
    }

    private LinkedHashMap<String, String> tenantPackageRow(TenantPackageDO tenantPackage) {
        return row("套餐ID", tenantPackage.getId(), "套餐名称", tenantPackage.getName(), "状态", tenantPackage.getStatus(),
                "备注", tenantPackage.getRemark(), "菜单ID集合", joinLongs(tenantPackage.getMenuIds()),
                "创建者", tenantPackage.getCreator(), "创建时间", tenantPackage.getCreateTime(),
                "更新者", tenantPackage.getUpdater(), "更新时间", tenantPackage.getUpdateTime(),
                "是否删除", tenantPackage.getDeleted());
    }

    private void replaceAllConfig(PackageWorkbook pkg, boolean restorePasswordHash,
                                  Map<Long, AdminUserDO> currentUsers) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        jdbcTemplate.update("DELETE FROM system_role_menu WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_user_role WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_user_post WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_users WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_role WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_dept WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_post WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_tenant_package");
        jdbcTemplate.update("DELETE FROM system_dict_data");
        jdbcTemplate.update("DELETE FROM system_dict_type");
        jdbcTemplate.update("DELETE FROM system_menu");

        pkg.rows(SHEET_MENUS).forEach(row -> menuMapper.insert(toMenu(row)));
        pkg.rows(SHEET_DICT_TYPE).forEach(row -> dictTypeMapper.insert(toDictType(row)));
        pkg.rows(SHEET_DICT_DATA).forEach(row -> dictDataMapper.insert(toDictData(row)));
        pkg.rows(SHEET_TENANT_PACKAGE).forEach(row -> tenantPackageMapper.insert(toTenantPackage(row)));
        pkg.rows(SHEET_DEPT).forEach(row -> deptMapper.insert(toDept(row, tenantId)));
        pkg.rows(SHEET_POST).forEach(row -> insertPost(row, tenantId));
        pkg.rows(SHEET_USERS).forEach(row -> userMapper.insert(toUser(row, tenantId, restorePasswordHash, currentUsers)));
        pkg.rows(SHEET_ROLES).forEach(row -> roleMapper.insert(toRole(row, tenantId)));
        pkg.rows(SHEET_ROLE_MENU).forEach(row -> insertRoleMenu(row, tenantId));
        pkg.rows(SHEET_USER_ROLE).forEach(row -> insertUserRole(row, tenantId));
        pkg.rows(SHEET_USER_POST).forEach(row -> insertUserPost(row, tenantId));
    }

    private void replaceCrossTenantConfig(PackageWorkbook pkg, boolean restorePasswordHash) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Map<String, AdminUserDO> currentUsersByUsername = userMapper.selectList().stream()
                .filter(user -> Objects.equals(user.getTenantId(), tenantId))
                .collect(Collectors.toMap(AdminUserDO::getUsername, Function.identity(), (first, second) -> first));

        jdbcTemplate.update("DELETE FROM system_role_menu WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_user_role WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_user_post WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_users WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_role WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_dept WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM system_post WHERE tenant_id = ?", tenantId);

        Map<Long, Long> deptIdMap = insertCrossTenantDepts(pkg.rows(SHEET_DEPT), tenantId);
        Map<Long, Long> postIdMap = insertCrossTenantPosts(pkg.rows(SHEET_POST));
        Map<Long, Long> roleIdMap = insertCrossTenantRoles(pkg.rows(SHEET_ROLES), tenantId, deptIdMap);
        Map<Long, Long> userIdMap = insertCrossTenantUsers(pkg.rows(SHEET_USERS), tenantId, restorePasswordHash,
                currentUsersByUsername, deptIdMap, postIdMap);
        updateCrossTenantDeptLeaders(pkg.rows(SHEET_DEPT), deptIdMap, userIdMap);
        insertCrossTenantRoleMenus(pkg.rows(SHEET_ROLE_MENU), tenantId, roleIdMap);
        insertCrossTenantUserRoles(pkg.rows(SHEET_USER_ROLE), roleIdMap, userIdMap);
        insertCrossTenantUserPosts(pkg.rows(SHEET_USER_POST), userIdMap, postIdMap);
    }

    private MenuDO toMenu(Map<String, String> row) {
        MenuDO menu = new MenuDO();
        menu.setId(parseLong(row.get("菜单ID")));
        menu.setName(row.get("菜单名称"));
        menu.setPermission(row.get("权限标识"));
        menu.setType(parseInteger(row.get("菜单类型")));
        menu.setSort(parseInteger(row.get("显示顺序")));
        menu.setParentId(parseLong(row.get("父菜单ID")));
        menu.setPath(row.get("路由地址"));
        menu.setIcon(row.get("菜单图标"));
        menu.setComponent(row.get("组件路径"));
        menu.setComponentName(row.get("组件名"));
        menu.setStatus(parseInteger(row.get("状态")));
        menu.setVisible(parseBoolean(row.get("是否可见")));
        menu.setKeepAlive(parseBoolean(row.get("是否缓存")));
        menu.setAlwaysShow(parseBoolean(row.get("是否总是显示")));
        fillBase(menu, row);
        return menu;
    }

    private RoleDO toRole(Map<String, String> row, Long tenantId) {
        RoleDO role = new RoleDO();
        role.setId(parseLong(row.get("角色ID")));
        role.setName(row.get("角色名称"));
        role.setCode(row.get("角色标识"));
        role.setSort(parseInteger(row.get("角色排序")));
        role.setStatus(parseInteger(row.get("角色状态")));
        role.setType(parseInteger(row.get("角色类型")));
        role.setRemark(row.get("备注"));
        role.setDataScope(parseInteger(row.get("数据范围")));
        role.setDataScopeDeptIds(parseLongSet(row.get("数据范围部门ID")));
        role.setTenantId(tenantId);
        fillBase(role, row);
        return role;
    }

    private AdminUserDO toUser(Map<String, String> row, Long tenantId, boolean restorePasswordHash,
                               Map<Long, AdminUserDO> currentUsers) {
        AdminUserDO user = new AdminUserDO();
        Long userId = parseLong(row.get("用户ID"));
        user.setId(userId);
        user.setUsername(row.get("用户账号"));
        if (restorePasswordHash) {
            user.setPassword(row.get(HEADER_PASSWORD_HASH));
            user.setPasswordUpdateTime(parseDateTime(row.get("密码更新时间")));
        } else {
            AdminUserDO currentUser = currentUsers.get(userId);
            if (currentUser == null) {
                user.setPassword("");
                user.setPasswordUpdateTime(null);
            } else {
                user.setPassword(currentUser.getPassword());
                user.setPasswordUpdateTime(currentUser.getPasswordUpdateTime());
            }
        }
        user.setNickname(row.get("用户昵称"));
        user.setRemark(row.get("备注"));
        user.setDeptId(parseLong(row.get("部门ID")));
        user.setPostIds(parseLongSet(row.get("岗位ID集合")));
        user.setEmail(row.get("用户邮箱"));
        user.setMobile(row.get("手机号码"));
        user.setSex(parseInteger(row.get("用户性别")));
        user.setAvatar(row.get("用户头像"));
        user.setStatus(parseInteger(row.get("状态")));
        user.setLoginIp(row.get("最后登录IP"));
        user.setLoginDate(parseDateTime(row.get("最后登录时间")));
        user.setTenantId(tenantId);
        fillBase(user, row);
        return user;
    }

    private DeptDO toDept(Map<String, String> row, Long tenantId) {
        DeptDO dept = new DeptDO();
        dept.setId(parseLong(row.get("部门ID")));
        dept.setName(row.get("部门名称"));
        dept.setParentId(parseLong(row.get("父部门ID")));
        dept.setSort(parseInteger(row.get("显示顺序")));
        dept.setLeaderUserId(parseLong(row.get("负责人用户ID")));
        dept.setPhone(row.get("联系电话"));
        dept.setEmail(row.get("邮箱"));
        dept.setStatus(parseInteger(row.get("状态")));
        dept.setTenantId(tenantId);
        fillBase(dept, row);
        return dept;
    }

    private DictTypeDO toDictType(Map<String, String> row) {
        DictTypeDO dictType = new DictTypeDO();
        dictType.setId(parseLong(row.get("字典ID")));
        dictType.setName(row.get("字典名称"));
        dictType.setType(row.get("字典类型"));
        dictType.setStatus(parseInteger(row.get("状态")));
        dictType.setRemark(row.get("备注"));
        dictType.setDeletedTime(parseDateTime(row.get("删除时间")));
        fillBase(dictType, row);
        return dictType;
    }

    private DictDataDO toDictData(Map<String, String> row) {
        DictDataDO dictData = new DictDataDO();
        dictData.setId(parseLong(row.get("字典数据ID")));
        dictData.setSort(parseInteger(row.get("字典排序")));
        dictData.setLabel(row.get("字典标签"));
        dictData.setValue(row.get("字典值"));
        dictData.setDictType(row.get("字典类型"));
        dictData.setStatus(parseInteger(row.get("状态")));
        dictData.setColorType(row.get("颜色类型"));
        dictData.setCssClass(row.get("CSS 样式"));
        dictData.setRemark(row.get("备注"));
        fillBase(dictData, row);
        return dictData;
    }

    private TenantPackageDO toTenantPackage(Map<String, String> row) {
        TenantPackageDO tenantPackage = new TenantPackageDO();
        tenantPackage.setId(parseLong(row.get("套餐ID")));
        tenantPackage.setName(row.get("套餐名称"));
        tenantPackage.setStatus(parseInteger(row.get("状态")));
        tenantPackage.setRemark(row.get("备注"));
        tenantPackage.setMenuIds(parseLongSet(row.get("菜单ID集合")));
        fillBase(tenantPackage, row);
        return tenantPackage;
    }

    private void insertPost(Map<String, String> row, Long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO system_post
                (id, name, code, sort, status, remark, tenant_id, creator, create_time, updater, update_time, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, parseLong(row.get("岗位ID")), row.get("岗位名称"), row.get("岗位编码"),
                parseInteger(row.get("岗位排序")), parseInteger(row.get("状态")), row.get("备注"), tenantId,
                row.get("创建者"), parseDateTime(row.get("创建时间")), row.get("更新者"),
                parseDateTime(row.get("更新时间")), parseBoolean(row.get("是否删除")));
    }

    private void insertRoleMenu(Map<String, String> row, Long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO system_role_menu
                (id, role_id, menu_id, tenant_id, creator, create_time, updater, update_time, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, parseLong(row.get("ID")), parseLong(row.get("角色ID")), parseLong(row.get("菜单ID")),
                tenantId, row.get("创建者"), parseDateTime(row.get("创建时间")), row.get("更新者"),
                parseDateTime(row.get("更新时间")), parseBoolean(row.get("是否删除")));
    }

    private void insertUserRole(Map<String, String> row, Long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO system_user_role
                (id, user_id, role_id, tenant_id, creator, create_time, updater, update_time, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, parseLong(row.get("ID")), parseLong(row.get("用户ID")), parseLong(row.get("角色ID")),
                tenantId, row.get("创建者"), parseDateTime(row.get("创建时间")), row.get("更新者"),
                parseDateTime(row.get("更新时间")), parseBoolean(row.get("是否删除")));
    }

    private void insertUserPost(Map<String, String> row, Long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO system_user_post
                (id, user_id, post_id, tenant_id, creator, create_time, updater, update_time, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, parseLong(row.get("ID")), parseLong(row.get("用户ID")), parseLong(row.get("岗位ID")),
                tenantId, row.get("创建者"), parseDateTime(row.get("创建时间")), row.get("更新者"),
                parseDateTime(row.get("更新时间")), parseBoolean(row.get("是否删除")));
    }

    private Map<Long, Long> insertCrossTenantDepts(List<LinkedHashMap<String, String>> rows, Long tenantId) {
        Map<Long, Long> deptIdMap = new LinkedHashMap<>();
        Set<Long> pending = rows.stream()
                .map(row -> parseLong(row.get("部门ID")))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        while (!pending.isEmpty()) {
            boolean progressed = false;
            for (Map<String, String> row : rows) {
                Long sourceDeptId = parseLong(row.get("部门ID"));
                if (sourceDeptId == null || !pending.contains(sourceDeptId)) {
                    continue;
                }
                Long sourceParentId = parseLong(row.get("父部门ID"));
                if (!Objects.equals(sourceParentId, DeptDO.PARENT_ID_ROOT)
                        && !deptIdMap.containsKey(sourceParentId)) {
                    continue;
                }
                DeptDO dept = toDept(row, tenantId);
                dept.setId(null);
                dept.setParentId(Objects.equals(sourceParentId, DeptDO.PARENT_ID_ROOT)
                        ? DeptDO.PARENT_ID_ROOT : deptIdMap.get(sourceParentId));
                dept.setLeaderUserId(null);
                deptMapper.insert(dept);
                deptIdMap.put(sourceDeptId, dept.getId());
                pending.remove(sourceDeptId);
                progressed = true;
            }
            if (!progressed) {
                throw new IllegalArgumentException("Cross-tenant dept import cannot resolve parent chain: " + pending);
            }
        }
        return deptIdMap;
    }

    private Map<Long, Long> insertCrossTenantPosts(List<LinkedHashMap<String, String>> rows) {
        Map<Long, Long> postIdMap = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            PostDO post = new PostDO();
            post.setId(null);
            post.setName(row.get("岗位名称"));
            post.setCode(row.get("岗位编码"));
            post.setSort(parseInteger(row.get("岗位排序")));
            post.setStatus(parseInteger(row.get("状态")));
            post.setRemark(row.get("备注"));
            fillBase(post, row);
            postMapper.insert(post);
            postIdMap.put(parseLong(row.get("岗位ID")), post.getId());
        }
        return postIdMap;
    }

    private Map<Long, Long> insertCrossTenantRoles(List<LinkedHashMap<String, String>> rows, Long tenantId,
                                                   Map<Long, Long> deptIdMap) {
        Map<Long, Long> roleIdMap = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            RoleDO role = toRole(row, tenantId);
            role.setId(null);
            role.setDataScopeDeptIds(parseLongSet(row.get("数据范围部门ID")).stream()
                    .map(deptIdMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
            roleMapper.insert(role);
            roleIdMap.put(parseLong(row.get("角色ID")), role.getId());
        }
        return roleIdMap;
    }

    private Map<Long, Long> insertCrossTenantUsers(List<LinkedHashMap<String, String>> rows, Long tenantId,
                                                   boolean restorePasswordHash,
                                                   Map<String, AdminUserDO> currentUsersByUsername,
                                                   Map<Long, Long> deptIdMap, Map<Long, Long> postIdMap) {
        Map<Long, Long> userIdMap = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            AdminUserDO currentUser = currentUsersByUsername.get(row.get("用户账号"));
            AdminUserDO user = toUser(row, tenantId, restorePasswordHash, Map.of());
            user.setId(null);
            user.setDeptId(mapNullableId(parseLong(row.get("部门ID")), deptIdMap));
            user.setPostIds(parseLongSet(row.get("岗位ID集合")).stream()
                    .map(postIdMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
            if (!restorePasswordHash) {
                if (currentUser != null) {
                    user.setPassword(currentUser.getPassword());
                    user.setPasswordUpdateTime(currentUser.getPasswordUpdateTime());
                } else {
                    user.setPassword("");
                    user.setPasswordUpdateTime(null);
                }
            }
            userMapper.insert(user);
            userIdMap.put(parseLong(row.get("用户ID")), user.getId());
        }
        return userIdMap;
    }

    private void updateCrossTenantDeptLeaders(List<LinkedHashMap<String, String>> rows, Map<Long, Long> deptIdMap,
                                              Map<Long, Long> userIdMap) {
        for (Map<String, String> row : rows) {
            Long sourceDeptId = parseLong(row.get("部门ID"));
            Long targetDeptId = deptIdMap.get(sourceDeptId);
            if (targetDeptId == null) {
                continue;
            }
            DeptDO updateObj = new DeptDO();
            updateObj.setId(targetDeptId);
            updateObj.setLeaderUserId(mapNullableId(parseLong(row.get("负责人用户ID")), userIdMap));
            deptMapper.updateById(updateObj);
        }
    }

    private void insertCrossTenantRoleMenus(List<LinkedHashMap<String, String>> rows, Long tenantId,
                                            Map<Long, Long> roleIdMap) {
        for (Map<String, String> row : rows) {
            Long targetRoleId = roleIdMap.get(parseLong(row.get("角色ID")));
            Long menuId = parseLong(row.get("菜单ID"));
            if (targetRoleId == null || menuId == null) {
                continue;
            }
            RoleMenuDO relation = new RoleMenuDO();
            relation.setId(null);
            relation.setRoleId(targetRoleId);
            relation.setMenuId(menuId);
            relation.setTenantId(tenantId);
            fillBase(relation, row);
            roleMenuMapper.insert(relation);
        }
    }

    private void insertCrossTenantUserRoles(List<LinkedHashMap<String, String>> rows, Map<Long, Long> roleIdMap,
                                            Map<Long, Long> userIdMap) {
        for (Map<String, String> row : rows) {
            Long targetUserId = userIdMap.get(parseLong(row.get("用户ID")));
            Long targetRoleId = roleIdMap.get(parseLong(row.get("角色ID")));
            if (targetUserId == null || targetRoleId == null) {
                continue;
            }
            UserRoleDO relation = new UserRoleDO();
            relation.setId(null);
            relation.setUserId(targetUserId);
            relation.setRoleId(targetRoleId);
            fillBase(relation, row);
            userRoleMapper.insert(relation);
        }
    }

    private void insertCrossTenantUserPosts(List<LinkedHashMap<String, String>> rows, Map<Long, Long> userIdMap,
                                            Map<Long, Long> postIdMap) {
        for (Map<String, String> row : rows) {
            Long targetUserId = userIdMap.get(parseLong(row.get("用户ID")));
            Long targetPostId = postIdMap.get(parseLong(row.get("岗位ID")));
            if (targetUserId == null || targetPostId == null) {
                continue;
            }
            UserPostDO relation = new UserPostDO();
            relation.setId(null);
            relation.setUserId(targetUserId);
            relation.setPostId(targetPostId);
            fillBase(relation, row);
            userPostMapper.insert(relation);
        }
    }

    private void writeManifest(Workbook workbook, String passwordPolicy, String packageSha256) {
        List<LinkedHashMap<String, String>> manifestRows = List.of(
                row("配置项", "formatVersion", "配置值", FORMAT_VERSION),
                row("配置项", "exportedAt", "配置值", DATE_TIME_FORMATTER.format(LocalDateTime.now())),
                row("配置项", "sourceTenantId", "配置值", TenantContextHolder.getRequiredTenantId()),
                row("配置项", "passwordPolicy", "配置值", passwordPolicy),
                row("配置项", "packageSha256", "配置值", packageSha256),
                row("配置项", "sheets", "配置值", String.join(",", REQUIRED_SHEETS))
        );
        writeSheet(workbook, SHEET_MANIFEST, List.of("配置项", "配置值"), manifestRows);
    }

    private void writeSheet(Workbook workbook, String sheetName, List<String> headers,
                            List<LinkedHashMap<String, String>> rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) {
            headerRow.createCell(index).setCellValue(headers.get(index));
            sheet.setColumnWidth(index, Math.min(28, Math.max(12, headers.get(index).length() + 6)) * 256);
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row excelRow = sheet.createRow(rowIndex + 1);
            Map<String, String> data = rows.get(rowIndex);
            for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                excelRow.createCell(cellIndex).setCellValue(data.getOrDefault(headers.get(cellIndex), ""));
            }
        }
    }

    private PackageWorkbook readPackage(byte[] content, List<String> errors) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Set<String> sheetNames = new LinkedHashSet<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                sheetNames.add(workbook.getSheetName(index));
            }
            for (String requiredSheet : REQUIRED_SHEETS) {
                if (!sheetNames.contains(requiredSheet)) {
                    errors.add(CONFIG_PACKAGE_SHEET_MISSING.getMsg().replace("{}", requiredSheet));
                }
            }
            Map<String, String> manifest = sheetNames.contains(SHEET_MANIFEST)
                    ? readManifest(workbook.getSheet(SHEET_MANIFEST), errors)
                    : Map.of();
            if (!FORMAT_VERSION.equals(manifest.get("formatVersion"))) {
                String version = StrUtil.blankToDefault(manifest.get("formatVersion"), "<empty>");
                errors.add(CONFIG_PACKAGE_FORMAT_UNSUPPORTED.getMsg().replace("{}", version));
            }
            String passwordPolicy = manifest.getOrDefault("passwordPolicy", "");
            boolean includePasswordHash = PASSWORD_POLICY_RESTORE.equals(passwordPolicy);
            Map<String, List<LinkedHashMap<String, String>>> sheets = new LinkedHashMap<>();
            for (String sheetName : dataSheetNames()) {
                if (!sheetNames.contains(sheetName)) {
                    sheets.put(sheetName, List.of());
                    continue;
                }
                sheets.put(sheetName, readSheet(workbook.getSheet(sheetName), headers(sheetName, includePasswordHash), errors));
            }
            return new PackageWorkbook(manifest, sheets);
        } catch (Exception exception) {
            throw exception(CONFIG_PACKAGE_MANIFEST_INVALID, exception.getMessage());
        }
    }

    private Map<String, String> readManifest(Sheet sheet, List<String> errors) {
        List<String> headers = readHeader(sheet);
        if (!List.of("配置项", "配置值").equals(headers)) {
            errors.add("manifest Sheet 表头必须为 配置项,配置值");
            return Map.of();
        }
        Map<String, String> manifest = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String key = formatter.formatCellValue(row.getCell(0)).trim();
            String value = formatter.formatCellValue(row.getCell(1)).trim();
            if (StrUtil.isNotBlank(key)) {
                manifest.put(key, value);
            }
        }
        if (StrUtil.isBlank(manifest.get("packageSha256"))) {
            errors.add("manifest 缺少 packageSha256");
        }
        if (StrUtil.isBlank(manifest.get("passwordPolicy"))) {
            errors.add("manifest 缺少 passwordPolicy");
        }
        return manifest;
    }

    private List<LinkedHashMap<String, String>> readSheet(Sheet sheet, List<String> expectedHeaders,
                                                          List<String> errors) {
        List<String> actualHeaders = readHeader(sheet);
        if (!expectedHeaders.equals(actualHeaders)) {
            errors.add("Sheet【" + sheet.getSheetName() + "】表头不匹配，期望 "
                    + expectedHeaders + "，实际 " + actualHeaders);
            return List.of();
        }
        List<LinkedHashMap<String, String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row excelRow = sheet.getRow(rowIndex);
            if (excelRow == null) {
                continue;
            }
            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            boolean hasValue = false;
            for (int cellIndex = 0; cellIndex < expectedHeaders.size(); cellIndex++) {
                String value = formatter.formatCellValue(excelRow.getCell(cellIndex)).trim();
                if (StrUtil.isNotBlank(value)) {
                    hasValue = true;
                }
                row.put(expectedHeaders.get(cellIndex), value);
            }
            if (hasValue) {
                rows.add(row);
            }
        }
        return rows;
    }

    private List<String> readHeader(Sheet sheet) {
        Row row = sheet.getRow(0);
        if (row == null) {
            return List.of();
        }
        DataFormatter formatter = new DataFormatter();
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < row.getLastCellNum(); index++) {
            Cell cell = row.getCell(index);
            headers.add(formatter.formatCellValue(cell).trim());
        }
        return headers;
    }

    private void validatePackageHash(PackageWorkbook pkg, boolean includePasswordHash, List<String> errors) {
        String actualHash = hashSnapshot(pkg.sheets(), includePasswordHash);
        if (!StrUtil.equals(actualHash, pkg.packageSha256())) {
            errors.add("packageSha256 不匹配，manifest=" + pkg.packageSha256() + "，actual=" + actualHash);
        }
    }

    private Long validateSourceTenant(PackageWorkbook pkg, List<String> errors) {
        if (StrUtil.isBlank(pkg.sourceTenantId())) {
            errors.add("manifest 缺少 sourceTenantId");
            return null;
        }
        Long sourceTenantId;
        try {
            sourceTenantId = parseLong(pkg.sourceTenantId());
        } catch (RuntimeException exception) {
            errors.add("manifest sourceTenantId 不是有效数字：" + pkg.sourceTenantId());
            return null;
        }
        return sourceTenantId;
    }

    private void validateFrontendComponents(PackageWorkbook pkg, Collection<String> availableComponents,
                                            List<String> errors) {
        Set<String> available = availableComponents == null ? Set.of() : availableComponents.stream()
                .map(this::normalizeComponentPath)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (available.isEmpty()) {
            errors.add("缺少当前前端构建组件清单");
            return;
        }
        for (Map<String, String> menu : pkg.rows(SHEET_MENUS)) {
            String component = normalizeComponentPath(menu.get("组件路径"));
            if (StrUtil.isBlank(component)) {
                continue;
            }
            if (!available.contains(component)) {
                errors.add("菜单【" + menu.get("菜单名称") + "】的组件路径【" + component + "】在当前前端构建中不存在");
            }
        }
    }

    private void validateReferences(PackageWorkbook pkg, List<String> errors) {
        Set<Long> menuIds = ids(pkg.rows(SHEET_MENUS), "菜单ID");
        Set<Long> roleIds = ids(pkg.rows(SHEET_ROLES), "角色ID");
        Set<Long> userIds = ids(pkg.rows(SHEET_USERS), "用户ID");
        Set<Long> postIds = ids(pkg.rows(SHEET_POST), "岗位ID");
        Set<Long> deptIds = ids(pkg.rows(SHEET_DEPT), "部门ID");
        Set<String> dictTypes = pkg.rows(SHEET_DICT_TYPE).stream()
                .map(row -> row.get("字典类型"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        requireUniqueIds(pkg.rows(SHEET_MENUS), "菜单ID", SHEET_MENUS, errors);
        requireUniqueIds(pkg.rows(SHEET_ROLES), "角色ID", SHEET_ROLES, errors);
        requireUniqueIds(pkg.rows(SHEET_USERS), "用户ID", SHEET_USERS, errors);
        requireRefs(pkg.rows(SHEET_MENUS), "父菜单ID", menuIds, "菜单父级不存在", errors, 0L);
        requireRefs(pkg.rows(SHEET_ROLE_MENU), "角色ID", roleIds, "角色菜单关系引用的角色不存在", errors);
        requireRefs(pkg.rows(SHEET_ROLE_MENU), "菜单ID", menuIds, "角色菜单关系引用的菜单不存在", errors);
        requireRefs(pkg.rows(SHEET_USER_ROLE), "用户ID", userIds, "用户角色关系引用的用户不存在", errors);
        requireRefs(pkg.rows(SHEET_USER_ROLE), "角色ID", roleIds, "用户角色关系引用的角色不存在", errors);
        requireRefs(pkg.rows(SHEET_USER_POST), "用户ID", userIds, "用户岗位关系引用的用户不存在", errors);
        requireRefs(pkg.rows(SHEET_USER_POST), "岗位ID", postIds, "用户岗位关系引用的岗位不存在", errors);
        requireRefs(pkg.rows(SHEET_DEPT), "父部门ID", deptIds, "部门父级不存在", errors, 0L);
        requireRefs(pkg.rows(SHEET_DEPT), "负责人用户ID", userIds, "部门负责人用户不存在", errors, null);
        requireRefs(pkg.rows(SHEET_USERS), "部门ID", deptIds, "用户部门不存在", errors, null);
        for (Map<String, String> role : pkg.rows(SHEET_ROLES)) {
            requireIdListRefs(role.get("数据范围部门ID"), deptIds, "角色数据范围部门不存在", errors);
        }
        for (Map<String, String> user : pkg.rows(SHEET_USERS)) {
            requireIdListRefs(user.get("岗位ID集合"), postIds, "用户岗位集合存在缺失岗位", errors);
        }
        for (Map<String, String> dictData : pkg.rows(SHEET_DICT_DATA)) {
            if (!dictTypes.contains(dictData.get("字典类型"))) {
                errors.add("字典数据引用的字典类型不存在：" + dictData.get("字典类型"));
            }
        }
        for (Map<String, String> tenantPackage : pkg.rows(SHEET_TENANT_PACKAGE)) {
            requireIdListRefs(tenantPackage.get("菜单ID集合"), menuIds, "租户套餐菜单集合存在缺失菜单", errors);
        }
    }

    private void validatePasswordPolicy(PackageWorkbook pkg, boolean restorePasswordHash, Long sourceTenantId,
                                        List<String> errors) {
        Long targetTenantId = TenantContextHolder.getRequiredTenantId();
        Map<Long, AdminUserDO> currentUsers = userMapper.selectList().stream()
                .filter(user -> Objects.equals(user.getTenantId(), targetTenantId))
                .collect(Collectors.toMap(AdminUserDO::getId, Function.identity(), (first, second) -> first));
        Map<String, AdminUserDO> currentUsersByUsername = userMapper.selectList().stream()
                .filter(user -> Objects.equals(user.getTenantId(), targetTenantId))
                .collect(Collectors.toMap(AdminUserDO::getUsername, Function.identity(), (first, second) -> first));
        boolean sameTenant = Objects.equals(sourceTenantId, targetTenantId);
        for (Map<String, String> user : pkg.rows(SHEET_USERS)) {
            Long userId = parseLong(user.get("用户ID"));
            if (restorePasswordHash) {
                if (StrUtil.isBlank(user.get(HEADER_PASSWORD_HASH))) {
                    errors.add("用户【" + user.get("用户账号") + "】缺少密码摘要");
                }
                continue;
            }
            if (sameTenant && !currentUsers.containsKey(userId)) {
                errors.add("用户【" + user.get("用户账号") + "】在目标环境不存在，preserve_existing_passwords 无法创建新用户");
            }
            if (!sameTenant && currentUsersByUsername.containsKey(user.get("用户账号"))) {
                continue;
            }
        }
    }

    private Long mapNullableId(Long sourceId, Map<Long, Long> idMap) {
        if (sourceId == null) {
            return null;
        }
        return idMap.get(sourceId);
    }

    private List<SystemConfigPackagePrecheckRespVO.SheetDiff> buildDiffs(
            Map<String, List<LinkedHashMap<String, String>>> packageSheets,
            Map<String, List<LinkedHashMap<String, String>>> currentSheets) {
        List<SystemConfigPackagePrecheckRespVO.SheetDiff> diffs = new ArrayList<>();
        for (String sheetName : dataSheetNames()) {
            List<LinkedHashMap<String, String>> packageRows = packageSheets.getOrDefault(sheetName, List.of());
            List<LinkedHashMap<String, String>> currentRows = currentSheets.getOrDefault(sheetName, List.of());
            String idHeader = headers(sheetName, true).get(0);
            Set<String> packageIds = packageRows.stream().map(row -> row.get(idHeader)).collect(Collectors.toSet());
            Set<String> currentIds = currentRows.stream().map(row -> row.get(idHeader)).collect(Collectors.toSet());
            int creates = (int) packageIds.stream().filter(id -> !currentIds.contains(id)).count();
            int updates = (int) packageIds.stream().filter(currentIds::contains).count();
            int deletes = (int) currentIds.stream().filter(id -> !packageIds.contains(id)).count();
            diffs.add(new SystemConfigPackagePrecheckRespVO.SheetDiff()
                    .setSheetName(sheetName)
                    .setPackageCount(packageRows.size())
                    .setCurrentCount(currentRows.size())
                    .setCreateCount(creates)
                    .setUpdateCount(updates)
                    .setDeleteCount(deletes));
        }
        return diffs;
    }

    private String hashSnapshot(Map<String, List<LinkedHashMap<String, String>>> sheets, boolean includePasswordHash) {
        StringBuilder builder = new StringBuilder();
        for (String sheetName : dataSheetNames()) {
            List<String> headers = headers(sheetName, includePasswordHash);
            builder.append("sheet=").append(sheetName).append('\n');
            for (Map<String, String> row : sheets.getOrDefault(sheetName, List.of())) {
                for (String header : headers) {
                    builder.append(header).append('=').append(StrUtil.trimToEmpty(row.get(header))).append('\u001F');
                }
                builder.append('\n');
            }
        }
        return sha256(builder.toString());
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void requireContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(CONFIG_PACKAGE_FILE_EMPTY);
        }
    }

    private List<String> headers(String sheetName, boolean includePasswordHash) {
        if (!SHEET_USERS.equals(sheetName)) {
            return BASE_HEADERS.get(sheetName);
        }
        List<String> headers = new ArrayList<>();
        headers.add("用户ID");
        headers.add("用户账号");
        if (includePasswordHash) {
            headers.add(HEADER_PASSWORD_HASH);
        }
        headers.addAll(List.of("密码更新时间", "用户昵称", "备注", "部门ID", "岗位ID集合", "用户邮箱", "手机号码",
                "用户性别", "用户头像", "状态", "最后登录IP", "最后登录时间", "租户ID", "创建者", "创建时间",
                "更新者", "更新时间", "是否删除"));
        return headers;
    }

    private static List<String> dataSheetNames() {
        return REQUIRED_SHEETS.stream().filter(sheet -> !SHEET_MANIFEST.equals(sheet)).toList();
    }

    private void fillBase(cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO target, Map<String, String> row) {
        target.setCreator(row.get("创建者"));
        target.setCreateTime(parseDateTime(row.get("创建时间")));
        target.setUpdater(row.get("更新者"));
        target.setUpdateTime(parseDateTime(row.get("更新时间")));
        target.setDeleted(parseBoolean(row.get("是否删除")));
    }

    private String normalizeComponentPath(String component) {
        if (StrUtil.isBlank(component)) {
            return "";
        }
        String normalized = component.trim()
                .replace("\\", "/")
                .replaceFirst("^@/views/", "")
                .replaceFirst("^src/views/", "")
                .replaceFirst("^/src/views/", "")
                .replaceFirst("^../views/", "");
        if (normalized.endsWith(".vue")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith(".tsx")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized.replaceAll("^/+", "");
    }

    private Set<Long> ids(List<LinkedHashMap<String, String>> rows, String header) {
        return rows.stream()
                .map(row -> parseLong(row.get(header)))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void requireUniqueIds(List<LinkedHashMap<String, String>> rows, String header, String sheetName,
                                  List<String> errors) {
        Set<Long> seen = new LinkedHashSet<>();
        for (Map<String, String> row : rows) {
            Long id = parseLong(row.get(header));
            if (id == null) {
                errors.add("Sheet【" + sheetName + "】存在空主键");
                continue;
            }
            if (!seen.add(id)) {
                errors.add("Sheet【" + sheetName + "】存在重复主键：" + id);
            }
        }
    }

    private void requireRefs(List<LinkedHashMap<String, String>> rows, String header, Set<Long> refs,
                             String message, List<String> errors) {
        requireRefs(rows, header, refs, message, errors, null);
    }

    private void requireRefs(List<LinkedHashMap<String, String>> rows, String header, Set<Long> refs,
                             String message, List<String> errors, Long allowedValue) {
        for (Map<String, String> row : rows) {
            Long id = parseLong(row.get(header));
            if (id == null || Objects.equals(id, allowedValue)) {
                continue;
            }
            if (!refs.contains(id)) {
                errors.add(message + "：" + header + "=" + id);
            }
        }
    }

    private void requireIdListRefs(String value, Set<Long> refs, String message, List<String> errors) {
        for (Long id : parseLongSet(value)) {
            if (!refs.contains(id)) {
                errors.add(message + "：" + id);
            }
        }
    }

    private static LinkedHashMap<String, String> row(Object... keyValues) {
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            row.put(String.valueOf(keyValues[index]), value(keyValues[index + 1]));
        }
        return row;
    }

    private static String value(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME_FORMATTER.format(dateTime);
        }
        if (value instanceof Timestamp timestamp) {
            return DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
        }
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        if (value instanceof byte[] bytes) {
            return Boolean.toString(bytes.length > 0 && bytes[0] != 0);
        }
        return String.valueOf(value);
    }

    private static String joinLongs(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return "";
        }
        return ids.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return Long.valueOf(cleanNumeric(value));
    }

    private static Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return Integer.valueOf(cleanNumeric(value));
    }

    private static String cleanNumeric(String value) {
        String normalized = value.trim();
        if (normalized.endsWith(".0")) {
            return normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }

    private static Boolean parseBoolean(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        String normalized = value.trim();
        return "true".equalsIgnoreCase(normalized) || "1".equals(normalized) || "是".equals(normalized);
    }

    private static LocalDateTime parseDateTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
    }

    private static Set<Long> parseLongSet(String value) {
        if (StrUtil.isBlank(value)) {
            return Set.of();
        }
        String normalized = value.trim().replace("[", "").replace("]", "");
        if (StrUtil.isBlank(normalized)) {
            return Set.of();
        }
        return StrUtil.splitTrim(normalized, ",").stream()
                .filter(StrUtil::isNotBlank)
                .map(SystemConfigPackageServiceImpl::parseLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record Snapshot(Map<String, List<LinkedHashMap<String, String>>> sheets) {
    }

    private record PackageWorkbook(Map<String, String> manifest,
                                   Map<String, List<LinkedHashMap<String, String>>> sheets) {

        String passwordPolicy() {
            return manifest.getOrDefault("passwordPolicy", "");
        }

        String sourceTenantId() {
            return manifest.getOrDefault("sourceTenantId", "");
        }

        String packageSha256() {
            return manifest.getOrDefault("packageSha256", "");
        }

        List<LinkedHashMap<String, String>> rows(String sheetName) {
            return sheets.getOrDefault(sheetName, List.of());
        }
    }
}
