package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProAutoScheduleAuditPermissionContractTest {

    @Test
    void applyAndReplan_shouldUseDedicatedPermissionAndWriteScheduleEventAudit() throws IOException {
        String controller = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "controller", "admin", "pro", "schedule",
                "MesProAutoScheduleController.java"), StandardCharsets.UTF_8);
        String service = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "service", "pro", "schedule",
                "MesProAutoScheduleServiceImpl.java"), StandardCharsets.UTF_8);

        assertTrue(controller.contains("mes:pro-auto-schedule:preview"));
        assertTrue(controller.contains("mes:pro-auto-schedule:apply"));
        assertTrue(controller.contains("mes:pro-auto-schedule:replan"));
        assertFalse(controller.contains("hasAnyPermissions('mes:pro-task:create', 'mes:pro-schedule-order:create')"));
        assertTrue(service.contains("AUTO_APPLY"));
        assertTrue(service.contains("REPLAN_APPLY"));
        assertTrue(service.contains("createdTaskIds"));
        assertTrue(service.contains("deletedTaskIds"));
        assertTrue(service.contains("preservedTaskIds"));
    }
}
