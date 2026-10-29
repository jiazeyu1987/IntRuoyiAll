package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAdminBusinessPermissionTest extends BaseMockitoUnitTest {
    @InjectMocks private PermissionServiceImpl service;
    @Mock private RoleService roleService;
    @Mock private MenuService menuService;
    @Mock private RoleMenuMapper roleMenuMapper;

    @Test void tenantAdminGetsBusinessMenuAndMatchingApiPermissionWithoutRoleMenuRows() {
        var roleIds = Set.of(5L);
        when(roleService.getRoleListFromCache(roleIds)).thenAnswer(invocation -> new ArrayList<>(List.of(
                new RoleDO().setId(5L).setCode("tenant_admin").setStatus(0))));
        MenuDO business = new MenuDO().setId(10L).setPermission("mes:pro-feedback:query").setStatus(0);
        MenuDO platform = new MenuDO().setId(11L).setPermission("system:tenant:update").setStatus(0);
        when(roleMenuMapper.selectListByRoleId(roleIds)).thenReturn(List.of());
        when(menuService.getMenuList()).thenReturn(List.of(business, platform));
        when(menuService.getMenuIdListByPermissionFromCache("mes:pro-feedback:query")).thenReturn(List.of(10L));
        when(menuService.getMenuList(List.of(10L))).thenReturn(List.of(business));
        assertEquals(Set.of(10L), service.getRoleMenuListByRoleId(roleIds));
        assertTrue(service.hasAnyPermissionsInRoles(roleIds, "mes:pro-feedback:query"));
        assertFalse(service.hasAnyPermissionsInRoles(roleIds, "mes:unknown:query"));
        assertFalse(service.hasAnyPermissionsInRoles(roleIds, "system:tenant:update"));
    }
}
