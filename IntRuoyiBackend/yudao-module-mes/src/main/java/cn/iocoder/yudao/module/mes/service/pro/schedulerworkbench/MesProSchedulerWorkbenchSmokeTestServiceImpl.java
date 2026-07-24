package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStatusRespVO;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_RUNNING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_START_FAILED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_STOP_FAILED;

@Slf4j
@Service
public class MesProSchedulerWorkbenchSmokeTestServiceImpl implements MesProSchedulerWorkbenchSmokeTestService {

    private static final String STATUS_IDLE = "IDLE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_STOPPED = "STOPPED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String DEFAULT_SCRIPT_NAME = "e2e:mes:smart-scheduling-smoke";
    private static final String SMOKE_STOP_AFTER_STAGE = "MES_SMOKE_STOP_AFTER_STAGE";
    private static final String STOP_AFTER_STAGE_ATTRIBUTE = "ATTRIBUTE";
    private static final TypeReference<PackageJson> PACKAGE_JSON_TYPE = new TypeReference<>() {
    };

    @Value("${yudao.mes.scheduler-workbench.smoke-test.frontend-directory:}")
    private String frontendDirectory;
    @Value("${yudao.mes.scheduler-workbench.smoke-test.script-name:" + DEFAULT_SCRIPT_NAME + "}")
    private String scriptName;

    @Resource
    private MesProSchedulerWorkbenchSmokeProcessController processController;

    private final Supplier<String> osNameSupplier;
    private final Clock clock;
    private MesProSchedulerWorkbenchSmokeProcess currentProcess;
    private String status = STATUS_IDLE;
    private String runId;
    private Instant startedAt;
    private Instant stoppedAt;
    private Instant finishedAt;
    private Integer exitCode;
    private String commandText;
    private String logFile;
    private String message = "冒烟测试未启动";
    private boolean feedbackApprovalEnabled;

    public MesProSchedulerWorkbenchSmokeTestServiceImpl() {
        this(() -> System.getProperty("os.name", ""), Clock.systemDefaultZone());
    }

    MesProSchedulerWorkbenchSmokeTestServiceImpl(Supplier<String> osNameSupplier, Clock clock) {
        this.osNameSupplier = osNameSupplier;
        this.clock = clock;
    }

    MesProSchedulerWorkbenchSmokeTestServiceImpl(MesProSchedulerWorkbenchSmokeProcessController processController,
                                                Supplier<String> osNameSupplier,
                                                Clock clock) {
        this.processController = processController;
        this.osNameSupplier = osNameSupplier;
        this.clock = clock;
    }

    @Override
    public synchronized MesProSchedulerWorkbenchSmokeTestStatusRespVO getStatus() {
        refreshFinishedProcessStatus();
        return buildStatus();
    }

