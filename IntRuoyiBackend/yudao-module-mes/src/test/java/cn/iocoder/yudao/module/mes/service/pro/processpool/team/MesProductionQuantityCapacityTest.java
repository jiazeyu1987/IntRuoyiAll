package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProductionQuantityCapacityTest {
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshots = mock(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
    private final MesProcessPoolReportAllocationMapper allocations = mock(MesProcessPoolReportAllocationMapper.class);
    private final MesProProcessPoolEventMapper events = mock(MesProProcessPoolEventMapper.class);
    private final MesProductionQuantityCapacityService service = new MesProductionQuantityCapacityService(orders, snapshots, allocations, events);

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return new MesProcessPoolActiveOrderProcessSnapshotDO().setActiveOrderId(1L).setWorkOrderId(2L)
                .setRouteProcessId(3L).setProcessId(4L).setErpFixedQuantitySnapshot(new BigDecimal("1000"))
                .setProductionQuantityFactorSnapshot(new BigDecimal("1.1"))
                .setPlannedQuantitySnapshot(new BigDecimal("1100.000000"))
                .setProductionConfigSnapshotJson("{\"outputMaterialIds\":[11,12]}");
    }

    private MesProFrontlineFeedbackSubmitReqVO request(String material11, String material12) {
        var request = new MesProFrontlineFeedbackSubmitReqVO();
        request.setProcessPoolContext(new MesProFrontlineProcessPoolContextReqVO()
                .setActiveOrderId(1L).setWorkOrderId(2L).setRouteProcessId(3L).setProcessId(4L));
        request.setFeedbackPayload(new MesProFrontlineFeedbackPayloadReqVO().setOutputQuantity(
                new BigDecimal(material11).min(new BigDecimal(material12))));
        request.setMaterialDetails(List.of(new MesProFrontlineFeedbackMaterialReqVO().setMaterialId(11L)
                        .setOutputQuantity(new BigDecimal(material11)),
                new MesProFrontlineFeedbackMaterialReqVO().setMaterialId(12L).setOutputQuantity(new BigDecimal(material12))));
        return request;
    }

    private void previous(String material11, String material12) {
        when(orders.selectByIdForUpdate(1L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(1L)
                .setWorkOrderId(2L).setRouteId(5L).setActiveStatus("ACTIVE"));
        when(snapshots.selectByActiveOrderAndProcess(1L,3L,4L)).thenReturn(snapshot());
        BigDecimal pool = new BigDecimal(material11).min(new BigDecimal(material12));
        when(allocations.selectListByActiveOrderIdsAndProcessForUpdate(List.of(1L),4L)).thenReturn(List.of(
                MesProcessPoolReportAllocationDO.builder().id(21L).eventId(22L).activeOrderId(1L)
                        .workOrderId(2L).routeProcessId(3L).processId(4L).allocatedQuantity(pool)
                        .lifecycleStatus("CURRENT").build()));
        when(events.selectProductionSubmitsByIdsForUpdate(List.of(22L))).thenReturn(List.of(
                MesProProcessPoolEventDO.builder().id(22L).workOrderId(2L).routeId(5L).routeProcessId(3L).processId(4L)
                        .eventType("PRODUCTION_SUBMIT").reportOutputQuantity(pool)
                        .rawPayload("{\"materialDetails\":[{\"materialId\":11,\"outputQuantity\":"+material11+"},{\"materialId\":12,\"outputQuantity\":"+material12+"}]}").build()));
    }

    @Test void ceilingIncludesPendingUnreviewedAllocationsAndAcceptsExactBoundary() {
        previous("1000", "1099.99");
        var capacity = service.evaluate(request("100", "0.01"));
        assertFalse(capacity.exceeded());
        assertEquals(0, capacity.projected().compareTo(new BigDecimal("1100")));
    }
    @Test void oneMaterialAboveCeilingBlocksEvenWhenConservativeProgressIsLow() {
        previous("1000", "1099.99");
        var capacity = service.evaluate(request("0", "0.02"));
        assertTrue(capacity.exceeded());
        assertEquals(0, capacity.projected().compareTo(new BigDecimal("1100.01")));
    }
    @Test void lossCoefficientDoesNotStackOnProductionFactor() {
        previous("1100", "1100");
        assertTrue(service.evaluate(request("1", "1")).exceeded());
    }
    @Test void completionFloorAndSubmissionCeilingRemainDistinct() {
        var range = MesProductionQuantityRange.of(snapshot());
        assertEquals(new BigDecimal("1000"),range.lower());
        assertEquals(new BigDecimal("1100.000000"),range.upper());
        var independent = MesProductionQuantityRange.of(snapshot()
                .setPlannedQuantitySnapshot(new BigDecimal("2500"))
                .setProductionQuantityFactorSnapshot(new BigDecimal("2.5")));
        assertEquals(range, independent);
        var configured = MesProductionQuantityRange.of(snapshot()
                .setProductionConfigSnapshotJson("{\"lossCoefficient\":1.2,\"outputMaterialIds\":[11,12]}"));
        assertEquals(new BigDecimal("1200.000000"), configured.upper());
        assertThrows(IllegalArgumentException.class, () -> MesProductionQuantityRange.of(snapshot()
                .setProductionConfigSnapshotJson("{\"lossCoefficient\":0.9}")));
    }

    private MesProFrontlineFeedbackSubmitReqVO withoutOutputMaterials(String quantity) {
        previous("600", "600");
        when(snapshots.selectByActiveOrderAndProcess(1L,3L,4L)).thenReturn(
                snapshot().setProductionConfigSnapshotJson("{\"outputMaterialIds\":[]}"));
        when(events.selectProductionSubmitsByIdsForUpdate(List.of(22L))).thenReturn(List.of(
                MesProProcessPoolEventDO.builder().id(22L).workOrderId(2L).routeId(5L)
                        .routeProcessId(3L).processId(4L).eventType("PRODUCTION_SUBMIT")
                        .reportOutputQuantity(new BigDecimal("600")).rawPayload("{}").build()));
        var command = request(quantity, quantity);
        command.setMaterialDetails(List.of());
        return command;
    }

    @Test void noOutputMaterialProcessBlocksSecondSubmissionUsingFormalAllocatedQuantity() {
        var capacity = service.evaluate(withoutOutputMaterials("600"));
        assertEquals(0, capacity.previous().compareTo(new BigDecimal("600")));
        assertEquals(0, capacity.projected().compareTo(new BigDecimal("1200")));
        assertTrue(capacity.exceeded());
    }

    @Test void noOutputMaterialProcessAcceptsExactCeilingWithoutRequiringMaterialDetails() {
        var capacity = service.evaluate(withoutOutputMaterials("500"));
        assertEquals(0, capacity.projected().compareTo(new BigDecimal("1100")));
        assertFalse(capacity.exceeded());
    }

    @Test void noOutputMaterialProcessStillRejectsMissingFormalSourceEvent() {
        var command = withoutOutputMaterials("500");
        when(events.selectProductionSubmitsByIdsForUpdate(List.of(22L))).thenReturn(List.of());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.evaluate(command));
    }
}
