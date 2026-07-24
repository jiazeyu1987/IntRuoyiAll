package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProAutoScheduleIssueBackflowContractTest {

    @Test
    void productionExceptionBackflow_shouldExposeCreateResolveAndTraceFields() throws IOException {
        String controller = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "controller", "admin", "pro", "schedule",
                "MesProAutoScheduleController.java"), StandardCharsets.UTF_8);
        String issueDO = Files.readString(Path.of("src", "main", "java", "cn", "iocoder", "yudao",
                "module", "mes", "dal", "dataobject", "pro", "schedule",
                "MesProScheduleIssueDO.java"), StandardCharsets.UTF_8);
        String sql = Files.readString(Path.of("..", "sql", "mysql",
                "20260624_mes_schedule_issue_structured_backflow.sql"), StandardCharsets.UTF_8);

        assertTrue(controller.contains("@PostMapping(\"/issues\")"), "必须提供生产异常登记接口");
        assertTrue(controller.contains("@PutMapping(\"/issues/resolve\")"), "必须提供生产异常关闭接口");
        for (String field : new String[]{"sourceType", "sourceId", "status", "resolutionReason", "resolvedBy", "resolvedAt"}) {
            assertTrue(issueDO.contains(field), "问题表对象必须包含追溯字段: " + field);
        }
        for (String column : new String[]{"source_type", "source_id", "status", "resolution_reason", "resolved_by", "resolved_at"}) {
            assertTrue(sql.contains(column), "结构化回流迁移必须包含字段: " + column);
        }
    }
}
