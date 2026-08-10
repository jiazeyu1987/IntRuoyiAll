package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCodeReadonlyCaseReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCodeReadonlyExecutionStartReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCheckpointSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestExecutionDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestExecutionMapper;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_EXECUTION_NOT_EXISTS;
import static org.mockito.Mockito.verify;

@Import({CodexTestCaseServiceImpl.class, CodexTestExecutionServiceImpl.class})
class CodexTestCodeReadonlyExecutionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CodexTestExecutionService codexTestExecutionService;
    @Resource
    private CodexTestCaseService codexTestCaseService;
    @Resource
    private CodexTestExecutionMapper codexTestExecutionMapper;
    @Resource
    private CodexTestExecutionCaseMapper codexTestExecutionCaseMapper;

    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private CodexTestRunnerBootstrapService codexTestRunnerBootstrapService;

    @Test
    void startCodeReadonlyExecution_upsertsDefinitionAndCreatesCliExecutionSnapshot() {
        CodexTestCodeReadonlyExecutionStartReqVO reqVO = new CodexTestCodeReadonlyExecutionStartReqVO();
        reqVO.setTargetTenantId(88L);
        reqVO.setCaseDefinition(buildCaseDefinition());

        Long executionId = codexTestExecutionService.startCodeReadonlyExecution(reqVO, 99L);

        verify(codexTestRunnerBootstrapService).ensureRunnerAvailable();
        CodexTestExecutionDO execution = codexTestExecutionMapper.selectById(executionId);
        assertEquals("PENDING", execution.getStatus());
        assertEquals("SEQUENTIAL", execution.getExecutionMode());
        assertEquals(88L, execution.getTargetTenantId());
        assertEquals(99L, execution.getRequestedBy());
        List<CodexTestExecutionCaseDO> executionCases =
                codexTestExecutionCaseMapper.selectListByExecutionId(executionId);
        assertEquals(1, executionCases.size());
        assertEquals("批记录测试-生产组长-01-工艺路线配置", executionCases.get(0).getCaseNameSnapshot());
        assertEquals("CODE_READONLY", executionCases.get(0).getAnalysisModeSnapshot());
        assertEquals("只读扫描当前代码，检查工艺路线配置", executionCases.get(0).getMethodTextSnapshot());
        assertEquals(executionId, codexTestExecutionService.getExecutionResult(executionId, 99L).getId());
        assertServiceException(() -> codexTestExecutionService.getExecutionResult(executionId, 100L),
                CODEX_TEST_EXECUTION_NOT_EXISTS);
    }

    @Test
    void upsertCodeReadonlyCase_updatesSameDefinitionWithoutCreatingDuplicate() {
        CodexTestCodeReadonlyCaseReqVO definition = buildCaseDefinition();
        Long firstCaseId = codexTestCaseService.upsertCodeReadonlyCase(definition);
        definition.setMethodText("只读扫描当前代码，检查更新后的职责描述");
        definition.getCheckpoints().get(0).setRemark("更新后的职责描述");

        Long secondCaseId = codexTestCaseService.upsertCodeReadonlyCase(definition);

        assertEquals(firstCaseId, secondCaseId);
        CodexTestCaseRespVO savedCase = codexTestCaseService.getCase(secondCaseId);
        assertEquals("CODE_READONLY", savedCase.getAnalysisMode());
        assertEquals("SEQUENTIAL", savedCase.getDefaultExecutionMode());
        assertEquals("只读扫描当前代码，检查更新后的职责描述", savedCase.getMethodText());
        assertEquals("更新后的职责描述", savedCase.getCheckpoints().get(0).getRemark());
    }

    private static CodexTestCodeReadonlyCaseReqVO buildCaseDefinition() {
        CodexTestCodeReadonlyCaseReqVO definition = new CodexTestCodeReadonlyCaseReqVO();
        definition.setName("批记录测试-生产组长-01-工艺路线配置");
        definition.setProject("批记录");
        definition.setMethodText("只读扫描当前代码，检查工艺路线配置");
        definition.setTestDataText("测试范围：生产组长职责-工艺路线配置");
        definition.setSort(1);
        CodexTestCheckpointSaveReqVO checkpoint = new CodexTestCheckpointSaveReqVO();
        checkpoint.setSort(1);
        checkpoint.setName("工艺路线生产组长配置");
        checkpoint.setRemark("生产组长可配置工艺路线");
        checkpoint.setExpectedText("当前代码完整支持描述要求");
        checkpoint.setSeverity("MAJOR");
        definition.setCheckpoints(List.of(checkpoint));
        return definition;
    }

}
