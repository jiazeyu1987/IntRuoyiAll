package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
class MesTeamLeaderActiveOrderReleaseLossReportWriterTest {

    private static final Long ACTIVE_ORDER_ID = 8101L;
    private static final Long WORK_ORDER_ID = 9001L;
    private static final Long ROUTE_ID = 7001L;
    private static final Long ROUTE_VERSION_ID = 7002L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;
    private static final Long BATCH_EXECUTION_ID = 9701L;
    private static final Long BATCH_TASK_ID = 9801L;
    private static final String REPORT_ID = "LOSS-REPORT-01";

    private static final List<String> REQUIRED_FIELDS = List.of(
            "productId", "batchCode", "routeProcessId", "processId", "outputQuantity",
            "qualifiedQuantity", "lossQuantity", "laborScrapQuantity", "materialScrapQuantity",
            "otherScrapQuantity", "lossDetails", "fillerUserId", "fillerSignedAt",
            "reviewerUserId", "reviewerSignedAt");

    @Mock
    private MesTeamLeaderActiveOrderReleaseLossSourceReader sourceReader;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    @Mock
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    @Mock
    private MesProBatchRecordReportMapper reportMapper;
    @Mock
    private MesProBatchRecordVersionMapper versionMapper;
    @Mock
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock
    private MesProBatchRecordExecutionService executionService;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    private MesTeamLeaderActiveOrderReleaseLossReportWriter writer;

    @BeforeEach
    void setUp() {
        writer = new MesTeamLeaderActiveOrderReleaseLossReportWriterImpl(sourceReader, bindingMapper, ruleMapper,
                reportMapper, versionMapper, batchTaskMapper, executionService, executionMapper, fieldAuditService);
    }

    @Test
    void shouldWritePositiveFormalLossIntoCurrentTraditionalLossReportTaskWithStableEvidence() {
        mockSuccessfulPlan();
        mockSuccessfulWrite();

        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());
        MesTeamLeaderActiveOrderReleaseLossReportWriteResult first = writer.write(plan, BATCH_EXECUTION_ID);
        MesTeamLeaderActiveOrderReleaseLossReportWriteResult replay = writer.write(plan, BATCH_EXECUTION_ID);

