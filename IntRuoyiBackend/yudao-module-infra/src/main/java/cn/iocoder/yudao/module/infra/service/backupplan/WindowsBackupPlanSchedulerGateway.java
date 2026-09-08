package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_COMMAND_FAILED;

@Component
public class WindowsBackupPlanSchedulerGateway implements BackupPlanSchedulerGateway {

    private static final List<String> TASK_NAMES = List.of("IntRuoyi Backup Full", "IntRuoyi Backup Incremental");
    private final CommandRunner commandRunner;
    private final BooleanSupplier windowsSupplier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WindowsBackupPlanSchedulerGateway() {
        this(WindowsBackupPlanSchedulerGateway::runProcessCommand,
                () -> System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"));
    }

    WindowsBackupPlanSchedulerGateway(CommandRunner commandRunner) {
        this(commandRunner, () -> true);
    }

    WindowsBackupPlanSchedulerGateway(CommandRunner commandRunner, BooleanSupplier windowsSupplier) {
        this.commandRunner = commandRunner;
        this.windowsSupplier = windowsSupplier;
    }

    @Override
    public BackupPlanSchedulerStatus getStatus() {
        List<BackupPlanSchedulerStatus> statuses = TASK_NAMES.stream().map(this::getTaskStatus).toList();
        BackupPlanSchedulerStatus result = new BackupPlanSchedulerStatus();
        result.setEnabled(statuses.stream().allMatch(status -> Boolean.TRUE.equals(status.getEnabled())));
        result.setNextRunTime(statuses.stream().map(BackupPlanSchedulerStatus::getNextRunTime)
                .filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(null));
        result.setLastRunTime(statuses.stream().map(BackupPlanSchedulerStatus::getLastRunTime)
                .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
        result.setQueryExitCode(statuses.stream().map(BackupPlanSchedulerStatus::getQueryExitCode)
                .filter(java.util.Objects::nonNull).filter(code -> code != 0).findFirst().orElse(0));
        result.setLastResultCode(statuses.stream().map(BackupPlanSchedulerStatus::getLastResultCode)
                .filter(java.util.Objects::nonNull).filter(code -> code != 0).findFirst()
                .orElseGet(() -> statuses.stream().map(BackupPlanSchedulerStatus::getLastResultCode)
                        .filter(java.util.Objects::nonNull).findFirst().orElse(null)));
        result.setTaskToRun(String.join(System.lineSeparator(), statuses.stream()
                .map(BackupPlanSchedulerStatus::getTaskToRun).filter(StrUtil::isNotBlank).toList()));
        result.setRawStatus(String.join(System.lineSeparator(), statuses.stream()
                .map(BackupPlanSchedulerStatus::getRawStatus).filter(StrUtil::isNotBlank).toList()));
        result.setBlockedReason(statuses.stream().map(BackupPlanSchedulerStatus::getBlockedReason)
                .filter(StrUtil::isNotBlank).findFirst().orElse(null));
        return result;
    }

    private BackupPlanSchedulerStatus getTaskStatus(String taskName) {
        assertWindows();
        String escapedTaskName = taskName.replace("'", "''");
        String script = "$task=Get-ScheduledTask -TaskName '" + escapedTaskName + "';"
                + "$info=Get-ScheduledTaskInfo -TaskName '" + escapedTaskName + "';"
                + "$next=if($info.NextRunTime -gt [datetime]::MinValue){$info.NextRunTime.ToString('o')}else{$null};"
                + "$last=if($info.LastRunTime -gt [datetime]::MinValue){$info.LastRunTime.ToString('o')}else{$null};"
                + "$run=(($task.Actions | ForEach-Object { $_.Execute + ' ' + $_.Arguments }) -join [Environment]::NewLine);"
                + "[pscustomobject]@{enabled=($task.State -ne 'Disabled');nextRunTime=$next;lastRunTime=$last;"
                + "lastResultCode=[int]$info.LastTaskResult;taskToRun=$run}|ConvertTo-Json -Compress";
        CommandResult commandResult = runCommandResult(powerShellCommand(script));
        String output = commandResult.output();
        BackupPlanSchedulerStatus status = new BackupPlanSchedulerStatus();
        status.setRawStatus(output);
        status.setQueryExitCode(commandResult.exitCode());
        if (commandResult.exitCode() != 0) {
            status.setEnabled(false);
            status.setBlockedReason(StrUtil.blankToDefault(output, "计划任务查询失败"));
            return status;
        }
        if (StrUtil.isBlank(output)) {
            status.setEnabled(false);
            status.setBlockedReason("计划任务查询无输出");
            return status;
        }
        String taskToRun;
        try {
            JsonNode task = objectMapper.readTree(output);
            status.setEnabled(task.path("enabled").asBoolean(false));
            status.setNextRunTime(parseDateTime(task.path("nextRunTime")));
            status.setLastRunTime(parseDateTime(task.path("lastRunTime")));
            status.setLastResultCode(task.path("lastResultCode").isInt()
                    ? task.path("lastResultCode").asInt() : null);
            taskToRun = task.path("taskToRun").asText("");
            status.setTaskToRun(taskToRun);
        } catch (IOException | RuntimeException ex) {
            status.setEnabled(false);
            status.setBlockedReason("计划任务状态 JSON 无法解析：" + ex.getMessage());
            return status;
        }
        if (!Boolean.TRUE.equals(status.getEnabled())) {
            status.setBlockedReason("计划任务已禁用");
            return status;
        }
        if (status.getNextRunTime() == null) {
            status.setBlockedReason("下次运行时间缺失");
            return status;
        }
        if (StrUtil.isNotBlank(taskToRun) && !taskToRun.contains("backup-ops.ps1")) {
            status.setBlockedReason("计划任务脚本路径异常");
        }
        return status;
    }

    @Override
    public void registerOrUpdate(BackupPlanSchedule schedule) {
        assertWindows();
        List<String> command = new ArrayList<>();
        command.add("powershell.exe");
        command.add("-NoProfile");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(schedule.getRegisterScriptPath().toString());
        command.add("-ConfigPath");
        command.add(schedule.getConfigPath().toString());
        command.add("-RepositoryEnvironment");
        command.add(schedule.getRepositoryEnvironment());
        runCommand(command, true);
    }

    @Override
    public void enable() {
        assertWindows();
        TASK_NAMES.forEach(taskName -> runCommand(powerShellCommand(
                "Enable-ScheduledTask -TaskName '" + taskName.replace("'", "''") + "' | Out-Null"), true));
    }

    @Override
    public void disable() {
        assertWindows();
        TASK_NAMES.forEach(taskName -> runCommand(powerShellCommand(
                "Disable-ScheduledTask -TaskName '" + taskName.replace("'", "''") + "' | Out-Null"), true));
    }

    private void assertWindows() {
        if (!windowsSupplier.getAsBoolean()) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "当前服务器不支持 Windows 计划任务控制，请先实现 Linux 调度器");
        }
    }

    private String runCommand(List<String> command, boolean failOnError) {
        CommandResult result = runCommandResult(command);
        if (result.exitCode() != 0 && failOnError) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, result.output());
        }
        return result.output();
    }

    private CommandResult runCommandResult(List<String> command) {
        return commandRunner.run(command);
    }

    private static CommandResult runProcessCommand(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "计划任务命令超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), output);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "计划任务命令被中断");
        }
    }

    private List<String> powerShellCommand(String script) {
        return List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script);
    }

    private LocalDateTime parseDateTime(JsonNode value) {
        if (value == null || value.isNull() || !value.isTextual() || StrUtil.isBlank(value.asText())) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.asText());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @FunctionalInterface
    interface CommandRunner {
        CommandResult run(List<String> command);
    }

    record CommandResult(int exitCode, String output) {
    }
}
