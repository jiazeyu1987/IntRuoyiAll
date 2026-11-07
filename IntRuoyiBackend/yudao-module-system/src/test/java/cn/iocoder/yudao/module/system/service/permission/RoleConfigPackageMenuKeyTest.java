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
    void sharedPageAndButtonPermissionResolvesBothMenus() {
        RoleConfigPackageServiceImpl service = serviceWithMenus(
                menu(101L, "system:role:query"), menu(202L, "system:role:query"));

        Map<String, Set<Long>> mapping = ReflectionTestUtils.invokeMethod(service, "buildMenuIdByKey");
        RoleConfigPackageServiceImpl.RoleConfigItem item = new RoleConfigPackageServiceImpl.RoleConfigItem();
        item.setMenuKeys(List.of("permission:system:role:query", "permission:system:role:query"));
        Set<Long> resolved = ReflectionTestUtils.invokeMethod(service, "resolveMenuIds", item, mapping, Set.of(101L, 202L));

        assertEquals(Set.of(101L, 202L), resolved);
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
