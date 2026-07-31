package cn.iocoder.yudao.module.system.service.codextest;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestRunnerSessionDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestRunnerSessionMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_CAPABILITY_MISSING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_OFFLINE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_STARTER_MISSING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_START_FAILED;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.RUNNER_ONLINE;

@Service
@Validated
public class CodexTestRunnerBootstrapServiceImpl implements CodexTestRunnerBootstrapService {

    @Value("${yudao.codex-test.runner.token:}")
    private String runnerToken;
    @Value("${yudao.codex-test.runner.heartbeat-timeout-seconds:60}")
    private Integer runnerHeartbeatTimeoutSeconds;
    @Value("${yudao.codex-test.runner.on-demand.enabled:true}")
    private Boolean runnerOnDemandEnabled;
    @Value("${yudao.codex-test.runner.on-demand.starter-script:}")
    private String runnerStarterScript;
    @Value("${yudao.codex-test.runner.on-demand.api-base:http://127.0.0.1:48081/admin-api}")
    private String runnerApiBase;
    @Value("${yudao.codex-test.runner.on-demand.frontend-base-url:http://127.0.0.1:8081}")
    private String runnerFrontendBaseUrl;
    @Value("${yudao.codex-test.runner.on-demand.management-tenant-id:1}")
    private String runnerManagementTenantId;
    @Value("${yudao.codex-test.runner.on-demand.runner-name:local-codex-runner-on-demand}")
    private String runnerName;
    @Value("${yudao.codex-test.runner.on-demand.node-command:node.exe}")
    private String nodeCommand;
    @Value("${yudao.codex-test.runner.on-demand.codex-command:codex.cmd}")
    private String codexCommand;
    @Value("${yudao.codex-test.runner.on-demand.restart-existing:true}")
    private Boolean restartExistingRunner;
    @Value("${yudao.codex-test.runner.on-demand.startup-probe-seconds:8}")
    private Integer startupProbeSeconds;
    @Value("${yudao.codex-test.runner.on-demand.startup-timeout-seconds:30}")
    private Integer startupTimeoutSeconds;
    @Value("${yudao.codex-test.runner.on-demand.poll-interval-millis:1000}")
    private Integer startupPollIntervalMillis;
    @Value("${yudao.codex-test.runner.on-demand.api-timeout-millis:30000}")
    private Integer apiTimeoutMillis;

    @Resource
    private CodexTestRunnerSessionMapper codexTestRunnerSessionMapper;

    @Override
    public synchronized void ensureRunnerAvailable() {
        if (hasOnlineRunnerWithRequiredCapabilities()) {
            return;
        }
        if (!Boolean.TRUE.equals(runnerOnDemandEnabled)) {
            throw exception(CODEX_TEST_RUNNER_OFFLINE);
        }
        Process starterProcess = startRunnerWrapper();
        waitForRunnerRegistration(starterProcess);
        if (!hasOnlineRunnerWithRequiredCapabilities()) {
            throw exception(CODEX_TEST_RUNNER_CAPABILITY_MISSING, "playwright,codex");
        }
    }

