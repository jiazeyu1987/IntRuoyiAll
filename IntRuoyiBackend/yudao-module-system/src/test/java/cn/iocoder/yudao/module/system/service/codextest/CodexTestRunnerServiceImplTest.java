package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionStartReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCheckpointResultReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerClaimRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestRunnerCompleteCaseReqVO;
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
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static org.junit.jupiter.api.Assertions.*;

@Import({CodexTestCaseServiceImpl.class, CodexTestExecutionServiceImpl.class, CodexTestRunnerServiceImpl.class})
class CodexTestRunnerServiceImplTest extends BaseDbUnitTest {

    private static final String RUNNER_TOKEN = "test-runner-token";

    @Resource
    private CodexTestCaseService codexTestCaseService;
    @Resource
    private CodexTestExecutionService codexTestExecutionService;
    @Resource
    private CodexTestRunnerService codexTestRunnerService;
    @Resource
    private CodexTestExecutionMapper codexTestExecutionMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;
    @Resource
    private CodexTestRunnerSessionMapper codexTestRunnerSessionMapper;
    @Resource
    private CodexTestCheckpointResultMapper codexTestCheckpointResultMapper;

    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private CodexTestRunnerBootstrapService codexTestRunnerBootstrapService;

    @BeforeEach
    void setUpRunnerToken() {
        ReflectionTestUtils.setField(codexTestRunnerService, "runnerToken", RUNNER_TOKEN);
    }

    @Test
    void runnerClaimAndCheckpointResult_keepsFailureEvidenceAndRollsUpBatchFailure() {
        Long runnerSessionId = registerRunner();
        Long caseId = codexTestCaseService.createCase(validScheduleCaseReq("排产手动重排", true));
        Long executionId = codexTestExecutionService.startExecution(startReq(caseId), 99L);

        CodexTestRunnerClaimRespVO claimRespVO = codexTestRunnerService.claimTasks(claimReq(runnerSessionId), RUNNER_TOKEN);

        assertEquals(1, claimRespVO.getTasks().size());
        CodexTestRunnerClaimRespVO.Task task = claimRespVO.getTasks().get(0);
        assertEquals(executionId, task.getExecutionId());
        assertEquals("在排产工单页签选择用户手写工单号后点击手动重排", task.getMethodText());
        assertEquals(2, task.getCheckpoints().size());
        assertEquals("CLAIMED", codexTestExecutionCaseMapper.selectById(task.getExecutionCaseId()).getStatus());

        CodexTestRunnerCheckpointResultReqVO invalidFail = resultReq(task.getExecutionCaseId(), "FAIL", "");
        assertServiceException(() -> codexTestRunnerService.saveCheckpointResult(invalidFail, RUNNER_TOKEN),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "FAIL 检查点必须包含差异描述");

        codexTestRunnerService.saveCheckpointResult(
                resultReq(task.getExecutionCaseId(), "FAIL", "产品编号没有变成橙色"), RUNNER_TOKEN);
        CodexTestRunnerCompleteCaseReqVO completeReqVO = new CodexTestRunnerCompleteCaseReqVO();
        completeReqVO.setExecutionCaseId(task.getExecutionCaseId());
        completeReqVO.setStatus("PASS");
        completeReqVO.setSummary("Runner 误报通过，但检查点结果失败");
        codexTestRunnerService.completeCase(completeReqVO, RUNNER_TOKEN);

        CodexTestExecutionCaseDO executionCase = codexTestExecutionCaseMapper.selectById(task.getExecutionCaseId());
        assertEquals("FAIL", executionCase.getStatus());
        assertEquals("产品编号没有变成橙色", executionCase.getFailureReason());
        CodexTestExecutionDO execution = codexTestExecutionMapper.selectById(executionId);
        assertEquals("FAIL", execution.getStatus());
    }

    @Test
    void registerRunner_stampsAuditFieldsWithoutLoginUser() {
        Long runnerSessionId = registerRunner();

        CodexTestRunnerSessionDO runnerSession = codexTestRunnerSessionMapper.selectById(runnerSessionId);

        assertEquals("codex-runner", runnerSession.getCreator());
        assertEquals("codex-runner", runnerSession.getUpdater());
    }

    @Test
    void registerRunner_allowsMissingTokenWhenLocalCliModeHasNoConfiguredToken() {
        ReflectionTestUtils.setField(codexTestRunnerService, "runnerToken", "");
        CodexTestRunnerRegisterReqVO registerReqVO = new CodexTestRunnerRegisterReqVO();
        registerReqVO.setRunnerName("local-tokenless-runner");
        registerReqVO.setCapabilities("{\"playwright\":true,\"codex\":true}");
        registerReqVO.setMaxParallelism(1);

        CodexTestRunnerRegisterRespVO registerRespVO = codexTestRunnerService.registerRunner(registerReqVO, null);

        CodexTestRunnerSessionDO runnerSession =
                codexTestRunnerSessionMapper.selectById(registerRespVO.getRunnerSessionId());
        assertEquals("local-tokenless-runner", runnerSession.getRunnerName());
    }

