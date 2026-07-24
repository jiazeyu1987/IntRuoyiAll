package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCopyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProRouteControllerPermissionTest {

    @Test
    void getRoutePage_allowsProcessUseRouteQueryPermissionsOnlyForRead() throws Exception {
        Method method = MesProRouteController.class.getDeclaredMethod("getRoutePage", MesProRoutePageReqVO.class);

        assertEquals(
                "@ss.hasAnyPermissions('mes:pro-route:query', 'mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void originalRouteMutations_keepOriginalRoutePermissions() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-route:create')",
                MesProRouteController.class.getDeclaredMethod(
                        "createRoute",
                        cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:update')",
                MesProRouteController.class.getDeclaredMethod(
                        "updateRoute",
                        cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:delete')",
                MesProRouteController.class.getDeclaredMethod("deleteRoute", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void copyRoute_keepsOriginalRouteCreatePermission() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-route:create')",
                MesProRouteController.class.getDeclaredMethod("copyRoute", MesProRouteCopyReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void scheduleConfigEndpoints_useScheduleRoutePermissions() throws Exception {
        assertEquals("@ss.hasPermission('mes:pro-route:schedule-config:update')",
                MesProRouteScheduleConfigController.class.getDeclaredMethod(
                        "saveConfig", MesProRouteScheduleConfigSaveReqVO.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:schedule-config:query')",
                MesProRouteScheduleConfigController.class.getDeclaredMethod(
                        "getConfigListByRouteVersion", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
    }
}
