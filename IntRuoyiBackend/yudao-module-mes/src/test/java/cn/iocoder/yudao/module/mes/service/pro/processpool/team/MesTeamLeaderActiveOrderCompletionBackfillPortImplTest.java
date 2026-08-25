package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(taskMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(task()));
        when(detailMapper.selectListByActiveOrderIdForUpdate(10L)).thenReturn(List.of(detail()));
        when(workOrderMapper.selectByIdForUpdate(30L)).thenReturn(workOrder());
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
        verify(backfillMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(row -> row.getTenantId().equals(1L)));
        assertTrue(captor.getAllValues().stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_NO_LOSS.equals(row.getBackfillType())));
        assertFalse(captor.getAllValues().stream().anyMatch(row ->
                MesProcessPoolActiveOrderCompletionBackfillDO.TYPE_LOSS_REPORT.equals(row.getBackfillType())));
        assertEquals(1101L, draft.getBatchRecordId());
        assertEquals(1102L, draft.getProcessInspectionId());
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

    private MesTeamLeaderActiveOrderReleaseLossSourceReadResult lossSources(BigDecimal loss) {
        MesProFeedbackDO feedback = MesProFeedbackDO.builder().id(501L).workOrderId(30L).routeId(40L)
                .processId(1L).unqualifiedQuantity(loss).build();
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder().id(401L).workOrderId(30L)
                .routeId(40L).routeProcessId(101L).processId(1L).build();
        return new MesTeamLeaderActiveOrderReleaseLossSourceReadResult()
                .setBlockers(List.of())
                .setProcessSources(List.of(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource()
                        .setSnapshot(snapshot()).setFeedback(feedback).setEvent(event).setAllocation(allocation())
                        .setReview(MesProcessPoolSubmissionReviewDO.builder().id(601L).build()).setLossDetails(List.of())));
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
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(101L).activeOrderId(10L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                .plannedQuantitySnapshot(BigDecimal.TEN).build();
    }

    private MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L).workOrderId(30L)
                .routeProcessId(101L).processId(1L).allocatedQuantity(BigDecimal.TEN).build();
    }

    private MesProcessPoolOrderProcessCompletionDO completion() {
        return MesProcessPoolOrderProcessCompletionDO.builder().id(301L).workOrderId(30L)
                .routeProcessId(101L).processId(1L).targetQuantity(BigDecimal.TEN)
                .confirmedQuantity(BigDecimal.TEN).completionStatus("COMPLETED").lastEventId(401L)
                .lastReviewId(601L).sourceEventIdsJson("[401]").sourceAllocationIdsJson("[201]")
                .aggregateHash("completion-hash").build();
    }

    private MesPqcInspectionTaskDO task() {
        return MesPqcInspectionTaskDO.builder().id(701L).activeOrderId(10L).workOrderId(30L).routeId(40L)
                .routeVersionId(41L).routeProcessId(101L).processId(1L).taskStatus("CONFIRMED").build();
    }

    private MesPqcProcessInspectionAggregateDetailDO detail() {
        return MesPqcProcessInspectionAggregateDetailDO.builder().id(801L).activeOrderId(10L)
                .workOrderId(30L).routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                .pqcTaskId(701L).build();
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
