package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProductionPickListSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest {

    private static final Long ACTIVE_ORDER_ID = 8101L;
    private static final Long WORK_ORDER_ID = 9001L;
    private static final Long ROUTE_ID = 7001L;
    private static final Long ROUTE_VERSION_ID = 7002L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;
    private static final Long BATCH_EXECUTION_ID = 9701L;
    private static final Long BATCH_TASK_ID = 9801L;

    @Mock
    private MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    @Mock
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Mock
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock
    private MesTeamLeaderBatchRecordBackfillService backfillService;
    @Mock
    private MesProductionPickListSourceService productionPickListSourceService;

    private MesTeamLeaderActiveOrderReleaseBatchRecordWriter writer;

    @BeforeEach
    void setUp() {
        writer = new MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl(
                bindingMapper, ruleMapper, batchTaskMapper, backfillService, productionPickListSourceService);
    }

    @Test
    void shouldWriteFormalBatchRecordIntoCurrentEdhrBatchTaskWithAuditAndSourceSignatures() {
        mockFormalPlanSources();
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID))
                .thenReturn(List.of(batchTask()));
        when(backfillService.backfillCompletedProcess(any()))
                .thenReturn(new MesTeamLeaderBatchRecordBackfillResult()
                        .setExecutionId(9901L)
                        .setAuditBatchId(9911L)
                        .setAppliedFieldCount(2)
                        .setCellValuesHash("cells-after")
                        .setFieldAuditHeadHash("audit-after"));

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command());
        MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult result =
                writer.write(plan, BATCH_EXECUTION_ID);

        assertAll(
                () -> assertTrue(plan.getBlockers().isEmpty()),
                () -> assertEquals("BATCH_RECORD", result.getDocumentType()),
                () -> assertEquals(List.of(9901L), result.getBatchRecordExecutionIds()),
                () -> assertEquals(List.of(9911L), result.getFieldAuditIds()),
                () -> assertTrue(result.getSourceObjectIds().containsAll(List.of(1001L, 7101L, 7201L, 7301L))),
                () -> assertFalse(result.getSourceValueHashes().isEmpty()),
                () -> assertEquals(2, result.getSignatureEvidence().size()),
                () -> assertTrue(result.getSignatureEvidence().stream().anyMatch(evidence ->
                        "FILLER".equals(evidence.getRole())
                                && Long.valueOf(1101L).equals(evidence.getSignatureId())
                                && Long.valueOf(2101L).equals(evidence.getUserId())
                                && LocalDateTime.of(2026, 8, 1, 8, 30).equals(evidence.getSignedAt()))),
                () -> assertTrue(result.getSignatureEvidence().stream().anyMatch(evidence ->
                        "REVIEWER".equals(evidence.getRole())
                                && Long.valueOf(1201L).equals(evidence.getSignatureId())
                                && Long.valueOf(3001L).equals(evidence.getUserId())
                                && LocalDateTime.of(2026, 8, 1, 9, 0).equals(evidence.getSignedAt()))));

        ArgumentCaptor<MesTeamLeaderBatchRecordBackfillCommand> captor =
                ArgumentCaptor.forClass(MesTeamLeaderBatchRecordBackfillCommand.class);
        verify(backfillService).backfillCompletedProcess(captor.capture());
        MesTeamLeaderBatchRecordBackfillCommand backfillCommand = captor.getValue();
        assertAll(
                () -> assertEquals(BATCH_EXECUTION_ID, backfillCommand.getBatchExecutionId()),
                () -> assertEquals(BATCH_TASK_ID, backfillCommand.getBatchExecutionTaskId()),
                () -> assertEquals("PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:agg-production-5001",
                        backfillCommand.getIdempotencyKey()),
                () -> assertEquals("agg-production-5001", backfillCommand.getAggregateHash()),
                () -> assertEquals(1001L, backfillCommand.getSourceEvents().get(0).getId()),
                () -> assertFalse(result.getBatchRecordExecutionIds().contains(8701L),
                        "历史 completion backfill execution 只能作为来源证据"));
    }

    @Test
    void shouldReturnSignatureBlockerWithoutOpeningExecutionWhenProductionReviewSignatureIsMissing() {
        mockFormalPlanSources();
        MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command = command();
        command.getProcessSources().get(0).getReviews().get(0).setReviewSignatureId(null);

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command);

        assertAll(
                () -> assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                        "PRODUCTION_SIGNATURE_REQUIRED".equals(blocker.getBlockerType())
                                && "PRODUCTION_REVIEW".equals(blocker.getObjectType())
                                && "7201".equals(blocker.getObjectId()))),
                () -> assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID)),
                () -> verify(batchTaskMapper, never()).selectListByBatchExecutionId(any()),
                () -> verify(backfillService, never()).backfillCompletedProcess(any()));
    }

    @Test
    void shouldReturnHistoryBlockerWhenProductionEventHasNoCurrentWorkOrderContext() {
        mockFormalPlanSources();
        MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command = command();
        command.getProcessSources().get(0).getSourceEvents().get(0).setWorkOrderId(null);

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command);

        assertAll(
                () -> assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                        "PRODUCTION_HISTORY_REQUIRED".equals(blocker.getBlockerType())
                                && "PRODUCTION_EVENT".equals(blocker.getObjectType())
                                && "1001".equals(blocker.getObjectId()))),
                () -> assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID)),
                () -> verify(batchTaskMapper, never()).selectListByBatchExecutionId(any()),
                () -> verify(backfillService, never()).backfillCompletedProcess(any()));
    }

    @Test
    void shouldPlanFormalSourcesBeforeTheCurrentBatchHasBeenCreated() {
        mockFormalPlanSources();
        MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command = command().setBatchExecutionId(null);

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command);

        assertTrue(plan.getBlockers().isEmpty());
        verify(batchTaskMapper, never()).selectListByBatchExecutionId(any());
        verify(backfillService, never()).backfillCompletedProcess(any());
    }

    @Test
    void shouldFailFastWithoutBackfillWhenCurrentBatchTaskDoesNotMatchFormalBinding() {
        mockFormalPlanSources();
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID))
                .thenReturn(List.of(batchTask().setBatchRecordReportId("OTHER-REPORT")));
        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command());

        assertAll(
                () -> assertTrue(plan.getBlockers().isEmpty()),
                () -> assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID)));
        verify(backfillService, never()).backfillCompletedProcess(any());
    }

    @Test
    void shouldUseStableBackfillIdempotencyForRepeatedCurrentBatchWrites() {
        mockFormalPlanSources();
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID))
                .thenReturn(List.of(batchTask()));
        when(backfillService.backfillCompletedProcess(any()))
                .thenReturn(new MesTeamLeaderBatchRecordBackfillResult()
                        .setExecutionId(9901L)
                        .setAuditBatchId(9911L)
                        .setAppliedFieldCount(2));
        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command());

        MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult first = writer.write(plan, BATCH_EXECUTION_ID);
        MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult replay = writer.write(plan, BATCH_EXECUTION_ID);

        assertEquals(first.getBatchRecordExecutionIds(), replay.getBatchRecordExecutionIds());
        assertEquals(first.getFieldAuditIds(), replay.getFieldAuditIds());
        ArgumentCaptor<MesTeamLeaderBatchRecordBackfillCommand> captor =
                ArgumentCaptor.forClass(MesTeamLeaderBatchRecordBackfillCommand.class);
        verify(backfillService, times(2)).backfillCompletedProcess(captor.capture());
        assertEquals(captor.getAllValues().get(0).getIdempotencyKey(),
                captor.getAllValues().get(1).getIdempotencyKey());
    }

    @Test
    void shouldPreflightProductionPickListMappingsBeforeAnyBatchRecordWrite() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(ROUTE_PROCESS_ID), "BATCH"))
                .thenReturn(List.of(binding()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(rule(1L, "outputQuantity"), pickRule(2L, "material.3201.lotNumber")));
        when(productionPickListSourceService.resolveValue(any()))
                .thenReturn(new MesProductionPickListSourceService.ResolvedValue(
                        9001L, 9101L, "LOT-FIRST", "pick-evidence"));

        MesTeamLeaderActiveOrderReleaseBatchRecordPlan plan = writer.plan(command());

        assertTrue(plan.getBlockers().isEmpty());
        assertTrue(plan.getSourceObjectIds().containsAll(List.of(9001L, 9101L)));
        assertTrue(plan.getSourceValueHashes().contains("pick-evidence"));
        verify(backfillService, never()).backfillCompletedProcess(any());
    }

    private void mockFormalPlanSources() {
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(ROUTE_PROCESS_ID), "BATCH"))
                .thenReturn(List.of(binding()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, "BR-FORM-A"))
                .thenReturn(List.of(rule(1L, "outputQuantity"), rule(2L, "pressure")));
    }

    private static MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand()
                .setTenantId(1L)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setPickListBindingId(8801L)
                .setWorkOrderId(WORK_ORDER_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setDccProjectCodeId(8001L)
                .setProductId(3101L)
                .setBatchCode("BATCH-9001")
                .setApplicantUserId(4101L)
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setWorkOrder(workOrder())
                .setSourceSnapshotHash("AO_RELEASE_SOURCE_V1:source-hash")
                .setProcessSources(List.of(new MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource()
                        .setSnapshot(snapshot())
                        .setCompletion(completion())
                        .setSourceEvents(List.of(event()))
                        .setAllocations(List.of(allocation()))
                        .setReviews(List.of(review()))));
    }

    private static MesProcessPoolActiveOrderProcessSnapshotDO snapshot() {
        return MesProcessPoolActiveOrderProcessSnapshotDO.builder()
                .id(4101L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completion() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(7301L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(8701L)
                .lastEventId(1001L)
                .lastReviewId(7201L)
                .sourceEventIdsJson("[1001]")
                .sourceAllocationIdsJson("[7101]")
                .aggregateHash("agg-production-5001")
                .backfillIdempotencyKey("PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:agg-production-5001")
                .build();
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .rawPayload("{\"outputQuantity\":80,\"pressure\":15}")
                .serverSubmitTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .signatureId(1101L)
                .signatureUserId(2101L)
                .signatureSnapshot("{\"signedAt\":\"2026-08-01T08:30:00\"}")
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(1001L)
                .reviewId(7201L)
                .leaderUserId(3001L)
                .activeOrderId(ACTIVE_ORDER_ID)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO review() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(7201L)
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType("PRODUCTION")
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .reviewSignatureId(1201L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signedAt\":\"2026-08-01T09:00:00\"}")
                .build();
    }

    private static MesProWorkOrderDO workOrder() {
        return MesProWorkOrderDO.builder()
                .id(WORK_ORDER_ID)
                .code("WO-9001")
                .batchCode("BATCH-9001")
                .productId(3101L)
                .quantity(new BigDecimal("80"))
                .build();
    }

    private static MesProRouteFlowProcessBatchRecordDO binding() {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(3001L)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .useType("BATCH")
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
                .formSlotType(null)
                .permissionScopeId(9901L)
                .build();
    }

    private static MesProBatchRecordCellLinkRuleDO rule(Long id, String sourceFieldCode) {
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
        rule.setId(id);
        rule.setRuleVersion(1L);
        rule.setScopeType("ROUTE_VERSION");
        rule.setScopeId(401L);
        rule.setSourceType("PROCESS_POOL_REPORT");
        rule.setSourceFieldCode(sourceFieldCode);
        rule.setTargetReportId("BR-FORM-A");
        rule.setTargetRowIndex(id.intValue() + 4);
        rule.setTargetColumnIndex(2);
        rule.setTargetCellKey("R" + (id + 4) + "C2");
        rule.setTargetValueType("NUMBER");
        rule.setEnabled(true);
        return rule;
    }

    private static MesProBatchRecordCellLinkRuleDO pickRule(Long id, String sourceFieldCode) {
        MesProBatchRecordCellLinkRuleDO rule = rule(id, sourceFieldCode);
        rule.setSourceType("PRODUCTION_PICK_LIST");
        rule.setTargetValueType("STRING");
        return rule;
    }

    private static MesProEdhrBatchExecutionTaskDO batchTask() {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(BATCH_TASK_ID)
                .batchExecutionId(BATCH_EXECUTION_ID)
                .nodeType("ROUTE_FORM")
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .recordCategory("BATCH_RECORD")
                .formSlotType(null)
                .routeBindingId(3001L)
                .build();
    }
}
