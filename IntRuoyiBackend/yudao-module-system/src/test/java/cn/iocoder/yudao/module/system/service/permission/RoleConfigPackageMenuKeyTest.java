package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleConfigPackageMenuKeyTest {

    @Test
    void retiredLogShellsKeepDistinctIdentitiesAndResolveAcrossEnvironmentIds() {
        MenuDO system = route(1L, 0L, 1, "/system", "", "", "系统管理");
        MenuDO audit = route(108L, 1L, 1, "log", "", "", "审计日志");
        MenuDO log = route(500L, 108L, 2, "operate-log", "system/operatelog/index", "SystemOperateLog", "操作日志");
        MenuDO login = route(501L, 500L, 3, "", "", "", "登录日志");
        MenuDO access = route(1078L, 500L, 3, "", "", "", "访问日志");
        MenuDO api = route(1083L, 500L, 3, "", "", "", "API 日志");
        RoleConfigPackageServiceImpl source = serviceWithMenus(system, audit, log, login, access, api);
        Map<Long, String> exported = ReflectionTestUtils.invokeMethod(source, "buildMenuKeyById");
        // Same package format, different database IDs and renamed ancestor labels.
        system.setName("系统设置");
        access.setId(2078L);
        RoleConfigPackageServiceImpl target = serviceWithMenus(system, audit, log, login, access, api);
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(target, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setMenuKeys(List.of(exported.get(1078L)));
        assertEquals(Set.of(2078L), ReflectionTestUtils.invokeMethod(target, "resolveMenuIds", item,
                mapping, Set.of(501L, 2078L, 1083L)));
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(target, "resolveMenuIds",
                item, mapping, Set.of(501L, 1083L)));
    }

    @Test
    void identicalPermissionFreeButtonsStillFailInsteadOfChoosingOne() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(
                route(501L, 0L, 3, "", "", "", "登录日志"),
                route(1501L, 0L, 3, "", "", "", "登录日志"));
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey"));
    }

    @Test
    void sharedPermissionRoundTripRetainsOnlyTheSelectedPqcPage() {
        MenuDO production = route(101L, 0L, 2, "/production", "Production", "", "生产");
        MenuDO pqc = route(202L, 0L, 2, "/pqc", "Pqc", "", "PQC");
        production.setPermission("mes:batch:query");
        pqc.setPermission("mes:batch:query");
        RoleConfigPackageServiceImpl service = serviceWithMenus(production, pqc);
        Map<Long, String> exported = ReflectionTestUtils.invokeMethod(service, "buildMenuKeyById");
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setMenuKeys(List.of(exported.get(202L)));
        assertEquals(Set.of(202L), ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L, 202L)));
        item.setMenuKeys(List.of("permission:mes:batch:query"));
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L, 202L)));
    }

    @Test
    void sharedLeaderButtonDoesNotGrantProductionPageOrOtherParentButtons() {
        MenuDO production = route(101L, 0L, 2, "/production", "Production", "", "生产组长");
        MenuDO leader = route(202L, 0L, 2, "/pqc-leader", "PqcLeader", "", "PQC组长");
        MenuDO button = route(303L, 202L, 3, "", "", "", "查询");
        production.setPermission("mes:leader:query");
        button.setPermission("mes:leader:query");
        RoleConfigPackageServiceImpl service = serviceWithMenus(production, leader, button);
        Map<Long, String> exported = ReflectionTestUtils.invokeMethod(service, "buildMenuKeyById");
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setMenuKeys(List.of(exported.get(303L)));
        assertEquals(Set.of(303L), ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L, 202L, 303L)));
    }

    @Test
    void equivalentButtonsRemainWithinTheirOwnParentPage() {
        MenuDO leader = route(101L, 0L, 2, "/leader", "Leader", "", "Leader");
        MenuDO other = route(202L, 0L, 2, "/other", "Other", "", "Other");
        MenuDO first = route(301L, 101L, 3, "", "", "", "Query");
        MenuDO duplicate = route(302L, 101L, 3, "", "", "", "Status query");
        MenuDO foreign = route(303L, 202L, 3, "", "", "", "Query");
        for (MenuDO button : List.of(first, duplicate, foreign)) {
            button.setPermission("mes:leader:query");
        }
        RoleConfigPackageServiceImpl service = serviceWithMenus(leader, other, first, duplicate, foreign);
        Map<Long, String> exported = ReflectionTestUtils.invokeMethod(service, "buildMenuKeyById");
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setMenuKeys(List.of(exported.get(301L)));
        assertEquals(Set.of(301L, 302L), ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item,
                mapping, Set.of(101L, 202L, 301L, 302L, 303L)));
    }

    @Test
    void duplicateRouteReportsActualKeyAndConflictingMenuIds() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(menu(101L, ""), menu(202L, ""));

        ServiceException error = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey"));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), error.getCode());
        assertTrue(error.getMessage().contains("目标环境存在重复菜单标识【route:"));
        assertTrue(error.getMessage().contains("101"));
        assertTrue(error.getMessage().contains("202"));
        assertFalse(error.getMessage().contains("{}"));
    }

    @Test
    void uniquePermissionsResolveToTargetMenuIds() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(
                menu(101L, "system:role:query"), menu(202L, "system:role:update"));

        Map<String, Set<Long>> result = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");

        assertEquals(Map.of("permission:system:role:query", Set.of(101L),
                "permission:system:role:update", Set.of(202L)), result);
    }

    @Test
    void missingPermissionStillFailsWithoutGrantingOtherMenus() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(menu(101L, "system:role:query"));
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setCode("scheduler");
        item.setMenuKeys(List.of("permission:system:role:update"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L)));
        assertTrue(error.getMessage().contains("permission:system:role:update"));
        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), error.getCode());
    }

    @Test
    void renamedDirectoryAndPageResolveTheSameRouteInsideTenantPackage() {
        MenuDO directory = route(101L, 0L, 1, "/mes/edhr-process-routes", "", "", "工艺路线");
        MenuDO page = route(202L, 101L, 2, "/mes/pro/route", "mes/pro/route/index", "MesProRoute", "工艺路线版本");
        RoleConfigPackageServiceImpl service = serviceWithMenus(directory, page);
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setMenuKeys(List.of("route:route:|1|/mes/edhr-process-routes|||工艺流程|2|/mes/pro/route|mes/pro/route/index|MesProRoute|工艺流程"));

        assertEquals(Set.of(202L), ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L, 202L)));
        ServiceException outsidePackage = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L)));
        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID.getCode(), outsidePackage.getCode());
    }

    @Test
    void sameLabelDoesNotResolveADifferentRouteOrComponent() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(
                route(101L, 0L, 2, "/mes/pro/route", "mes/pro/route/index", "MesProRoute", "工艺流程"));
        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        for (String key : List.of(
                "route:|2|/mes/pro/other|mes/pro/route/index|MesProRoute|工艺流程",
                "route:|2|/mes/pro/route|mes/pro/other/index|MesProRoute|工艺流程",
                "route:permission:other:query|2|/mes/pro/route|mes/pro/route/index|MesProRoute|工艺流程")) {
            RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
            item.setMenuKeys(List.of(key));
            ServiceException error = assertThrows(ServiceException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L)));
            assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), error.getCode());
        }
    }

    @Test
    void duplicateStructuralRoutesRemainAmbiguousAfterLabelChanges() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(
                route(101L, 0L, 1, "/mes/routes", "", "", "工艺流程"),
                route(202L, 0L, 1, "/mes/routes", "", "", "工艺路线"));
        ServiceException error = assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey"));
        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_REFERENCE_MISSING.getCode(), error.getCode());
        assertTrue(error.getMessage().contains("101"));
        assertTrue(error.getMessage().contains("202"));
    }

    private MenuDO route(Long id, Long parentId, int type, String path, String component, String componentName, String name) {
        MenuDO menu = menu(id, "");
        menu.setParentId(parentId);
        menu.setType(type);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setComponentName(componentName);
        menu.setName(name);
        return menu;
    }

    private RoleConfigPackageServiceImpl serviceWithMenus(MenuDO... menus) {
        MenuMapper mapper = mock(MenuMapper.class);
        when(mapper.selectList()).thenReturn(List.of(menus));
        RoleConfigPackageServiceImpl service = new RoleConfigPackageServiceImpl();
        ReflectionTestUtils.setField(service, "menuMapper", mapper);
        return service;
    }

    private MenuDO menu(Long id, String permission) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setPermission(permission);
        return menu;
    }
}
