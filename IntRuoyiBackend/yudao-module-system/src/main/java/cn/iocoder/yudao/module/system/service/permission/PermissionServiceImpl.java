package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 权限 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private static final Pattern LOG_PERMISSION_PATTERN = Pattern.compile("(^|[:\\-])log([:\\-]|$)");

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private DeptService deptService;
    @Resource
    private AdminUserService userService;
    @Resource
    private SystemEntitlementService systemEntitlementService;
    @Resource
    private TemporaryRoleGrantService temporaryRoleGrantService;
    @Resource
    private PermissionCommandProtocol permissionCommandProtocol;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private TenantMapper tenantMapper;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        // 如果为空，说明已经有权限
        if (ArrayUtil.isEmpty(permissions)) {
            return true;
        }

        // 情况一：动态权益优先判断。无静态角色的责任型用户也可能拥有最小入口权限。
        for (String permission : permissions) {
            if (systemEntitlementService.hasAnyPermission(userId, permission)) {
                return true;
            }
        }

        // 情况二：先判断用户永久角色，永久角色可放行时不记录临时权限使用
        List<RoleDO> permanentRoles = getEnablePermanentRoleListByUserIdFromCache(userId);
        for (String permission : permissions) {
            if (hasAnyPermission(permanentRoles, permission)) {
                return true;
            }
        }

        // 情况三：如果永久角色是超管，也说明有权限
        if (CollUtil.isNotEmpty(permanentRoles)
                && roleService.hasAnySuperAdmin(convertSet(permanentRoles, RoleDO::getId))) {
            return true;
        }

        // 情况四：永久角色无法放行时，才判断临时角色并记录使用审计
        List<RoleDO> temporaryRoles = getEnableTemporaryRoleListByUserId(userId);
        if (CollUtil.isEmpty(temporaryRoles)) {
            return false;
        }
        for (String permission : permissions) {
            if (hasAnyPermission(temporaryRoles, permission)) {
                temporaryRoleGrantService.recordPermissionUse(userId, permission);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyPermissionsInRoles(Collection<Long> roleIds, String... permissions) {
        if (CollectionUtil.isEmpty(roleIds) || ArrayUtil.isEmpty(permissions)) {
            return false;
        }
        List<RoleDO> roles = roleService.getRoleListFromCache(roleIds);
        if (CollUtil.isEmpty(roles)) {
            return false;
        }
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        if (CollUtil.isEmpty(roles)) {
            return false;
        }
        for (String permission : permissions) {
            if (hasAnyPermission(roles, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断指定角色，是否拥有该 permission 权限
     *
     * @param roles 指定角色数组
     * @param permission 权限标识
     * @return 是否拥有
     */
    private boolean hasAnyPermission(List<RoleDO> roles, String permission) {
        List<Long> menuIds = menuService.getMenuIdListByPermissionFromCache(permission);
        // 采用严格模式，如果权限找不到对应的 Menu 的话，也认为没有权限
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }

        // 判断是否有权限
        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        for (Long menuId : menuIds) {
            // 获得拥有该菜单的角色编号集合
            Set<Long> menuRoleIds = getSelf().getMenuRoleIdListByMenuIdFromCache(menuId);
            // 如果有交集，说明有权限
            if (CollUtil.containsAny(menuRoleIds, roleIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        // 如果为空，说明已经有权限
        if (ArrayUtil.isEmpty(roles)) {
            return true;
        }

        // 获得当前登录的角色。如果为空，说明没有权限
        List<RoleDO> roleList = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roleList)) {
            return false;
        }

        // 判断是否有角色
        Set<String> userRoles = convertSet(roleList, RoleDO::getCode);
        return CollUtil.containsAny(userRoles, Sets.newHashSet(roles));
    }

    @Override
    public boolean hasAnyRolesOrSuperAdmin(Long userId, String... roles) {
        List<RoleDO> roleList = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roleList)) {
            return false;
        }
        Set<Long> roleIds = convertSet(roleList, RoleDO::getId);
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return true;
        }
        if (ArrayUtil.isEmpty(roles)) {
            return true;
        }
        Set<String> userRoles = convertSet(roleList, RoleDO::getCode);
        return CollUtil.containsAny(userRoles, Sets.newHashSet(roles));
    }

    // ========== 角色-菜单的相关方法  ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
            allEntries = true),
            @CacheEvict(value = RedisKeyConstants.PERMISSION_MENU_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，主要一次更新涉及到的 menuIds 较多，反倒批量会更快
    })
    @GxpWriteOperation(operationId = "system.permission.role-menu.assign")
    public void assignRoleMenu(Long roleId, Set<Long> menuIds, String reason, String idempotencyKey) {
        requireGxpPermissionAuditEvidence(reason, idempotencyKey);
        Map<String, Object> requested = permissionSetState("roleId", roleId, "menuIds", menuIds);
        var command = permissionCommandProtocol.begin("system.permission.role-menu.assign", roleId,
                reason, idempotencyKey, requested);
        applyRoleMenu(command, roleId, menuIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST, allEntries = true),
            @CacheEvict(value = RedisKeyConstants.PERMISSION_MENU_ID_LIST, allEntries = true)
    })
    public void assignTenantRoleMenu(Long tenantId, Long roleId, Set<Long> menuIds, String managementPermission,
                                     String reason, String idempotencyKey) {
        requireTenantAdministration(tenantId, managementPermission);
        var command = permissionCommandProtocol.beginTenantAdministration("system.permission.role-menu.assign", roleId,
                reason, idempotencyKey, permissionSetState("roleId", roleId, "menuIds", menuIds), tenantId, managementPermission);
        applyRoleMenu(command, roleId, menuIds);
    }

    private void applyRoleMenu(PermissionCommandProtocol.Command command, Long roleId, Set<Long> menuIds) {
        if (command.replayed()) return;
        requirePermissionRole(command.tenantId(), roleId);
        // 获得角色拥有菜单编号
        Set<Long> dbMenuIds = convertSet(roleMenuMapper.selectPermissionRowsForUpdate(command.tenantId(), roleId), RoleMenuDO::getMenuId);
        Set<Long> beforeMenuIds = sortedLongSet(dbMenuIds);
        // 计算新增和删除的菜单编号
        Set<Long> menuIdList = CollUtil.emptyIfNull(menuIds);
        Collection<Long> createMenuIds = CollUtil.subtract(menuIdList, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, menuIdList);
        // 执行新增和删除。对于已经授权的菜单，不用做任何处理
        if (CollUtil.isNotEmpty(createMenuIds)) {
            roleMenuMapper.insertBatch(CollectionUtils.convertList(createMenuIds, menuId -> {
                RoleMenuDO entity = new RoleMenuDO();
                entity.setRoleId(roleId);
                entity.setMenuId(menuId);
                entity.setTenantId(command.tenantId());
                return entity;
            }));
        }
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            roleMenuMapper.deleteListByRoleIdAndMenuIds(roleId, deleteMenuIds);
        }
        var persisted = convertSet(roleMenuMapper.selectPermissionRowsForUpdate(command.tenantId(), roleId), RoleMenuDO::getMenuId);
        permissionCommandProtocol.finish(command,
                permissionSetState("roleId", roleId, "menuIds", beforeMenuIds),
                permissionSetState("roleId", roleId, "menuIds", persisted));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
                    allEntries = true), // allEntries 清空所有缓存，此处无法方便获得 roleId 对应的 menu 缓存们
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST,
                    allEntries = true) // allEntries 清空所有缓存，此处无法方便获得 roleId 对应的 user 缓存们
    })
    public void processRoleDeleted(Long roleId) {
        // 标记删除 UserRole
        userRoleMapper.deleteListByRoleId(roleId);
        // 标记删除 RoleMenu
        roleMenuMapper.deleteListByRoleId(roleId);
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public void processMenuDeleted(Long menuId) {
        roleMenuMapper.deleteListByMenuId(menuId);
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }

        // 如果是管理员的情况下，获取全部菜单编号
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return convertSet(menuService.getMenuList(), MenuDO::getId);
        }
        // 如果是非管理员的情况下，获得拥有的菜单编号
        return convertSet(roleMenuMapper.selectListByRoleId(roleIds), RoleMenuDO::getMenuId);
    }

    @Override
    public Set<Long> getDynamicMenuListByUserId(Long userId) {
        return systemEntitlementService.getActiveMenuIdsByUserId(userId);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId) {
        return convertSet(roleMenuMapper.selectListByMenuId(menuId), RoleMenuDO::getRoleId);
    }

    // ========== 用户-角色的相关方法  ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    @GxpWriteOperation(operationId = "system.permission.user-role.assign")
    public void assignUserRole(Long userId, Set<Long> roleIds, String reason, String idempotencyKey) {
        requireGxpPermissionAuditEvidence(reason, idempotencyKey);
        var command = permissionCommandProtocol.begin("system.permission.user-role.assign", userId,
                reason, idempotencyKey, permissionSetState("userId", userId, "roleIds", roleIds));
        applyUserRole(command, userId, roleIds, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void initializeTenantAdministrator(Long tenantId, Long userId, Long roleId, String reason, String idempotencyKey) {
        requireTenantAdministration(tenantId, "system:tenant:create");
        var command = permissionCommandProtocol.beginTenantAdministration("system.permission.user-role.assign", userId,
                reason, idempotencyKey, permissionSetState("userId", userId, "roleIds", Set.of(roleId)),
                tenantId, "system:tenant:create");
        if (command.replayed()) return;
        TenantDO tenant = tenantMapper.selectById(tenantId);
        RoleDO role = roleMapper.selectPermissionSubjectForUpdate(tenantId, roleId);
        if (tenant == null || !Objects.equals(userId, tenant.getContactUserId()) || role == null
                || !RoleCodeEnum.TENANT_ADMIN.getCode().equals(role.getCode())
                || !Objects.equals(RoleTypeEnum.SYSTEM.getType(), role.getType())) {
            throw new IllegalStateException("Tenant administrator initialization subject mismatch");
        }
        applyUserRole(command, userId, Set.of(roleId), true);
    }

    private void requireTenantAdministration(Long tenantId, String permission) {
        if (!Set.of("system:tenant:create", "system:tenant:update", "system:tenant-package:update").contains(permission)
                || !Objects.equals(tenantId, TenantContextHolder.getRequiredTenantId())) {
            throw new IllegalStateException("Tenant administration target or action mismatch");
        }
        var actor = SecurityFrameworkUtils.getLoginUser();
        if (actor == null || actor.getId() == null || actor.getTenantId() == null) {
            throw new IllegalStateException("Authenticated tenant administrator required");
        }
        TenantUtils.execute(actor.getTenantId(), () -> {
            TenantDO platform = tenantMapper.selectById(actor.getTenantId());
            if (platform == null || !Objects.equals(platform.getPackageId(), TenantDO.PACKAGE_ID_SYSTEM)
                    || !hasAnyPermissions(actor.getId(), permission)) {
                throw new IllegalStateException("Tenant administration permission denied");
            }
        });
    }

    private void applyUserRole(PermissionCommandProtocol.Command command, Long userId, Set<Long> roleIds,
                               boolean initializeAdministrator) {
        if (command.replayed()) return;
        if (adminUserMapper.selectPermissionSubjectForUpdate(command.tenantId(), userId) == null) {
            throw new IllegalStateException("Permission user subject not found in tenant");
        }
        // 获得角色拥有角色编号
        Set<Long> dbRoleIds = convertSet(userRoleMapper.selectPermissionRowsForUpdate(command.tenantId(), userId),
                UserRoleDO::getRoleId);
        if (initializeAdministrator) {
            if (!dbRoleIds.isEmpty()) throw new IllegalStateException("Tenant administrator already initialized");
        } else {
            validateAssignableUserRoles(dbRoleIds, roleIds);
        }
        Set<Long> beforeRoleIds = sortedLongSet(dbRoleIds);
        // 计算新增和删除的角色编号
        Set<Long> roleIdList = CollUtil.emptyIfNull(roleIds);
        Collection<Long> createRoleIds = CollUtil.subtract(roleIdList, dbRoleIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbRoleIds, roleIdList);
        // 执行新增和删除。对于已经授权的角色，不用做任何处理
        if (!CollectionUtil.isEmpty(createRoleIds)) {
            userRoleMapper.insertBatch(CollectionUtils.convertList(createRoleIds, roleId -> {
                UserRoleDO entity = new UserRoleDO();
                entity.setUserId(userId);
                entity.setRoleId(roleId);
                entity.setTenantId(command.tenantId());
                return entity;
            }));
        }
        if (!CollectionUtil.isEmpty(deleteMenuIds)) {
            userRoleMapper.deleteListByUserIdAndRoleIdIds(userId, deleteMenuIds);
        }
        var persisted = convertSet(userRoleMapper.selectPermissionRowsForUpdate(command.tenantId(), userId), UserRoleDO::getRoleId);
        permissionCommandProtocol.finish(command,
                permissionSetState("userId", userId, "roleIds", beforeRoleIds),
                permissionSetState("userId", userId, "roleIds", persisted));
    }

    private void validateAssignableUserRoles(Collection<Long> currentRoleIds, Collection<Long> targetRoleIds) {
        if (CollectionUtil.isEmpty(targetRoleIds)) {
            return;
        }
        if (hasAnyRestrictedRole(currentRoleIds)) {
            return;
        }
        if (hasAnyRestrictedRole(targetRoleIds)) {
            throw exception(USER_ASSIGN_HIGH_PERMISSION_FORBIDDEN);
        }
    }

    private boolean hasAnyRestrictedRole(Collection<Long> roleIds) {
        if (CollectionUtil.isEmpty(roleIds)) {
            return false;
        }
        List<RoleDO> roles = roleService.getRoleListFromCache(roleIds);
        if (CollUtil.isEmpty(roles)) {
            return false;
        }
        for (RoleDO role : roles) {
            if (role != null && RoleCodeEnum.isAdminRole(role.getCode())) {
                return true;
            }
        }

        Set<Long> menuIds = convertSet(roleMenuMapper.selectListByRoleId(roleIds), RoleMenuDO::getMenuId);
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }
        List<MenuDO> menus = menuService.getMenuList(menuIds);
        return menus.stream().anyMatch(menu -> menu != null && isLogPermission(menu.getPermission()));
    }

    private boolean isLogPermission(String permission) {
        return permission != null && LOG_PERMISSION_PATTERN.matcher(permission).find();
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void processUserDeleted(Long userId) {
        userRoleMapper.deleteListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return convertSet(userRoleMapper.selectListByUserId(userId), UserRoleDO::getRoleId);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public Set<Long> getUserRoleIdListByUserIdFromCache(Long userId) {
        return getUserRoleIdListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByRoleId(Collection<Long> roleIds) {
        return convertSet(userRoleMapper.selectListByRoleIds(roleIds), UserRoleDO::getUserId);
    }

    /**
     * 获得用户拥有的角色，并且这些角色是开启状态的
     *
     * @param userId 用户编号
     * @return 用户拥有的角色
     */
    @VisibleForTesting
    List<RoleDO> getEnableUserRoleListByUserIdFromCache(Long userId) {
        List<RoleDO> roles = getEnablePermanentRoleListByUserIdFromCache(userId);
        roles.addAll(getEnableTemporaryRoleListByUserId(userId));
        return roles;
    }

    private List<RoleDO> getEnablePermanentRoleListByUserIdFromCache(Long userId) {
        // 获得用户拥有的角色编号
        Set<Long> roleIds = new LinkedHashSet<>(getSelf().getUserRoleIdListByUserIdFromCache(userId));
        // 获得角色数组，并移除被禁用的
        List<RoleDO> roles = new ArrayList<>(roleService.getRoleListFromCache(roleIds));
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        return roles;
    }

    @Override
    public Set<Long> getTemporaryMenuListByUserId(Long userId) {
        var roles=getEnableTemporaryRoleListByUserId(userId).stream()
                .filter(role->Objects.equals(role.getTenantId(),
                        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getTenantId())).toList();
        if (roles.isEmpty()) return Set.of();
        // Use explicit role-menu grants, including TEMP roles carrying an admin code.
        return roleMenuMapper.selectListByRoleId(roles.stream().map(RoleDO::getId).toList()).stream()
                .map(RoleMenuDO::getMenuId).collect(java.util.stream.Collectors.toSet());
    }

    private List<RoleDO> getEnableTemporaryRoleListByUserId(Long userId) {
        Set<Long> temporaryRoleIds = temporaryRoleGrantService.getActiveRoleIdsByUserId(userId, LocalDateTime.now());
        if (CollUtil.isEmpty(temporaryRoleIds)) {
            return new ArrayList<>();
        }
        List<RoleDO> temporaryRoles = new ArrayList<>(roleService.getRoleListFromCache(temporaryRoleIds));
        temporaryRoles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        return temporaryRoles;
    }

    // ========== 用户-部门的相关方法  ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GxpWriteOperation(operationId = "system.permission.role-data-scope.assign")
    public void assignRoleDataScope(Long roleId, Integer dataScope, Set<Long> dataScopeDeptIds,
                                    String reason, String idempotencyKey) {
        requireGxpPermissionAuditEvidence(reason, idempotencyKey);
        Map<String, Object> requested = new LinkedHashMap<>();
        requested.put("roleId", String.valueOf(roleId));
        requested.put("dataScope", dataScope);
        requested.put("dataScopeDeptIds", PermissionCommandProtocol.ids(dataScopeDeptIds));
        var command = permissionCommandProtocol.begin("system.permission.role-data-scope.assign", roleId,
                reason, idempotencyKey, requested);
        if (command.replayed()) return;
        RoleDO beforeRole = requirePermissionRole(command.tenantId(), roleId);
        Map<String, Object> beforeState = roleDataScopeState(beforeRole);
        roleService.updateRoleDataScope(roleId, dataScope, dataScopeDeptIds);
        permissionCommandProtocol.finish(command, beforeState,
                roleDataScopeState(requirePermissionRole(command.tenantId(), roleId)));
    }

    @Override
    @DataPermission(enable = false) // 关闭数据权限，不然就会出现递归获取数据权限的问题
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        // 获得用户的角色
        List<RoleDO> roles = getEnableUserRoleListByUserIdFromCache(userId);

        // 如果角色为空，则只能查看自己
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        if (CollUtil.isEmpty(roles)) {
            result.setSelf(true);
            return result;
        }

        // 获得用户的部门编号的缓存，通过 Guava 的 Suppliers 惰性求值，即有且仅有第一次发起 DB 的查询
        Supplier<Long> userDeptId = Suppliers.memoize(() -> userService.getUser(userId).getDeptId());
        // 遍历每个角色，计算
        for (RoleDO role : roles) {
            // 为空时，跳过
            if (role.getDataScope() == null) {
                continue;
            }
            // 情况一，ALL
            if (Objects.equals(role.getDataScope(), DataScopeEnum.ALL.getScope())) {
                result.setAll(true);
                continue;
            }
            // 情况二，DEPT_CUSTOM
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
                CollUtil.addAll(result.getDeptIds(), role.getDataScopeDeptIds());
                // 自定义可见部门时，保证可以看到自己所在的部门。否则，一些场景下可能会有问题。
                // 例如说，登录时，基于 t_user 的 username 查询会可能被 dept_id 过滤掉
                CollectionUtils.addIfNotNull(result.getDeptIds(), userDeptId.get());
                continue;
            }
            // 情况三，DEPT_ONLY
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_ONLY.getScope())) {
                CollectionUtils.addIfNotNull(result.getDeptIds(), userDeptId.get());
                continue;
            }
            // 情况四，DEPT_DEPT_AND_CHILD
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_AND_CHILD.getScope())) {
                Long deptId = userDeptId.get();
                // 用户未设置部门，直接跳过；否则 getChildDeptIdListFromCache 走缓存注解会因 null key 报错
                if (deptId == null) {
                    continue;
                }
                CollUtil.addAll(result.getDeptIds(), deptService.getChildDeptIdListFromCache(deptId));
                // 添加本身部门编号
                result.getDeptIds().add(deptId);
                continue;
            }
            // 情况五，SELF
            if (Objects.equals(role.getDataScope(), DataScopeEnum.SELF.getScope())) {
                result.setSelf(true);
                continue;
            }
            // 未知情况，error log 即可
            log.error("[getDeptDataPermission][LoginUser({}) role({}) 无法处理]", userId, toJsonString(result));
        }
        return result;
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private PermissionServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

    private void requireGxpPermissionAuditEvidence(String reason, String idempotencyKey) {
        PermissionCommandProtocol.validateKey(idempotencyKey);
        if (StrUtil.hasBlank(reason, idempotencyKey)) {
            throw new IllegalArgumentException("GxP permission audit reason and idempotencyKey are required");
        }
    }

    private RoleDO requirePermissionRole(Long tenantId, Long roleId) {
        RoleDO role = roleMapper.selectPermissionSubjectForUpdate(tenantId, roleId);
        if (role == null) throw new IllegalStateException("Permission role subject not found in tenant");
        return role;
    }

    private Map<String, Object> permissionSetState(String subjectField, Long id, String setField, Collection<Long> values) {
        return Map.of(subjectField, String.valueOf(id), setField, PermissionCommandProtocol.ids(values));
    }

    private Map<String, Object> roleDataScopeState(RoleDO role) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("roleId", role.getId().toString());
        state.put("dataScope", role.getDataScope());
        state.put("dataScopeDeptIds", PermissionCommandProtocol.ids(role.getDataScopeDeptIds()));
        return state;
    }

    private Set<Long> sortedLongSet(Collection<Long> values) {
        return values == null ? Set.of() : new TreeSet<>(values);
    }

}
