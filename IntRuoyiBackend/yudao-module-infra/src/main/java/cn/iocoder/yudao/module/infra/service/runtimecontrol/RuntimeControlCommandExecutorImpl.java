package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_COMMAND_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_SCRIPT_NOT_EXISTS;

@Component
public class RuntimeControlCommandExecutorImpl implements RuntimeControlCommandExecutor {

    private static final Duration RESTART_COMMAND_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration OPERATION_COMMAND_TIMEOUT = Duration.ofHours(2);
    private static final Duration DETACHED_OPERATION_START_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PROCESS_TERMINATION_TIMEOUT = Duration.ofSeconds(10);
    private static final boolean WINDOWS = System.getProperty("os.name")
            .toLowerCase(Locale.ROOT).contains("win");

    @Resource
    private RuntimeControlProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RuntimeControlStatusResult queryStatus(RuntimeControlCommand command) {
        try {
            String output = execute(command, true, null, resolveStatusCommandTimeout());
            if (StrUtil.isBlank(output)) {
                return RuntimeControlStatusResult.error("Status script returned empty output");
            }
            return objectMapper.readValue(output, RuntimeControlStatusResult.class);
        } catch (Exception ex) {
            return RuntimeControlStatusResult.error(ex.getMessage());
        }
    }

