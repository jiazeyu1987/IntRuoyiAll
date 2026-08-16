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
    void registerOrUpdateShouldPropagateRepositoryEnvironmentToRegistrar() {
        List<List<String>> commands = new ArrayList<>();
        WindowsBackupPlanSchedulerGateway gateway = new WindowsBackupPlanSchedulerGateway(command -> {
            commands.add(command);
            return new WindowsBackupPlanSchedulerGateway.CommandResult(0, "");
        });
        BackupPlanSchedule schedule = new BackupPlanSchedule();
        schedule.setRegisterScriptPath(Path.of("script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1"));
        schedule.setConfigPath(Path.of("script/backup-ops/config/backup-ops.config.json"));
        schedule.setRepositoryEnvironment("backup");

        gateway.registerOrUpdate(schedule);

        List<String> command = commands.get(0);
        int repositoryArgumentIndex = command.indexOf("-RepositoryEnvironment");
        assertTrue(repositoryArgumentIndex > 0);
        assertEquals("backup", command.get(repositoryArgumentIndex + 1));
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
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0, """
                Status: Disabled
                Next Run Time: 2026/7/26 1:30:00
                Last Run Time: 2026/7/25 1:30:00
                Last Result: 0
                Task To Run: powershell.exe -File E:\\IntRuoyi\\IntRuoyiBackend\\script\\backup-ops\\scripts\\backup-ops.ps1
                """);

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertFalse(status.getEnabled());
        assertEquals("计划任务已禁用", status.getBlockedReason());
    }

    @Test
    void getStatusShouldBlockWhenNextRunTimeIsMissing() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0, """
                Status: Ready
                Next Run Time: N/A
                Last Run Time: 2026/7/25 1:30:00
                Last Result: 0
                Task To Run: powershell.exe -File E:\\IntRuoyi\\IntRuoyiBackend\\script\\backup-ops\\scripts\\backup-ops.ps1
                """);

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertTrue(status.getEnabled());
        assertEquals("下次运行时间缺失", status.getBlockedReason());
    }

    @Test
    void getStatusShouldExposeLastResultAndTaskCommandForServiceHealth() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0, """
                Status: Ready
                Next Run Time: 2026/7/26 1:30:00
                Last Run Time: 2026/7/25 1:30:00
                Last Result: 1
                Task To Run: powershell.exe -File E:\\IntRuoyi\\IntRuoyiBackend\\script\\backup-ops\\scripts\\backup-ops.ps1
                """);

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertTrue(status.getEnabled());
        assertEquals(LocalDateTime.of(2026, 7, 26, 1, 30), status.getNextRunTime());
        assertEquals(1, status.getLastResultCode());
        assertTrue(status.getTaskToRun().contains("backup-ops.ps1"));
    }

    @Test
    void getStatusShouldBlockWhenTaskCommandDoesNotPointToBackupOps() {
        WindowsBackupPlanSchedulerGateway gateway = gatewayReturning(0, """
                Status: Ready
                Next Run Time: 2026/7/26 1:30:00
                Last Run Time: 2026/7/25 1:30:00
                Last Result: 0
                Task To Run: powershell.exe -File E:\\legacy\\legacy-backup.ps1
                """);

        BackupPlanSchedulerStatus status = gateway.getStatus();

        assertEquals("计划任务脚本路径异常", status.getBlockedReason());
    }

    private WindowsBackupPlanSchedulerGateway gatewayReturning(int exitCode, String output) {
        return new WindowsBackupPlanSchedulerGateway(command ->
                new WindowsBackupPlanSchedulerGateway.CommandResult(exitCode, output));
    }
}