    @Test
    void claimTasks_sequentialNodeChainOnlyClaimsFirstNodeWhenCapacityIsGreaterThanOne() {
        Long runnerSessionId = registerRunner();
        Long firstCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        Long secondCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录创建执行", 2));
        Long executionId = codexTestExecutionService.startExecution(
                startReq(firstCaseId, secondCaseId), 99L);

        CodexTestRunnerClaimRespVO claimRespVO =
                codexTestRunnerService.claimTasks(claimReq(runnerSessionId, 2), RUNNER_TOKEN);

        assertEquals(1, claimRespVO.getTasks().size());
        assertEquals("批记录前置检查", claimRespVO.getTasks().get(0).getCaseName());
        List<CodexTestExecutionCaseDO> executionCases =
                codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals("CLAIMED", executionCases.get(0).getStatus());
        assertEquals("PENDING", executionCases.get(1).getStatus());
    }

    @Test
    void claimTasks_independentSequentialCasesUseAvailableCapacity() {
        Long runnerSessionId = registerRunner();
        Long firstCaseId = codexTestCaseService.createCase(validScheduleCaseReq("独立顺序测试项一", false));
        Long secondCaseId = codexTestCaseService.createCase(validScheduleCaseReq("独立顺序测试项二", false));
        codexTestExecutionService.startExecution(startReq(firstCaseId, secondCaseId), 99L);

        CodexTestRunnerClaimRespVO claimRespVO =
                codexTestRunnerService.claimTasks(claimReq(runnerSessionId, 2), RUNNER_TOKEN);

        assertEquals(2, claimRespVO.getTasks().size());
        assertEquals(List.of("独立顺序测试项一", "独立顺序测试项二"),
                claimRespVO.getTasks().stream().map(CodexTestRunnerClaimRespVO.Task::getCaseName).toList());
    }

    @Test
    void completeCase_failedIndependentSequentialCaseAllowsRemainingCaseToRun() {
        Long runnerSessionId = registerRunner();
        Long firstCaseId = codexTestCaseService.createCase(validScheduleCaseReq("独立顺序测试项一", false));
        Long secondCaseId = codexTestCaseService.createCase(validScheduleCaseReq("独立顺序测试项二", false));
        Long executionId = codexTestExecutionService.startExecution(startReq(firstCaseId, secondCaseId), 99L);
        CodexTestRunnerClaimRespVO.Task firstTask =
                codexTestRunnerService.claimTasks(claimReq(runnerSessionId), RUNNER_TOKEN).getTasks().get(0);
        CodexTestRunnerCompleteCaseReqVO failedReqVO = new CodexTestRunnerCompleteCaseReqVO();
        failedReqVO.setExecutionCaseId(firstTask.getExecutionCaseId());
        failedReqVO.setStatus("FAIL");
        failedReqVO.setSummary("独立测试项失败");
        codexTestRunnerService.saveCheckpointResult(
                resultReq(firstTask.getExecutionCaseId(), "FAIL", "独立测试项失败"), RUNNER_TOKEN);
        CodexTestRunnerCheckpointResultReqVO firstCaseSecondCheckpoint =
                resultReq(firstTask.getExecutionCaseId(), "PASS", "");
        firstCaseSecondCheckpoint.setCheckpointSort(2);
        codexTestRunnerService.saveCheckpointResult(firstCaseSecondCheckpoint, RUNNER_TOKEN);

        codexTestRunnerService.completeCase(failedReqVO, RUNNER_TOKEN);

        CodexTestRunnerClaimRespVO.Task secondTask =
                codexTestRunnerService.claimTasks(claimReq(runnerSessionId), RUNNER_TOKEN).getTasks().get(0);
        assertEquals("独立顺序测试项二", secondTask.getCaseName());
        CodexTestRunnerCompleteCaseReqVO passedReqVO = new CodexTestRunnerCompleteCaseReqVO();
        passedReqVO.setExecutionCaseId(secondTask.getExecutionCaseId());
        passedReqVO.setStatus("PASS");
        passedReqVO.setSummary("独立测试项继续执行完成");
        codexTestRunnerService.saveCheckpointResult(
                resultReq(secondTask.getExecutionCaseId(), "PASS", ""), RUNNER_TOKEN);
        CodexTestRunnerCheckpointResultReqVO secondCaseSecondCheckpoint =
                resultReq(secondTask.getExecutionCaseId(), "PASS", "");
        secondCaseSecondCheckpoint.setCheckpointSort(2);
        codexTestRunnerService.saveCheckpointResult(secondCaseSecondCheckpoint, RUNNER_TOKEN);

        codexTestRunnerService.completeCase(passedReqVO, RUNNER_TOKEN);

        assertEquals("FAIL", codexTestExecutionMapper.selectById(executionId).getStatus());
    }

