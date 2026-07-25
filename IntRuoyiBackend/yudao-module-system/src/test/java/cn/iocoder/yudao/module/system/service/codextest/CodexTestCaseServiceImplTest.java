package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
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
        assertEquals("在排产工单页签选择用户手写工单号后点击手动重排", testCase.getMethodText());
        assertEquals("来源生产工单号=881MO093613,881MO093615", testCase.getTestDataText());
        List<CodexTestCheckpointDO> checkpoints = codexTestCheckpointMapper.selectListByCaseId(caseId);
        assertEquals(2, checkpoints.size());
        assertEquals("重排成功", checkpoints.get(0).getExpectedText());
        assertEquals("只有两个目标工单进入甘特图", checkpoints.get(1).getExpectedText());
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
        assertTrue(testCase.getParallelSafe());
        List<CodexTestCheckpointDO> checkpoints = codexTestCheckpointMapper.selectListByCaseId(caseId);
        assertEquals(1, checkpoints.size());
        assertEquals("最近一次成功排产时间更新", checkpoints.get(0).getExpectedText());
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

    static CodexTestCheckpointSaveReqVO checkpoint(Integer sort, String expectedText) {
        CodexTestCheckpointSaveReqVO checkpoint = new CodexTestCheckpointSaveReqVO();
        checkpoint.setSort(sort);
        checkpoint.setName("检查点 " + sort);
        checkpoint.setExpectedText(expectedText);
        checkpoint.setSeverity("MAJOR");
        return checkpoint;
    }

}
