package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.*;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthPlatformConfigurationBoundaryTest {
    @ParameterizedTest @ValueSource(booleans={false,true})
    void platformEntriesFollowServerBoundaryWhileBusinessEntriesRemain(boolean platformAllowed) {
        var controller=new AuthController();
        var permissions=mock(PermissionService.class);
        var users=mock(AdminUserService.class);
        var roles=mock(RoleService.class);
        var menus=mock(MenuService.class);
        Map.of("permissionService",permissions,"userService",users,"roleService",roles,"menuService",menus)
                .forEach((key,value)->ReflectionTestUtils.setField(controller,key,value));
        var platform=new MenuDO().setId(10L).setParentId(0L).setType(2).setName("Menus").setPath("menu")
                .setPermission("system:menu:query").setStatus(0).setSort(1).setVisible(true);
        var business=new MenuDO().setId(20L).setParentId(0L).setType(2).setName("Dictionary").setPath("dict")
                .setPermission("system:dict:query").setStatus(0).setSort(2).setVisible(true);
        when(users.getUser(7L)).thenReturn(new AdminUserDO().setId(7L).setUsername("operator"));
        when(permissions.getUserRoleIdListByUserId(7L)).thenReturn(Set.of(8L));
        when(roles.getRoleList(Set.of(8L))).thenReturn(List.of(new RoleDO().setId(8L).setStatus(0).setCode("tenant_admin")));
        when(permissions.getRoleMenuListByRoleId(Set.of(8L))).thenReturn(Set.of(10L,20L));
        when(permissions.hasAnyPermissions(7L,"system:menu:query")).thenReturn(platformAllowed);
        when(menus.getMenuList(anyCollection())).thenAnswer(call->{
            Collection<Long> ids=call.getArgument(0);
            return List.of(platform,business).stream().filter(menu->ids.contains(menu.getId())).toList();
        });
        when(menus.filterDisableMenus(anyList())).thenAnswer(call->call.getArgument(0));
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            var result=controller.getPermissionInfo().getData();
            assertEquals(platformAllowed,result.getPermissions().contains("system:menu:query"));
            assertTrue(result.getPermissions().contains("system:dict:query"));
            assertEquals(platformAllowed,result.getMenus().stream().anyMatch(menu->menu.getId().equals(10L)));
            assertTrue(result.getMenus().stream().anyMatch(menu->menu.getId().equals(20L)));
        }
    }
}