    private Process startRunnerWrapper() {
        Path starterScript = validateStarterScript();
        List<String> command = new ArrayList<>();
        command.add("powershell.exe");
        command.add("-NoProfile");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(starterScript.toString());
        addNamedArgument(command, "-ApiBase", runnerApiBase);
        addNamedArgument(command, "-FrontendBaseUrl", runnerFrontendBaseUrl);
        addNamedArgument(command, "-TenantId", runnerManagementTenantId);
        addNamedArgument(command, "-RunnerName", runnerName);
        addNamedArgument(command, "-NodeCommand", nodeCommand);
        addNamedArgument(command, "-CodexCommand", codexCommand);
        addNamedArgument(command, "-PollIntervalMs", String.valueOf(startupPollIntervalMillis));
        addNamedArgument(command, "-ApiTimeoutMs", String.valueOf(apiTimeoutMillis));
        addNamedArgument(command, "-StartupProbeSeconds", String.valueOf(startupProbeSeconds));
        if (Boolean.TRUE.equals(restartExistingRunner)) {
            command.add("-RestartExisting");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (StrUtil.isBlank(runnerToken)) {
            processBuilder.environment().remove("CODEX_TEST_RUNNER_TOKEN");
        } else {
            processBuilder.environment().put("CODEX_TEST_RUNNER_TOKEN", runnerToken);
        }
        processBuilder.redirectErrorStream(true);
        if (starterScript.getParent() != null) {
            processBuilder.directory(starterScript.getParent().toFile());
        }
        try {
            return processBuilder.start();
        } catch (IOException ex) {
            throw exception(CODEX_TEST_RUNNER_START_FAILED, ex.getMessage());
        }
    }

    private Path validateStarterScript() {
        if (StrUtil.isBlank(runnerStarterScript)) {
            throw exception(CODEX_TEST_RUNNER_STARTER_MISSING,
                    "yudao.codex-test.runner.on-demand.starter-script");
        }
        Path starterScript = Path.of(runnerStarterScript).toAbsolutePath().normalize();
        if (!Files.isRegularFile(starterScript)) {
            throw exception(CODEX_TEST_RUNNER_STARTER_MISSING, starterScript.toString());
        }
        if (!starterScript.toString().toLowerCase().endsWith(".ps1")) {
            throw exception(CODEX_TEST_RUNNER_START_FAILED, "Runner 启动器必须是受控 PowerShell 脚本：" + starterScript);
        }
        return starterScript;
    }

    private void waitForRunnerRegistration(Process starterProcess) {
        long deadline = System.nanoTime() + Math.max(1, startupTimeoutSeconds) * 1_000_000_000L;
        boolean starterExitedSuccessfully = false;
        while (System.nanoTime() < deadline) {
            if (hasOnlineRunnerWithRequiredCapabilities()) {
                return;
            }
            if (!starterProcess.isAlive()) {
                int exitCode = starterProcess.exitValue();
                if (exitCode != 0) {
                    throw exception(CODEX_TEST_RUNNER_START_FAILED,
                            "启动脚本退出码 " + exitCode + sanitizeProcessOutput(readProcessOutput(starterProcess)));
                }
                starterExitedSuccessfully = true;
            }
            sleepForNextProbe();
        }
        if (starterProcess.isAlive()) {
            starterProcess.destroyForcibly();
        }
        String reason = starterExitedSuccessfully ? "启动脚本已退出但 Runner 未注册在线心跳"
                : "启动脚本超时且 Runner 未注册在线心跳";
        throw exception(CODEX_TEST_RUNNER_START_FAILED, reason);
    }

    private boolean hasOnlineRunnerWithRequiredCapabilities() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(runnerHeartbeatTimeoutSeconds);
        List<CodexTestRunnerSessionDO> onlineRunners = codexTestRunnerSessionMapper.selectOnlineSessions(threshold);
        if (CollUtil.isEmpty(onlineRunners)) {
            return false;
        }
        return onlineRunners.stream().anyMatch(this::hasRequiredCapabilities);
    }

    private boolean hasRequiredCapabilities(CodexTestRunnerSessionDO runnerSession) {
        return RUNNER_ONLINE.equals(runnerSession.getStatus())
                && StrUtil.contains(runnerSession.getCapabilitiesJson(), "playwright")
                && StrUtil.contains(runnerSession.getCapabilitiesJson(), "codex");
    }

    private void addNamedArgument(List<String> command, String name, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        command.add(name);
        command.add(value);
    }

    private void sleepForNextProbe() {
        try {
            Thread.sleep(Math.max(50, startupPollIntervalMillis));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception(CODEX_TEST_RUNNER_START_FAILED, "等待 Runner 注册时被中断");
        }
    }

    private String readProcessOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private String sanitizeProcessOutput(String rawOutput) {
        if (StrUtil.isBlank(rawOutput)) {
            return "";
        }
        String sanitized = StrUtil.isBlank(runnerToken) ? rawOutput
                : rawOutput.replace(runnerToken, "******");
        sanitized = sanitized.replaceAll("[\\r\\n]+", " ").trim();
        if (sanitized.length() > 800) {
            sanitized = sanitized.substring(0, 800) + "...";
        }
        return "，输出：" + sanitized;
    }

}
