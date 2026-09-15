package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseLossSourceReaderTest {

    private static final Long ACTIVE_ORDER_ID = 8101L;
    private static final Long WORK_ORDER_ID = 9001L;
    private static final Long ROUTE_ID = 7001L;
    private static final Long ROUTE_VERSION_ID = 7002L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;

    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesFrontlineProcessMaterialService materialService;
    @Mock private ErpKingdeeProductionReplenishmentListMapper replenishmentMapper;
    @Mock private ErpKingdeeProductionReplenishmentListItemMapper replenishmentItemMapper;
    private MesTeamLeaderActiveOrderReleaseLossSourceReader reader;

    @BeforeEach
    void setUp() {
        reader = new MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl(
                eventMapper, feedbackMapper, allocationMapper, reviewMapper,
                workOrderMapper, materialService, replenishmentItemMapper, replenishmentMapper);
        when(workOrderMapper.selectByIdForUpdate(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).code("MO-9001").build());
        var event = event("{}");
        org.mockito.Mockito.lenient().when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(event));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(List.of(allocation()));
        org.mockito.Mockito.lenient().when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L))).thenReturn(List.of(feedback()));
        when(reviewMapper.selectListByEventIdForUpdate(event.getId())).thenReturn(List.of(review()));
    }

    @Test
    void crossOrderSourceCanPassTheRealLossReaderAndWriter() {
        var sourceEvent = event("{}").setWorkOrderId(9000L);
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(List.of(1001L))).thenReturn(List.of(sourceEvent));
        when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L)))
                .thenReturn(List.of(feedback().setWorkOrderId(9000L)));
        var writer = new MesTeamLeaderActiveOrderReleaseLossReportWriterImpl(
                reader, null, null, null, null, null, null, null, null, null, null);

        var plan = writer.plan(command().setConfirmNoReplenishmentInfo(true));

        assertTrue(plan.getBlockers().isEmpty(), () -> plan.getBlockers().toString());
        assertEquals("NO_LOSS", plan.getLossDecision());
        assertEquals(9000L, sourceEvent.getWorkOrderId());
    }

    @Test
    void crossOrderFeedbackMustStillBelongToItsSourceEvent() {
        when(eventMapper.selectProductionSubmitsByIdsForUpdate(List.of(1001L)))
                .thenReturn(List.of(event("{}").setWorkOrderId(9000L)));
        var result = reader.read(command().setConfirmNoReplenishmentInfo(true));
        assertTrue(result.getBlockers().stream().anyMatch(b -> "LOSS_SOURCE_REQUIRED".equals(b.getBlockerType())));
    }

    @Test
    void noReplenishmentMustAskForCompletionConfirmation() {
        var result = reader.read(command());
        assertTrue(result.getBlockers().stream().anyMatch(blocker ->
                "NO_REPLENISHMENT_CONFIRMATION_REQUIRED".equals(blocker.getBlockerType())));
    }

    @Test
    void confirmedNoReplenishmentIgnoresReusableProductionLoss() {
        var result = reader.read(command().setConfirmNoReplenishmentInfo(true));
        assertTrue(result.getBlockers().isEmpty());
        var source = result.getProcessSources().get(0);
        assertEquals(new BigDecimal("2.500"), source.getFeedback().getUnqualifiedQuantity());
        assertEquals(BigDecimal.ZERO, source.getFormalLossQuantity());
        assertEquals(false, source.getHasActualLoss());
        assertEquals(true, source.getZeroLossConfirmed());
        assertTrue(source.getReplenishmentSources().isEmpty());
    }

    @Test
    void multipleReplenishmentDocumentsRetainEverySourceAndSumActualQuantities() {
        when(replenishmentItemMapper.selectListByProductionOrderNo("MO-9001")).thenReturn(List.of(
                ErpKingdeeProductionReplenishmentListItemDO.builder().id(9101L).productionReplenishmentListId(91L)
                        .productionOrderNo("MO-9001").materialNumber("MAT-1").materialName("材料一")
                        .lotNumber("LOT-1").actualQuantity(new BigDecimal("3.000")).build(),
                ErpKingdeeProductionReplenishmentListItemDO.builder().id(9201L).productionReplenishmentListId(92L)
                        .productionOrderNo("MO-9001").materialNumber("MAT-1").materialName("材料一")
                        .lotNumber("LOT-2").actualQuantity(new BigDecimal("4.000")).build()));
        when(replenishmentMapper.selectBatchIds(List.of(91L, 92L))).thenReturn(List.of(
                ErpKingdeeProductionReplenishmentListDO.builder().id(91L).sourceBillNo("BL-91").documentStatus("C").build(),
                ErpKingdeeProductionReplenishmentListDO.builder().id(92L).sourceBillNo("BL-92").documentStatus("C").build()));
        when(materialService.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(List.of(new MesFrontlineProcessMaterial(1L, "MAT-1", "材料一", "", "INPUT",
                        BigDecimal.ONE, List.of(), null, null, null, List.of(), List.of(), List.of(), null)));
        var result = reader.read(command());
        assertTrue(result.getBlockers().isEmpty());
        var source = result.getProcessSources().get(0);
        assertEquals(new BigDecimal("7.000"), source.getFormalLossQuantity());
        assertEquals(List.of("BL-91", "BL-92"), source.getReplenishmentSources().stream()
                .map(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource::getSourceBillNo).toList());
        assertEquals(List.of("LOT-1", "LOT-2"), source.getReplenishmentSources().stream()
                .map(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ReplenishmentSource::getLotNumber).toList());
    }

    @Test
    void replenishmentQuantityAndAllDocumentIdentitiesAreAuthoritative() {
        stubReplenishment("C", new BigDecimal("3.000"));
        var result = reader.read(command());
        assertTrue(result.getBlockers().isEmpty());
        var source = result.getProcessSources().get(0);
        assertEquals(new BigDecimal("3.000"), source.getFormalLossQuantity());
        assertEquals("BL-91", source.getReplenishmentSources().get(0).getSourceBillNo());
        assertEquals("LOT-1", source.getReplenishmentSources().get(0).getLotNumber());
        assertEquals(9101L, source.getReplenishmentSources().get(0).getItemId());
        assertEquals("REQUIRED", source.getLossDecision());
    }

    @Test
    void invalidOrUnauditedReplenishmentCannotBeConfirmedAway() {
        stubReplenishment("A", new BigDecimal("3.000"));
        var result = reader.read(command().setConfirmNoReplenishmentInfo(true));
        assertTrue(result.getBlockers().stream().anyMatch(b -> "LOSS_REPLENISHMENT_SOURCE_INVALID".equals(b.getBlockerType())));
    }

    @Test
    void absentActualQuantityCannotUseRequestedOrBaseQuantity() {
        stubReplenishment("C", null);
        var result = reader.read(command().setConfirmNoReplenishmentInfo(true));
        assertTrue(result.getBlockers().stream().anyMatch(b -> "LOSS_REPLENISHMENT_SOURCE_INVALID".equals(b.getBlockerType())));
    }

    @Test
    void unmatchedMaterialBlocksInsteadOfDeclaringNoLoss() {
        stubReplenishment("C", BigDecimal.ONE);
        when(materialService.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(List.of());
        var result = reader.read(command().setConfirmNoReplenishmentInfo(true));
        assertTrue(result.getBlockers().stream().anyMatch(b -> "LOSS_REPLENISHMENT_MATERIAL_UNBOUND".equals(b.getBlockerType())));
    }

    @Test
    void multipleSubmissionsDoNotCountReplenishmentTwice() {
        stubReplenishment("C", new BigDecimal("3.000"));
        var second = event(1002L, 5102L, "{}");
        org.mockito.Mockito.lenient().when(eventMapper.selectProductionSubmitsByIdsForUpdate(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(event("{}"), second));
        when(feedbackMapper.selectListByIdsForUpdate(List.of(5101L, 5102L)))
                .thenReturn(List.of(feedback(), feedback(5102L, BigDecimal.ONE)));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(ACTIVE_ORDER_ID)).thenReturn(List.of(allocation(), allocation(1002L, 7102L, 7202L)));
        when(reviewMapper.selectListByEventIdForUpdate(1002L)).thenReturn(List.of(review(1002L, 7202L)));
        var result = reader.read(command());
        assertTrue(result.getBlockers().isEmpty());
        assertEquals(new BigDecimal("3.000"), result.getProcessSources().stream()
                .map(MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource::getFormalLossQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(1, result.getProcessSources().stream().mapToInt(source -> source.getReplenishmentSources().size()).sum());
    }

    private void stubReplenishment(String status, BigDecimal quantity) {
        when(replenishmentItemMapper.selectListByProductionOrderNo("MO-9001")).thenReturn(List.of(
                ErpKingdeeProductionReplenishmentListItemDO.builder().id(9101L).productionReplenishmentListId(91L)
                        .productionOrderNo("MO-9001").materialNumber("MAT-1").materialName("材料一")
                        .lotNumber("LOT-1").actualQuantity(quantity).baseActualQuantity(BigDecimal.TEN).build()));
        when(replenishmentMapper.selectBatchIds(List.of(91L))).thenReturn(List.of(
                ErpKingdeeProductionReplenishmentListDO.builder().id(91L).sourceBillNo("BL-91").documentStatus(status).build()));
        when(materialService.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(List.of(new MesFrontlineProcessMaterial(1L, "MAT-1", "材料一", "", "INPUT",
                        BigDecimal.ONE, List.of(), null, null, null, List.of(), List.of(), List.of(), null)));
    }

    private static MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseLossReportPlanCommand()
                .setRequireNoReplenishmentConfirmation(true)
                .setTenantId(1L)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setWorkOrderId(WORK_ORDER_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setProductId(3101L)
                .setBatchCode("BATCH-9001")
                .setSourceSnapshotHash("AO_RELEASE_SOURCE_V1:loss-source")
                .setProcessSnapshots(List.of(MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                        .id(4101L)
                        .activeOrderId(ACTIVE_ORDER_ID)
                        .workOrderId(WORK_ORDER_ID)
                        .routeId(ROUTE_ID)
                        .routeVersionId(ROUTE_VERSION_ID)
                        .routeProcessId(ROUTE_PROCESS_ID)
                        .processId(PROCESS_ID)
                        .build()));
    }

    private static MesProProcessPoolEventDO event(String payload) {
        return event(1001L, 5101L, payload);
    }

    private static MesProProcessPoolEventDO event(Long id, String payload) {
        return event(id, 5101L, payload);
    }

    private static MesProProcessPoolEventDO event(Long id, Long feedbackSourceId, String payload) {
        return MesProProcessPoolEventDO.builder()
                .id(id)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(feedbackSourceId)
                .rawPayload(payload)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .signatureId(1101L)
                .signatureUserId(2101L)
                .signatureSnapshot("{\"signedAt\":\"2026-08-01T08:30:00\"}")
                .build();
    }

    private static MesProFeedbackDO feedback() {
        return feedback(5101L, new BigDecimal("2.500"));
    }

    private static MesProFeedbackDO feedback(Long id, BigDecimal unqualifiedQuantity) {
        return MesProFeedbackDO.builder()
                .id(id)
                .code("FB-" + id)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .feedbackTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .feedbackQuantity(new BigDecimal("100.000"))
                .qualifiedQuantity(new BigDecimal("100.000").subtract(unqualifiedQuantity))
                .unqualifiedQuantity(unqualifiedQuantity)
                .laborScrapQuantity(new BigDecimal("1.000"))
                .materialScrapQuantity(new BigDecimal("1.500"))
                .otherScrapQuantity(BigDecimal.ZERO)
                .lossReasonId(8301L)
                .lossReasonCodeSnapshot("LOSS-001")
                .lossReasonNameSnapshot("正常损耗")
                .feedbackUserId(2101L)
                .approveUserId(3001L)
                .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return allocation(1001L, 7101L, 7201L);
    }

    private static MesProcessPoolReportAllocationDO allocation(Long eventId, Long allocationId, Long reviewId) {
        return MesProcessPoolReportAllocationDO.builder()
                .id(allocationId)
                .eventId(eventId)
                .reviewId(reviewId)
                .leaderUserId(3001L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("100.000"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO review() {
        return review(1001L, 7201L);
    }

    private static MesProcessPoolSubmissionReviewDO review(Long eventId, Long reviewId) {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(reviewId)
                .eventId(eventId)
                .leaderUserId(3001L)
                .leaderType("PRODUCTION")
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .reviewSignatureId(1201L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signedAt\":\"2026-08-01T09:00:00\"}")
                .build();
    }
}
