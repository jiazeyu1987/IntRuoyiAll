package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRoleCombinationTest extends BaseMockitoUnitTest {
    @Spy @InjectMocks private PermissionServiceImpl service;
    @Mock private RoleService roleService;
    @Mock private MenuService menuService;
    @Mock private RoleMenuMapper roleMenuMapper;
    @Mock private DeptService deptService;
    @Mock private AdminUserService userService;

    private RoleDO role(long id, int scope) {
        return new RoleDO().setId(id).setCode("test_" + id).setStatus(0).setDataScope(scope);
    }

    @Test void customScopeDoesNotImplicitlyGrantTheUsersDepartment() {
        doReturn(List.of(role(1, DataScopeEnum.DEPT_CUSTOM.getScope()).setDataScopeDeptIds(Set.of(21L))))
                .when(service).getEnableUserRoleListByUserIdFromCache(10L);
        var scope = service.getDeptDataPermission(10L);
        assertEquals(Set.of(21L), scope.getDeptIds());
        assertFalse(scope.getSelf());
        assertFalse(scope.getAll());
        verifyNoInteractions(userService);
    }

    @Test void customSelfAndDepartmentRolesCombineAsAnExplicitUnion() {
        doReturn(List.of(role(1, DataScopeEnum.DEPT_CUSTOM.getScope()).setDataScopeDeptIds(Set.of(21L)),
                role(2, DataScopeEnum.SELF.getScope()), role(3, DataScopeEnum.DEPT_AND_CHILD.getScope())))
                .when(service).getEnableUserRoleListByUserIdFromCache(10L);
        when(userService.getUser(10L)).thenReturn(new AdminUserDO().setDeptId(22L));
        when(deptService.getChildDeptIdListFromCache(22L)).thenReturn(Set.of(23L));
        var scope = service.getDeptDataPermission(10L);
        assertEquals(Set.of(21L, 22L, 23L), scope.getDeptIds());
        assertTrue(scope.getSelf());
        assertFalse(scope.getAll());
    }

    @Test void allScopeRoleAllowsAllDataWhenCombinedWithRestrictedRoles() {
        doReturn(List.of(role(1, DataScopeEnum.SELF.getScope()), role(2, DataScopeEnum.ALL.getScope())))
                .when(service).getEnableUserRoleListByUserIdFromCache(10L);
        assertTrue(service.getDeptDataPermission(10L).getAll());
    }

    @Test void disabledRolesDoNotProvideMenusOrApiActionsAndCachedListsStayIntact() {
        var roles = List.of(role(1, 5), role(2, 5).setStatus(1));
        when(roleService.getRoleListFromCache(Set.of(1L, 2L))).thenReturn(roles);
        when(menuService.getMenuList()).thenReturn(List.of());
        when(roleMenuMapper.selectListByRoleId(Set.of(1L)))
                .thenReturn(List.of(new RoleMenuDO().setRoleId(1L).setMenuId(100L)));
        when(menuService.getMenuIdListByPermissionFromCache("system:user:query")).thenReturn(List.of(100L));
        when(menuService.getMenuIdListByPermissionFromCache("system:user:delete")).thenReturn(List.of(200L));
        doReturn(Set.of(1L)).when(service).getMenuRoleIdListByMenuIdFromCache(100L);
        doReturn(Set.of(2L)).when(service).getMenuRoleIdListByMenuIdFromCache(200L);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(service);
            assertEquals(Set.of(100L), service.getRoleMenuListByRoleId(Set.of(1L, 2L)));
            assertTrue(service.hasAnyPermissionsInRoles(Set.of(1L, 2L), "system:user:query"));
            assertFalse(service.hasAnyPermissionsInRoles(Set.of(1L, 2L), "system:user:delete"));
        }
        assertEquals(2, roles.size());
    }

    @Test void enabledRolesCombineMenusAndApiActionsWithoutGrantingOtherOperations() {
        var ids = Set.of(1L, 2L);
        when(roleService.getRoleListFromCache(ids)).thenReturn(List.of(role(1, 5), role(2, 5)));
        when(menuService.getMenuList()).thenReturn(List.of());
        when(roleMenuMapper.selectListByRoleId(ids)).thenReturn(List.of(
                new RoleMenuDO().setRoleId(1L).setMenuId(100L), new RoleMenuDO().setRoleId(2L).setMenuId(200L)));
        when(menuService.getMenuIdListByPermissionFromCache("system:user:query")).thenReturn(List.of(100L));
        when(menuService.getMenuIdListByPermissionFromCache("system:user:update")).thenReturn(List.of(200L));
        doReturn(Set.of(1L)).when(service).getMenuRoleIdListByMenuIdFromCache(100L);
        doReturn(Set.of(2L)).when(service).getMenuRoleIdListByMenuIdFromCache(200L);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(PermissionServiceImpl.class)).thenReturn(service);
            assertEquals(Set.of(100L, 200L), service.getRoleMenuListByRoleId(ids));
            assertTrue(service.hasAnyPermissionsInRoles(ids, "system:user:query"));
            assertTrue(service.hasAnyPermissionsInRoles(ids, "system:user:update"));
            assertFalse(service.hasAnyPermissionsInRoles(ids, "system:user:delete"));
        }
    }
}
