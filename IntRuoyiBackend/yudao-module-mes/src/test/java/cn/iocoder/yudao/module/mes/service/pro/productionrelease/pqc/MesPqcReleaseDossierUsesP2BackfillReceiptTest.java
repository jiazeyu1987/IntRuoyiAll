package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseBatchRecordWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossReportWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseProcessInspectionWriter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcReleaseDossierUsesP2BackfillReceiptTest {

    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    @Mock private MesProProcessPoolEventMapper eventMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock private MesTeamLeaderActiveOrderReleaseBatchRecordWriter batchRecordWriter;
    @Mock private MesTeamLeaderActiveOrderReleaseProcessInspectionWriter processInspectionWriter;
    @Mock private MesTeamLeaderActiveOrderReleaseLossReportWriter lossReportWriter;
    @Mock private MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher sourceSnapshotHasher;
    @Mock private MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort completionReceiptPort;

    private MesPqcReleaseDossierPortImpl port;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        port = new MesPqcReleaseDossierPortImpl(activeOrderMapper, pickListBindingMapper, workOrderMapper,
                processSnapshotMapper, completionMapper, pqcTaskMapper, aggregateDetailMapper,
                eventMapper, allocationMapper, reviewMapper, batchRecordWriter, processInspectionWriter,
                lossReportWriter, sourceSnapshotHasher, completionReceiptPort);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void p2CompletionBackfillReceiptIsTheFormalPqcReleaseDossier() {
        when(activeOrderMapper.selectById(10L)).thenReturn(activeOrder());
        when(pickListBindingMapper.selectListByActiveOrderId(10L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingDO.builder().id(601L).activeOrderId(10L).build()));
        when(workOrderMapper.selectById(20L)).thenReturn(workOrder());
        when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(snapshot()));
        when(completionMapper.selectListByWorkOrderIds(List.of(20L))).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(10L)).thenReturn(List.of());
        when(aggregateDetailMapper.selectListByActiveOrderId(10L)).thenReturn(List.of());
        when(sourceSnapshotHasher.hash(any())).thenReturn("application-source-hash");
        when(completionReceiptPort.getByActiveOrderId(10L, 1L)).thenReturn(p2Receipt());

        MesPqcReleaseDossierPlan plan = port.plan(application(), 7101L);
        MesPqcReleaseDossierWriteResult write = port.write(plan, 9001L);

        assertTrue(Boolean.TRUE.equals(plan.getCompletionBackfillDossier()));
        assertEquals(List.of(91L), write.getBatchRecordEvidenceIds());
        assertEquals(List.of(92L), write.getProcessInspectionEvidenceIds());
        assertEquals(List.of(), write.getLossReportEvidenceIds());
        assertEquals("NOT_REQUIRED", write.getLossReportStatus());
        assertEquals(Boolean.FALSE, write.getHasActualLoss());
        assertEquals(BigDecimal.ZERO, write.getLossQuantity());
        verify(batchRecordWriter, never()).plan(any());
        verify(batchRecordWriter, never()).write(any(), any());
        verify(processInspectionWriter, never()).plan(any());
        verify(processInspectionWriter, never()).write(any(), any());
        verify(lossReportWriter, never()).plan(any());
        verify(lossReportWriter, never()).write(any(), any());
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(55L).setActiveOrderId(10L).setWorkOrderId(20L)
                .setRouteId(30L).setRouteVersionId(31L).setProductId(40L)
                .setBatchCode("BATCH-10").setSourceSnapshotHash("application-source-hash");
    }

    private MesProcessPoolActiveOrderDO activeOrder() {
        return MesProcessPoolActiveOrderDO.builder()
                .id(10L).workOrderId(20L).routeId(30L).routeVersionId(31L)
                .dccProjectCodeId(80L).build();
    }

    private MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(20L).code("WO-10").productId(40L).batchCode("BATCH-10").build();
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(501L).activeOrderId(10L).workOrderId(20L)
                .routeId(30L).routeVersionId(31L).routeProcessId(301L).processId(401L).build();
    }

    private MesFlow6CompletionBackfillReceipt p2Receipt() {
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(90L).setTenantId(1L).setActiveOrderId(10L).setWorkOrderId(20L)
                .setRouteId(30L).setRouteVersionId(31L).setBatchCode("BATCH-10")
                .setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setBatchRecordStatus("SUCCESS").setProcessInspectionStatus("SUCCESS")
                .setBatchRecordId(91L).setProcessInspectionId(92L)
                .setSourceSnapshotHash("p2-detail-source-hash")
                .setFormalSourceSnapshotJson("{\"p2\":\"formal-detail\"}")
                .setSignatureSnapshotJson("{\"signed\":true}")
                .setBatchRecordSourceIdsJson("[101,102]")
                .setProcessInspectionSourceIdsJson("[201,202]")
                .setHasActualLoss(false).setLossQuantity(BigDecimal.ZERO)
                .setLossReportStatus("NOT_REQUIRED")
                .setZeroLossConfirmationSnapshot("{\"status\":\"NO_FORMAL_LOSS\"}");
    }
}
