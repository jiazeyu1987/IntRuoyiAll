package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCasePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestNodeChainOptionRespVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCaseSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.codextest.vo.CodexTestCheckpointSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestCheckpointDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCaseMapper;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestCheckpointMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RESULT_SCHEMA_INVALID;
import static org.junit.jupiter.api.Assertions.*;

@Import(CodexTestCaseServiceImpl.class)
class CodexTestCaseServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CodexTestCaseService codexTestCaseService;
    @Resource
    private CodexTestCaseMapper codexTestCaseMapper;
    @Resource
    private CodexTestCheckpointMapper codexTestCheckpointMapper;

    @Test
    void createCase_persistsNaturalLanguageMethodAndArbitraryCheckpoints() {
        CodexTestCaseSaveReqVO reqVO = buildCaseReq("排产手动重排", false);

        Long caseId = codexTestCaseService.createCase(reqVO);

        CodexTestCaseDO testCase = codexTestCaseMapper.selectById(caseId);
        assertEquals("排产手动重排", testCase.getName());
        assertEquals("智能排产", testCase.getProject());
        assertEquals("在排产工单页签选择用户手写工单号后点击手动重排", testCase.getMethodText());
        assertEquals("来源生产工单号=881MO093613,881MO093615", testCase.getTestDataText());
        List<CodexTestCheckpointDO> checkpoints = codexTestCheckpointMapper.selectListByCaseId(caseId);
        assertEquals(2, checkpoints.size());
        assertEquals("重排成功", checkpoints.get(0).getExpectedText());
        assertEquals("只有两个目标工单进入甘特图", checkpoints.get(1).getExpectedText());
    }

    @Test
    void createCase_acceptsProcessRouteProject() {
        CodexTestCaseSaveReqVO reqVO = buildCaseReq("工艺路线基础信息与工序维护闭环", false);
        reqVO.setProject("工艺路线");

        Long caseId = codexTestCaseService.createCase(reqVO);

        assertEquals("工艺路线", codexTestCaseMapper.selectById(caseId).getProject());
    }

    @Test
    void createCase_persistsNodeChainFields() {
        CodexTestCaseSaveReqVO reqVO = buildNodeChainCaseReq("批记录前置检查", 1);

        Long caseId = codexTestCaseService.createCase(reqVO);

        CodexTestCaseDO testCase = codexTestCaseMapper.selectById(caseId);
        assertEquals("批记录串行验证", testCase.getNodeChainName());
        assertEquals(1, testCase.getNodeChainSort());
        assertEquals("SEQUENTIAL", testCase.getDefaultExecutionMode());
        assertFalse(testCase.getParallelSafe());
    }

    @Test
    void createCase_rejectsNodeChainWithoutSort() {
        CodexTestCaseSaveReqVO reqVO = buildNodeChainCaseReq("批记录前置检查", 1);
        reqVO.setNodeChainSort(null);

        assertServiceException(() -> codexTestCaseService.createCase(reqVO),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项的串内序号必须大于 0");
    }

    @Test
    void createCase_rejectsDuplicateNodeChainSort() {
        codexTestCaseService.createCase(buildNodeChainCaseReq("批记录前置检查", 1));
        CodexTestCaseSaveReqVO duplicateReqVO = buildNodeChainCaseReq("批记录创建执行", 1);

        assertServiceException(() -> codexTestCaseService.createCase(duplicateReqVO),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串【批记录串行验证】已存在第 1 节点");
    }

    @Test
    void createCase_rejectsParallelNodeChain() {
        CodexTestCaseSaveReqVO reqVO = buildNodeChainCaseReq("批记录前置检查", 1);
        reqVO.setDefaultExecutionMode("PARALLEL");

        assertServiceException(() -> codexTestCaseService.createCase(reqVO),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项只能使用顺序执行");
    }

    @Test
    void createCase_rejectsParallelSafeNodeChain() {
        CodexTestCaseSaveReqVO reqVO = buildNodeChainCaseReq("批记录前置检查", 1);
        reqVO.setParallelSafe(true);

        assertServiceException(() -> codexTestCaseService.createCase(reqVO),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "节点串测试项不允许标记为并行安全");
    }

    @Test
    void getNodeChainOptions_returnsDistinctChainsWithNodeCounts() {
        codexTestCaseService.createCase(buildNodeChainCaseReq("批记录前置检查", 1));
        codexTestCaseService.createCase(buildNodeChainCaseReq("批记录创建执行", 2));
        CodexTestCaseSaveReqVO routeReqVO = buildNodeChainCaseReq("工艺路线前置检查", 1);
        routeReqVO.setNodeChainName("工艺路线串行验证");
        routeReqVO.setProject("工艺路线");
        codexTestCaseService.createCase(routeReqVO);

        List<CodexTestNodeChainOptionRespVO> options = codexTestCaseService.getNodeChainOptions();

        assertEquals(2, options.size());
        assertEquals("工艺路线串行验证", options.get(0).getName());
        assertEquals("工艺路线", options.get(0).getProject());
        assertEquals(1, options.get(0).getNodeCount());
        assertEquals(2, options.get(0).getNextNodeSort());
        assertEquals("批记录串行验证", options.get(1).getName());
        assertEquals("批记录", options.get(1).getProject());
        assertEquals(2, options.get(1).getNodeCount());
        assertEquals(3, options.get(1).getNextNodeSort());
    }

    @Test
    void getCasePage_filtersOneNodeChainAndOrdersByNodeSort() {
        codexTestCaseService.createCase(buildNodeChainCaseReq("批记录创建执行", 2));
        codexTestCaseService.createCase(buildNodeChainCaseReq("批记录前置检查", 1));
        CodexTestCaseSaveReqVO routeReqVO = buildNodeChainCaseReq("工艺路线前置检查", 1);
        routeReqVO.setNodeChainName("工艺路线串行验证");
        routeReqVO.setProject("工艺路线");
        codexTestCaseService.createCase(routeReqVO);
        CodexTestCasePageReqVO pageReqVO = new CodexTestCasePageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setNodeChainName("批记录串行验证");

        PageResult<CodexTestCaseRespVO> pageResult = codexTestCaseService.getCasePage(pageReqVO);

        assertEquals(2, pageResult.getTotal());
        assertEquals(List.of("批记录前置检查", "批记录创建执行"),
                pageResult.getList().stream().map(CodexTestCaseRespVO::getName).toList());
    }

    @Test
    void updateCase_replacesCheckpointSnapshotForFutureExecutions() {
        Long caseId = codexTestCaseService.createCase(buildCaseReq("排产手动重排", false));
        CodexTestCaseSaveReqVO updateReqVO = buildCaseReq("排产手动重排-更新", true);
        updateReqVO.setId(caseId);
        updateReqVO.setCheckpoints(List.of(checkpoint(1, "最近一次成功排产时间更新")));

        codexTestCaseService.updateCase(updateReqVO);

        CodexTestCaseDO testCase = codexTestCaseMapper.selectById(caseId);
        assertEquals("排产手动重排-更新", testCase.getName());
        assertEquals("智能排产", testCase.getProject());
        assertTrue(testCase.getParallelSafe());
        List<CodexTestCheckpointDO> checkpoints = codexTestCheckpointMapper.selectListByCaseId(caseId);
        assertEquals(1, checkpoints.size());
        assertEquals("最近一次成功排产时间更新", checkpoints.get(0).getExpectedText());
    }

    @Test
    void createCase_rejectsUnknownProject() {
        CodexTestCaseSaveReqVO reqVO = buildCaseReq("未知项目测试项", false);
        reqVO.setProject("其它");

        assertServiceException(() -> codexTestCaseService.createCase(reqVO),
                CODEX_TEST_RESULT_SCHEMA_INVALID, "测试项项目必须是 智能排产、文控、批记录 或 工艺路线");
    }

    @Test
    void updateCase_allowsRepeatedCheckpointReplacement() {
        Long caseId = codexTestCaseService.createCase(buildCaseReq("排产手动重排", false));
        CodexTestCaseSaveReqVO firstUpdateReqVO = buildCaseReq("排产手动重排-第一次更新", true);
        firstUpdateReqVO.setId(caseId);
        firstUpdateReqVO.setCheckpoints(List.of(checkpoint(1, "第一次更新目标")));
        codexTestCaseService.updateCase(firstUpdateReqVO);

        CodexTestCaseSaveReqVO secondUpdateReqVO = buildCaseReq("排产手动重排-第二次更新", false);
        secondUpdateReqVO.setId(caseId);
        secondUpdateReqVO.setCheckpoints(List.of(
                checkpoint(1, "第二次更新目标 A"),
                checkpoint(2, "第二次更新目标 B")));
        codexTestCaseService.updateCase(secondUpdateReqVO);

        CodexTestCaseDO testCase = codexTestCaseMapper.selectById(caseId);
        assertEquals("排产手动重排-第二次更新", testCase.getName());
        List<CodexTestCheckpointDO> checkpoints = codexTestCheckpointMapper.selectListByCaseId(caseId);
        assertEquals(2, checkpoints.size());
        assertEquals("第二次更新目标 A", checkpoints.get(0).getExpectedText());
        assertEquals("第二次更新目标 B", checkpoints.get(1).getExpectedText());
    }

    static CodexTestCaseSaveReqVO buildCaseReq(String name, boolean parallelSafe) {
        CodexTestCaseSaveReqVO reqVO = new CodexTestCaseSaveReqVO();
        reqVO.setName(name);
        reqVO.setProject("智能排产");
        reqVO.setMethodText("在排产工单页签选择用户手写工单号后点击手动重排");
        reqVO.setTestDataText("来源生产工单号=881MO093613,881MO093615");
        reqVO.setDefaultExecutionMode("SEQUENTIAL");
        reqVO.setParallelSafe(parallelSafe);
        reqVO.setStatus("ENABLE");
        reqVO.setSort(10);
        reqVO.setCheckpoints(List.of(
                checkpoint(1, "重排成功"),
                checkpoint(2, "只有两个目标工单进入甘特图")));
        return reqVO;
    }

    static CodexTestCaseSaveReqVO buildNodeChainCaseReq(String name, int nodeChainSort) {
        CodexTestCaseSaveReqVO reqVO = buildCaseReq(name, false);
        reqVO.setProject("批记录");
        reqVO.setNodeChainName("批记录串行验证");
        reqVO.setNodeChainSort(nodeChainSort);
        return reqVO;
    }

    static CodexTestCheckpointSaveReqVO checkpoint(Integer sort, String expectedText) {
        CodexTestCheckpointSaveReqVO checkpoint = new CodexTestCheckpointSaveReqVO();
        checkpoint.setSort(sort);
        checkpoint.setName("检查点 " + sort);
        checkpoint.setExpectedText(expectedText);
        checkpoint.setSeverity("MAJOR");
        return checkpoint;
    }

}
