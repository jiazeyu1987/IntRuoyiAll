package cn.iocoder.yudao.module.system.service.codextest;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCheckpointResultReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCompleteCaseReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerProgressReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerStatusRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointResultDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestRunnerSessionDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointResultMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestRunnerSessionMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_CAPABILITY_MISSING;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_OFFLINE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_TOKEN_INVALID;
import static cn.iocoder.yudao.module.system.service.codextest.CodexTestConstants.*;

@Service
@Validated
public class CodexTestRunnerServiceImpl implements CodexTestRunnerService {

    @Value("${yudao.codex-test.runner.token:}")
    private String runnerToken;
    @Value("${yudao.codex-test.runner.heartbeat-timeout-seconds:60}")
    private Integer runnerHeartbeatTimeoutSeconds;
    @Value("${yudao.codex-test.runner.max-claim-size:5}")
    private Integer maxClaimSize;

    @Resource
    private CodexTestRunnerSessionMapper codexTestRunnerSessionMapper;
    @Resource
    private CodexTestExecutionMapper codexTestExecutionMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;
    @Resource
    private CodexTestCheckpointResultMapper codexTestCheckpointResultMapper;
    @Resource
    private CodexTestExecutionService codexTestExecutionService;

    @Override
    public void validateRunnerToken(String token) {
        if (StrUtil.isBlank(runnerToken)) {
            return;
        }
        if (!Objects.equals(runnerToken, token)) {
            throw exception(CODEX_TEST_RUNNER_TOKEN_INVALID);
        }
    }

