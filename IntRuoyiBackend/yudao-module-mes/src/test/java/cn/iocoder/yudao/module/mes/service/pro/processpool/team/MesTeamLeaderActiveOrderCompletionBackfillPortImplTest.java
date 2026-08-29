package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderCompletionBackfillPortImplTest {

    @Mock private MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock private MesPqcInspectionTaskMapper taskMapper;
    @Mock private MesPqcProcessInspectionAggregateDetailMapper detailMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesTeamLeaderActiveOrderReleaseLossSourceReader lossSourceReader;
    @Mock private MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    @Mock private MesWmProductIssueMapper productIssueMapper;
    @Mock private MesWmProductIssueDetailMapper productIssueDetailMapper;

    private MesTeamLeaderActiveOrderCompletionBackfillPortImpl port;

    @BeforeEach
    void setUp() {
        port = new MesTeamLeaderActiveOrderCompletionBackfillPortImpl(snapshotMapper, allocationMapper,
                completionMapper, taskMapper, detailMapper, workOrderMapper, lossSourceReader, backfillMapper,
                productIssueMapper, productIssueDetailMapper);
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(snapshot()));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(allocation()));
        when(completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L))).thenReturn(List.of(completion()));
        org.mockito.Mockito.lenient().when(completionMapper.updateById(
                org.mockito.ArgumentMatchers.any(MesProcessPoolOrderProcessCompletionDO.class)))
                .thenReturn(1);
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task()));
        when(detailMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(detail()));
        org.mockito.Mockito.lenient().when(workOrderMapper.selectByIdForUpdate(30L)).thenReturn(workOrder());
        org.mockito.Mockito.lenient().when(productIssueMapper.selectListByWorkOrderIdForUpdate(30L))
                .thenReturn(List.of(productIssue()));
        org.mockito.Mockito.lenient().when(productIssueDetailMapper.selectListByIssueIdForUpdate(901L))
                .thenReturn(List.of(productIssueDetail()));
        org.mockito.Mockito.lenient().when(backfillMapper.insert(any(MesProcessPoolActiveOrderCompletionBackfillDO.class)))
                .thenAnswer(invocation -> {
                    MesProcessPoolActiveOrderCompletionBackfillDO row = invocation.getArgument(0);
                    row.setId(switch (row.getBackfillType()) {
                        case MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_BATCH_RECORD -> 1101L;
                        case MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_PROCESS_INSPECTION -> 1102L;
                        case MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT -> 1103L;
                        case MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_NO_LOSS -> 1104L;
                        default -> throw new IllegalStateException("unexpected type");
                    });
                    return 1;
                });
    }

    @Test
    void noLossMaterializesOnlyBatchAndInspectionRows() {
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO));

        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = port.prepare(20L, order(), command());
        draft.setMaterializedBy(20L);
        port.write(draft, 10L);

        assertFalse(draft.getHasActualLoss());
        assertTrue(draft.getFormalSourceSnapshotJson().contains("formalProductIssue"));
        assertTrue(draft.getSignatureSnapshotJson().contains("productionCompletionSignatures"));
        assertEquals(1L, draft.getTenantId());
        assertEquals("NOT_REQUIRED", draft.getLossReportStatus());
        ArgumentCaptor<MesProcessPoolActiveOrderCompletionBackfillDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderCompletionBackfillDO.class);
        verify(backfillMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(row -> row.getTenantId().equals(1L)));
        assertFalse(captor.getAllValues().stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_NO_LOSS.equals(row.getBackfillType())));
        assertFalse(captor.getAllValues().stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT.equals(row.getBackfillType())));
        assertEquals(1101L, draft.getBatchRecordId());
        assertEquals(1102L, draft.getProcessInspectionId());
        verify(completionMapper).updateById(org.mockito.ArgumentMatchers.argThat(
                (MesProcessPoolOrderProcessCompletionDO row) ->
                MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS.equals(row.getBackfillStatus())
                        && Long.valueOf(1101L).equals(row.getBackfillExecutionId())));
    }

    @Test
    void positiveLossMaterializesLossRowAndFormalLossRecord() {
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ONE));

        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = port.prepare(20L, order(), command());
        draft.setMaterializedBy(20L);
        port.write(draft, 10L);

        assertTrue(draft.getHasActualLoss());
        assertEquals(BigDecimal.ONE, draft.getLossQuantity());
        assertEquals(1103L, draft.getLossRecordId());
        ArgumentCaptor<MesProcessPoolActiveOrderCompletionBackfillDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolActiveOrderCompletionBackfillDO.class);
        verify(backfillMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT.equals(row.getBackfillType())));
        assertFalse(captor.getAllValues().stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_NO_LOSS.equals(row.getBackfillType())));
    }

    @Test
    void invalidLossDraftMustFailBeforeAnyMaterializationRow() {
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO));
        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = port.prepare(20L, order(), command())
                .setMaterializedBy(20L).setHasActualLoss(true).setLossQuantity(BigDecimal.ONE)
                .setLossRecordId(null).setLossReportStatus("SUCCESS");

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> port.write(draft, 10L));

        org.mockito.Mockito.verifyNoInteractions(backfillMapper);
    }

    @Test
    void blockedLossDraftMustFailBeforeAnyMaterializationRow() {
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO));
        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = port.prepare(20L, order(), command())
                .setMaterializedBy(20L)
                .setLossConditionFactsJson("[{\"processId\":1,\"status\":\"BLOCKED\","
                        + "\"hasActualLoss\":false,\"lossQuantity\":0,\"sourceHash\":\"blocked-hash\"}]");

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> port.write(draft, 10L));

        org.mockito.Mockito.verifyNoInteractions(backfillMapper);
    }

    @Test
    void missingOrMismatchedFormalIssueMustFailBeforeBackfill() {
        cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO mismatched = productIssue();
        mismatched.setWorkOrderId(999L);
        when(productIssueMapper.selectListByWorkOrderIdForUpdate(30L)).thenReturn(List.of(mismatched));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> port.prepare(20L, order(), command()));

        org.mockito.Mockito.verifyNoInteractions(productIssueDetailMapper, lossSourceReader, backfillMapper);
    }

    @Test
    void formalSourceValueChangeMustChangeSnapshotHash() {
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO));
        MesTeamLeaderActiveOrderCompletionBackfillDraft before = port.prepare(20L, order(), command());
        cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO changed =
                productIssueDetail();
        changed.setBatchCode("CHANGED-BATCH");
        when(productIssueDetailMapper.selectListByIssueIdForUpdate(901L)).thenReturn(List.of(changed));

        MesTeamLeaderActiveOrderCompletionBackfillDraft after = port.prepare(20L, order(), command());

        org.junit.jupiter.api.Assertions.assertNotEquals(before.getSourceSnapshotHash(), after.getSourceSnapshotHash());
    }

    @Test
    void activeOrderCompletionReplayMustKeepSourceHashAfterCompletionStateChanges() {
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO));
        MesProcessPoolActiveOrderDO beforeOrder = order();
        MesTeamLeaderActiveOrderCompletionBackfillDraft before = port.prepare(20L, beforeOrder, command());

        MesProcessPoolActiveOrderDO completedOrder = order().setVersion(3).setActiveStatus("COMPLETED")
                .setBusinessStatus("COMPLETED");
        MesTeamLeaderActiveOrderCompletionBackfillDraft after = port.prepare(20L, completedOrder, command());

        assertEquals(before.getSourceSnapshotHash(), after.getSourceSnapshotHash());
    }

    @Test
    void productionProcessWithoutConfiguredPqcTaskDoesNotBlockInspectionBackfill() {
        MesProcessPoolActiveOrderProcessSnapshotDO secondSnapshot = snapshot(102L, 1L);
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(snapshot(101L, 1L), secondSnapshot));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(allocation(201L, 101L), allocation(202L, 102L)));
        when(completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L)))
                .thenReturn(List.of(completion(301L, 101L, 201L), completion(302L, 102L, 202L)));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task(701L, 101L)));
        when(detailMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(detail(801L, 701L, 101L)));
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO,
                List.of(snapshot(101L, 1L), secondSnapshot)));

        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = port.prepare(20L, order(), command());
        draft.setMaterializedBy(20L);
        port.write(draft, 10L);

        assertEquals(1102L, draft.getProcessInspectionId());
    }

    @Test
    void duplicateCompletedProcessesMustBeRejectedBeforeBackfill() {
        when(completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L)))
                .thenReturn(List.of(completion(301L, 101L, 201L), completion(302L, 101L, 202L)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> port.prepare(20L, order(), command()));

        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING.getCode(), ex.getCode());
    }

    @Test
    void multipleInspectionDetailsForOneConfirmedTaskMustBeAccepted() {
        when(detailMapper.selectListByActiveOrderIdForUpdate(10L))
                .thenReturn(List.of(detail(801L, 701L, 101L, 2), detail(802L, 701L, 101L, 2)));
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task(701L, 101L, 2)));
        when(lossSourceReader.read(any())).thenReturn(lossSources(BigDecimal.ZERO));

        MesTeamLeaderActiveOrderCompletionBackfillDraft draft = port.prepare(20L, order(), command());

        assertTrue(draft.getProcessInspectionSourceIdsJson().contains("701"));
        assertTrue(draft.getProcessInspectionSourceIdsJson().contains("801"));
        assertTrue(draft.getProcessInspectionSourceIdsJson().contains("802"));
    }

    @Test
    void inspectionDetailWithoutFormalPieceSourceMustBeRejectedBeforeBackfill() {
        MesPqcProcessInspectionAggregateDetailDO invalid = detail(801L, 701L, 101L)
                .setSourcePieceDetailId(null);
        when(detailMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(invalid));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> port.prepare(20L, order(), command()));

        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING.getCode(), ex.getCode());
    }

    private MesTeamLeaderActiveOrderReleaseLossSourceReadResult lossSources(BigDecimal loss) {
        return lossSources(loss, List.of(snapshot()));
    }

    private MesTeamLeaderActiveOrderReleaseLossSourceReadResult lossSources(
            BigDecimal loss, List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots) {
        MesProFeedbackDO feedback = MesProFeedbackDO.builder().id(501L).workOrderId(30L).routeId(40L)
                .processId(1L).unqualifiedQuantity(loss).build();
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder().id(401L).workOrderId(30L)
                .routeId(40L).routeProcessId(101L).processId(1L).build();
        return new MesTeamLeaderActiveOrderReleaseLossSourceReadResult()
                .setBlockers(List.of())
                .setProcessSources(snapshots.stream().map(snapshot ->
                        new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource()
                                .setSnapshot(snapshot).setFeedback(feedback).setEvent(event)
                                .setAllocation(allocation()).setReview(MesProcessPoolSubmissionReviewDO.builder()
                                        .id(601L).build()).setLossDetails(List.of())).toList());
    }

    private MesTeamLeaderActiveOrderCompletionCommand command() {
        return new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(10L)
                .setExpectedVersion(2).setIdempotencyKey("key");
    }

    private MesProcessPoolActiveOrderDO order() {
        MesProcessPoolActiveOrderDO order = MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L)
                .workOrderId(30L).routeId(40L).routeVersionId(41L).activeStatus("ACTIVE").version(2).build();
        order.setTenantId(1L);
        return order;
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return snapshot(101L, 1L);
    }

    private MesProcessPoolActiveOrderProcessSnapshotDO snapshot(Long routeProcessId, Long processId) {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(routeProcessId).activeOrderId(10L)
                .workOrderId(30L).routeId(40L).routeVersionId(41L).routeProcessId(routeProcessId).processId(processId)
                .plannedQuantitySnapshot(BigDecimal.TEN).build();
    }

    private MesProcessPoolReportAllocationDO allocation() {
        return allocation(201L, 101L);
    }

    private MesProcessPoolReportAllocationDO allocation(Long id, Long routeProcessId) {
        return MesProcessPoolReportAllocationDO.builder().id(id).activeOrderId(10L).workOrderId(30L)
                .routeProcessId(routeProcessId).processId(1L).allocatedQuantity(BigDecimal.TEN).build();
    }

    private MesProcessPoolOrderProcessCompletionDO completion() {
        return completion(301L, 101L, 201L);
    }

    private MesProcessPoolOrderProcessCompletionDO completion(Long id, Long routeProcessId, Long allocationId) {
        return MesProcessPoolOrderProcessCompletionDO.builder().id(id).workOrderId(30L)
                .routeProcessId(routeProcessId).processId(1L).targetQuantity(BigDecimal.TEN)
                .confirmedQuantity(BigDecimal.TEN).completionStatus("COMPLETED").lastEventId(401L)
                .lastReviewId(601L).sourceEventIdsJson("[401]").sourceAllocationIdsJson("[" + allocationId + "]")
                .aggregateHash("completion-hash").build();
    }

    private MesPqcInspectionTaskDO task() {
        return task(701L, 101L);
    }

    private MesPqcInspectionTaskDO task(Long id, Long routeProcessId) {
        return task(id, routeProcessId, 1);
    }

    private MesPqcInspectionTaskDO task(Long id, Long routeProcessId, Integer actualInspectionQuantity) {
        return MesPqcInspectionTaskDO.builder().id(id).activeOrderId(10L).workOrderId(30L).routeId(40L)
                .routeVersionId(41L).routeProcessId(routeProcessId).processId(1L)
                .actualInspectionQuantity(actualInspectionQuantity).submittedEventId(401L)
                .taskStatus("CONFIRMED").build();
    }

    private MesPqcProcessInspectionAggregateDetailDO detail() {
        return detail(801L, 701L, 101L);
    }

    private MesPqcProcessInspectionAggregateDetailDO detail(Long id, Long taskId, Long routeProcessId) {
        return detail(id, taskId, routeProcessId, 1);
    }

    private MesPqcProcessInspectionAggregateDetailDO detail(Long id, Long taskId, Long routeProcessId,
                                                            Integer actualInspectionQuantity) {
        return MesPqcProcessInspectionAggregateDetailDO.builder().id(id).activeOrderId(10L)
                .workOrderId(30L).routeId(40L).routeVersionId(41L).routeProcessId(routeProcessId).processId(1L)
                .sourcePqcRecordId(901L).sourcePieceDetailId(id + 1000L).eventId(401L).reviewId(601L)
                .actualInspectionQuantity(actualInspectionQuantity).pqcTaskId(taskId).build();
    }

    private MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder().id(30L).productId(1001L).batchCode("BATCH-30").build();
    }

    private cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO productIssue() {
        return cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO.builder()
                .id(901L).workOrderId(30L).status(MesWmProductIssueStatusEnum.FINISHED.getStatus())
                .code("ISSUE-30").build();
    }

    private cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO productIssueDetail() {
        return cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO.builder()
                .id(902L).issueId(901L).lineId(903L).materialStockId(904L).itemId(1001L)
                .quantity(BigDecimal.ONE).batchId(905L).batchCode("RAW-30").build();
    }
}
