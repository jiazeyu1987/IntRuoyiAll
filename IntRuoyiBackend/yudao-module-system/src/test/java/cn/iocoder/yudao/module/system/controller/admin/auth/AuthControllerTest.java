package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.system.service.auth.AdminAuthService;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.social.SocialClientService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static cn.hutool.core.collection.ListUtil.toList;
import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AuthControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AdminAuthService authService;
    @Mock
    private AdminUserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private MenuService menuService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private SocialClientService socialClientService;

    @Test
    void getPermissionInfoHidesSrmMenusAndPermissionsWhenUserLacksSrmAdminRole() {
        Long loginUserId = 100L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("admin");
        RoleDO superAdminRole = randomPojo(RoleDO.class)
                .setId(1L)
                .setCode(RoleCodeEnum.SUPER_ADMIN.getCode())
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO systemMenu = menu(900L, 0L, "系统管理", "/system", null, MenuTypeEnum.DIR.getType());
        MenuDO systemPermission = menu(901L, 900L, "用户查询", "user", "system:user:query", MenuTypeEnum.BUTTON.getType());
        MenuDO srmRoot = menu(991000L, 0L, "SRM", "/srm", null, MenuTypeEnum.DIR.getType());
        MenuDO srmPage = menu(991001L, 991000L, "供应商门户审核", "supplier-portal-review",
                "srm:supplier-portal:review", MenuTypeEnum.MENU.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of(1L));
        when(roleService.getRoleList(eq(Set.of(1L)))).thenReturn(toList(superAdminRole));
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of(1L)))).thenReturn(asSet(900L, 901L, 991000L, 991001L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(systemMenu, systemPermission, srmRoot, srmPage));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(systemMenu, systemPermission, srmRoot, srmPage));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertEquals(Set.of(RoleCodeEnum.SUPER_ADMIN.getCode()), respVO.getRoles());
            assertTrue(respVO.getPermissions().contains("system:user:query"));
            assertFalse(respVO.getPermissions().contains("srm:supplier-portal:review"));
            assertEquals(1, respVO.getMenus().size());
            assertEquals("/system", respVO.getMenus().get(0).getPath());
        }
    }

    @Test
    void getPermissionInfoKeepsSrmMenusAndPermissionsWhenUserHasSrmAdminRole() {
        Long loginUserId = 100L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("admin");
        RoleDO superAdminRole = randomPojo(RoleDO.class)
                .setId(1L)
                .setCode(RoleCodeEnum.SUPER_ADMIN.getCode())
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        RoleDO srmAdminRole = randomPojo(RoleDO.class)
                .setId(2L)
                .setCode(RoleCodeEnum.SRM_ADMIN.getCode())
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO systemMenu = menu(900L, 0L, "系统管理", "/system", null, MenuTypeEnum.DIR.getType());
        MenuDO srmRoot = menu(991000L, 0L, "SRM", "/srm", null, MenuTypeEnum.DIR.getType());
        MenuDO srmPage = menu(991001L, 991000L, "供应商门户审核", "supplier-portal-review",
                "srm:supplier-portal:review", MenuTypeEnum.MENU.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(asSet(1L, 2L));
        when(roleService.getRoleList(eq(asSet(1L, 2L)))).thenReturn(toList(superAdminRole, srmAdminRole));
        when(permissionService.getRoleMenuListByRoleId(eq(asSet(1L, 2L)))).thenReturn(asSet(900L, 991000L, 991001L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(systemMenu, srmRoot, srmPage));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(systemMenu, srmRoot, srmPage));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(respVO.getPermissions().contains("srm:supplier-portal:review"));
            assertEquals(2, respVO.getMenus().size());
            assertTrue(respVO.getMenus().stream().anyMatch(item -> "/srm".equals(item.getPath())));
        }
    }

    @Test
    void getPermissionInfoKeepsPermissionWhenOnlyApprovalCenterMinimalMenusAreAssigned() {
        Long loginUserId = 100L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("zhaojie");
        RoleDO approvalEntryRole = randomPojo(RoleDO.class)
                .setId(910295L)
                .setCode("approval_center_entry")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO approvalCenterMenu = menu(1200L, 1185L, "审批中心", "task", null, MenuTypeEnum.MENU.getType());
        MenuDO taskQueryPermission = menu(1221L, 1207L, "流程任务的查询", "", "bpm:task:query", MenuTypeEnum.BUTTON.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of(910295L));
        when(roleService.getRoleList(eq(Set.of(910295L)))).thenReturn(toList(approvalEntryRole));
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of(910295L)))).thenReturn(asSet(1200L, 1221L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(approvalCenterMenu, taskQueryPermission));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList());

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(respVO.getPermissions().contains("bpm:task:query"));
            assertTrue(respVO.getMenus().isEmpty());
        }
    }

    @Test
    void getPermissionInfoHidesDccAdminMenuWhenUserIsNotAdmin() {
        Long loginUserId = 100L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("aoteman");
        RoleDO dccRole = randomPojo(RoleDO.class)
                .setId(3L)
                .setCode("doc_control")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO dccRoot = menu(6800L, 0L, "文控中心", "/dcc", null, MenuTypeEnum.DIR.getType());
        MenuDO dccCategories = menu(6803L, 6800L, "文控权限", "controlled-file/categories",
                "dcc:controlled-file:category:manage", MenuTypeEnum.MENU.getType());
        MenuDO dccAdmin = menu(6819L, 6800L, "文控管理员", "controlled-file/admin",
                "dcc:controlled-file:category:manage", MenuTypeEnum.MENU.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of(3L));
        when(roleService.getRoleList(eq(Set.of(3L)))).thenReturn(toList(dccRole));
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of(3L)))).thenReturn(asSet(6800L, 6803L, 6819L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(dccRoot, dccCategories, dccAdmin));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(dccRoot, dccCategories, dccAdmin));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(respVO.getPermissions().contains("dcc:controlled-file:category:manage"));
            assertFalse(containsMenuPath(respVO.getMenus(), "controlled-file/admin"));
            assertTrue(containsMenuPath(respVO.getMenus(), "controlled-file/categories"));
        }
    }

    @Test
    void getPermissionInfoKeepsDccAdminMenuWhenUserIsAdmin() {
        Long loginUserId = 1L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("admin");
        RoleDO superAdminRole = randomPojo(RoleDO.class)
                .setId(1L)
                .setCode(RoleCodeEnum.SUPER_ADMIN.getCode())
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO dccRoot = menu(6800L, 0L, "文控中心", "/dcc", null, MenuTypeEnum.DIR.getType());
        MenuDO dccAdmin = menu(6819L, 6800L, "文控管理员", "controlled-file/admin",
                "dcc:controlled-file:category:manage", MenuTypeEnum.MENU.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of(1L));
        when(roleService.getRoleList(eq(Set.of(1L)))).thenReturn(toList(superAdminRole));
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of(1L)))).thenReturn(asSet(6800L, 6819L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(dccRoot, dccAdmin));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(dccRoot, dccAdmin));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(containsMenuPath(respVO.getMenus(), "controlled-file/admin"));
        }
    }

    @Test
    void getPermissionInfoHidesPuhuiScheduleMenuWhenUserLacksPuhuiScheduleAdminRole() {
        Long loginUserId = 100L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("aoteman");
        RoleDO schedulerRole = randomPojo(RoleDO.class)
                .setId(3L)
                .setCode("mes_scheduler")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO mesRoot = menu(5100L, 0L, "MES 系统", "/mes", null, MenuTypeEnum.DIR.getType());
        MenuDO smartScheduling = menu(900120L, 5100L, "智能排产", "smart-scheduling",
                "mes:pro-smart-scheduling:query", MenuTypeEnum.DIR.getType());
        MenuDO puhuiSchedule = menu(900104L, 900120L, "璞慧排产", "/mes/pro/puhui-schedule",
                "mes:pro-puhui-schedule:query", MenuTypeEnum.MENU.getType())
                .setComponentName("MesProPuhuiSchedule");

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of(3L));
        when(roleService.getRoleList(eq(Set.of(3L)))).thenReturn(toList(schedulerRole));
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of(3L)))).thenReturn(asSet(5100L, 900120L, 900104L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(mesRoot, smartScheduling, puhuiSchedule));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(mesRoot, smartScheduling, puhuiSchedule));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertFalse(respVO.getPermissions().contains("mes:pro-puhui-schedule:query"));
            assertFalse(containsMenuPath(respVO.getMenus(), "/mes/pro/puhui-schedule"));
        }
    }

    @Test
    void getPermissionInfoKeepsPuhuiScheduleMenuWhenUserHasPuhuiScheduleAdminRole() {
        Long loginUserId = 100L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("admin");
        RoleDO puhuiAdminRole = randomPojo(RoleDO.class)
                .setId(910300L)
                .setCode("mes_puhui_schedule_admin")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO mesRoot = menu(5100L, 0L, "MES 系统", "/mes", null, MenuTypeEnum.DIR.getType());
        MenuDO smartScheduling = menu(900120L, 5100L, "智能排产", "smart-scheduling",
                "mes:pro-smart-scheduling:query", MenuTypeEnum.DIR.getType());
        MenuDO puhuiSchedule = menu(900104L, 900120L, "璞慧排产", "/mes/pro/puhui-schedule",
                "mes:pro-puhui-schedule:query", MenuTypeEnum.MENU.getType())
                .setComponentName("MesProPuhuiSchedule");

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of(910300L));
        when(roleService.getRoleList(eq(Set.of(910300L)))).thenReturn(toList(puhuiAdminRole));
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of(910300L)))).thenReturn(asSet(5100L, 900120L, 900104L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(mesRoot, smartScheduling, puhuiSchedule));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(mesRoot, smartScheduling, puhuiSchedule));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(respVO.getPermissions().contains("mes:pro-puhui-schedule:query"));
            assertTrue(containsMenuPath(respVO.getMenus(), "/mes/pro/puhui-schedule"));
        }
    }

    @Test
    void getPermissionInfoIncludesDynamicEntitlementMenusWhenUserHasNoStaticRole() {
        Long loginUserId = 200L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("limin");
        MenuDO batchExecutionMenu = menu(7100L, 0L, "批次执行", "edhr-batch-execution",
                "mes:pro-edhr-batch-execution:query", MenuTypeEnum.MENU.getType());
        MenuDO batchExecutionUpdate = menu(7101L, 7100L, "批次执行填写", "",
                "mes:pro-edhr-batch-execution:update", MenuTypeEnum.BUTTON.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of());
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of()))).thenReturn(Set.of());
        when(permissionService.getDynamicMenuListByUserId(eq(loginUserId))).thenReturn(asSet(7100L, 7101L));
        when(menuService.getMenuList(anyCollection()))
                .thenReturn(toList(batchExecutionMenu, batchExecutionUpdate));
        when(menuService.filterDisableMenus(anyList()))
                .thenReturn(toList(batchExecutionMenu));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(respVO.getRoles().isEmpty());
            assertTrue(respVO.getPermissions().contains("mes:pro-edhr-batch-execution:query"));
            assertTrue(respVO.getPermissions().contains("mes:pro-edhr-batch-execution:update"));
            assertTrue(containsMenuPath(respVO.getMenus(), "edhr-batch-execution"));
        }
    }

    @Test
    void getPermissionInfoIncludesDynamicEntitlementMenuAncestorsWithoutExpandingPermissions() {
        Long loginUserId = 201L;
        AdminUserDO user = randomPojo(AdminUserDO.class).setId(loginUserId).setUsername("wangxin");
        MenuDO mesFeedbackRoot = menu(5700L, 0L, "生产反馈", "/mes/pro/feedback",
                "mes:pro-feedback:manage", MenuTypeEnum.DIR.getType());
        MenuDO batchExecutionMenu = menu(900033L, 5700L, "eDHR批次执行",
                "feedback/edhr-batch-execution", "mes:pro-edhr-batch-execution:query",
                MenuTypeEnum.MENU.getType());
        MenuDO batchExecutionUpdate = menu(900036L, 900033L, "eDHR批次执行更新", "",
                "mes:pro-edhr-batch-execution:update", MenuTypeEnum.BUTTON.getType());

        when(userService.getUser(eq(loginUserId))).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(eq(loginUserId))).thenReturn(Set.of());
        when(permissionService.getRoleMenuListByRoleId(eq(Set.of()))).thenReturn(Set.of());
        when(permissionService.getDynamicMenuListByUserId(eq(loginUserId))).thenReturn(asSet(900033L, 900036L));
        when(menuService.getMenu(eq(5700L))).thenReturn(mesFeedbackRoot);
        when(menuService.getMenuList(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return toList(mesFeedbackRoot, batchExecutionMenu, batchExecutionUpdate).stream()
                    .filter(menu -> ids.contains(menu.getId()))
                    .toList();
        });
        when(menuService.filterDisableMenus(anyList())).thenAnswer(invocation ->
                keepMenusWithPresentParents(invocation.getArgument(0)));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(loginUserId);

            AuthPermissionInfoRespVO respVO = authController.getPermissionInfo().getData();

            assertTrue(respVO.getRoles().isEmpty());
            assertTrue(containsMenuPath(respVO.getMenus(), "feedback/edhr-batch-execution"));
            assertTrue(respVO.getPermissions().contains("mes:pro-edhr-batch-execution:query"));
            assertTrue(respVO.getPermissions().contains("mes:pro-edhr-batch-execution:update"));
            assertFalse(respVO.getPermissions().contains("mes:pro-feedback:manage"));
        }
    }

    private static boolean containsMenuPath(List<AuthPermissionInfoRespVO.MenuVO> menus, String path) {
        if (menus == null) {
            return false;
        }
        for (AuthPermissionInfoRespVO.MenuVO menu : menus) {
            if (path.equals(menu.getPath()) || containsMenuPath(menu.getChildren(), path)) {
                return true;
            }
        }
        return false;
    }

    private static List<MenuDO> keepMenusWithPresentParents(List<MenuDO> menus) {
        Set<Long> ids = menus.stream().map(MenuDO::getId).collect(java.util.stream.Collectors.toSet());
        return menus.stream()
                .filter(menu -> Long.valueOf(0L).equals(menu.getParentId()) || ids.contains(menu.getParentId()))
                .toList();
    }

    private static MenuDO menu(Long id, Long parentId, String name, String path, String permission, Integer type) {
        return new MenuDO()
                .setId(id)
                .setParentId(parentId)
                .setName(name)
                .setPath(path)
                .setPermission(permission)
                .setType(type)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setSort(id.intValue())
                .setVisible(true)
                .setKeepAlive(true)
                .setAlwaysShow(false);
    }
}
