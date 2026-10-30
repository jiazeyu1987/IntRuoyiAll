package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationRolePageAccessTest {
    @Mock PermissionService permissionService;
    @Mock RoleService roleService;
    @Mock MenuService menuService;
    @InjectMocks OrganizationRolePageAccess access;
    private MenuDO page;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        page = new MenuDO().setId(10L).setParentId(0L).setStatus(0)
                .setVisible(true).setComponent("system/post/index");
        when(permissionService.getUserRoleIdListByUserId(1L)).thenReturn(Set.of(2L));
        when(roleService.getRoleList(Set.of(2L))).thenReturn(List.of(new RoleDO().setId(2L).setStatus(0)));
        when(permissionService.getRoleMenuListByRoleId(Set.of(2L))).thenReturn(Set.of(10L));
        when(menuService.getMenuList(Set.of(10L))).thenReturn(List.of(page));
    }

    @Test void menuGrantAllowsReadWithoutQueryButton() { assertTrue(access.canRead(1L)); }
    @Test void anonymousDenied() { assertFalse(access.canRead(null)); }
    @Test void hiddenPageDenied() { page.setVisible(false); assertFalse(access.canRead(1L)); }
    @Test void disabledPageDenied() { page.setStatus(1); assertFalse(access.canRead(1L)); }
    @Test void unrelatedMenuDenied() { page.setComponent("system/user/index"); assertFalse(access.canRead(1L)); }
    @Test void missingParentDenied() { page.setParentId(20L); assertFalse(access.canRead(1L)); }
    @Test void disabledParentDenied() {
        page.setParentId(20L);
        when(menuService.getMenu(20L)).thenReturn(new MenuDO().setId(20L).setParentId(0L).setStatus(1));
        assertFalse(access.canRead(1L));
    }
    @Test void grantedChildExposesParentPage() {
        when(menuService.getMenuList(Set.of(10L))).thenReturn(List.of(
                new MenuDO().setId(11L).setParentId(10L).setStatus(0)));
        when(menuService.getMenu(10L)).thenReturn(page);
        assertTrue(access.canRead(1L));
    }
    @Test void disabledRoleDenied() {
        when(roleService.getRoleList(Set.of(2L))).thenReturn(List.of(new RoleDO().setId(2L).setStatus(1)));
        assertFalse(access.canRead(1L));
    }
}
