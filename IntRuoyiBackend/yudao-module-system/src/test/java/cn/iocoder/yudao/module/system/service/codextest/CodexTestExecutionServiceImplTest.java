package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
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
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_PARALLEL_UNSAFE_CASE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_OFFLINE;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void startSequentialExecution_createsHistoricalSnapshotsAndInitialCheckpointResults() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", false));
        insertOnlineRunner();
        CodexTestExecutionStartReqVO reqVO = startReq("SEQUENTIAL", caseId);

        Long executionId = codexTestExecutionService.startExecution(reqVO, 99L);

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
    void startParallelExecution_rejectsUnsafeCaseWithoutDowngradingToSequential() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", false));
        insertOnlineRunner();

        assertServiceException(() -> codexTestExecutionService.startExecution(startReq("PARALLEL", caseId), 99L),
                CODEX_TEST_PARALLEL_UNSAFE_CASE, "排产手动重排");
    }

    @Test
    void startExecution_rejectsWhenRunnerOffline() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", true));

        assertServiceException(() -> codexTestExecutionService.startExecution(startReq("SEQUENTIAL", caseId), 99L),
                CODEX_TEST_RUNNER_OFFLINE);
    }

    @Test
    void getExecutionMonitor_returnsUnfinishedExecutionDetails() {
        Long caseId = codexTestCaseService.createCase(CodexTestCaseServiceImplTest.buildCaseReq("排产手动重排", false));
        insertOnlineRunner();
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

    private void insertOnlineRunner() {
        CodexTestRunnerSessionDO runner = new CodexTestRunnerSessionDO();
        runner.setRunnerName("local-runner");
        runner.setStatus("ONLINE");
        runner.setCapabilitiesJson("{\"playwright\":true,\"codex\":true}");
        runner.setMaxParallelism(2);
        runner.setLastHeartbeatTime(LocalDateTime.now());
        runner.setCurrentRunningCount(0);
        codexTestRunnerSessionMapper.insert(runner);
    }

    private CodexTestExecutionStartReqVO startReq(String mode, Long caseId) {
        CodexTestExecutionStartReqVO reqVO = new CodexTestExecutionStartReqVO();
        reqVO.setTargetTenantId(88L);
        reqVO.setExecutionMode(mode);
        reqVO.setCaseIds(List.of(caseId));
        return reqVO;
    }

}
