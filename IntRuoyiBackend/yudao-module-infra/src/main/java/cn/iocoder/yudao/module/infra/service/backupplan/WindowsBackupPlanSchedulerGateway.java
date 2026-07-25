package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_COMMAND_FAILED;

@Component
public class WindowsBackupPlanSchedulerGateway implements BackupPlanSchedulerGateway {

    private static final String TASK_NAME = "IntRuoyi Backup Scheduled";
    private static final DateTimeFormatter SCHTASKS_DATE_TIME = DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss");

    @Override
    public BackupPlanSchedulerStatus getStatus() {
        assertWindows();
        CommandResult commandResult = runCommandResult(List.of("schtasks", "/Query", "/TN", TASK_NAME, "/V", "/FO", "LIST"));
        String output = commandResult.output();
        BackupPlanSchedulerStatus status = new BackupPlanSchedulerStatus();
        status.setRawStatus(output);
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
        status.setEnabled(!containsLineValue(output, "Status", "Disabled")
                && !containsLineValue(output, "Scheduled Task State", "Disabled"));
        status.setNextRunTime(parseDateTime(valueOf(output, "Next Run Time")));
        status.setLastRunTime(parseDateTime(valueOf(output, "Last Run Time")));
        status.setLastResultCode(parseInteger(valueOf(output, "Last Result")));
        String taskToRun = valueOf(output, "Task To Run");
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
        runCommand(command, true);
    }

    @Override
    public void enable() {
        assertWindows();
        runCommand(List.of("schtasks", "/Change", "/TN", TASK_NAME, "/ENABLE"), true);
    }

    @Override
    public void disable() {
        assertWindows();
        runCommand(List.of("schtasks", "/Change", "/TN", TASK_NAME, "/DISABLE"), true);
    }

    private void assertWindows() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
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

    private boolean containsLineValue(String output, String key, String expectedValue) {
        return expectedValue.equalsIgnoreCase(valueOf(output, key));
    }

    private String valueOf(String output, String key) {
        for (String line : output.split("\\R")) {
            int index = line.indexOf(':');
            if (index <= 0) {
                continue;
            }
            String lineKey = line.substring(0, index).trim();
            if (key.equalsIgnoreCase(lineKey)) {
                return line.substring(index + 1).trim();
            }
        }
        return "";
    }

    private LocalDateTime parseDateTime(String value) {
        if (StrUtil.isBlank(value) || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, SCHTASKS_DATE_TIME);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record CommandResult(int exitCode, String output) {
    }
}
