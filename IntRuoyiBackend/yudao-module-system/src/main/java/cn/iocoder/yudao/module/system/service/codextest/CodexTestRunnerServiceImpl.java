package cn.iocoder.yudao.module.system.service.codextest;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCheckpointResultReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCompleteCaseReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerHeartbeatRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerRegisterRespVO;
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

import java.time.LocalDateTime;
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
        if (StrUtil.isBlank(runnerToken) || !Objects.equals(runnerToken, token)) {
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
        List<CodexTestRunnerClaimRespVO.Task> tasks = codexTestExecutionCaseMapper
                .selectPendingClaimCandidates(capacity).stream()
                .filter(executionCase -> codexTestExecutionCaseMapper.claim(executionCase.getId(),
                        runnerSession.getId(), LocalDateTime.now()) == 1)
                .map(executionCase -> buildTask(executionCase, runnerSession.getId()))
                .toList();
        codexTestRunnerSessionMapper.heartbeat(runnerSession.getId(), LocalDateTime.now(), tasks.size());
        CodexTestRunnerClaimRespVO respVO = new CodexTestRunnerClaimRespVO();
        respVO.setTasks(tasks);
        return respVO;
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
