package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MesFrontlinePqcSubmittedInspectionTest {
    @Test
    void readsActualQuantityAndLossAndKeepsEachCardsSamplesInOrder() {
        var task = task();
        var snapshot = MesFrontlinePqcContextServiceImpl.buildSubmittedInspection(task,
                event("{\"scrapQuantity\":2}"), List.of(piece("A", 2, "不合格"),
                        piece("B", 1, "1.25"), piece("A", 1, "合格")));
        assertEquals(3, snapshot.getActualInspectionQuantity());
        assertEquals(2, snapshot.getScrapQuantity());
        assertEquals(List.of("合格", "不合格"), snapshot.getItems().stream()
                .filter(item -> item.getItemCode().equals("A")).findFirst().orElseThrow().getSampleValues());
        assertEquals(List.of("1.25"), snapshot.getItems().stream()
                .filter(item -> item.getItemCode().equals("B")).findFirst().orElseThrow().getSampleValues());
    }

    @Test
    void missingLossOrSamplesAndForeignTaskDetailsFailInsteadOfDisplayingDefaults() {
        assertThrows(ServiceException.class, () -> MesFrontlinePqcContextServiceImpl.buildSubmittedInspection(
                task(), event("{}"), List.of(piece("A", 1, "合格"))));
        assertThrows(ServiceException.class, () -> MesFrontlinePqcContextServiceImpl.buildSubmittedInspection(
                task(), event("{\"scrapQuantity\":0}"), List.of()));
        var foreign = piece("A", 1, "合格").setTaskId(2L);
        assertThrows(ServiceException.class, () -> MesFrontlinePqcContextServiceImpl.buildSubmittedInspection(
                task(), event("{\"scrapQuantity\":0}"), List.of(foreign)));
    }

    private MesPqcInspectionTaskDO task() {
        return new MesPqcInspectionTaskDO().setId(1L).setPlannedInspectionQuantity(80).setActualInspectionQuantity(3);
    }
    private MesProProcessPoolEventDO event(String payload) {
        return new MesProProcessPoolEventDO().setRawPayload(payload);
    }
    private MesPqcInspectionPieceDetailDO piece(String item, int sample, String value) {
        return new MesPqcInspectionPieceDetailDO().setTaskId(1L).setItemCode(item)
                .setSampleNo(sample).setMeasuredValue(value).setSelectedEquipmentNumber("设备001");
    }
}
