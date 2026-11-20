package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.auth.AuthController;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.*;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.service.tenant.TenantPackageService;
import cn.iocoder.yudao.module.system.service.tenant.TenantServiceImpl;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises real package/menu filtering, authorization and login projection without a running server. */
class TenantAdminPackageBoundaryTest {
    @ParameterizedTest
    @ValueSource(strings = {"srm", "crm", "ai"})
    void packageControlsAdministratorMenusAndApiEvenWithStaleRoleGrants(String module) {
        var permissionService = new PermissionServiceImpl();
        var menuService = new MenuServiceImpl();
        var tenantService = new TenantServiceImpl();
        var authController = new AuthController();
        var menuMapper = mock(MenuMapper.class);
        var tenantMapper = mock(TenantMapper.class);
        var packageService = mock(TenantPackageService.class);
        var roles = mock(RoleService.class);
        var roleMenus = mock(RoleMenuMapper.class);
        var userRoles = mock(UserRoleMapper.class);
        var users = mock(AdminUserService.class);
        var properties = new TenantProperties();
        properties.setEnable(true);

        ReflectionTestUtils.setField(tenantService, "tenantProperties", properties);
        ReflectionTestUtils.setField(tenantService, "tenantMapper", tenantMapper);
        ReflectionTestUtils.setField(tenantService, "tenantPackageService", packageService);
        ReflectionTestUtils.setField(menuService, "menuMapper", menuMapper);
        ReflectionTestUtils.setField(menuService, "tenantService", tenantService);
        ReflectionTestUtils.setField(permissionService, "menuService", menuService);
        ReflectionTestUtils.setField(permissionService, "roleService", roles);
        ReflectionTestUtils.setField(permissionService, "roleMenuMapper", roleMenus);
        ReflectionTestUtils.setField(permissionService, "userRoleMapper", userRoles);
        ReflectionTestUtils.setField(permissionService, "systemEntitlementService", mock(SystemEntitlementService.class));
        ReflectionTestUtils.setField(permissionService, "temporaryRoleGrantService", mock(TemporaryRoleGrantService.class));
        ReflectionTestUtils.setField(authController, "permissionService", permissionService);
        ReflectionTestUtils.setField(authController, "menuService", menuService);
        ReflectionTestUtils.setField(authController, "roleService", roles);
        ReflectionTestUtils.setField(authController, "userService", users);

        String permission = module + ":example:query";
        List<MenuDO> menus = List.of(
                menu(1L, 0L, "mes", null, 1), menu(11L, 1L, "work", "mes:pro-feedback:query", 2),
                menu(2L, 0L, module, null, 1), menu(22L, 2L, "example", permission, 2));
        var tenantPackage = new TenantPackageDO().setId(114L).setMenuIds(Set.of(1L, 11L));
        when(tenantMapper.selectById(169L)).thenReturn(new TenantDO().setId(169L).setPackageId(114L));
        when(packageService.getTenantPackage(114L)).thenReturn(tenantPackage);
        when(menuMapper.selectList(any(MenuListReqVO.class))).thenAnswer(invocation -> new ArrayList<>(menus));
        when(menuMapper.selectListByPermission(permission)).thenReturn(List.of(menus.get(3)));
        when(menuMapper.selectByIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return menus.stream().filter(menu -> ids.contains(menu.getId())).toList();
        });
        when(menuMapper.selectById(1L)).thenReturn(menus.get(0));
        when(menuMapper.selectById(2L)).thenReturn(menus.get(2));
        var admin = new RoleDO().setId(5L).setCode("tenant_admin").setStatus(0);
        admin.setTenantId(169L);
        when(roles.getRoleListFromCache(Set.of(5L))).thenReturn(List.of(admin));
        when(roles.getRoleList(Set.of(5L))).thenReturn(List.of(admin));
        when(roleMenus.selectListByRoleId(Set.of(5L))).thenReturn(List.of(
                new RoleMenuDO().setRoleId(5L).setMenuId(2L),
                new RoleMenuDO().setRoleId(5L).setMenuId(22L)));
        when(userRoles.selectListByUserId(100L)).thenReturn(List.of(new UserRoleDO().setUserId(100L).setRoleId(5L)));
        when(users.getUser(100L)).thenReturn(new AdminUserDO().setId(100L).setUsername("yingtaigroupadmin"));

        TenantContextHolder.setTenantId(169L);
        try (var security = mockStatic(SecurityFrameworkUtils.class);
             var spring = mockStatic(SpringUtil.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            spring.when(() -> SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(permissionService);
            for (boolean included : List.of(false, true, false)) {
                tenantPackage.setMenuIds(included ? Set.of(1L, 11L, 2L, 22L) : Set.of(1L, 11L));
                assertEquals(included, permissionService.hasAnyPermissions(100L, permission));
                assertEquals(included, permissionService.hasAnyPermissionsInRoles(Set.of(5L), permission));
                var info = authController.getPermissionInfo().getData();
                assertEquals(included, info.getPermissions().contains(permission));
                assertEquals(included, info.getMenus().stream().anyMatch(menu -> module.equals(menu.getPath())));
                assertTrue(info.getMenus().stream().anyMatch(menu -> "mes".equals(menu.getPath())));
                assertTrue(info.getPermissions().contains("mes:pro-feedback:query"));
            }
        } finally {
            TenantContextHolder.clear();
        }
    }

    private MenuDO menu(long id, long parentId, String path, String permission, int type) {
        return new MenuDO().setId(id).setParentId(parentId).setPath(path).setName(path)
                .setPermission(permission).setType(type).setStatus(0).setVisible(true).setSort(1);
    }
}
