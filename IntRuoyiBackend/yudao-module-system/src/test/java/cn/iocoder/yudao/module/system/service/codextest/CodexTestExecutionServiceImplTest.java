package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestExecutionStartReqVO;
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
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_PARALLEL_UNSAFE_CASE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_START_FAILED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@Import({CodexTestCaseServiceImpl.class, CodexTestExecutionServiceImpl.class})
class CodexTestExecutionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CodexTestCaseService codexTestCaseService;
    @Resource
    private CodexTestExecutionService codexTestExecutionService;
    @Resource
    private CodexTestRunnerSessionMapper codexTestRunnerSessionMapper;
    @Resource
    private CodexTestExecutionMapper codexTestExecutionMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;
    @Resource
    private CodexTestCheckpointResultMapper codexTestCheckpointResultMapper;

    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private CodexTestRunnerBootstrapService codexTestRunnerBootstrapService;

    @Test
    void startSequentialExecution_createsHistoricalSnapshotsAndInitialCheckpointResults() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", false));
        CodexTestExecutionStartReqVO reqVO = startReq("SEQUENTIAL", caseId);

        Long executionId = codexTestExecutionService.startExecution(reqVO, 99L);

        verify(codexTestRunnerBootstrapService).ensureRunnerAvailable();
        CodexTestExecutionDO execution = codexTestExecutionMapper.selectById(executionId);
        assertEquals("PENDING", execution.getStatus());
        assertEquals(88L, execution.getTargetTenantId());
        assertEquals(99L, execution.getRequestedBy());
        List<CodexTestExecutionCaseDO> executionCases = codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals(1, executionCases.size());
        assertEquals("排产手动重排", executionCases.get(0).getCaseNameSnapshot());
        assertEquals(2, executionCases.get(0).getCheckpointCount());
        List<CodexTestCheckpointResultDO> results =
                codexTestCheckpointResultMapper.selectListByExecutionCaseId(executionCases.get(0).getId());
        assertEquals(2, results.size());
        assertEquals("NOT_RUN", results.get(0).getStatus());
        assertEquals("重排成功", results.get(0).getExpectedTextSnapshot());
    }

    @Test
    void startSequentialExecution_ordersNodeChainByConfiguredSort() {
        Long firstCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        Long secondCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录创建执行", 2));
        Long thirdCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录通知核验", 3));

        Long executionId = codexTestExecutionService.startExecution(
                startReq("SEQUENTIAL", thirdCaseId, firstCaseId, secondCaseId), 99L);

        List<CodexTestExecutionCaseDO> executionCases =
                codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals(List.of("批记录前置检查", "批记录创建执行", "批记录通知核验"),
                executionCases.stream().map(CodexTestExecutionCaseDO::getCaseNameSnapshot).toList());
    }

    @Test
    void startSequentialExecution_allowsFirstNodePrefixWithoutRequiringWholeChain() {
        Long firstCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录创建执行", 2));
        codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录通知核验", 3));

        Long executionId = codexTestExecutionService.startExecution(startReq("SEQUENTIAL", firstCaseId), 99L);

        List<CodexTestExecutionCaseDO> executionCases =
                codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals(1, executionCases.size());
        assertEquals("批记录前置检查", executionCases.get(0).getCaseNameSnapshot());
    }

    @Test
    void startSequentialExecution_rejectsIncompleteNodeChainSelection() {
        codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        Long secondCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录创建执行", 2));

        assertServiceException(
                () -> codexTestExecutionService.startExecution(startReq("SEQUENTIAL", secondCaseId), 99L),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串必须从第 1 节点开始连续选择");
    }

    @Test
    void startSequentialExecution_rejectsMixedNodeChains() {
        Long batchChainCaseId = codexTestCaseService.createCase(
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("批记录前置检查", 1));
        CodexTestCaseSaveReqVO anotherChainReq =
                CodexTestCaseServiceImplTest.buildNodeChainCaseReq("工艺路线前置检查", 1);
        anotherChainReq.setNodeChainName("工艺路线串行验证");
        anotherChainReq.setProject("工艺路线");
        Long routeChainCaseId = codexTestCaseService.createCase(anotherChainReq);

        assertServiceException(
                () -> codexTestExecutionService.startExecution(
                        startReq("SEQUENTIAL", batchChainCaseId, routeChainCaseId), 99L),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "一次执行只能选择一个节点串");
    }

    @Test
    void startParallelExecution_rejectsUnsafeCaseWithoutDowngradingToSequential() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", false));

        assertServiceException(() -> codexTestExecutionService.startExecution(startReq("PARALLEL", caseId), 99L),
                CODEX_TEST_PARALLEL_UNSAFE_CASE, "排产手动重排");
    }

    @Test
    void startExecution_rejectsWhenOnDemandRunnerCannotStart() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", true));
        doThrow(exception(CODEX_TEST_RUNNER_START_FAILED, "Runner 按需启动脚本未配置"))
                .when(codexTestRunnerBootstrapService).ensureRunnerAvailable();

        assertServiceException(() -> codexTestExecutionService.startExecution(startReq("SEQUENTIAL", caseId), 99L),
                CODEX_TEST_RUNNER_START_FAILED, "Runner 按需启动脚本未配置");
    }

    @Test
    void getExecutionMonitor_returnsUnfinishedExecutionDetails() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", false));
        Long executionId = codexTestExecutionService.startExecution(startReq("SEQUENTIAL", caseId), 99L);

        List<CodexTestExecutionRespVO> monitorList = codexTestExecutionService.getExecutionMonitor();

        assertEquals(1, monitorList.size());
        CodexTestExecutionRespVO execution = monitorList.get(0);
        assertEquals(executionId, execution.getId());
        assertEquals("PENDING", execution.getStatus());
        assertEquals(1, execution.getCases().size());
        assertEquals("排产手动重排", execution.getCases().get(0).getCaseNameSnapshot());
        assertEquals(2, execution.getCases().get(0).getCheckpointResults().size());
    }

    private CodexTestExecutionStartReqVO startReq(String mode, Long... caseIds) {
        CodexTestExecutionStartReqVO reqVO = new CodexTestExecutionStartReqVO();
        reqVO.setTargetTenantId(88L);
        reqVO.setExecutionMode(mode);
        reqVO.setCaseIds(List.of(caseIds));
        return reqVO;
    }

}