    private Duration resolveStatusCommandTimeout() {
        Duration timeout = properties == null ? null : properties.getStatusCommandTimeout();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED,
                    "statusCommandTimeout missing: set yudao.runtime-control.status-command-timeout to a positive duration");
        }
        return timeout;
    }

    @Override
    public String executeForOutput(RuntimeControlCommand command, Duration timeout) {
        return execute(command, true, null, timeout);
    }

    @Override
    public void restart(RuntimeControlCommand command) {
        execute(command, false, null, RESTART_COMMAND_TIMEOUT);
    }

    @Override
    public void executeOperation(RuntimeControlCommand command, Path logPath) {
        execute(command, false, logPath, OPERATION_COMMAND_TIMEOUT);
    }

    @Override
    public void executeDetachedOperation(RuntimeControlCommand command, Path logPath, String operationId,
                                         String successSummary) {
        Path repoRoot = resolveRepoRoot();
        Path script = resolveScript(command.getScriptPath(), repoRoot);
        if (!Files.isRegularFile(script)) {
            throw exception(RUNTIME_CONTROL_SCRIPT_NOT_EXISTS, script.toString());
        }
        List<String> commandLine = buildCommandLine(script);
        commandLine.addAll(command.getArguments());
        prepareOperationLog(command, logPath, commandLine);
        Path runnerScript = writeDetachedRunnerScript(operationId, logPath, commandLine, successSummary);
        List<String> dockerCommand = buildDetachedDockerCommand(operationId, runnerScript);
        String containerId = runCommand(dockerCommand, DETACHED_OPERATION_START_TIMEOUT).trim();
        appendDetachedRunnerStart(logPath, runnerScript, containerId);
    }

    private String execute(RuntimeControlCommand command, boolean captureOutput, Path logPath, Duration timeout) {
        Path repoRoot = resolveRepoRoot();
        Path script = resolveScript(command.getScriptPath(), repoRoot);
        if (!Files.isRegularFile(script)) {
            throw exception(RUNTIME_CONTROL_SCRIPT_NOT_EXISTS, script.toString());
        }
        List<String> commandLine = buildCommandLine(script);
        commandLine.addAll(command.getArguments());
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.directory(repoRoot.toFile());
        processBuilder.redirectErrorStream(true);
        prepareOperationLog(command, logPath, commandLine);
        if (logPath != null) {
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
        }
        try {
            Process process = processBuilder.start();
            boolean finished;
            try {
                finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                terminateCommandProcess(process);
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "Command interrupted");
            }
            if (!finished) {
                terminateCommandProcess(process);
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "Command timed out");
            }
            String output = logPath == null
                    ? new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "";
            if (process.exitValue() != 0) {
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED,
                        logPath == null ? output : ("exitCode=" + process.exitValue() + ", log=" + logPath));
            }
            return captureOutput ? output : "";
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        }
    }

    private Path writeDetachedRunnerScript(String operationId, Path logPath, List<String> commandLine,
                                           String successSummary) {
        Path stateDir = Path.of(properties.getStateDir()).normalize();
        Path operationPath = stateDir.resolve(operationId + ".json");
        Path runnerScript = stateDir.resolve("runners").resolve(operationId + ".sh");
        String commandArray = commandLine.stream().map(this::shellQuote).collect(Collectors.joining(" "));
        String script = """
                #!/usr/bin/env bash
                set -euo pipefail

                OPERATION_PATH=%s
                LOG_PATH=%s
                SUCCESS_SUMMARY=%s

                markOperationStatus() {
                  local status="$1"
                  local summary="$2"
                  python3 - "$OPERATION_PATH" "$status" "$summary" <<'PY'
                import json
                import os
                import sys

                path, status, summary = sys.argv[1:4]
                with open(path, "r", encoding="utf-8") as source:
                    data = json.load(source)
                data["status"] = status
                data["summary"] = summary
                tmp_path = path + ".tmp"
                with open(tmp_path, "w", encoding="utf-8") as target:
                    json.dump(data, target, ensure_ascii=False, indent=2)
                    target.write("\\n")
                os.replace(tmp_path, path)
                PY
                }

                COMMAND=(%s)
                set +e
                "${COMMAND[@]}" >> "$LOG_PATH" 2>&1
                exit_code=$?
                set -e

                if [ "$exit_code" -eq 0 ]; then
                  markOperationStatus succeeded "$SUCCESS_SUMMARY"
                  exit 0
                fi

                markOperationStatus failed "exitCode=${exit_code}, log=${LOG_PATH}"
                exit "$exit_code"
                """.formatted(shellQuote(operationPath.toString()), shellQuote(logPath.toString()),
                shellQuote(successSummary), commandArray);
        try {
            Files.createDirectories(runnerScript.getParent());
            Files.writeString(runnerScript, script, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return runnerScript;
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        }
    }

    private List<String> buildDetachedDockerCommand(String operationId, Path runnerScript) {
        List<String> dockerCommand = new ArrayList<>();
        dockerCommand.add("docker");
        dockerCommand.add("run");
        dockerCommand.add("-d");
        dockerCommand.add("--rm");
        dockerCommand.add("--name");
        dockerCommand.add("intruoyi-runtime-control-op-" + sanitizeContainerName(operationId));
        dockerCommand.add("--network");
        dockerCommand.add("host");
        for (String mount : properties.getBackupOps().getLinuxRunnerMounts()) {
            dockerCommand.add("-v");
            dockerCommand.add(mount);
        }
        dockerCommand.add(resolveLinuxRunnerImage());
        dockerCommand.add("bash");
        dockerCommand.add(runnerScript.toString());
        return dockerCommand;
    }

    private String resolveLinuxRunnerImage() {
        Path envPath = Path.of(properties.getBackupOps().getLinuxRuntimeEnvPath()).normalize();
        String imageTag = readEnvValue(envPath, "IMAGE_TAG");
        if (StrUtil.isBlank(imageTag)) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "IMAGE_TAG missing in " + envPath);
        }
        return properties.getBackupOps().getLinuxRunnerImageRepository() + ":" + imageTag;
    }

    private String readEnvValue(Path envPath, String key) {
        if (!Files.isRegularFile(envPath)) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "Runtime env file not found: " + envPath);
        }
        try {
            for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                if (line.startsWith(key + "=")) {
                    return line.substring((key + "=").length()).trim();
                }
            }
            return "";
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        }
    }

    private String runCommand(List<String> commandLine, Duration timeout) {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished;
            try {
                finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                terminateCommandProcess(process);
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "Command interrupted");
            }
            if (!finished) {
                terminateCommandProcess(process);
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "Command timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED, output);
            }
            return output;
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        }
    }

    private void terminateCommandProcess(Process process) {
        InterruptionTracker interruption = new InterruptionTracker(Thread.interrupted());
        long deadlineNanos = System.nanoTime() + PROCESS_TERMINATION_TIMEOUT.toNanos();
        RuntimeException failure = null;
        try {
            boolean terminated = WINDOWS
                    ? terminateWindowsProcessTree(process, deadlineNanos, interruption)
                    : terminateSingleProcess(process, deadlineNanos, interruption);
            if (!terminated) {
                failure = exception(RUNTIME_CONTROL_COMMAND_FAILED,
                        "Command cleanup failed: process did not terminate within " + PROCESS_TERMINATION_TIMEOUT);
            }
        } catch (RuntimeException ex) {
            failure = ex;
        } finally {
            try {
                closeProcessStreams(process);
            } catch (RuntimeException closeFailure) {
                if (failure == null) {
                    failure = closeFailure;
                } else {
                    failure.addSuppressed(closeFailure);
                }
            } finally {
                interruption.restore();
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private boolean terminateWindowsProcessTree(Process process, long deadlineNanos,
                                                InterruptionTracker interruption) {
        Process taskkill = null;
        RuntimeException taskkillFailure = null;
        List<ProcessHandle> descendants = new ArrayList<>(process.toHandle().descendants().toList());
        try {
            taskkill = new ProcessBuilder("taskkill.exe", "/PID", Long.toString(process.pid()), "/T", "/F")
                    .redirectErrorStream(true)
                    .start();
            boolean taskkillTerminated = awaitProcessExit(taskkill, deadlineNanos, interruption);
            String output = taskkillTerminated
                    ? new String(taskkill.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "";
            if (!taskkillTerminated) {
                taskkill.destroyForcibly();
                awaitProcessExit(taskkill, System.nanoTime() + Duration.ofSeconds(1).toNanos(), interruption);
                taskkillFailure = exception(RUNTIME_CONTROL_COMMAND_FAILED,
                        "Command cleanup failed: taskkill did not finish within " + PROCESS_TERMINATION_TIMEOUT);
            } else if (taskkill.exitValue() != 0) {
                taskkillFailure = exception(RUNTIME_CONTROL_COMMAND_FAILED,
                        "Command cleanup failed: taskkill exitCode=" + taskkill.exitValue() + ", output=" + output);
            }
        } catch (IOException ex) {
            taskkillFailure = exception(RUNTIME_CONTROL_COMMAND_FAILED,
                    "Command cleanup failed: " + ex.getMessage());
        } finally {
            if (taskkill != null) {
                try {
                    closeProcessStreams(taskkill);
                } catch (RuntimeException closeFailure) {
                    if (taskkillFailure == null) {
                        taskkillFailure = closeFailure;
                    } else {
                        taskkillFailure.addSuppressed(closeFailure);
                    }
                }
            }
        }

        if (process.isAlive()) {
            descendants.addAll(process.toHandle().descendants()
                    .filter(candidate -> descendants.stream().noneMatch(known -> known.pid() == candidate.pid()))
                    .toList());
        }
        long forcedTerminationDeadline = Math.max(deadlineNanos,
                System.nanoTime() + Duration.ofSeconds(1).toNanos());
        process.destroyForcibly();
        for (int i = descendants.size() - 1; i >= 0; i--) {
            ProcessHandle descendant = descendants.get(i);
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }
        boolean terminated = awaitProcessExit(process, forcedTerminationDeadline, interruption);
        for (ProcessHandle descendant : descendants) {
            terminated = awaitProcessExit(descendant, forcedTerminationDeadline, interruption) && terminated;
        }
        if (taskkillFailure != null) {
            if (!terminated) {
                taskkillFailure.addSuppressed(exception(RUNTIME_CONTROL_COMMAND_FAILED,
                        "Command cleanup failed: observed Windows process tree did not terminate"));
            }
            throw taskkillFailure;
        }
        return terminated;
    }

    private boolean terminateSingleProcess(Process process, long deadlineNanos,
                                           InterruptionTracker interruption) {
        process.destroyForcibly();
        return awaitProcessExit(process, deadlineNanos, interruption);
    }

    private boolean awaitProcessExit(Process process, long deadlineNanos, InterruptionTracker interruption) {
        while (process.isAlive()) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return false;
            }
            try {
                return process.waitFor(remainingNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                interruption.markInterrupted();
            }
        }
        return true;
    }

    private void closeProcessStreams(Process process) {
        try (var standardOutput = process.getInputStream();
             var standardError = process.getErrorStream();
             var standardInput = process.getOutputStream()) {
            // Closing all process pipes after exit releases their native Windows handles before returning.
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED,
                    "Command cleanup failed: unable to close process streams: " + ex.getMessage());
        }
    }

    private boolean awaitProcessExit(ProcessHandle process, long deadlineNanos,
                                     InterruptionTracker interruption) {
        while (process.isAlive()) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return false;
            }
            try {
                process.onExit().get(remainingNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                interruption.markInterrupted();
            } catch (java.util.concurrent.TimeoutException ex) {
                return false;
            } catch (java.util.concurrent.ExecutionException ex) {
                throw exception(RUNTIME_CONTROL_COMMAND_FAILED,
                        "Command cleanup failed: unable to observe process exit: " + ex.getCause().getMessage());
            }
        }
        return true;
    }

    private static final class InterruptionTracker {

        private boolean interrupted;

        private InterruptionTracker(boolean interrupted) {
            this.interrupted = interrupted;
        }

        private void markInterrupted() {
            interrupted = true;
        }

        private void restore() {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void appendDetachedRunnerStart(Path logPath, Path runnerScript, String containerId) {
        try {
            Files.writeString(logPath, """

                    detachedRunnerScript=%s
                    detachedRunnerContainer=%s
                    """.formatted(runnerScript, containerId), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        }
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private String sanitizeContainerName(String operationId) {
        String sanitized = operationId.replaceAll("[^A-Za-z0-9_.-]", "-");
        return sanitized.length() <= 60 ? sanitized : sanitized.substring(0, 60);
    }

    private void prepareOperationLog(RuntimeControlCommand command, Path logPath, List<String> commandLine) {
        if (logPath == null) {
            return;
        }
        try {
            Files.createDirectories(logPath.getParent());
            String header = """
                    # Runtime Control Operation
                    environment=%s
                    component=%s
                    script=%s
                    command=%s

                    """.formatted(command.getEnvironment(), command.getComponent(), command.getScriptPath(),
                    String.join(" ", commandLine));
            Files.writeString(logPath, header, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, ex.getMessage());
        }
    }

    private List<String> buildCommandLine(Path script) {
        String fileName = script.getFileName().toString().toLowerCase();
        List<String> commandLine = new ArrayList<>();
        if (fileName.endsWith(".ps1")) {
            commandLine.add("powershell.exe");
            commandLine.add("-NoProfile");
            commandLine.add("-ExecutionPolicy");
            commandLine.add("Bypass");
            commandLine.add("-File");
            commandLine.add(script.toString());
            return commandLine;
        }
        if (fileName.endsWith(".sh")) {
            commandLine.add("bash");
            commandLine.add(script.toString());
            return commandLine;
        }
        if (fileName.endsWith(".py")) {
            commandLine.add("python3");
            commandLine.add(script.toString());
            return commandLine;
        }
        commandLine.add(script.toString());
        return commandLine;
    }

    private Path resolveRepoRoot() {
        if (properties == null || StrUtil.isBlank(properties.getRepoRoot())) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED,
                    "repoRoot missing: set yudao.runtime-control.repo-root or INTRUOYI_RUNTIME_CONTROL_REPO_ROOT");
        }
        Path repoRoot;
        try {
            repoRoot = Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED,
                    "repoRoot invalid: " + properties.getRepoRoot() + ", " + ex.getMessage());
        }
        if (!Files.isDirectory(repoRoot)) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "repoRoot directory not found: " + repoRoot);
        }
        return repoRoot;
    }

    private Path resolveScript(String scriptPath, Path repoRoot) {
        Path path = Path.of(scriptPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return repoRoot.resolve(scriptPath).normalize();
    }
}
