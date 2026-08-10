package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionOpenOrCreateByContextRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordCellLinkRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveChangesCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSaveResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest {

    private static final long TENANT_ID = 1L;
    private static final long ACTIVE_ORDER_ID = 101L;
    private static final long WORK_ORDER_ID = 201L;
    private static final long PRODUCT_ID = 301L;
    private static final long ROUTE_ID = 401L;
    private static final long ROUTE_VERSION_ID = 402L;
    private static final long ROUTE_PROCESS_ID = 501L;
    private static final long PROCESS_ID = 502L;
    private static final long TASK_ID = 601L;
    private static final long EVENT_ID = 602L;
    private static final long PQC_RECORD_ID = 603L;
    private static final long REVIEW_ID = 604L;
    private static final long AGGREGATE_ID = 605L;
    private static final long REGULATION_ID = 701L;
    private static final long REGULATION_VERSION_ID = 702L;
    private static final long BINDING_ID = 801L;
    private static final String REPORT_ID = "PI-REPORT-1";
    private static final long BATCH_EXECUTION_ID = 901L;
    private static final long BATCH_TASK_ID = 902L;
    private static final long EXECUTION_ID = 903L;
    private static final long AUDIT_BATCH_ID = 904L;
    private static final LocalDateTime INSPECTED_AT = LocalDateTime.of(2026, 8, 9, 9, 10, 11);
    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 8, 9, 10, 11, 12);

    private MesTeamLeaderActiveOrderReleaseProcessInspectionReader reader;
    private MesProRouteFlowProcessBatchRecordMapper bindingMapper;
    private MesProBatchRecordCellLinkRuleMapper ruleMapper;
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    private MesProBatchRecordExecutionService executionService;
    private MesProBatchRecordExecutionMapper executionMapper;
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    private MesTeamLeaderActiveOrderReleaseProcessInspectionWriter writer;

    @BeforeEach
    void setUp() {
        reader = mock(MesTeamLeaderActiveOrderReleaseProcessInspectionReader.class);
        bindingMapper = mock(MesProRouteFlowProcessBatchRecordMapper.class);
        ruleMapper = mock(MesProBatchRecordCellLinkRuleMapper.class);
        batchTaskMapper = mock(MesProEdhrBatchExecutionTaskMapper.class);
        executionService = mock(MesProBatchRecordExecutionService.class);
        executionMapper = mock(MesProBatchRecordExecutionMapper.class);
        fieldAuditService = mock(MesProBatchRecordExecutionFieldAuditService.class);
        writer = new MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl(reader, bindingMapper, ruleMapper,
                batchTaskMapper, executionService, executionMapper, fieldAuditService);
    }

    @Test
    void confirmedAggregateWritesTraditionalCurrentBatchExecutionWithExactEvidence() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command = command();
        stubFormalPlan(command, source());
        stubWriteTarget();

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command);
        MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult result =
                writer.write(plan, BATCH_EXECUTION_ID);

        assertTrue(plan.getBlockers().isEmpty());
        assertEquals(2, plan.getSignatureEvidence().size());
        assertEquals(List.of(1101L, 1102L), plan.getSignatureEvidence().stream()
                .map(MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence::getSignatureId)
                .toList());
        assertEquals(List.of(INSPECTED_AT, REVIEWED_AT), plan.getSignatureEvidence().stream()
                .map(MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence::getSignedAt)
                .toList());
        assertEquals("PROCESS_INSPECTION", result.getDocumentType());
        assertEquals(List.of(EXECUTION_ID), result.getBatchRecordExecutionIds());
        assertEquals(List.of(AUDIT_BATCH_ID), result.getFieldAuditIds());
        assertFalse(result.getSourceValueHashes().isEmpty());

        ArgumentCaptor<MesProBatchRecordExecutionOpenOrCreateByContextReqVO> openCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionOpenOrCreateByContextReqVO.class);
        verify(executionService).openOrCreateByContext(openCaptor.capture());
        MesProBatchRecordExecutionOpenOrCreateByContextReqVO open = openCaptor.getValue();
        assertEquals(BATCH_EXECUTION_ID, open.getBatchExecutionId());
        assertEquals(BATCH_TASK_ID, open.getTaskId());
        assertEquals(REPORT_ID, open.getBatchRecordReportId());
        assertEquals("PROCESS_INSPECTION", open.getFormSlotType());
        assertEquals("INTERNAL_RECORD", open.getRecordCategory());
        assertEquals("INTERNAL_TRACE", open.getValidationProfile());
        assertEquals(BINDING_ID, open.getRouteBindingId());

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService).saveSystemCellLinkChanges(auditCaptor.capture());
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand audit = auditCaptor.getValue();
        assertTrue(audit.getIdempotencyKey().startsWith("AO_RELEASE_PQC_AGGREGATE_DETAIL:"));
        assertEquals(17, audit.getChanges().size());
        assertTrue(audit.getChanges().stream().anyMatch(change -> "10.5".equals(change.getNewValueDisplay())));
        assertTrue(audit.getChanges().stream().anyMatch(change -> "11001".equals(change.getNewValueDisplay())));
        assertTrue(audit.getChanges().stream().anyMatch(change -> "2026-08-09 10:11:12"
                .equals(change.getNewValueDisplay())));
    }

    @Test
    void submittedOnlyTaskReturnsBlockerWithoutAnyTargetWrite() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source = source();
        source.getTask().setTaskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED);
        when(reader.read(any())).thenReturn(bundle(source));

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command());

        assertEquals(List.of("PQC_CONFIRMED_AGGREGATE_REQUIRED"), blockerTypes(plan));
        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test
    void qaEquipmentMismatchReturnsLocatedBlocker() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source = source();
        source.getAggregateDetails().get(0).setSelectedEquipmentId(9999L);
        when(reader.read(any())).thenReturn(bundle(source));
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(any(), any())).thenReturn(List.of(binding()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport(any(), any(), any())).thenReturn(rules());

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command());

        assertEquals(List.of("PQC_QA_ITEM_MISMATCH"), blockerTypes(plan));
        assertEquals("PRESSURE", plan.getBlockers().get(0).getObjectCode());
        assertEquals(String.valueOf(AGGREGATE_ID), plan.getBlockers().get(0).getObjectId());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void invalidNumericAggregateCannotBeAcceptedAsFailedJudgement() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source = source();
        source.getAggregateDetails().get(0).setItemResult("not-a-number");
        source.getAggregateDetails().get(0).setMeasuredValue("not-a-number");
        source.getAggregateDetails().get(0).setJudgement(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE);
        source.getPqcRecord().setInspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_FAILURE);
        when(reader.read(any())).thenReturn(bundle(source));

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command());

        assertEquals(List.of("PQC_QA_ITEM_MISMATCH"), blockerTypes(plan));
        assertEquals("PRESSURE", plan.getBlockers().get(0).getObjectCode());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void choiceAggregateKeepsItsFormalMeasuredValueDuringSideEffectFreePlan() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command = command();
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source = source();
        MesPqcProcessInspectionAggregateDetailDO detail = source.getAggregateDetails().get(0);
        detail.setStandardLowerLimit(null);
        detail.setStandardUpperLimit(null);
        detail.setStandardUnit(null);
        detail.setStandardPrecision(null);
        detail.setResultType("CHOICE");
        detail.setItemResult("合格");
        detail.setMeasuredValue("合格");
        MesQaInspectionRegulationItemDO item = source.getRegulationItems().get(0);
        item.setStandardLowerLimit(null);
        item.setStandardUpperLimit(null);
        item.setStandardUnit(null);
        item.setStandardPrecision(null);
        item.setResultType("CHOICE");
        when(reader.read(command)).thenReturn(bundle(source));
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(any(), any())).thenReturn(List.of(binding()));
        List<MesProBatchRecordCellLinkRuleDO> choiceRules = new ArrayList<>(rules());
        choiceRules.removeIf(rule -> List.of("standardLowerLimit", "standardUpperLimit", "standardUnit",
                "standardPrecision").contains(rule.getSourceFieldCode()));
        choiceRules.stream().filter(rule -> "measuredValue".equals(rule.getSourceFieldCode())).findFirst()
                .orElseThrow().setTargetValueType("STRING");
        when(ruleMapper.selectEnabledListByScopeAndTargetReport(any(), any(), any())).thenReturn(choiceRules);

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command);

        assertTrue(plan.getBlockers().isEmpty());
        assertEquals("合格", plan.getPreparedInspections().get(0).getMappedValues().stream()
                .filter(value -> "measuredValue".equals(value.getRule().getSourceFieldCode()))
                .findFirst().orElseThrow().getValue());
        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test
    void missingRequiredMappingReturnsLocatedBlocker() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command = command();
        when(reader.read(command)).thenReturn(bundle(source()));
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(any(), any())).thenReturn(List.of(binding()));
        List<MesProBatchRecordCellLinkRuleDO> incomplete = new ArrayList<>(rules());
        incomplete.removeIf(rule -> "reviewedAt".equals(rule.getSourceFieldCode()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport(any(), any(), any())).thenReturn(incomplete);

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command);

        assertEquals(List.of("PROCESS_INSPECTION_MAPPING_REQUIRED"), blockerTypes(plan));
        assertEquals("reviewedAt", plan.getBlockers().get(0).getObjectCode());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void routeBoundProcessInspectionFormTemplateIsFormalSourceButBlocksUntilDynamicAutoWriteExists() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command = command();
        when(reader.read(command)).thenReturn(bundle(source()));
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(any(), any()))
                .thenReturn(List.of(dynamicFormBinding(28L, "PI_" + ROUTE_PROCESS_ID)));

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command);

        assertEquals(List.of("PROCESS_INSPECTION_DYNAMIC_FORM_AUTOWRITE_REQUIRED"), blockerTypes(plan));
        assertEquals(String.valueOf(BINDING_ID), plan.getBlockers().get(0).getObjectId());
        assertEquals("PI_" + ROUTE_PROCESS_ID, plan.getBlockers().get(0).getObjectCode());
        verify(ruleMapper, never()).selectEnabledListByScopeAndTargetReport(any(), any(), any());
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void missingPqcReviewSignatureReturnsBlocker() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source = source();
        source.getReview().setReviewSignatureId(null);
        when(reader.read(any())).thenReturn(bundle(source));

        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command());

        assertEquals(List.of("PQC_SIGNATURE_REQUIRED"), blockerTypes(plan));
        verify(executionService, never()).openOrCreateByContext(any());
    }

    @Test
    void wrongCurrentBatchTaskFailsBeforeOpeningExecution() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command = command();
        stubFormalPlan(command, source());
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan = writer.plan(command);
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(
                batchTask().setFormSlotType("MAIN")));

        assertThrows(ServiceException.class, () -> writer.write(plan, BATCH_EXECUTION_ID));
        verify(executionService, never()).openOrCreateByContext(any());
        verify(fieldAuditService, never()).saveSystemCellLinkChanges(any());
    }

    @Test
    void replayUsesStableIdempotencyAndSourceHashesIgnoreRawPayload() {
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command = command();
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource firstSource = source();
        stubFormalPlan(command, firstSource);
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan firstPlan = writer.plan(command);
        reset(reader, bindingMapper, ruleMapper);
        MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource replaySource = source();
        replaySource.getEvent().setRawPayload("different-raw-payload-that-is-not-a-source");
        stubFormalPlan(command, replaySource);
        MesTeamLeaderActiveOrderReleaseProcessInspectionPlan replayPlan = writer.plan(command);
        assertEquals(firstPlan.getSourceValueHashes(), replayPlan.getSourceValueHashes());

        stubWriteTarget();
        writer.write(replayPlan, BATCH_EXECUTION_ID);
        writer.write(replayPlan, BATCH_EXECUTION_ID);

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSaveChangesCommand> captor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSaveChangesCommand.class);
        verify(fieldAuditService, times(2)).saveSystemCellLinkChanges(captor.capture());
        assertEquals(captor.getAllValues().get(0).getIdempotencyKey(),
                captor.getAllValues().get(1).getIdempotencyKey());
    }

    private void stubFormalPlan(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command,
                                MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        when(reader.read(command)).thenReturn(bundle(source));
        when(bindingMapper.selectListByRouteProcessIdsAndUseType(any(), any())).thenReturn(List.of(binding()));
        when(ruleMapper.selectEnabledListByScopeAndTargetReport(any(), any(), any())).thenReturn(rules());
    }

    private void stubWriteTarget() {
        when(batchTaskMapper.selectListByBatchExecutionId(BATCH_EXECUTION_ID)).thenReturn(List.of(batchTask()));
        when(executionService.openOrCreateByContext(any())).thenReturn(
                new MesProBatchRecordExecutionOpenOrCreateByContextRespVO().setId(EXECUTION_ID));
        when(executionMapper.selectById(EXECUTION_ID)).thenReturn(execution());
        when(fieldAuditService.saveSystemCellLinkChanges(any())).thenReturn(
                new MesProBatchRecordExecutionFieldAuditSaveResult()
                        .setAuditBatchId(AUDIT_BATCH_ID)
                        .setCellValuesHash("after-cell-hash")
                        .setFieldAuditHeadHash("after-audit-head")
                        .setChangedFieldCount(17));
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command() {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand()
                .setTenantId(TENANT_ID)
                .setActiveOrderId(ACTIVE_ORDER_ID)
                .setWorkOrderId(WORK_ORDER_ID)
                .setProductId(PRODUCT_ID)
                .setRouteId(ROUTE_ID)
                .setRouteVersionId(ROUTE_VERSION_ID)
                .setBatchCode("BATCH-001")
                .setSourceSnapshotHash("source-snapshot-001");
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionReader.SourceBundle bundle(
            MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source) {
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionReader.SourceBundle()
                .setSources(List.of(source));
    }

    private static MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source() {
        MesPqcInspectionTaskDO task = MesPqcInspectionTaskDO.builder()
                .id(TASK_ID).activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID)
                .regulationVersionId(REGULATION_VERSION_ID).inspectionType("PQC")
                .businessDate(LocalDate.of(2026, 8, 9)).shiftCode("DAY").roundNo(1)
                .plannedInspectionQuantity(1).actualInspectionQuantity(1)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED).build();
        task.setTenantId(TENANT_ID);
        MesPqcProcessInspectionAggregateDetailDO detail = MesPqcProcessInspectionAggregateDetailDO.builder()
                .id(AGGREGATE_ID).sourcePqcRecordId(PQC_RECORD_ID).sourcePieceDetailId(606L)
                .eventId(EVENT_ID).reviewId(REVIEW_ID).productionSubmitEventId(607L).pqcTaskId(TASK_ID)
                .activeOrderId(ACTIVE_ORDER_ID).workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID)
                .routeVersionId(ROUTE_VERSION_ID).routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID)
                .regulationVersionId(REGULATION_VERSION_ID).inspectionType("PQC")
                .businessDate(LocalDate.of(2026, 8, 9)).shiftCode("DAY").roundNo(1)
                .actualInspectionQuantity(1).sampleNo(1).itemCode("PRESSURE").itemName("压力")
                .inspectionMethod("压力表测量").standardText("10.0-11.0 MPa")
                .selectedEquipmentId(10001L).selectedEquipmentCode("EQ-P").selectedEquipmentName("压力表")
                .selectedEquipmentNumber("EQ-P-01").standardLowerLimit(new BigDecimal("10.0"))
                .standardUpperLimit(new BigDecimal("11.0")).standardUnit("MPa").standardPrecision(1)
                .resultType("NUMBER").itemResult("10.5").measuredValue("10.5")
                .judgement(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .aggregatedAt(LocalDateTime.of(2026, 8, 9, 10, 12, 13)).build();
        detail.setTenantId(TENANT_ID);
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                .id(EVENT_ID).eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID).actualEmployeeId(11001L).feedbackSourceType("MES_PQC_INSPECTION_TASK")
                .feedbackSourceId(TASK_ID).serverSubmitTime(INSPECTED_AT).signatureId(1101L)
                .signatureUserId(11001L).signatureSnapshot("pqc-signature-evidence")
                .rawPayload("raw-payload-must-not-enter-hash").build();
        event.setTenantId(TENANT_ID);
        MesProProcessPoolPqcRecordDO record = MesProProcessPoolPqcRecordDO.builder()
                .id(PQC_RECORD_ID).eventId(EVENT_ID).productionSubmitEventId(607L).workOrderId(WORK_ORDER_ID)
                .routeId(ROUTE_ID).routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID)
                .actualEmployeeId(11001L).signatureId(1101L).signatureUserId(11001L)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .serverSubmitTime(INSPECTED_AT)
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED)
                .processInspectionReviewId(REVIEW_ID).build();
        record.setTenantId(TENANT_ID);
        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .id(REVIEW_ID).eventId(EVENT_ID).leaderUserId(12001L).leaderType("PQC")
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED).reviewedAt(REVIEWED_AT)
                .reviewSignatureId(1102L).reviewSignatureUserId(12001L)
                .reviewSignatureSnapshotJson("pqc-review-signature-evidence").build();
        review.setTenantId(TENANT_ID);
        MesQaInspectionRegulationDO regulation = MesQaInspectionRegulationDO.builder()
                .id(REGULATION_ID).productId(PRODUCT_ID).routeId(ROUTE_ID).routeVersionId(ROUTE_VERSION_ID)
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID)
                .ownerModule(MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .lifecycleStatus("PUBLISHED").currentVersionId(REGULATION_VERSION_ID).build();
        regulation.setTenantId(TENANT_ID);
        MesQaInspectionRegulationVersionDO version = MesQaInspectionRegulationVersionDO.builder()
                .id(REGULATION_VERSION_ID).regulationId(REGULATION_ID).versionNo("V1")
                .lifecycleStatus("PUBLISHED").publishedAt(LocalDateTime.of(2026, 8, 1, 8, 0))
                .snapshotJson("qa-version-snapshot").build();
        version.setTenantId(TENANT_ID);
        MesQaInspectionRegulationItemDO item = MesQaInspectionRegulationItemDO.builder()
                .id(703L).regulationVersionId(REGULATION_VERSION_ID).inspectionType("PQC")
                .itemCode("PRESSURE").itemName("压力").inspectionMethod("压力表测量")
                .standardText("10.0-11.0 MPa").standardLowerLimit(new BigDecimal("10.0"))
                .standardUpperLimit(new BigDecimal("11.0")).standardUnit("MPa").standardPrecision(1)
                .equipmentRequired(true).resultType("NUMBER").build();
        item.setTenantId(TENANT_ID);
        MesQaInspectionRegulationItemEquipmentDO equipment = MesQaInspectionRegulationItemEquipmentDO.builder()
                .id(704L).regulationVersionId(REGULATION_VERSION_ID).inspectionType("PQC")
                .itemCode("PRESSURE").equipmentId(10001L).equipmentCode("EQ-P").equipmentName("压力表")
                .equipmentNumber("EQ-P-01").defaultFlag(true).sort(1).build();
        equipment.setTenantId(TENANT_ID);
        return new MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource()
                .setTask(task).setAggregateDetails(List.of(detail)).setEvent(event).setPqcRecord(record)
                .setReview(review).setRegulation(regulation).setRegulationVersion(version)
                .setRegulationItems(List.of(item)).setRegulationItemEquipment(List.of(equipment));
    }

    private static MesProRouteFlowProcessBatchRecordDO binding() {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(BINDING_ID).routeId(ROUTE_ID).routeProcessId(ROUTE_PROCESS_ID).useType("BATCH")
                .batchRecordReportId(REPORT_ID).batchRecordDefinitionId(802L).batchRecordVersionId(803L)
                .formSlotType("PROCESS_INSPECTION").recordCategory("INTERNAL_RECORD")
                .validationProfile("INTERNAL_TRACE").ownerRoleKey("QUALITY")
                .recordCategorySnapshotHash("binding-snapshot").slotConfigSnapshotHash("slot-snapshot")
                .build();
    }

    private static MesProRouteFlowProcessBatchRecordDO dynamicFormBinding(Long templateId, String bindingKey) {
        return MesProRouteFlowProcessBatchRecordDO.builder()
                .id(BINDING_ID).routeId(ROUTE_ID).routeProcessId(ROUTE_PROCESS_ID).useType("BATCH")
                .formSlotType("PROCESS_INSPECTION").formBindingKey(bindingKey).formTemplateId(templateId)
                .formTemplateNameSnapshot("过程检验记录表单")
                .lastPublishedTemplateVersionId(2801L).lastPublishedTemplateVersionNo("V1")
                .recordCategory("INTERNAL_RECORD").validationProfile("INTERNAL_TRACE").ownerRoleKey("QUALITY")
                .recordCategorySnapshotHash("binding-snapshot").slotConfigSnapshotHash("slot-snapshot")
                .build();
    }

    private static List<MesProBatchRecordCellLinkRuleDO> rules() {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("itemCode", "STRING");
        fields.put("itemName", "STRING");
        fields.put("inspectionMethod", "STRING");
        fields.put("standardText", "STRING");
        fields.put("standardLowerLimit", "NUMBER");
        fields.put("standardUpperLimit", "NUMBER");
        fields.put("standardUnit", "STRING");
        fields.put("standardPrecision", "NUMBER");
        fields.put("resultType", "STRING");
        fields.put("measuredValue", "NUMBER");
        fields.put("judgement", "STRING");
        fields.put("selectedEquipmentId", "NUMBER");
        fields.put("selectedEquipmentNumber", "STRING");
        fields.put("inspectorUserId", "NUMBER");
        fields.put("inspectedAt", "DATETIME");
        fields.put("reviewerUserId", "NUMBER");
        fields.put("reviewedAt", "DATETIME");
        List<MesProBatchRecordCellLinkRuleDO> result = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldCode = entry.getKey();
            boolean header = fieldCode.startsWith("inspector") || fieldCode.startsWith("reviewer")
                    || fieldCode.endsWith("At");
            result.add(rule(2000L + index, fieldCode,
                    header ? headerKey(fieldCode) : itemKey(fieldCode), entry.getValue(), index, 1));
            index += 1;
        }
        return result;
    }

    private static MesProBatchRecordCellLinkRuleDO rule(long id, String fieldCode, String sourceCellKey,
                                                        String valueType, int row, int column) {
        MesProBatchRecordCellLinkRuleDO rule = new MesProBatchRecordCellLinkRuleDO();
        rule.setId(id);
        rule.setScopeType("ROUTE_VERSION");
        rule.setScopeId(803L);
        rule.setRouteId(ROUTE_ID);
        rule.setBatchRecordDefinitionId(802L);
        rule.setBatchRecordVersionId(803L);
        rule.setSourceType("PQC_AGGREGATE_DETAIL");
        rule.setSourceCellKey(sourceCellKey);
        rule.setSourceFieldCode(fieldCode);
        rule.setSourceValueType(valueType);
        rule.setTargetReportId(REPORT_ID);
        rule.setTargetRowIndex(row);
        rule.setTargetColumnIndex(column);
        rule.setTargetCellKey(row + ":" + column);
        rule.setTargetValueType(valueType);
        rule.setTemplateSnapshotHash("target-template-snapshot");
        rule.setRuleVersion(1L);
        rule.setEnabled(true);
        return rule;
    }

    private static MesProEdhrBatchExecutionTaskDO batchTask() {
        return MesProEdhrBatchExecutionTaskDO.builder()
                .id(BATCH_TASK_ID).batchExecutionId(BATCH_EXECUTION_ID).nodeType("ROUTE_FORM")
                .routeProcessId(ROUTE_PROCESS_ID).processId(PROCESS_ID).batchRecordReportId(REPORT_ID)
                .batchRecordDefinitionId(802L).batchRecordVersionId(803L).formSlotType("PROCESS_INSPECTION")
                .recordCategory("INTERNAL_RECORD").validationProfile("INTERNAL_TRACE")
                .ownerRoleKey("QUALITY").routeBindingId(BINDING_ID).routeBindingSnapshotHash("binding-snapshot")
                .slotConfigSnapshotHash("slot-snapshot").build();
    }

    private static MesProBatchRecordExecutionDO execution() {
        List<Map<String, Object>> fields = new ArrayList<>();
        List<Map<String, Object>> cells = new ArrayList<>();
        int index = 0;
        for (MesProBatchRecordCellLinkRuleDO rule : rules()) {
            fields.add(Map.of("fieldPath", "cells[" + index + "]", "fieldKey", "field-" + index,
                    "rowIndex", rule.getTargetRowIndex(), "columnIndex", rule.getTargetColumnIndex(),
                    "valueType", rule.getTargetValueType(), "required", true, "label", rule.getSourceFieldCode()));
            cells.add(Map.of("rowIndex", rule.getTargetRowIndex(), "columnIndex", rule.getTargetColumnIndex(),
                    "value", "", "valueDisplay", ""));
            index += 1;
        }
        return MesProBatchRecordExecutionDO.builder()
                .id(EXECUTION_ID).workOrderId(WORK_ORDER_ID).routeId(ROUTE_ID).routeProcessId(ROUTE_PROCESS_ID)
                .taskId(BATCH_TASK_ID).batchExecutionId(BATCH_EXECUTION_ID).batchRecordReportId(REPORT_ID)
                .batchRecordDefinitionId(802L).batchRecordVersionId(803L).formSlotType("PROCESS_INSPECTION")
                .recordCategory("INTERNAL_RECORD").validationProfile("INTERNAL_TRACE")
                .routeBindingId(BINDING_ID).routeBindingSnapshotHash("binding-snapshot")
                .slotConfigSnapshotHash("slot-snapshot").batchCode("BATCH-001").status(0)
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of("fields", fields)))
                .cellValuesJson(JsonUtils.toJsonString(cells)).cellValuesHash("base-cell-hash")
                .fieldAuditRevision(0L).fieldAuditHeadHash("base-audit-head").build();
    }

    private static List<String> blockerTypes(MesTeamLeaderActiveOrderReleaseProcessInspectionPlan plan) {
        return plan.getBlockers().stream().map(MesTeamLeaderActiveOrderReleaseBlocker::getBlockerType).toList();
    }

    private static String itemKey(String fieldCode) {
        return "PQC|PRESSURE|1|" + fieldCode;
    }

    private static String headerKey(String fieldCode) {
        return "PQC|" + fieldCode;
    }
}
