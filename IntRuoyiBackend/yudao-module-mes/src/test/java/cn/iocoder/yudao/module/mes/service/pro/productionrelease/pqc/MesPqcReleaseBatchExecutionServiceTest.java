package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFlow6CompletionBackfillReceipt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcReleaseBatchExecutionServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PQC_USER_ID = 7101L;
    private static final Long APPLICATION_ID = 7001L;
    private static final Long PQC_WORK_TASK_ID = 8001L;
    private static final Long BATCH_EXECUTION_ID = 9001L;
    private static final int VERSION = 1;

    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProductionReleaseRequiredCandidateResolver candidateResolver;
    @Mock private MesPqcReleaseDossierPort dossierPort;
    @Mock private MesProductionReleaseBatchExecutionPort batchExecutionPort;
    @Mock private MesProductionReleaseReportStageInitializer reportStageInitializer;
    @Mock private MesReleaseFlowAuditRecorder auditRecorder;

    private MesPqcProductionReleaseService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        service = new MesPqcProductionReleaseServiceImpl(
                applicationMapper, workTaskMapper, candidateResolver, dossierPort,
                batchExecutionPort, reportStageInitializer, auditRecorder,
                Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC));
        lenient().when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(application());
        lenient().when(workTaskMapper.selectById(PQC_WORK_TASK_ID)).thenReturn(workTask());
        lenient().when(candidateResolver.resolveRequiredCandidates(
                        TENANT_ID, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        91L, MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER,
                        List.of(PQC_USER_ID, 7102L), "pqc-candidate-hash"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void pqcApproveCreatesBatchExecutionOnlyAfterPqcRelease() {
        List<MesProductionReleaseReportUploadTaskReceipt> reportTasks = reportTasks();
        when(dossierPort.readCompletionReceipt(99L, TENANT_ID)).thenReturn(validFlow6Receipt(false));
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(reportStageInitializer.initializeRequiredReportStage(any()))
                .thenReturn(new MesProductionReleaseReportStageInitializationResult()
                        .setReportUploadTasks(reportTasks)
                        .setReportSnapshotHash("report-hash"));
        when(applicationMapper.approveFromPending(eq(APPLICATION_ID), eq(VERSION), eq(BATCH_EXECUTION_ID),
                eq(PQC_USER_ID), any(), eq("report-hash"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("APPROVE"))).thenReturn(1);

        MesPqcProductionReleaseDecisionResult result = service.approve(PQC_USER_ID,
                new MesPqcProductionReleaseApproveCommand()
                        .setApplicationId(APPLICATION_ID)
                        .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                        .setExpectedVersion(VERSION)
                        .setIdempotencyKey("pqc-approve-7001")
                        .setApprovalOpinion("正式来源核对通过")
                        .setEntryType("ACTIVE_ORDER_PQC")
                        .setEntryBusinessId("release-application-7001")
                        .setSourceCredentialType("CompletionBackfillReceipt")
                        .setSourceCredentialId("99")
                        .setSourceContextHash("source-context-7001")
                        .setSourceSnapshotHash("source-hash")
                        .setPayloadHash("payload-7001")
                        .setSourceVersion("source-v1")
                        .setSourceBundleHash("bundle-hash")
                        .setPickListBindingId(601L)
                        .setPickListId(602L)
                        .setBatchPickListRelationId(603L)
                        .setBindingVersion(1L)
                        .setPickListHeaderSnapshotHash("pick-header-hash")
                        .setPickListLineSnapshotHash("pick-line-hash")
                        .setCompletionTransactionId("completion-tx-7001")
                        .setExpectedActiveOrderVersion(1L)
                        .setSourceEvidence(sourceEvidence())
                        .setCompletionBackfillReceiptId("99"));

        assertEquals("APPROVE", result.getDecision());
        assertEquals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, result.getStatus());
        assertEquals(BATCH_EXECUTION_ID, result.getBatchExecutionId());
        assertEquals(List.of(101L), result.getBatchRecordEvidenceIds());
        assertEquals(List.of(201L), result.getProcessInspectionEvidenceIds());
        assertEquals(List.of(), result.getLossReportEvidenceIds());
        assertEquals(4, result.getReportUploadTasks().size());
        assertTrue(result.getReportUploadTasks().stream().allMatch(
                item -> MesProEdhrWorkTaskStatus.TODO.equals(item.getStatus())));
        verify(batchExecutionPort).openOrCreate(argThat(item ->
                "ACTIVE_ORDER_PQC".equals(item.getEntryType())
                        && "release-application-7001".equals(item.getEntryBusinessId())
                        && "99".equals(item.getSourceCredentialId())
                        && "source-context-7001".equals(item.getSourceContextHash())
                        && "source-hash".equals(item.getSourceSnapshotHash())
                        && "payload-7001".equals(item.getPayloadHash())));
        verify(dossierPort).readCompletionReceipt(99L, TENANT_ID);
        verify(reportStageInitializer).initializeRequiredReportStage(any());
    }

    @Test
    void pqcApproveConsumesPersistedCompletionReceiptWithoutWritingReleaseDossier() {
        MesFlow6CompletionBackfillReceipt receipt = validFlow6Receipt(false)
                .setRequestIdempotencyKey("pqc-approve-receipt");
        when(dossierPort.readCompletionReceipt(99L, TENANT_ID)).thenReturn(receipt);
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(reportStageInitializer.initializeRequiredReportStage(any()))
                .thenReturn(new MesProductionReleaseReportStageInitializationResult()
                        .setReportUploadTasks(reportTasks())
                        .setReportSnapshotHash("report-hash"));
        when(applicationMapper.approveFromPending(eq(APPLICATION_ID), eq(VERSION), eq(BATCH_EXECUTION_ID),
                eq(PQC_USER_ID), any(), eq("report-hash"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("APPROVE"))).thenReturn(1);

        service.approve(PQC_USER_ID, approveCommand("pqc-approve-receipt")
                .setCompletionBackfillReceiptId("99"));

        verify(dossierPort).readCompletionReceipt(99L, TENANT_ID);
    }

    @Test
    void pqcRejectDoesNotCreateBatchExecutionOrDownstreamDocuments() {
        when(applicationMapper.rejectFromPending(eq(APPLICATION_ID), eq(VERSION), eq(PQC_USER_ID),
                any(), eq("检验结论不通过"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("REJECT"))).thenReturn(1);

        MesPqcProductionReleaseDecisionResult result = service.reject(PQC_USER_ID,
                new MesPqcProductionReleaseRejectCommand()
                        .setApplicationId(APPLICATION_ID)
                        .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                        .setExpectedVersion(VERSION)
                        .setIdempotencyKey("pqc-reject-7001")
                        .setRejectReason("检验结论不通过"));

        assertEquals("REJECT", result.getDecision());
        assertEquals(MesReleaseFlowStatus.PQC_RELEASE_REJECTED, result.getStatus());
        assertTrue(result.getBatchRecordEvidenceIds().isEmpty());
        assertTrue(result.getProcessInspectionEvidenceIds().isEmpty());
        assertTrue(result.getLossReportEvidenceIds().isEmpty());
        assertTrue(result.getReportUploadTasks().isEmpty());
        verify(batchExecutionPort, never()).openOrCreate(any());
        verify(reportStageInitializer, never()).initializeRequiredReportStage(any());
    }

    @Test
    void pqcUserOutsideFrozenCandidateSnapshotIsForbiddenEvenWithCurrentRole() {
        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(7999L, approveCommand("pqc-approve-forbidden")));

        assertEquals(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionPort, never()).openOrCreate(any());
    }

    @Test
    void dynamicFormCannotReplaceFormalProductionReleaseDocuments() {
        when(dossierPort.readCompletionReceipt(99L, TENANT_ID)).thenThrow(blocker(
                MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                "formal process-inspection report binding is required"));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(PQC_USER_ID, approveCommand("pqc-approve-form-source")));

        assertEquals(MesReleaseFlowBlockerType.PROCESS_INSPECTION_SOURCE_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionPort, never()).openOrCreate(any());
    }

    @Test
    void legacyBatchExecutionWithoutReleaseApplicationAssociationCannotBeReused() {
        when(dossierPort.readCompletionReceipt(99L, TENANT_ID)).thenReturn(validFlow6Receipt(false));
        when(batchExecutionPort.openOrCreate(any())).thenThrow(blocker(
                MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED,
                "legacy batch execution lacks the release application association"));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(PQC_USER_ID, approveCommand("pqc-approve-legacy-batch")));

        assertEquals(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
    }

    @Test
    void sameIdempotencyKeyAndPayloadReturnsStoredDecisionReceipt() {
        MesPqcProductionReleaseApproveCommand command = approveCommand("pqc-approve-replay");
        String payloadHash = MesReleaseFlowIdempotency.payloadHash(
                "APPROVE", String.valueOf(APPLICATION_ID), String.valueOf(PQC_WORK_TASK_ID),
                String.valueOf(VERSION), null);
        MesPqcProductionReleaseDecisionResult stored = new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(APPLICATION_ID)
                .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                .setDecision("APPROVE")
                .setStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setBatchExecutionId(BATCH_EXECUTION_ID)
                .setBatchRecordEvidenceIds(List.of(101L))
                .setProcessInspectionEvidenceIds(List.of(201L))
                .setLossReportEvidenceIds(List.of(301L))
                .setReportUploadTasks(reportTasks())
                .setSourceSnapshotHash("source-hash")
                .setReportSnapshotHash("report-hash")
                .setVersion(2)
                .setDecidedBy(PQC_USER_ID)
                .setDecidedAt(LocalDateTime.of(2026, 8, 15, 12, 0))
                .setDecisionIdempotencyKey(command.getIdempotencyKey())
                .setDecisionPayloadHash(payloadHash);
        MesProcessPoolActiveOrderReleaseApplicationDO processed = application()
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(2)
                .setDossierSummaryJson(JSON.toJSONString(stored));
        when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(processed);

        MesPqcProductionReleaseDecisionResult replay = service.approve(PQC_USER_ID, command);

        assertEquals(BATCH_EXECUTION_ID, replay.getBatchExecutionId());
        assertEquals("pqc-approve-replay", replay.getDecisionIdempotencyKey());
        verify(batchExecutionPort, never()).openOrCreate(any());
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadIsRejected() {
        MesPqcProductionReleaseDecisionResult stored = new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(APPLICATION_ID)
                .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                .setDecision("APPROVE")
                .setStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setBatchRecordEvidenceIds(List.of(101L))
                .setProcessInspectionEvidenceIds(List.of(201L))
                .setLossReportEvidenceIds(List.of(301L))
                .setReportUploadTasks(reportTasks())
                .setDecisionIdempotencyKey("pqc-approve-conflict")
                .setDecisionPayloadHash("different-payload-hash");
        when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(application()
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(2)
                .setDossierSummaryJson(JSON.toJSONString(stored)));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(PQC_USER_ID, approveCommand("pqc-approve-conflict")));

        assertEquals(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionPort, never()).openOrCreate(any());
    }

    @Test
    void reportStageFailureStopsApplicationAndWorkTaskTransition() {
        when(dossierPort.readCompletionReceipt(99L, TENANT_ID)).thenReturn(validFlow6Receipt(false));
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(reportStageInitializer.initializeRequiredReportStage(any())).thenThrow(blocker(
                MesReleaseFlowBlockerType.REPORT_OWNER_REQUIRED, "one report owner is missing"));

        assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(PQC_USER_ID, approveCommand("pqc-approve-report-failure")));

        verify(applicationMapper, never()).approveFromPending(any(), any(), any(), any(), any(), any(), any());
        verify(workTaskMapper, never()).completePqcDecisionTask(any(), any(), any());
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void decisionMethodsUseOneRequiredRollbackTransaction() throws Exception {
        Transactional approve = MesPqcProductionReleaseServiceImpl.class
                .getMethod("approve", Long.class, MesPqcProductionReleaseApproveCommand.class)
                .getAnnotation(Transactional.class);
        Transactional reject = MesPqcProductionReleaseServiceImpl.class
                .getMethod("reject", Long.class, MesPqcProductionReleaseRejectCommand.class)
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRED, approve.propagation());
        assertEquals(Propagation.REQUIRED, reject.propagation());
        assertEquals(Exception.class, approve.rollbackFor()[0]);
        assertEquals(Exception.class, reject.rollbackFor()[0]);
    }

    private MesPqcProductionReleaseApproveCommand approveCommand(String key) {
        return new MesPqcProductionReleaseApproveCommand()
                .setApplicationId(APPLICATION_ID)
                .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                .setExpectedVersion(VERSION)
                .setIdempotencyKey(key)
                .setCompletionBackfillReceiptId("99")
                .setSourceCredentialType("CompletionBackfillReceipt")
                .setSourceCredentialId("99")
                .setSourceContextHash("source-context-7001")
                .setSourceSnapshotHash("source-hash")
                .setPayloadHash("payload-7001")
                .setSourceVersion("source-v1")
                .setSourceBundleHash("bundle-hash")
                .setPickListBindingId(601L)
                .setPickListId(602L)
                .setBatchPickListRelationId(603L)
                .setBindingVersion(1L)
                .setPickListHeaderSnapshotHash("pick-header-hash")
                .setPickListLineSnapshotHash("pick-line-hash")
                .setCompletionTransactionId("completion-tx-7001")
                .setExpectedActiveOrderVersion(1L)
                .setSourceEvidence(sourceEvidence());
    }

    private List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence> sourceEvidence() {
        return List.of(
                new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence()
                        .setSourceType("PRODUCTION").setSourceId("production-1").setSourceVersion("v1")
                        .setSourceSnapshotHash("production-hash").setPayloadHash("production-payload")
                        .setSignature("production-signature"),
                new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence()
                        .setSourceType("PQC").setSourceId("pqc-1").setSourceVersion("v1")
                        .setSourceSnapshotHash("pqc-hash").setPayloadHash("pqc-payload")
                        .setSignature("pqc-signature"),
                new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchExecutionSourceEvidence()
                        .setSourceType("LOSS").setSourceId("loss-1").setSourceVersion("v1")
                        .setSourceSnapshotHash("loss-hash").setPayloadHash("loss-payload")
                        .setSignature("loss-signature"));
    }

    private MesFlow6CompletionBackfillReceipt validFlow6Receipt(boolean actualLoss) {
        return new MesFlow6CompletionBackfillReceipt()
                .setReceiptId(99L)
                .setTenantId(TENANT_ID)
                .setActiveOrderId(2001L)
                .setWorkOrderId(3001L)
                .setBatchCode("BATCH-001")
                .setRouteId(4001L)
                .setRouteVersionId(4002L)
                .setRequestIdempotencyKey("pqc-approve-receipt")
                .setCreatedAt(LocalDateTime.of(2026, 8, 25, 12, 0))
                .setSourceSnapshotHash("source-hash")
                .setFormalSourceSnapshotJson("{\"formal\":true}")
                .setSignatureSnapshotJson("{\"signature\":true}")
                .setCompletionVersion(2)
                .setStatus(MesFlow6CompletionBackfillReceipt.STATUS_BACKFILL_SUCCEEDED)
                .setBatchRecordStatus("SUCCESS")
                .setProcessInspectionStatus("SUCCESS")
                .setBatchRecordId(101L)
                .setProcessInspectionId(201L)
                .setHasActualLoss(actualLoss)
                .setLossQuantity(actualLoss ? java.math.BigDecimal.ONE : java.math.BigDecimal.ZERO)
                .setLossReportStatus(actualLoss ? "SUCCESS" : "NOT_REQUIRED")
                .setLossRecordId(actualLoss ? 301L : null)
                .setZeroLossConfirmationSnapshot(actualLoss ? null : "{\"status\":\"NO_LOSS\"}")
                .setReceiptHash("receipt-hash");
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(APPLICATION_ID)
                .setActiveOrderId(2001L)
                .setWorkOrderId(3001L)
                .setWorkOrderCode("WO-001")
                .setRouteId(4001L)
                .setRouteVersionId(4002L)
                .setProductId(5001L)
                .setBatchCode("BATCH-001")
                .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setSourceSnapshotHash("source-hash")
                .setVersion(VERSION);
    }

    private MesProEdhrWorkTaskDO workTask() {
        return new MesProEdhrWorkTaskDO()
                .setId(PQC_WORK_TASK_ID)
                .setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(APPLICATION_ID)
                .setCandidateUserSnapshot("7101,7102")
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
    }

    private List<MesProductionReleaseReportUploadTaskReceipt> reportTasks() {
        return List.of(
                reportTask("INCOMING_INSPECTION_REPORT", 11L, 21L, List.of(7201L)),
                reportTask("STERILIZATION_REPORT", 12L, 22L, List.of(7202L)),
                reportTask("FINISHED_PRODUCT_INSPECTION_REPORT", 13L, 23L, List.of(7203L)),
                reportTask("FINISHED_PRODUCT_INSPECTION_RECORD", 14L, 24L, List.of(7203L)));
    }

    private MesProductionReleaseReportUploadTaskReceipt reportTask(
            String nodeType, Long batchTaskId, Long workTaskId, List<Long> candidateUserIds) {
        return new MesProductionReleaseReportUploadTaskReceipt()
                .setNodeType(nodeType)
                .setBatchTaskId(batchTaskId)
                .setWorkTaskId(workTaskId)
                .setCandidateUserIds(candidateUserIds)
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
    }

    private MesReleaseFlowBlockerException blocker(MesReleaseFlowBlockerType type, String reason) {
        return new MesReleaseFlowBlockerException(reason, new MesReleaseFlowFailureRespVO()
                .setStage("SP_2")
                .setCurrentStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                .setBlockers(List.of(new MesReleaseFlowBlocker()
                        .setBlockerType(type)
                        .setObjectType("RELEASE_APPLICATION")
                        .setObjectId(String.valueOf(APPLICATION_ID))
                        .setReason(reason))));
    }
}
