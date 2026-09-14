package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CanonicalPqcSubmissionV2Test {

    @Test
    void hashExcludesRemovedManualDefectDescription() {
        MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder()
                .id(1001L)
                .activeOrderId(2001L)
                .regulationVersionId(3001L)
                .qaProcessId(4001L)
                .qaItemCode("QA-001")
                .inspectionRuleKey("FIRST")
                .build();
        MesFrontlinePqcSubmitCommand command = MesFrontlinePqcSubmitCommand.builder()
                .actualEmployeeId(5001L)
                .productionSubmitEventId(6001L)
                .actualInspectionQuantity(1)
                .scrapQuantity(0)
                .build();
        MesPqcInspectionPieceDetailDO detail = MesPqcInspectionPieceDetailDO.builder()
                .itemCode("QA-001")
                .sampleNo(1)
                .selectedEquipmentId(7001L)
                .selectedEquipmentNumber("EQ-001")
                .measuredValue("合格")
                .build();

        String actual = CanonicalPqcSubmissionV2.hash(task, command, List.of(detail));
        String expectedWithoutDescription = CanonicalPqcSubmissionV2.hash(task, command, List.of(detail));
        String legacyWithDescription = CanonicalPqcSubmissionV1.hash(
                task, command, "历史不良说明", List.of(detail));

        assertEquals(expectedWithoutDescription, actual);
        assertNotEquals(legacyWithDescription, actual);
    }
}