        assertAll(
                () -> assertTrue(plan.getBlockers().isEmpty()),
                () -> assertEquals("LOSS_REPORT", first.getDocumentType()),
                () -> assertEquals(List.of(9901L), first.getBatchRecordExecutionIds()),
                () -> assertEquals(List.of(9911L), first.getFieldAuditIds()),
                () -> assertEquals(first.getBatchRecordExecutionIds(), replay.getBatchRecordExecutionIds()),
                () -> assertEquals(first.getFieldAuditIds(), replay.getFieldAuditIds()),
                () -> assertTrue(first.getSourceObjectIds().containsAll(List.of(1001L, 5101L, 7101L, 7201L))),
                () -> assertFalse(first.getSourceValueHashes().isEmpty()),
                () -> assertEquals(2, first.getSignatureEvidence().size()),
                () -> assertTrue(first.getSignatureEvidence().stream().anyMatch(evidence ->
                        "FILLER".equals(evidence.getRole()) && Long.valueOf(1101L).equals(evidence.getSignatureId())
                                && Long.valueOf(2101L).equals(evidence.getUserId())
                                && LocalDateTime.of(2026, 8, 1, 8, 30).equals(evidence.getSignedAt()))),
                () -> assertTrue(first.getSignatureEvidence().stream().anyMatch(evidence ->
                        "REVIEWER".equals(evidence.getRole()) && Long.valueOf(1201L).equals(evidence.getSignatureId())
                                && Long.valueOf(3001L).equals(evidence.getUserId())
                                && LocalDateTime.of(2026, 8, 1, 9, 0).equals(evidence.getSignedAt()))),
                () -> assertEquals(1, first.getDocumentEvidence().size()),
                () -> assertTrue(first.getDocumentEvidence().get(0).isSourceConsistent()),
                () -> assertEquals(REQUIRED_FIELDS.size(), first.getDocumentEvidence().get(0).getRequiredFieldCount()),
                () -> assertEquals(REQUIRED_FIELDS.size(), first.getDocumentEvidence().get(0)
                        .getAuditedRequiredFieldCount()));

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> openCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(executionService, times(2)).openOrCreateByContext(openCaptor.capture());
        assertTrue(openCaptor.getAllValues().stream().allMatch(request ->
                BATCH_EXECUTION_ID.equals(request.getBatchExecutionId())
                        && BATCH_TASK_ID.equals(request.getTaskId())
                        && REPORT_ID.equals(request.getBatchRecordReportId())
                        && "LOSS_REPORT".equals(request.getFormSlotType())
                        && "INTERNAL_RECORD".equals(request.getRecordCategory())
                        && "INTERNAL_TRACE".equals(request.getValidationProfile())));

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService, times(2)).saveSystemCellLinkChanges(auditCaptor.capture());
        assertEquals(auditCaptor.getAllValues().get(0).getIdempotencyKey(),
                auditCaptor.getAllValues().get(1).getIdempotencyKey());
        assertTrue(auditCaptor.getAllValues().get(0).getIdempotencyKey().startsWith("AO_RELEASE_LOSS_REPORT:"));
        assertEquals(REQUIRED_FIELDS.size(), auditCaptor.getAllValues().get(0).getChanges().size());
        assertTrue(auditCaptor.getAllValues().get(0).getChanges().stream()
                .noneMatch(change -> "999".equals(change.getNewValueDisplay())));
    }

    @Test
    void shouldBlockBeforeBindingOrWriteWhenFeedbackTotalAndSignedDetailsDoNotReconcile() {
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult sources = formalSources();
        sources.getProcessSources().get(0).getFeedback().setUnqualifiedQuantity(new BigDecimal("3.000"));
        when(sourceReader.read(any())).thenReturn(sources);

        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());

        assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                "LOSS_SOURCE_REQUIRED".equals(blocker.getBlockerType())
                        && ROUTE_PROCESS_ID.equals(blocker.getRouteProcessId())
                        && "lossQuantity".equals(blocker.getFieldCode())));
        assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID));
        verify(bindingMapper, never()).selectListByRouteProcessIdsAndUseType(any(), any());
        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test
    void shouldBlockWhenAnyLossDetailReasonDoesNotMatchTheFormalFeedbackSnapshot() {
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult sources = formalSources();
        sources.getProcessSources().get(0).setLossDetails(List.of(
                new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail()
                        .setReasonId(8301L)
                        .setReasonCode("LOSS-001")
                        .setReasonName("正常损耗")
                        .setQuantity(new BigDecimal("1.250")),
                new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail()
                        .setReasonId(8399L)
                        .setReasonCode("UNMAPPED-REASON")
                        .setReasonName("未建立正式快照的原因")
                        .setQuantity(new BigDecimal("1.250"))));
        when(sourceReader.read(any())).thenReturn(sources);

        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());

        assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                "LOSS_SOURCE_REQUIRED".equals(blocker.getBlockerType())
                        && ROUTE_PROCESS_ID.equals(blocker.getRouteProcessId())
                        && "lossDetails".equals(blocker.getFieldCode())));
        assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID));
        verify(bindingMapper, never()).selectListByRouteProcessIdsAndUseType(any(), any());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void shouldBlockZeroLossWithoutExplicitFormalConfirmationMapping() {
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult sources = formalSources();
        MesProFeedbackDO feedback = sources.getProcessSources().get(0).getFeedback();
        feedback.setQualifiedQuantity(feedback.getFeedbackQuantity());
        feedback.setUnqualifiedQuantity(BigDecimal.ZERO);
        feedback.setLaborScrapQuantity(BigDecimal.ZERO);
        feedback.setMaterialScrapQuantity(BigDecimal.ZERO);
        feedback.setOtherScrapQuantity(BigDecimal.ZERO);
        feedback.setLossReasonId(null);
        feedback.setLossReasonCodeSnapshot(null);
        feedback.setLossReasonNameSnapshot(null);
        sources.getProcessSources().get(0).setLossDetails(List.of());
        when(sourceReader.read(any())).thenReturn(sources);

        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());

        assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                "ZERO_LOSS_CONFIRMATION_UNSUPPORTED".equals(blocker.getBlockerType())));
        assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID));
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void shouldReturnMappingBlockerWithMissingRequiredFieldLocatorBeforeWrite() {
        when(sourceReader.read(any())).thenReturn(formalSources());
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(ROUTE_PROCESS_ID), "BATCH"))
                .thenReturn(List.of(binding()));
        when(reportMapper.selectByReportId(REPORT_ID)).thenReturn(report());
        when(versionMapper.selectById(401L)).thenReturn(version());
        List<MesProBatchRecordCellLinkRuleDO> incomplete = new ArrayList<>(rules());
        incomplete.removeIf(rule -> "reviewerSignedAt".equals(rule.getSourceFieldCode()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, REPORT_ID))
                .thenReturn(incomplete);

        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());

        assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                "LOSS_REPORT_MAPPING_REQUIRED".equals(blocker.getBlockerType())
                        && "reviewerSignedAt".equals(blocker.getFieldCode())));
        assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID));
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void shouldReturnSignatureBlockerBeforeBindingWhenProductionReviewSignatureIsMissing() {
        MesTeamLeaderActiveOrderReleaseLossSourceReadResult sources = formalSources();
        sources.getProcessSources().get(0).getReview().setReviewSignatureId(null);
        when(sourceReader.read(any())).thenReturn(sources);

        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());

        assertTrue(plan.getBlockers().stream().anyMatch(blocker ->
                "PRODUCTION_SIGNATURE_REQUIRED".equals(blocker.getBlockerType())
                        && ROUTE_PROCESS_ID.equals(blocker.getRouteProcessId())));
        verify(bindingMapper, never()).selectListByRouteProcessIdsAndUseType(any(), any());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void shouldFailFastBeforeExecutionWhenCurrentBatchTaskDoesNotMatchTraditionalBinding() {
        mockSuccessfulPlan();
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID))
                .thenReturn(List.of(batchTask().setBatchRecordReportId("OTHER-REPORT")));
        MesTeamLeaderActiveOrderReleaseLossReportPlan plan = writer.plan(command());

        assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID));

        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    private void mockSuccessfulPlan() {
        when(sourceReader.read(any())).thenReturn(formalSources());
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(List.of(ROUTE_PROCESS_ID), "BATCH"))
                .thenReturn(List.of(binding()));
        when(reportMapper.selectByReportId(REPORT_ID)).thenReturn(report());
        when(versionMapper.selectById(401L)).thenReturn(version());
        when(ruleMapper.selectEnabledListByScopeAndTargetReport("ROUTE_VERSION", 401L, REPORT_ID))
                .thenReturn(rules());
    }

    private void mockSuccessfulWrite() {
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(batchTask()));
        when(executionService.openOrCreateByContext(any())).thenReturn(
                new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(9901L));
        when(executionMapper.selectById(9901L)).thenReturn(execution());
        when(fieldAuditService.saveSystemCellLinkChanges(any())).thenReturn(
                new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setAuditBatchId(9911L)
                        .setFieldAuditRevision(15L)
                        .setFieldAuditHeadHash("loss-audit-head")
                        .setCellValuesHash("loss-cell-values")
                        .setChangedFieldCount(REQUIRED_FIELDS.size()));
    }

    private static MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseLossReportPlanCommand()
                .setTenantId(1L)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setWorkOrderId(WORK_ORDER_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setProductId(3101L)
                .setBatchCode("BATCH-9001")
                .setSourceSnapshotHash("AO_RELEASE_SOURCE_V1:loss-source")
                .setProcessSnapshots(List.of(snapshot()));
    }

    private static MesTeamLeaderActiveOrderReleaseLossSourceReadResult formalSources() {
        return new MesTeamLeaderActiveOrderReleaseLossSourceReadResult()
                .setProcessSources(List.of(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource()
                        .setSnapshot(snapshot())
                        .setFeedback(feedback())
                        .setEvent(event())
                        .setAllocation(allocation())
                        .setReview(review())
                        .setLossDetails(List.of(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.LossDetail()
                                .setReasonId(8301L)
                                .setReasonCode("LOSS-001")
                                .setReasonName("正常损耗")
                                .setQuantity(new BigDecimal("2.500"))))))
                .setBlockers(List.of());
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

    private static MesProFeedbackDO feedback() {
        return MesProFeedbackDO.builder()
                .id(5101L)
                .code("FB-5101")
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .processId(PROCESS_ID)
                .feedbackTime(LocalDateTime.of(2026, 8, 1, 8, 30))
                .feedbackQuantity(new BigDecimal("100.000"))
                .qualifiedQuantity(new BigDecimal("97.500"))
                .unqualifiedQuantity(new BigDecimal("2.500"))
                .laborScrapQuantity(new BigDecimal("1.000"))
                .materialScrapQuantity(new BigDecimal("1.500"))
                .otherScrapQuantity(BigDecimal.ZERO)
                .lossReasonId(8301L)
                .lossReasonCodeSnapshot("LOSS-001")
                .lossReasonNameSnapshot("正常损耗")
                .feedbackUserId(2101L)
                .approveUserId(3001L)
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
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(5101L)
                .rawPayload("{\"lossQuantity\":999}")
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
                .allocatedQuantity(new BigDecimal("100.000"))
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

    private static MesProRouteFlowProcessBatchRecordDO binding() {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(3001L)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .useType("BATCH")
                .batchRecordReportId(REPORT_ID)
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .formSlotType("LOSS_REPORT")
                .recordCategory("INTERNAL_RECORD")
                .validationProfile("INTERNAL_TRACE")
                .permissionScopeId(9901L)
                .recordCategorySnapshotHash("record-category-snapshot")
                .slotConfigSnapshotHash("loss-slot-snapshot")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .build();
    }

    private static MesProBatchRecordReportDO report() {
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setId(3301L);
        report.setReportId(REPORT_ID);
        report.setReportCode("LOSS-R-01");
        report.setReportName("生产损耗单");
        report.setFormSlotType("LOSS_REPORT");
        report.setBatchRecordDefinitionId(400L);
        report.setBatchRecordVersionId(401L);
        report.setSourceFileSha256("loss-template-sha256");
        return report;
    }

    private static MesProBatchRecordVersionDO version() {
        return MesProBatchRecordVersionDO.builder()
                .id(401L)
                .definitionId(400L)
                .versionNo("V1.0")
                .status("APPROVED")
                .routeId(ROUTE_ID)
                .sourceFileSha256("loss-template-sha256")
                .build();
    }

    private static List<MesProBatchRecordCellLinkRuleDO> rules() {
        List<MesProBatchRecordCellLinkRuleDO> rules = new ArrayList<>();
        for (int index = 0; index < REQUIRED_FIELDS.size(); index++) {
            String sourceField = REQUIRED_FIELDS.get(index);
            MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
            rule.setId(6000L + index);
            rule.setRuleVersion(1L);
            rule.setScopeType("ROUTE_VERSION");
            rule.setScopeId(401L);
            rule.setSourceType("PRODUCTION_LOSS");
            rule.setSourceFieldCode(sourceField);
            rule.setTargetReportId(REPORT_ID);
            rule.setTargetRowIndex(10 + index);
            rule.setTargetColumnIndex(2);
            rule.setTargetCellKey("R" + (10 + index) + "C2");
            rule.setTargetValueType(numericField(sourceField) ? "NUMBER" : "STRING");
            rule.setAggregationStrategy("LAST");
            rule.setEnabled(true);
            rules.add(rule);
        }
        return rules;
    }

    private static boolean numericField(String field) {
        return !List.of("batchCode", "lossDetails", "fillerSignedAt", "reviewerSignedAt").contains(field);
    }

    private static MesProEdhrBatchExecutionTaskDO batchTask() {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(BATCH_TASK_ID)
                .batchExecutionId(BATCH_EXECUTION_ID)
                .nodeType("ROUTE_FORM")
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .batchRecordReportId(REPORT_ID)
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .formSlotType("LOSS_REPORT")
                .recordCategory("INTERNAL_RECORD")
                .validationProfile("INTERNAL_TRACE")
                .routeBindingId(3001L)
                .routeBindingSnapshotHash("record-category-snapshot")
                .slotConfigSnapshotHash("loss-slot-snapshot")
                .build();
    }

    private static MesProBatchRecordExecutionDO execution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(9901L)
                .workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .taskId(BATCH_TASK_ID)
                .batchExecutionId(BATCH_EXECUTION_ID)
                .batchRecordReportId(REPORT_ID)
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .formSlotType("LOSS_REPORT")
                .recordCategory("INTERNAL_RECORD")
                .validationProfile("INTERNAL_TRACE")
                .routeBindingId(3001L)
                .routeBindingSnapshotHash("record-category-snapshot")
                .slotConfigSnapshotHash("loss-slot-snapshot")
                .batchCode("BATCH-9001")
                .executionSnapshotJson(executionSnapshotJson())
                .cellValuesJson("[]")
                .cellValuesHash("before-loss-cells")
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash("before-loss-audit")
                .build();
    }

    private static String executionSnapshotJson() {
        StringBuilder json = new StringBuilder("{\"fields\":[");
        for (int index = 0; index < REQUIRED_FIELDS.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            String field = REQUIRED_FIELDS.get(index);
            json.append("{\"rowIndex\":").append(10 + index)
                    .append(",\"columnIndex\":2,\"fieldPath\":\"sheet.R")
                    .append(10 + index).append("C2\",\"fieldKey\":\"").append(field)
                    .append("\",\"valueType\":\"")
                    .append(numericField(field) ? "NUMBER" : "STRING")
                    .append("\",\"defaultValue\":null}");
        }
        return json.append("]}").toString();
    }
}
