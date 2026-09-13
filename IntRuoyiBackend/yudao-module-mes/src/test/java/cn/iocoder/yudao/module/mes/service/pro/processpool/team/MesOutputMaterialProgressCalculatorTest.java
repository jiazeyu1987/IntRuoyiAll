package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesOutputMaterialProgressCalculatorTest {

    @Test
    void partialAllocationCapsWholeEventMaterialDetails() {
        BigDecimal progress = MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(
                activeOrder(10L, 30L),
                snapshot(10L, 30L, "{\"outputMaterialIds\":[501]}"),
                List.of(productionSubmit(401L, 30L,
                        "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":100}]}")),
                List.of(allocation(201L, 10L, 30L, 401L, "40")));

        assertEquals(BigDecimal.valueOf(40).setScale(6), progress);
    }

    @Test
    void currentOrderProgressIgnoresOtherOrderAllocationQuantityForSameEvent() {
        BigDecimal progress = MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(
                activeOrder(10L, 30L),
                snapshot(10L, 30L, "{\"outputMaterialIds\":[501]}"),
                List.of(productionSubmit(401L, 30L,
                        "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":100}]}")),
                List.of(
                        allocation(201L, 10L, 30L, 401L, "40"),
                        allocation(202L, 11L, 31L, 401L, "60")));

        assertEquals(BigDecimal.valueOf(40).setScale(6), progress);
    }

    @Test
    void multiOutputMaterialsAreCappedByAllocationQuantityBeforeMinimumProgress() {
        BigDecimal progress = MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress(
                activeOrder(10L, 30L),
                snapshot(10L, 30L, "{\"outputMaterialIds\":[501,502]}"),
                List.of(productionSubmit(401L, 30L,
                        "{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":100},"
                                + "{\"materialId\":502,\"outputQuantity\":80}]}")),
                List.of(allocation(201L, 10L, 30L, 401L, "40")));

        assertEquals(BigDecimal.valueOf(40).setScale(6), progress);
    }

    private MesProcessPoolActiveOrderDO activeOrder(Long activeOrderId, Long workOrderId) {
        return MesProcessPoolActiveOrderDO.builder()
                .id(activeOrderId)
                .workOrderId(workOrderId)
                .routeId(40L)
                .build();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot(Long activeOrderId, Long workOrderId,
                                                                String productionConfigSnapshotJson) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeId(40L)
                .routeProcessId(101L)
                .processId(1L)
                .plannedQuantitySnapshot(BigDecimal.valueOf(100))
                .productionConfigSnapshotJson(productionConfigSnapshotJson)
                .build();
    }

    private MesProProcessPoolEventDO productionSubmit(Long eventId, Long workOrderId, String rawPayload) {
        return MesProProcessPoolEventDO.builder()
                .id(eventId)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(workOrderId)
                .routeId(40L)
                .routeProcessId(101L)
                .processId(1L)
                .rawPayload(rawPayload)
                .build();
    }

    private MesProcessPoolReportAllocationDO allocation(Long allocationId, Long activeOrderId, Long workOrderId,
                                                        Long eventId, String allocatedQuantity) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(allocationId)
                .eventId(eventId)
                .activeOrderId(activeOrderId)
                .workOrderId(workOrderId)
                .routeProcessId(101L)
                .processId(1L)
                .allocatedQuantity(new BigDecimal(allocatedQuantity))
                .build();
    }
}
