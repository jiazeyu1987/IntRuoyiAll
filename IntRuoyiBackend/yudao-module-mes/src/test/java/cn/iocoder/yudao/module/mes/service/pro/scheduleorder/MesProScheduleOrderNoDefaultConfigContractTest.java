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
        String autoScheduleService = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "schedule",
                "MesProAutoScheduleServiceImpl.java"), StandardCharsets.UTF_8);
        String scheduleDefaultCompatibilityPolicy = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "schedule", "component",
                "ScheduleDefaultCompatibilityPolicy.java"), StandardCharsets.UTF_8);
        String routeProcessController = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "controller", "admin", "pro", "route",
                "MesProRouteProcessController.java"), StandardCharsets.UTF_8);
        String routeResourceService = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "route",
                "MesProRouteResourceServiceImpl.java"), StandardCharsets.UTF_8);
        String routeResourceContract = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "route",
                "MesProRouteResourceService.java"), StandardCharsets.UTF_8);
        String routeResourceController = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "controller", "admin", "pro", "route",
                "MesProRouteResourceController.java"), StandardCharsets.UTF_8);
        String errorCodeConstants = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "enums", "ErrorCodeConstants.java"), StandardCharsets.UTF_8);

        assertTrue(scheduleDefaultCompatibilityPolicy.contains("DEFAULT_SHIFT_HOURS"));
        assertTrue(scheduleDefaultCompatibilityPolicy.contains("new BigDecimal(\"10.5\")"));
        assertTrue(scheduleOrderService.contains("scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing()"));
        assertTrue(autoScheduleService.contains("scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing()"));
        assertFalse(scheduleOrderService.contains("DEFAULT_WORKER_QUANTITY"));
        assertFalse(routeProcessController.contains("DEFAULT_SHIFT_HOURS"));
        assertFalse(routeProcessController.contains("DEFAULT_WORKER_QUANTITY"));
        assertFalse(routeResourceService.contains("DEFAULT_SHIFT_HOURS"));
        assertTrue(routeProcessController.contains("PRO_ROUTE_SCHEDULE_SHIFT_HOURS_REQUIRED"));
        assertTrue(routeResourceContract.contains(
                "PageResult<MesProRouteResourceRespVO> getResourcePage(@Valid MesProRouteResourcePageReqVO pageReqVO)"));
        assertTrue(routeResourceController.contains("@GetMapping(\"/page\")"));
        assertFalse(routeResourceController.contains("@PostMapping"));
        assertFalse(routeResourceController.contains("@PutMapping"));
        assertFalse(routeResourceController.contains("@DeleteMapping"));
        assertTrue(errorCodeConstants.contains("PRO_ROUTE_RESOURCE_READONLY"));
        assertTrue(errorCodeConstants.contains("排产资源缺少班次小时"));
        assertTrue(errorCodeConstants.contains("排产资源缺少人员数量"));
    }
}
