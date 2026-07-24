package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleOrderNoDefaultConfigContractTest {

    @Test
    void schedulingCode_shouldUseExplicitDefaultShiftHoursOnly() throws IOException {
        String scheduleOrderService = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "scheduleorder",
                "MesProScheduleOrderServiceImpl.java"), StandardCharsets.UTF_8);
        String routeProcessController = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "controller", "admin", "pro", "route",
                "MesProRouteProcessController.java"), StandardCharsets.UTF_8);
        String routeResourceService = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "route",
                "MesProRouteResourceServiceImpl.java"), StandardCharsets.UTF_8);
        String errorCodeConstants = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "enums", "ErrorCodeConstants.java"), StandardCharsets.UTF_8);

        assertFalse(scheduleOrderService.contains("DEFAULT_SHIFT_HOURS"));
        assertFalse(scheduleOrderService.contains("DEFAULT_WORKER_QUANTITY"));
        assertFalse(routeProcessController.contains("DEFAULT_SHIFT_HOURS"));
        assertFalse(routeProcessController.contains("DEFAULT_WORKER_QUANTITY"));
        assertFalse(routeResourceService.contains("DEFAULT_SHIFT_HOURS"));
        assertTrue(scheduleOrderService.contains("PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED"));
        assertTrue(routeProcessController.contains("PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED"));
        assertTrue(routeResourceService.contains("PRO_ROUTE_RESOURCE_READONLY"));
        assertTrue(errorCodeConstants.contains("排产资源缺少班次小时"));
        assertTrue(errorCodeConstants.contains("排产资源缺少人员数量"));
    }
}
