package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleReplanApprovalContractTest {

    @Test
    void manualReplanMustNotBeApprovalBackedFormCenterAction() throws Exception {
        Path executorPath = Path.of("src", "main", "java", "cn", "iocoder", "yudao", "module", "mes",
                "service", "pro", "schedule", "MesProScheduleReplanFormEffectExecutor.java");
        assertFalse(Files.exists(executorPath),
                "manual replan is confirmed as non-approval action and must not register MES_SCHEDULE_REPLAN executor");

        Path approvalSeedPath = Path.of("..", "sql", "mysql", "20260720_mes_schedule_replan_form_policy_seed.sql");
        String approvalSeed = Files.readString(approvalSeedPath, StandardCharsets.UTF_8);
        assertTrue(approvalSeed.contains("'MES_SCHEDULE_REPLAN'"),
                "historical seed is kept only so the retirement migration can be verified");

        Path retirePath = Path.of("..", "sql", "mysql", "20260721_mes_schedule_replan_approval_retire.sql");
        assertTrue(Files.exists(retirePath),
                "manual replan approval policy must have a release-managed retirement migration");

        String retireSql = Files.readString(retirePath, StandardCharsets.UTF_8);
        assertTrue(retireSql.contains("bpm_business_approval_policy"));
        assertFalse(retireSql.contains("bpm_form_action_policy"));
        assertTrue(retireSql.contains("'MES_SCHEDULE_REPLAN'"));
        assertTrue(retireSql.contains("'SCHEDULE_REPLAN_SCOPE'"));
        assertTrue(retireSql.contains("'REPLAN'"));
        assertTrue(retireSql.contains("status` = 'DISABLED'"));
        assertTrue(retireSql.contains("SIGNAL SQLSTATE '45000'"));
    }
}