    @Override
    public synchronized MesProSchedulerWorkbenchSmokeTestStatusRespVO start(MesProSchedulerWorkbenchSmokeTestStartReqVO reqVO) {
        refreshFinishedProcessStatus();
        if (isRunning()) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_RUNNING);
        }

        boolean requestedFeedbackApprovalEnabled = reqVO != null && Boolean.TRUE.equals(reqVO.getFeedbackApprovalEnabled());
        Path frontendDir = resolveFrontendDirectory();
        validatePackageScript(frontendDir);
        String effectiveScriptName = resolveScriptName();
        String effectiveRunId = buildRunId();
        Path logPath = frontendDir.resolve("output").resolve("smart-scheduling-smoke").resolve(effectiveRunId)
                .resolve("backend-runner.log");
        createParentDirectory(logPath);

        List<String> command = buildCommand(effectiveScriptName);
        Map<String, String> environment = new HashMap<>();
        environment.put("MES_SMOKE_RUN_ID", effectiveRunId);
        environment.put("MES_SMOKE_BACKEND_RUNNER_LOG", logPath.toString());
        environment.put(SMOKE_STOP_AFTER_STAGE, requestedFeedbackApprovalEnabled ? "" : STOP_AFTER_STAGE_ATTRIBUTE);

        try {
            currentProcess = processController.start(command, frontendDir.toFile(), environment, logPath.toFile());
        } catch (IOException ex) {
            log.warn("Failed to start smart scheduling smoke test, frontendDir={}, command={}",
                    frontendDir, command, ex);
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_START_FAILED, ex.getMessage());
        }

        status = STATUS_RUNNING;
        runId = effectiveRunId;
        startedAt = Instant.now(clock);
        stoppedAt = null;
        finishedAt = null;
        exitCode = null;
        commandText = String.join(" ", command);
        logFile = logPath.toString();
        feedbackApprovalEnabled = requestedFeedbackApprovalEnabled;
        message = "冒烟测试已启动";
        log.info("Smart scheduling smoke test started, runId={}, pid={}, command={}, frontendDir={}, logFile={}, feedbackApprovalEnabled={}",
                runId, currentProcess.pid(), commandText, frontendDir, logFile, feedbackApprovalEnabled);
        return buildStatus();
    }

    @Override
    public synchronized MesProSchedulerWorkbenchSmokeTestStatusRespVO stop() {
        refreshFinishedProcessStatus();
        if (!isRunning()) {
            return buildStatus();
        }
        processController.stop(currentProcess, isWindows());
        if (currentProcess.isAlive()) {
            message = "冒烟测试结束失败，进程仍在运行";
            log.warn("Smart scheduling smoke test stop failed, runId={}, pid={}", runId, currentProcess.pid());
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_STOP_FAILED, currentProcess.pid());
        }
        stoppedAt = Instant.now(clock);
        exitCode = currentProcess.exitCode();
        status = exitCode != null && exitCode != 0 ? STATUS_FAILED : STATUS_STOPPED;
        message = STATUS_FAILED.equals(status) ? "冒烟测试已结束但退出码非 0" : "冒烟测试已结束";
        log.info("Smart scheduling smoke test stopped, runId={}, pid={}, exitCode={}",
                runId, currentProcess.pid(), exitCode);
        return buildStatus();
    }

    void setFrontendDirectory(String frontendDirectory) {
        this.frontendDirectory = frontendDirectory;
    }

    void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    private boolean isRunning() {
        return currentProcess != null && currentProcess.isAlive();
    }

    private void refreshFinishedProcessStatus() {
        if (currentProcess == null || currentProcess.isAlive() || !STATUS_RUNNING.equals(status)) {
            return;
        }
        finishedAt = Instant.now(clock);
        exitCode = currentProcess.exitCode();
        status = exitCode != null && exitCode == 0 ? STATUS_STOPPED : STATUS_FAILED;
        message = STATUS_STOPPED.equals(status) ? "冒烟测试已完成" : "冒烟测试执行失败";
    }

    private MesProSchedulerWorkbenchSmokeTestStatusRespVO buildStatus() {
        MesProSchedulerWorkbenchSmokeTestStatusRespVO respVO = new MesProSchedulerWorkbenchSmokeTestStatusRespVO();
        respVO.setStatus(status);
        respVO.setRunning(isRunning());
        respVO.setFeedbackApprovalEnabled(feedbackApprovalEnabled);
        respVO.setRunId(runId);
        respVO.setOsName(osNameSupplier.get());
        respVO.setPid(currentProcess == null ? null : currentProcess.pid());
        respVO.setStartedAt(startedAt);
        respVO.setStoppedAt(stoppedAt);
        respVO.setFinishedAt(finishedAt);
        respVO.setExitCode(exitCode);
        respVO.setFrontendDirectory(StrUtil.isBlank(frontendDirectory) ? null : frontendDirectory.trim());
        respVO.setScriptName(resolveScriptName());
        respVO.setCommandText(commandText);
        respVO.setLogFile(logFile);
        respVO.setMessage(message);
        return respVO;
    }

    private Path resolveFrontendDirectory() {
        if (StrUtil.isBlank(frontendDirectory)) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED,
                    "yudao.mes.scheduler-workbench.smoke-test.frontend-directory");
        }
        Path path = Path.of(frontendDirectory.trim()).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED, path);
        }
        return path;
    }

    private String resolveScriptName() {
        return StrUtil.blankToDefault(scriptName, DEFAULT_SCRIPT_NAME).trim();
    }

    private void validatePackageScript(Path frontendDir) {
        Path packageJsonPath = frontendDir.resolve("package.json");
        if (!Files.isRegularFile(packageJsonPath)) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED, packageJsonPath);
        }
        PackageJson packageJson;
        try {
            packageJson = JsonUtils.parseObject(Files.readString(packageJsonPath, StandardCharsets.UTF_8), PACKAGE_JSON_TYPE);
        } catch (IOException | RuntimeException ex) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED, packageJsonPath);
        }
        if (packageJson == null || packageJson.getScripts() == null
                || !packageJson.getScripts().containsKey(resolveScriptName())) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED, resolveScriptName());
        }
    }

    private List<String> buildCommand(String effectiveScriptName) {
        return List.of(isWindows() ? "npm.cmd" : "npm", "run", effectiveScriptName);
    }

    private boolean isWindows() {
        return Objects.toString(osNameSupplier.get(), "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String buildRunId() {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock))
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")
                .replace("Z", "");
        return "SMART-SCHED-" + timestamp;
    }

    private void createParentDirectory(Path logPath) {
        try {
            Files.createDirectories(logPath.getParent());
        } catch (IOException ex) {
            throw exception(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED, logPath.getParent());
        }
    }

    @Data
    private static class PackageJson {
        private Map<String, String> scripts;
    }

}
