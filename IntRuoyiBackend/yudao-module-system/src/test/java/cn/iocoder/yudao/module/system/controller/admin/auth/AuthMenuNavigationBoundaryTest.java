package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO.MenuVO;
import cn.iocoder.yudao.module.system.convert.auth.AuthConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthMenuNavigationBoundaryTest {
    @ParameterizedTest
    @CsvSource({"production_leader_permission,false", "pqc_leader_permission,false", "common,false", "production_leader_permission,true"})
    void buttonsPreserveAuthorityWithoutGrantingPageNavigation(String roleCode, boolean grantFeedbackPage) {
        var controller = new AuthController();
        var permissions = mock(PermissionService.class);
        var users = mock(AdminUserService.class);
        var roles = mock(RoleService.class);
        var menus = mock(MenuService.class);
        Map.of("permissionService", permissions, "userService", users, "roleService", roles, "menuService", menus)
                .forEach((name, value) -> ReflectionTestUtils.setField(controller, name, value));
        var directory = menu(100, 0, 1, "Execution", "/execution", "");
        var feedback = menu(110, 100, 2, "Feedback", "/feedback", "mes:feedback:query");
        var button = menu(111, 110, 3, "Create", "", "mes:feedback:create");
        var workbench = menu(120, 100, 2, "Workbench", "/workbench", "mes:workbench:query");
        var emptySystem = menu(200, 0, 1, "System", "/system", "");
        var emptyAudit = menu(210, 200, 1, "Audit", "log", "");
        List<MenuDO> catalog = List.of(directory, feedback, button, workbench, emptySystem, emptyAudit);
        Set<Long> grants = new HashSet<>(Set.of(100L, 111L, 120L, 200L, 210L));
        if (grantFeedbackPage) grants.add(110L);
        when(users.getUser(7L)).thenReturn(new AdminUserDO().setId(7L).setUsername("worker"));
        when(permissions.getUserRoleIdListByUserId(7L)).thenReturn(Set.of(8L));
        when(roles.getRoleList(Set.of(8L))).thenReturn(List.of(new RoleDO().setId(8L).setStatus(0).setCode(roleCode)));
        when(permissions.getRoleMenuListByRoleId(Set.of(8L))).thenReturn(grants);
        when(menus.getMenuList(anyCollection())).thenAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return catalog.stream().filter(item -> ids.contains(item.getId())).toList();
        });
        when(menus.getMenu(anyLong())).thenAnswer(call -> catalog.stream()
                .filter(item -> item.getId().equals(call.getArgument(0))).findFirst().orElse(null));
        when(menus.filterDisableMenus(anyList())).thenAnswer(call -> call.getArgument(0));
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            var result = controller.getPermissionInfo().getData();
            assertTrue(result.getPermissions().contains("mes:feedback:create"));
            assertEquals(grantFeedbackPage, result.getPermissions().contains("mes:feedback:query"));
            assertEquals(1, result.getMenus().size(), "Empty audit/system directories must be removed");
            var children = result.getMenus().get(0).getChildren();
            assertTrue(children.stream().filter(node -> node.getId() == 120L).findFirst().orElseThrow().getVisible());
            assertEquals(grantFeedbackPage, children.stream().filter(node -> node.getId() == 110L)
                    .findFirst().orElseThrow().getVisible());
            assertTrue(feedback.getVisible(), "Projection must not mutate cached menu definitions");
        }
    }

    @Test
    void hiddenDetailPagesKeepHiddenRoutingShellsAndVisiblePagesKeepTheirGroups() {
        var root = menu(1, 0, 1, "Root", "/root", "");
        var nested = menu(2, 1, 1, "Nested", "nested", "");
        var detail = menu(3, 2, 2, "Detail", "detail", "query").setVisible(false);
        List<MenuVO> hidden = AuthConvert.INSTANCE.buildMenuTree(List.of(root, nested, detail));
        assertEquals(1, hidden.size());
        assertFalse(hidden.get(0).getVisible());
        assertFalse(hidden.get(0).getChildren().get(0).getVisible());
        assertEquals(3L, hidden.get(0).getChildren().get(0).getChildren().get(0).getId());
        detail.setVisible(true);
        assertTrue(AuthConvert.INSTANCE.buildMenuTree(List.of(root, nested, detail)).get(0).getVisible());
    }

    private MenuDO menu(long id, long parent, int type, String name, String path, String permission) {
        return new MenuDO().setId(id).setParentId(parent).setType(type).setName(name).setPath(path)
                .setPermission(permission).setStatus(0).setSort((int) id).setVisible(true);
    }
}