    @Override
    public CodexTestRunnerRegisterRespVO registerRunner(CodexTestRunnerRegisterReqVO registerReqVO, String token) {
        validateRunnerToken(token);
        validateCapabilities(registerReqVO.getCapabilities());
        CodexTestRunnerSessionDO runnerSession = new CodexTestRunnerSessionDO();
        runnerSession.setRunnerName(registerReqVO.getRunnerName());
        runnerSession.setStatus(RUNNER_ONLINE);
        runnerSession.setCapabilitiesJson(registerReqVO.getCapabilities());
        runnerSession.setMaxParallelism(registerReqVO.getMaxParallelism());
        runnerSession.setPlaywrightVersion(registerReqVO.getPlaywrightVersion());
        runnerSession.setCodexVersion(registerReqVO.getCodexVersion());
        runnerSession.setLastHeartbeatTime(LocalDateTime.now());
        runnerSession.setCurrentRunningCount(0);
        CodexTestRunnerAuditSupport.stampRunnerAudit(runnerSession);
        codexTestRunnerSessionMapper.insert(runnerSession);
        CodexTestRunnerRegisterRespVO respVO = new CodexTestRunnerRegisterRespVO();
        respVO.setRunnerSessionId(runnerSession.getId());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CodexTestRunnerClaimRespVO claimTasks(CodexTestRunnerClaimReqVO claimReqVO, String token) {
        validateRunnerToken(token);
        CodexTestRunnerSessionDO runnerSession = validateOnlineRunner(claimReqVO.getRunnerSessionId());
        int capacity = Math.min(Math.min(claimReqVO.getCapacity(), maxClaimSize), runnerSession.getMaxParallelism());
        List<CodexTestRunnerClaimRespVO.Task> tasks = new ArrayList<>(capacity);
        for (CodexTestExecutionCaseDO executionCase : codexTestExecutionCaseMapper.selectPendingClaimCandidates()) {
            if (tasks.size() >= capacity) {
                break;
            }
            if (!isClaimable(executionCase)) {
                continue;
            }
            if (codexTestExecutionCaseMapper.claim(
                    executionCase.getId(), runnerSession.getId(), LocalDateTime.now()) == 1) {
                tasks.add(buildTask(executionCase, runnerSession.getId()));
            }
        }
        codexTestRunnerSessionMapper.heartbeat(runnerSession.getId(), LocalDateTime.now(), tasks.size());
        CodexTestRunnerClaimRespVO respVO = new CodexTestRunnerClaimRespVO();
        respVO.setTasks(tasks);
        return respVO;
    }

    private boolean isClaimable(CodexTestExecutionCaseDO executionCase) {
        CodexTestExecutionDO execution = codexTestExecutionMapper.selectById(executionCase.getExecutionId());
        if (execution == null) {
            throw exception(CODEX_TEST_EXECUTION_NOT_EXISTS);
        }
        return !Boolean.TRUE.equals(execution.getNodeChainExecution())
                || codexTestExecutionCaseMapper.selectEarlierNotPassedCount(
                        executionCase.getExecutionId(), executionCase.getId()) == 0;
    }

    @Override
    public CodexTestRunnerHeartbeatRespVO heartbeat(CodexTestRunnerHeartbeatReqVO heartbeatReqVO, String token) {
        validateRunnerToken(token);
        validateOnlineRunner(heartbeatReqVO.getRunnerSessionId());
        List<Long> runningIds = heartbeatReqVO.getRunningExecutionCaseIds() == null
                ? List.of() : heartbeatReqVO.getRunningExecutionCaseIds();
        codexTestRunnerSessionMapper.heartbeat(heartbeatReqVO.getRunnerSessionId(), LocalDateTime.now(), runningIds.size());
        List<Long> cancelIds = CollUtil.isEmpty(runningIds) ? List.of()
                : codexTestExecutionCaseMapper.selectListByIds(runningIds).stream()
                .filter(executionCase -> EXECUTION_CANCELED.equals(executionCase.getStatus()))
                .map(CodexTestExecutionCaseDO::getId)
                .toList();
        CodexTestRunnerHeartbeatRespVO respVO = new CodexTestRunnerHeartbeatRespVO();
        respVO.setServerTime(LocalDateTime.now());
        respVO.setCancelExecutionCaseIds(cancelIds);
        return respVO;
    }

    @Override
    public CodexTestRunnerStatusRespVO getRunnerStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(runnerHeartbeatTimeoutSeconds);
        List<CodexTestRunnerSessionDO> recentSessions = codexTestRunnerSessionMapper.selectLatestSessions(20);
        List<CodexTestRunnerSessionDO> onlineSessions = recentSessions.stream()
                .filter(runnerSession -> RUNNER_ONLINE.equals(runnerSession.getStatus()))
                .filter(runnerSession -> !runnerSession.getLastHeartbeatTime().isBefore(threshold))
                .toList();
        CodexTestRunnerSessionDO latestSession = recentSessions.stream().findFirst().orElse(null);
        boolean requiredCapabilitiesPresent = onlineSessions.stream().anyMatch(this::hasRequiredCapabilities);

        CodexTestRunnerStatusRespVO respVO = new CodexTestRunnerStatusRespVO();
        respVO.setOnline(!onlineSessions.isEmpty() && requiredCapabilitiesPresent);
        respVO.setOnlineCount(onlineSessions.size());
        respVO.setStaleRunnerCount((int) recentSessions.stream()
                .filter(runnerSession -> RUNNER_ONLINE.equals(runnerSession.getStatus()))
                .filter(runnerSession -> runnerSession.getLastHeartbeatTime().isBefore(threshold))
                .count());
        respVO.setCurrentRunningCount(onlineSessions.stream()
                .map(CodexTestRunnerSessionDO::getCurrentRunningCount)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum));
        respVO.setRequiredCapabilitiesPresent(requiredCapabilitiesPresent);
        respVO.setHeartbeatTimeoutSeconds(runnerHeartbeatTimeoutSeconds);
        if (latestSession != null) {
            respVO.setLatestRunnerSessionId(latestSession.getId());
            respVO.setLatestRunnerName(latestSession.getRunnerName());
            respVO.setLatestRunnerStatus(latestSession.getStatus());
            respVO.setLastHeartbeatTime(latestSession.getLastHeartbeatTime());
            respVO.setHeartbeatAgeSeconds(resolveHeartbeatAgeSeconds(latestSession.getLastHeartbeatTime(), now));
        }
        fillRunnerStatusMessage(respVO);
        return respVO;
    }

    @Override
    public void saveCheckpointResult(CodexTestRunnerCheckpointResultReqVO resultReqVO, String token) {
        validateRunnerToken(token);
        if (!CHECKPOINT_RESULT_STATUSES.contains(resultReqVO.getStatus())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "检查点状态必须是 PASS、FAIL 或 BLOCKED");
        }
        if (CHECKPOINT_FAIL.equals(resultReqVO.getStatus()) && StrUtil.isBlank(resultReqVO.getMismatchDescription())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "FAIL 检查点必须包含差异描述");
        }
        CodexTestCheckpointResultDO checkpointResult = codexTestCheckpointResultMapper.selectByCaseAndSort(
                resultReqVO.getExecutionCaseId(), resultReqVO.getCheckpointSort());
        if (checkpointResult == null) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "检查点结果不存在");
        }
        CodexTestExecutionCaseDO executionCase = validateExecutionCaseExists(resultReqVO.getExecutionCaseId());
        if (!List.of(EXECUTION_CLAIMED, EXECUTION_RUNNING).contains(executionCase.getStatus())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行项状态不允许回写检查点");
        }
        codexTestExecutionCaseMapper.markRunning(executionCase.getId(), executionCase.getRunnerSessionId(), LocalDateTime.now());
        codexTestCheckpointResultMapper.updateResult(checkpointResult.getId(), resultReqVO.getStatus(),
                resultReqVO.getActualText(), resultReqVO.getMismatchDescription(),
                resultReqVO.getScreenshotArtifactId(), LocalDateTime.now());
    }

    @Override
    public void reportProgress(CodexTestRunnerProgressReqVO progressReqVO, String token) {
        validateRunnerToken(token);
        validateProgress(progressReqVO);
        CodexTestExecutionCaseDO executionCase = validateExecutionCaseExists(progressReqVO.getExecutionCaseId());
        if (!List.of(EXECUTION_CLAIMED, EXECUTION_RUNNING).contains(executionCase.getStatus())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行项状态不允许回写进度");
        }
        LocalDateTime startedAt = executionCase.getStartedAt() == null ? LocalDateTime.now() : null;
        if (codexTestExecutionCaseMapper.updateProgress(executionCase.getId(), executionCase.getRunnerSessionId(),
                progressReqVO.getPhase(), progressReqVO.getCurrentMethodSort(),
                progressReqVO.getCurrentCheckpointSort(), progressReqVO.getProgressMessage(), startedAt) != 1) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行项状态不允许回写进度");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCase(CodexTestRunnerCompleteCaseReqVO completeReqVO, String token) {
        validateRunnerToken(token);
        if (!COMPLETE_CASE_STATUSES.contains(completeReqVO.getStatus())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行项完成状态必须是 PASS、FAIL、BLOCKED 或 TIMEOUT");
        }
        CodexTestExecutionCaseDO executionCase = validateExecutionCaseExists(completeReqVO.getExecutionCaseId());
        List<CodexTestCheckpointResultDO> results =
                codexTestCheckpointResultMapper.selectListByExecutionCaseId(executionCase.getId());
        String finalStatus = resolveFinalCaseStatus(completeReqVO.getStatus(), results);
        String failureReason = resolveFailureReason(finalStatus, completeReqVO.getSummary(), results);
        LocalDateTime finishedAt = completeReqVO.getFinishedAt() == null ? LocalDateTime.now() : completeReqVO.getFinishedAt();
        if (codexTestExecutionCaseMapper.complete(executionCase.getId(), executionCase.getRunnerSessionId(),
                finalStatus, failureReason, completeReqVO.getStartedAt(), finishedAt) != 1) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行项状态不允许完成");
        }
        codexTestExecutionService.rollupExecution(executionCase.getExecutionId());
    }

    private void validateCapabilities(String capabilities) {
        if (!StrUtil.contains(capabilities, "playwright")) {
            throw exception(CODEX_TEST_RUNNER_CAPABILITY_MISSING, "playwright");
        }
        if (!StrUtil.contains(capabilities, "codex")) {
            throw exception(CODEX_TEST_RUNNER_CAPABILITY_MISSING, "codex");
        }
    }

    private boolean hasRequiredCapabilities(CodexTestRunnerSessionDO runnerSession) {
        return StrUtil.contains(runnerSession.getCapabilitiesJson(), "playwright")
                && StrUtil.contains(runnerSession.getCapabilitiesJson(), "codex");
    }

    private long resolveHeartbeatAgeSeconds(LocalDateTime heartbeatTime, LocalDateTime now) {
        if (heartbeatTime == null) {
            return -1L;
        }
        return Math.max(0L, Duration.between(heartbeatTime, now).getSeconds());
    }

    private void fillRunnerStatusMessage(CodexTestRunnerStatusRespVO respVO) {
        if (Boolean.TRUE.equals(respVO.getOnline())) {
            respVO.setStatus("ONLINE");
            respVO.setMessage("Runner 在线，最近心跳 " + respVO.getHeartbeatAgeSeconds() + " 秒前");
            return;
        }
        if (respVO.getLatestRunnerSessionId() == null) {
            respVO.setStatus("OFFLINE");
            respVO.setMessage("未发现 Codex Runner 注册记录，请启动本机 Runner");
            return;
        }
        if (respVO.getOnlineCount() > 0 && !Boolean.TRUE.equals(respVO.getRequiredCapabilitiesPresent())) {
            respVO.setStatus("CAPABILITY_MISSING");
            respVO.setMessage("在线 Runner 缺少 playwright 或 codex 能力，请检查 Runner 启动环境");
            return;
        }
        if (respVO.getHeartbeatAgeSeconds() == null || respVO.getHeartbeatAgeSeconds() < 0) {
            respVO.setStatus("OFFLINE");
            respVO.setMessage("最近 Runner 心跳时间缺失，请重新启动本机 Runner");
            return;
        }
        respVO.setStatus("STALE");
        respVO.setMessage("Runner 心跳已过期 " + respVO.getHeartbeatAgeSeconds()
                + " 秒，超过 " + respVO.getHeartbeatTimeoutSeconds() + " 秒，请检查或重启本机 Runner");
    }

    private void validateProgress(CodexTestRunnerProgressReqVO progressReqVO) {
        if (!PROGRESS_PHASES.contains(progressReqVO.getPhase())) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "执行阶段必须是 METHOD、CHECKPOINT 或 DONE");
        }
        if (PROGRESS_PHASE_METHOD.equals(progressReqVO.getPhase())
                && (progressReqVO.getCurrentMethodSort() == null || progressReqVO.getCurrentMethodSort() < 1)) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "METHOD 阶段必须包含当前测试方法项序号");
        }
        if (PROGRESS_PHASE_CHECKPOINT.equals(progressReqVO.getPhase())
                && (progressReqVO.getCurrentCheckpointSort() == null || progressReqVO.getCurrentCheckpointSort() < 1)) {
            throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "CHECKPOINT 阶段必须包含当前目标项序号");
        }
    }

    private CodexTestRunnerSessionDO validateOnlineRunner(Long runnerSessionId) {
        CodexTestRunnerSessionDO runnerSession = codexTestRunnerSessionMapper.selectById(runnerSessionId);
        if (runnerSession == null || !RUNNER_ONLINE.equals(runnerSession.getStatus())
                || runnerSession.getLastHeartbeatTime().isBefore(LocalDateTime.now().minusSeconds(runnerHeartbeatTimeoutSeconds))) {
            throw exception(CODEX_TEST_RUNNER_OFFLINE);
        }
        return runnerSession;
    }

    private CodexTestRunnerClaimRespVO.Task buildTask(CodexTestExecutionCaseDO claimedCase, Long runnerSessionId) {
        CodexTestExecutionCaseDO executionCase = codexTestExecutionCaseMapper.selectById(claimedCase.getId());
        CodexTestExecutionDO execution = codexTestExecutionMapper.selectById(executionCase.getExecutionId());
        codexTestExecutionMapper.updateStatus(execution.getId(), EXECUTION_RUNNING, LocalDateTime.now(),
                null, execution.getSummary(), runnerSessionId);
        CodexTestRunnerClaimRespVO.Task task = new CodexTestRunnerClaimRespVO.Task();
        task.setExecutionId(execution.getId());
        task.setExecutionCaseId(executionCase.getId());
        task.setTargetTenantId(execution.getTargetTenantId());
        task.setExecutionMode(execution.getExecutionMode());
        task.setAnalysisMode(StrUtil.blankToDefault(
                executionCase.getAnalysisModeSnapshot(), ANALYSIS_MODE_PLAYWRIGHT_E2E));
        task.setCaseName(executionCase.getCaseNameSnapshot());
        task.setMethodText(executionCase.getMethodTextSnapshot());
        task.setTestDataText(executionCase.getTestDataTextSnapshot());
        task.setCheckpoints(codexTestCheckpointResultMapper.selectListByExecutionCaseId(executionCase.getId()).stream()
                .map(this::toClaimCheckpoint)
                .toList());
        return task;
    }

    private CodexTestRunnerClaimRespVO.Checkpoint toClaimCheckpoint(CodexTestCheckpointResultDO checkpointResult) {
        CodexTestRunnerClaimRespVO.Checkpoint checkpoint = new CodexTestRunnerClaimRespVO.Checkpoint();
        checkpoint.setSort(checkpointResult.getCheckpointSort());
        checkpoint.setName(checkpointResult.getCheckpointNameSnapshot());
        checkpoint.setExpectedText(checkpointResult.getExpectedTextSnapshot());
        checkpoint.setSeverity("MAJOR");
        return checkpoint;
    }

    private CodexTestExecutionCaseDO validateExecutionCaseExists(Long executionCaseId) {
        CodexTestExecutionCaseDO executionCase = codexTestExecutionCaseMapper.selectById(executionCaseId);
        if (executionCase == null) {
            throw exception(CODEX_TEST_EXECUTION_NOT_EXISTS);
        }
        return executionCase;
    }

    private String resolveFinalCaseStatus(String runnerStatus, List<CodexTestCheckpointResultDO> results) {
        if (EXECUTION_BLOCKED.equals(runnerStatus) || EXECUTION_TIMEOUT.equals(runnerStatus)) {
            return runnerStatus;
        }
        if (results.stream().anyMatch(result -> CHECKPOINT_FAIL.equals(result.getStatus()))) {
            return EXECUTION_FAIL;
        }
        if (results.stream().anyMatch(result -> CHECKPOINT_BLOCKED.equals(result.getStatus()))) {
            return EXECUTION_BLOCKED;
        }
        if (results.stream().allMatch(result -> CHECKPOINT_PASS.equals(result.getStatus()))) {
            return EXECUTION_PASS;
        }
        throw exception(CODEX_TEST_RESULT_SCHEMA_INVALID, "完成执行项前必须回写全部检查点结果");
    }

    private String resolveFailureReason(String finalStatus, String runnerSummary, List<CodexTestCheckpointResultDO> results) {
        if (EXECUTION_FAIL.equals(finalStatus)) {
            return results.stream()
                    .filter(result -> CHECKPOINT_FAIL.equals(result.getStatus()))
                    .map(CodexTestCheckpointResultDO::getMismatchDescription)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(runnerSummary);
        }
        if (EXECUTION_BLOCKED.equals(finalStatus) || EXECUTION_TIMEOUT.equals(finalStatus)) {
            return runnerSummary;
        }
        return null;
    }

}
