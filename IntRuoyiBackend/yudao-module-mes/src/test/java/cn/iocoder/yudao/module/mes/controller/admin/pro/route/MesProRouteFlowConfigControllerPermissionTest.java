package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig.MesProRouteFlowConfigSaveReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesProRouteFlowConfigControllerPermissionTest {

    @Test
    void controller_registersFlowConfigBaseRoute() {
        RequestMapping mapping = MesProRouteFlowConfigController.class.getAnnotation(RequestMapping.class);

        assertNotNull(mapping);
        assertEquals("/mes/pro/route/flow-config", mapping.value()[0]);
    }

    @Test
    void processConfigList_registersGetEndpointAndQueryPermissions() throws Exception {
        Method method = MesProRouteFlowConfigController.class.getDeclaredMethod(
                "getRouteFlowProcessConfigList", Long.class, String.class, Long.class);

        assertNotNull(method.getAnnotation(GetMapping.class));
        assertEquals(
                "@ss.hasAnyPermissions('mes:pro-route:schedule-config:query', 'mes:pro-route:batch-record-config:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void saveSchedule_registersScheduleSaveEndpointAndUpdatePermission() throws Exception {
        Method method = MesProRouteFlowConfigController.class.getDeclaredMethod(
                "saveRouteFlowScheduleConfig", MesProRouteFlowConfigSaveReqVO.class);

        assertEquals("/schedule/save", method.getAnnotation(PostMapping.class).value()[0]);
        assertEquals(
                "@ss.hasPermission('mes:pro-route:schedule-config:update')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void saveBatchRecord_registersBatchRecordSaveEndpointAndUpdatePermission() throws Exception {
        Method method = MesProRouteFlowConfigController.class.getDeclaredMethod(
                "saveRouteFlowBatchRecordConfig", MesProRouteFlowConfigSaveReqVO.class);

        assertEquals("/batch-record/save", method.getAnnotation(PostMapping.class).value()[0]);
        assertEquals(
                "@ss.hasPermission('mes:pro-route:batch-record-config:update')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
