package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcInspectionItemPlan;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MesOrderQaStageSubmissionTest {
    @org.mockito.InjectMocks
    private MesFrontlinePqcContextServiceImpl service;
    @Test
    void oneStageStoresAllItemsWithTheirOwnSampleQuantities() {
        var task = MesPqcInspectionTaskDO.builder().id(1L).plannedInspectionQuantity(5)
                .inspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("A", 2, "B", 5))).build();
        var command = command(List.of("合格", "合格"));
        List<MesPqcInspectionPieceDetailDO> pieces = ReflectionTestUtils.invokeMethod(
                service, "buildPieceDetails", task, command,
                List.of(item("A"), item("B")));
        assertNotNull(pieces);
        assertEquals(7, pieces.size());
        assertEquals(2, pieces.stream().filter(piece -> "A".equals(piece.getItemCode())).count());
        assertEquals(5, pieces.stream().filter(piece -> "B".equals(piece.getItemCode())).count());
        assertTrue(pieces.stream().allMatch(piece -> Long.valueOf(1).equals(piece.getTaskId())));
    }

    @Test
    void rejectsUsingTheStageMaximumAsEveryItemsSampleQuantity() {
        var task = MesPqcInspectionTaskDO.builder().id(1L).plannedInspectionQuantity(5)
                .inspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("A", 2, "B", 5))).build();
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "buildPieceDetails", task,
                command(List.of("合格", "合格", "合格", "合格", "合格")), List.of(item("A"), item("B"))));
    }

    private MesFrontlinePqcSubmitCommand command(List<String> a) {
        var command = new MesFrontlinePqcSubmitCommand();
        command.setActualInspectionQuantity(5);
        command.setItemResults(List.of(
                MesFrontlinePqcSubmitCommand.ItemResult.builder().itemCode("A").sampleValues(a).build(),
                MesFrontlinePqcSubmitCommand.ItemResult.builder().itemCode("B")
                        .sampleValues(List.of("合格", "合格", "合格", "合格", "合格")).build()));
        return command;
    }

    private MesFrontlinePqcInspectionItem item(String code) {
        return new MesFrontlinePqcInspectionItem(code, code, "目视", "合格", null, "QA抽样",
                null, null, null, null, false, "BOOLEAN", List.of());
    }
}