    @Test
    void completeCase_failedSequentialNodeBlocksRemainingNodesAndCheckpoints() {
        Long runnerSessionId = registerRunner();
        Long firstCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        Long secondCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录创建执行", 2));
        Long executionId = codexTestExecutionService.startExecution(
                startReq(firstCaseId, secondCaseId), 99L);
        CodexTestRunnerClaimRespVO.Task firstTask =
                codexTestRunnerService.claimTasks(claimReq(runnerSessionId, 2), RUNNER_TOKEN)
                        .getTasks().get(0);
        codexTestRunnerService.saveCheckpointResult(
                resultReq(firstTask.getExecutionCaseId(), "FAIL", "前置配置不符合要求"), RUNNER_TOKEN);
        CodexTestRunnerCompleteCaseReqVO completeReqVO = new CodexTestRunnerCompleteCaseReqVO();
        completeReqVO.setExecutionCaseId(firstTask.getExecutionCaseId());
        completeReqVO.setStatus("FAIL");
        completeReqVO.setSummary("批记录前置检查失败");

        codexTestRunnerService.completeCase(completeReqVO, RUNNER_TOKEN);

        List<CodexTestExecutionCaseDO> executionCases =
                codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals("FAIL", executionCases.get(0).getStatus());
        assertEquals("BLOCKED", executionCases.get(1).getStatus());
        assertEquals("前置节点未通过，串行节点串已停止", executionCases.get(1).getFailureReason());
        List<CodexTestCheckpointResultDO> blockedResults =
                codexTestCheckpointResultMapper.selectListByExecutionCaseId(executionCases.get(1).getId());
        assertTrue(blockedResults.stream().allMatch(result -> "BLOCKED".equals(result.getStatus())));
        assertTrue(blockedResults.stream().allMatch(
                result -> "前置节点未通过，串行节点串已停止".equals(result.getMismatchDescription())));
        assertEquals("FAIL", codexTestExecutionMapper.selectById(executionId).getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLOCKED", "TIMEOUT"})
    void completeCase_nonPassingSequentialStatusBlocksRemainingNodes(String firstNodeStatus) {
        Long runnerSessionId = registerRunner();
        Long firstCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        Long secondCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录创建执行", 2));
        Long executionId = codexTestExecutionService.startExecution(
                startReq(firstCaseId, secondCaseId), 99L);
        CodexTestRunnerClaimRespVO.Task firstTask =
                codexTestRunnerService.claimTasks(claimReq(runnerSessionId, 2), RUNNER_TOKEN)
                        .getTasks().get(0);
        CodexTestRunnerCompleteCaseReqVO completeReqVO = new CodexTestRunnerCompleteCaseReqVO();
        completeReqVO.setExecutionCaseId(firstTask.getExecutionCaseId());
        completeReqVO.setStatus(firstNodeStatus);
        completeReqVO.setSummary("前置节点未完成");

        codexTestRunnerService.completeCase(completeReqVO, RUNNER_TOKEN);

        List<CodexTestExecutionCaseDO> executionCases =
                codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals(firstNodeStatus, executionCases.get(0).getStatus());
        assertEquals("BLOCKED", executionCases.get(1).getStatus());
        assertEquals("前置节点未通过，串行节点串已停止", executionCases.get(1).getFailureReason());
        assertEquals("FAIL", codexTestExecutionMapper.selectById(executionId).getStatus());
    }

    @Test
    void getRunnerStatus_reportsStaleRunnerWithDiagnosticMessage() {
        ReflectionTestUtils.setField(codexTestRunnerService, "runnerHeartbeatTimeoutSeconds", 60);
        CodexTestRunnerSessionDO runnerSession = new CodexTestRunnerSessionDO();
        runnerSession.setRunnerName("local-runner-stale");
        runnerSession.setStatus("ONLINE");
        runnerSession.setCapabilitiesJson("{\"playwright\":true,\"codex\":true}");
        runnerSession.setMaxParallelism(1);
        runnerSession.setLastHeartbeatTime(LocalDateTime.now().minusSeconds(120));
        runnerSession.setCurrentRunningCount(0);
        codexTestRunnerSessionMapper.insert(runnerSession);

        CodexTestRunnerStatusRespVO status = codexTestRunnerService.getRunnerStatus();

        assertFalse(status.getOnline());
        assertEquals(0, status.getOnlineCount());
        assertEquals(1, status.getStaleRunnerCount());
        assertEquals(runnerSession.getId(), status.getLatestRunnerSessionId());
        assertTrue(status.getHeartbeatAgeSeconds() >= 60);
        assertTrue(status.getMessage().contains("心跳已过期"));
    }

    @Test
    void reportProgress_updatesRunningCaseAndMonitorDetailFields() {
        Long runnerSessionId = registerRunner();
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", true));
        Long executionId = codexTestExecutionService.startExecution(startReq(caseId), 99L);
        CodexTestRunnerClaimRespVO.Task task = codexTestRunnerService.claimTasks(claimReq(runnerSessionId), RUNNER_TOKEN)
                .getTasks().get(0);

        CodexTestRunnerProgressReqVO methodProgressReqVO = new CodexTestRunnerProgressReqVO();
        methodProgressReqVO.setExecutionCaseId(task.getExecutionCaseId());
        methodProgressReqVO.setPhase("METHOD");
        methodProgressReqVO.setCurrentMethodSort(2);
        methodProgressReqVO.setProgressMessage("正在执行测试方法项第 2 项");
        codexTestRunnerService.reportProgress(methodProgressReqVO, RUNNER_TOKEN);

        CodexTestExecutionCaseDO methodProgressCase = codexTestExecutionCaseMapper.selectById(task.getExecutionCaseId());
        assertEquals("RUNNING", methodProgressCase.getStatus());
        assertEquals("METHOD", methodProgressCase.getProgressPhase());
        assertEquals(2, methodProgressCase.getCurrentMethodSort());
        assertEquals("正在执行测试方法项第 2 项", methodProgressCase.getProgressMessage());

        CodexTestRunnerProgressReqVO checkpointProgressReqVO = new CodexTestRunnerProgressReqVO();
        checkpointProgressReqVO.setExecutionCaseId(task.getExecutionCaseId());
        checkpointProgressReqVO.setPhase("CHECKPOINT");
        checkpointProgressReqVO.setCurrentCheckpointSort(1);
        checkpointProgressReqVO.setProgressMessage("正在验证目标项第 1 项");
        codexTestRunnerService.reportProgress(checkpointProgressReqVO, RUNNER_TOKEN);

        CodexTestExecutionCaseDO checkpointProgressCase = codexTestExecutionCaseMapper.selectById(task.getExecutionCaseId());
        assertEquals("CHECKPOINT", checkpointProgressCase.getProgressPhase());
        assertEquals(1, checkpointProgressCase.getCurrentCheckpointSort());
        assertEquals("正在验证目标项第 1 项", checkpointProgressCase.getProgressMessage());
        assertEquals("CHECKPOINT", codexTestExecutionService.getExecution(executionId).getCases().get(0).getProgressPhase());
    }

    private Long registerRunner() {
        CodexTestRunnerRegisterReqVO registerReqVO = new CodexTestRunnerRegisterReqVO();
        registerReqVO.setRunnerName("local-runner");
        registerReqVO.setCapabilities("{\"playwright\":true,\"codex\":true}");
        registerReqVO.setMaxParallelism(2);
        CodexTestRunnerRegisterRespVO registerRespVO = codexTestRunnerService.registerRunner(registerReqVO, RUNNER_TOKEN);
        return registerRespVO.getRunnerSessionId();
    }

    private CodexTestCaseSaveReqVO validScheduleCaseReq(String name, boolean parallelSafe) {
        CodexTestCaseSaveReqVO reqVO = CodexTestCaseServiceImplTest.buildCaseReq(name, parallelSafe);
        reqVO.setProject("智能排产");
        return reqVO;
    }

    private CodexTestExecutionStartReqVO startReq(Long... caseIds) {
        CodexTestExecutionStartReqVO reqVO = new CodexTestExecutionStartReqVO();
        reqVO.setTargetTenantId(88L);
        reqVO.setExecutionMode("SEQUENTIAL");
        reqVO.setCaseIds(List.of(caseIds));
        return reqVO;
    }

    private CodexTestRunnerClaimReqVO claimReq(Long runnerSessionId) {
        return claimReq(runnerSessionId, 1);
    }

    private CodexTestRunnerClaimReqVO claimReq(Long runnerSessionId, int capacity) {
        CodexTestRunnerClaimReqVO reqVO = new CodexTestRunnerClaimReqVO();
        reqVO.setRunnerSessionId(runnerSessionId);
        reqVO.setCapacity(capacity);
        return reqVO;
    }

    private CodexTestRunnerCheckpointResultReqVO resultReq(Long executionCaseId, String status, String mismatch) {
        CodexTestRunnerCheckpointResultReqVO reqVO = new CodexTestRunnerCheckpointResultReqVO();
        reqVO.setExecutionCaseId(executionCaseId);
        reqVO.setCheckpointSort(1);
        reqVO.setStatus(status);
        reqVO.setActualText("真实页面观测结果");
        reqVO.setMismatchDescription(mismatch);
        return reqVO;
    }

}
