package cn.iocoder.yudao.module.infra.service.backupplan;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsBackupPlanSchedulerGatewayTest {

    @Test
    void getStatusShouldAggregateFullAndIncrementalTasks() {
        List<List<String>> commands = new ArrayList<>();
        WindowsBackupPlanSchedulerGateway gateway = new WindowsBackupPlanSchedulerGateway(command -> {
            commands.add(command);
            String script = command.get(command.size() - 1);
            String nextRun = script.contains("Backup Full") ? "2026-07-27T01:00:00" : "2026-07-26T02:00:00";
            return new WindowsBackupPlanSchedulerGateway.CommandResult(0, """
                    {"enabled":true,"nextRunTime":"%s","lastRunTime":"2026-07-25T02:00:00",\
                    "lastResultCode":0,"taskToRun":"powershell.exe -File E:\\\\IntRuoyi\\\\IntRuoyiBackend\\\\script\\\\backup-ops\\\\scripts\\\\backup-ops.ps1"}
                    """.formatted(nextRun));
        });

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertTrue(commands.stream().anyMatch(command -> command.get(command.size() - 1).contains("IntRuoyi Backup Full")));
        assertTrue(commands.stream().anyMatch(command -> command.get(command.size() - 1).contains("IntRuoyi Backup Incremental")));
        assertTrue(status.getEnabled());
        assertEquals(LocalDateTime.of(2026, 7, 26, 2, 0), status.getNextRunTime());
        assertEquals(0, status.getLastResultCode());
    }

    @Test
    void enableAndDisableShouldChangeBothTasks() {
        List<List<String>> commands = new ArrayList<>();
        WindowsBackupPlanSchedulerGateway gateway = new WindowsBackupPlanSchedulerGateway(command -> {
            commands.add(command);
            return new WindowsBackupPlanSchedulerGateway.CommandResult(0, "");
        });

        gateway.enable();
        gateway.disable();

        assertTrue(commands.stream().anyMatch(command -> command.get(command.size() - 1)
                .contains("Enable-ScheduledTask -TaskName 'IntRuoyi Backup Full'")));
        assertTrue(commands.stream().anyMatch(command -> command.get(command.size() - 1)
                .contains("Enable-ScheduledTask -TaskName 'IntRuoyi Backup Incremental'")));
        assertTrue(commands.stream().anyMatch(command -> command.get(command.size() - 1)
                .contains("Disable-ScheduledTask -TaskName 'IntRuoyi Backup Full'")));
        assertTrue(commands.stream().anyMatch(command -> command.get(command.size() - 1)
                .contains("Disable-ScheduledTask -TaskName 'IntRuoyi Backup Incremental'")));
    }

    @Test
    void registerOrUpdateShouldPropagateRepositoryEnvironmentToRegistrar() {
        List<List<String>> commands = new ArrayList<>();
        WindowsBackupPlanSchedulerGateway gateway = new WindowsBackupPlanSchedulerGateway(command -> {
            commands.add(command);
            return new WindowsBackupPlanSchedulerGateway.CommandResult(0, "");
        });
        BackupPlanSchedule schedule = new BackupPlanSchedule();
        schedule.setRegisterScriptPath(Path.of("script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1"));
        schedule.setConfigPath(Path.of("script/backup-ops/config/backup-ops.config.json"));
        schedule.setRepositoryEnvironment("test");

        gateway.registerOrUpdate(schedule);

        List<String> command = commands.get(0);
        int repositoryArgumentIndex = command.indexOf("-RepositoryEnvironment");
        assertTrue(repositoryArgumentIndex > 0);
        assertEquals("test", command.get(repositoryArgumentIndex + 1));
    }

    @Test
    void getStatusShouldExposeQueryExitCodeAndFailureReason() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(1, "ERROR: task missing");

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertEquals(1, status.getQueryExitCode());
        assertFalse(status.getEnabled());
        assertTrue(status.getBlockedReason().contains("task missing"));
    }

    @Test
    void getStatusShouldMarkDisabledTaskAsBlockedState() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0,
                schedulerJson(false, "2026-07-26T01:30:00", 0,
                        "powershell.exe -File E:\\IntRuoyi\\IntRuoyiBackend\\script\\backup-ops\\scripts\\backup-ops.ps1"));

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertFalse(status.getEnabled());
        assertEquals("计划任务已禁用", status.getBlockedReason());
    }

    @Test
    void getStatusShouldBlockWhenNextRunTimeIsMissing() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0,
                schedulerJson(true, null, 0,
                        "powershell.exe -File E:\\IntRuoyi\\IntRuoyiBackend\\script\\backup-ops\\scripts\\backup-ops.ps1"));

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertTrue(status.getEnabled());
        assertEquals("下次运行时间缺失", status.getBlockedReason());
    }

    @Test
    void getStatusShouldExposeLastResultAndTaskCommandForServiceHealth() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0,
                schedulerJson(true, "2026-07-26T01:30:00", 1,
                        "powershell.exe -File E:\\IntRuoyi\\IntRuoyiBackend\\script\\backup-ops\\scripts\\backup-ops.ps1"));

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertTrue(status.getEnabled());
        assertEquals(LocalDateTime.of(2026, 7, 26, 1, 30), status.getNextRunTime());
        assertEquals(1, status.getLastResultCode());
        assertTrue(status.getTaskToRun().contains("backup-ops.ps1"));
    }

    @Test
    void getStatusShouldBlockWhenTaskCommandDoesNotPointToBackupOps() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0,
                schedulerJson(true, "2026-07-26T01:30:00", 0,
                        "powershell.exe -File E:\\legacy\\legacy-backup.ps1"));

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertEquals("计划任务脚本路径异常", status.getBlockedReason());
    }

    private WindowsBackupPlanSchedulerGateway gatewayReturning(int exitCode, String output) {
        return new WindowsBackupPlanSchedulerGateway(command ->
                new WindowsBackupPlanSchedulerGateway.CommandResult(exitCode, output));
    }

    private String schedulerJson(boolean enabled, String nextRunTime, int lastResultCode, String taskToRun) {
        String nextRunJson = nextRunTime == null ? "null" : "\"" + nextRunTime + "\"";
        return "{\"enabled\":" + enabled + ",\"nextRunTime\":" + nextRunJson
                + ",\"lastRunTime\":\"2026-07-25T01:30:00\",\"lastResultCode\":" + lastResultCode
                + ",\"taskToRun\":\"" + taskToRun.replace("\\", "\\\\") + "\"}";
    }
}
