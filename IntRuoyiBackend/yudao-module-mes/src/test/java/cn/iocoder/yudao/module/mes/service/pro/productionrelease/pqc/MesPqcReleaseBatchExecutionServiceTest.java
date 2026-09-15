package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlocker;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowFailureRespVO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
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
import static org.mockito.ArgumentMatchers.isNull;
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
    @Mock private MesPqcReleaseDossierPort dossierPort;
    @Mock private MesProductionReleaseBatchExecutionPort batchExecutionPort;
    @Mock private MesProductionReleaseReportStageInitializer reportStageInitializer;
    @Mock private MesReleaseFlowAuditRecorder auditRecorder;
    @Mock private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock private MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    @Mock private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;

    private MesPqcProductionReleaseService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        service = new MesPqcProductionReleaseServiceImpl(
                applicationMapper, workTaskMapper, dossierPort,
                batchExecutionPort, reportStageInitializer, auditRecorder, signatureService,
                nonconformanceReviewService, nonconformanceReviewMapper,
                Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC));
        lenient().when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(application());
        lenient().when(workTaskMapper.selectById(PQC_WORK_TASK_ID)).thenReturn(workTask());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void dynamicLossPassesTheRealDossierPortAndPqcDecisionAndReplaysTypedEvidence() {
        var batchWriter = org.mockito.Mockito.mock(MesTeamLeaderActiveOrderReleaseBatchRecordWriter.class);
        var inspectionWriter = org.mockito.Mockito.mock(MesTeamLeaderActiveOrderReleaseProcessInspectionWriter.class);
        var lossWriter = org.mockito.Mockito.mock(MesTeamLeaderActiveOrderReleaseLossReportWriter.class);
        var realPort = org.mockito.Mockito.spy(new MesPqcReleaseDossierPortImpl(
                null, null, null, null, null, null, null, null, null, null, batchWriter, inspectionWriter, lossWriter, null));
        var plan = new MesPqcReleaseDossierPlan().setSourceSnapshotHash("source-hash")
                .setBatchRecordPlan(new MesTeamLeaderActiveOrderReleaseBatchRecordPlan().setSourceObjectIds(List.of(1L)).setSourceValueHashes(List.of("batch")))
                .setProcessInspectionPlan(new MesTeamLeaderActiveOrderReleaseProcessInspectionPlan().setSourceObjectIds(List.of(2L)).setSourceValueHashes(List.of("inspection")))
                .setLossReportPlan(new MesTeamLeaderActiveOrderReleaseLossReportPlan().setSourceObjectIds(List.of(3L)).setSourceValueHashes(List.of("loss")));
        org.mockito.Mockito.doReturn(plan).when(realPort).plan(any(), eq(PQC_USER_ID));
        when(batchWriter.write(plan.getBatchRecordPlan(), BATCH_EXECUTION_ID)).thenReturn(
                new MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult().setDocumentType("BATCH_RECORD")
                        .setBatchRecordExecutionIds(List.of(101L)).setSourceObjectIds(List.of(1L)).setSourceValueHashes(List.of("batch")).setBlockers(List.of()));
        when(inspectionWriter.write(plan.getProcessInspectionPlan(), BATCH_EXECUTION_ID)).thenReturn(
                new MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult().setDocumentType("PROCESS_INSPECTION")
                        .setBatchRecordExecutionIds(List.of(201L)).setFormCenterInstanceIds(List.of())
                        .setFieldAuditIds(List.of(202L)).setFieldAuditHeadHashes(List.of("inspection-audit"))
                        .setSourceObjectIds(List.of(2L)).setSourceValueHashes(List.of("inspection")).setBlockers(List.of()));
        when(lossWriter.write(plan.getLossReportPlan(), BATCH_EXECUTION_ID)).thenReturn(
                new MesTeamLeaderActiveOrderReleaseLossReportWriteResult().setDocumentType("LOSS_REPORT")
                        .setBatchRecordExecutionIds(List.of()).setFormCenterInstanceIds(List.of(301L))
                        .setFieldAuditIds(List.of(302L)).setFieldAuditHeadHashes(List.of("loss-audit"))
                        .setHasActualLoss(true).setLossReportStatus("SUCCESS").setLossQuantity(java.math.BigDecimal.ONE)
                        .setSourceSnapshotHash("loss-source").setSourceObjectIds(List.of(3L)).setSourceValueHashes(List.of("loss")).setBlockers(List.of()));
        service = new MesPqcProductionReleaseServiceImpl(applicationMapper, workTaskMapper, realPort,
                batchExecutionPort, reportStageInitializer, auditRecorder, signatureService,
                nonconformanceReviewService, nonconformanceReviewMapper,
                Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC));
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(reportStageInitializer.initializeRequiredReportStage(any())).thenReturn(
                new MesProductionReleaseReportStageInitializationResult().setReportUploadTasks(reportTasks()).setReportSnapshotHash("report-hash"));
        when(applicationMapper.approveFromPending(eq(APPLICATION_ID), eq(VERSION), eq(BATCH_EXECUTION_ID),
                eq(PQC_USER_ID), any(), eq("report-hash"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("APPROVE"))).thenReturn(1);
        when(signatureService.recordPqcReleaseSignature(eq(PQC_USER_ID), eq(BATCH_EXECUTION_ID),
                eq("signature-password"), eq("正式来源核对通过"))).thenReturn(9901L);
        var command = approveCommand("dynamic-loss-flow").setApprovalOpinion("正式来源核对通过");
        var result = service.approve(PQC_USER_ID, command);
        assertEquals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, result.getStatus());
        assertTrue(result.getLossReportEvidenceIds().isEmpty());
        assertEquals(List.of(301L), result.getLossReportFormCenterInstanceIds());
        assertEquals(List.of(302L), result.getLossReportFieldAuditIds());
        assertEquals(4, result.getReportUploadTasks().size());
        var persisted = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(applicationMapper).approveFromPending(eq(APPLICATION_ID), eq(VERSION), eq(BATCH_EXECUTION_ID),
                eq(PQC_USER_ID), any(), eq("report-hash"), persisted.capture());
        when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(application()
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING).setVersion(VERSION + 1)
                .setDossierSummaryJson(persisted.getValue()));
        var replay = service.approve(PQC_USER_ID, command);
        assertEquals(result.getLossReportFormCenterInstanceIds(), replay.getLossReportFormCenterInstanceIds());
        assertEquals(result.getLossReportFieldAuditHeadHashes(), replay.getLossReportFieldAuditHeadHashes());
        verify(lossWriter).write(plan.getLossReportPlan(), BATCH_EXECUTION_ID);
    }

    @Test
    void pqcApproveCreatesBatchExecutionOnlyAfterPqcRelease() {
        MesPqcReleaseDossierPlan dossierPlan = new MesPqcReleaseDossierPlan()
                .setSourceSnapshotHash("source-hash");
        MesPqcReleaseDossierWriteResult dossierWrite = new MesPqcReleaseDossierWriteResult()
                .setBatchRecordEvidenceIds(List.of(101L))
                .setProcessInspectionEvidenceIds(List.of(201L))
                .setLossReportEvidenceIds(List.of())
                .setLossReportStatus("NOT_REQUIRED")
                .setHasActualLoss(false)
                .setLossQuantity(java.math.BigDecimal.ZERO);
        List<MesProductionReleaseReportUploadTaskReceipt> reportTasks = reportTasks();
        when(dossierPort.plan(any(), eq(PQC_USER_ID))).thenReturn(dossierPlan);
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(dossierPort.write(dossierPlan, BATCH_EXECUTION_ID)).thenReturn(dossierWrite);
        when(reportStageInitializer.initializeRequiredReportStage(any()))
                .thenReturn(new MesProductionReleaseReportStageInitializationResult()
                        .setReportUploadTasks(reportTasks)
                        .setReportSnapshotHash("report-hash"));
        when(applicationMapper.approveFromPending(eq(APPLICATION_ID), eq(VERSION), eq(BATCH_EXECUTION_ID),
                eq(PQC_USER_ID), any(), eq("report-hash"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("APPROVE"))).thenReturn(1);
        when(signatureService.recordPqcReleaseSignature(
                eq(PQC_USER_ID), eq(BATCH_EXECUTION_ID), eq("signature-password"), eq("正式来源核对通过")))
                .thenReturn(9901L);

        MesPqcProductionReleaseDecisionResult result = service.approve(PQC_USER_ID,
                new MesPqcProductionReleaseApproveCommand()
                        .setApplicationId(APPLICATION_ID)
                        .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                        .setExpectedVersion(VERSION)
                        .setIdempotencyKey("pqc-approve-7001")
                        .setSignaturePassword("signature-password")
                        .setApprovalOpinion("正式来源核对通过"));

        assertEquals("APPROVE", result.getDecision());
        assertEquals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, result.getStatus());
        assertEquals(BATCH_EXECUTION_ID, result.getBatchExecutionId());
        assertEquals(9901L, result.getSignatureId());
        assertEquals(List.of(101L), result.getBatchRecordEvidenceIds());
        assertEquals(List.of(201L), result.getProcessInspectionEvidenceIds());
        assertEquals(List.of(), result.getProcessInspectionFormCenterInstanceIds());
        assertEquals(List.of(), result.getLossReportEvidenceIds());
        assertEquals(4, result.getReportUploadTasks().size());
        assertTrue(result.getReportUploadTasks().stream().allMatch(
                item -> MesProEdhrWorkTaskStatus.TODO.equals(item.getStatus())));
        verify(batchExecutionPort).openOrCreate(argThat(item ->
                "ACTIVE_ORDER_PQC".equals(item.getEntryType())
                        && "7001".equals(item.getEntryBusinessId())
                        && Long.valueOf(2001L).equals(item.getActiveOrderId())
                        && item.getSourceCredentialId() == null
                        && item.getSourceSnapshotHash() == null));
        verify(dossierPort).write(dossierPlan, BATCH_EXECUTION_ID);
        verify(reportStageInitializer).initializeRequiredReportStage(any());
        verify(nonconformanceReviewService).ensureWorkOrderNotFrozen(3001L, "PQC放行");
        verify(signatureService).validatePqcSubmitSignature(PQC_USER_ID, "signature-password");
    }

    @Test
    void pqcApproveAcceptsDynamicFormCenterProcessInspectionEvidenceReceipt() {
        MesPqcReleaseDossierPlan dossierPlan = new MesPqcReleaseDossierPlan()
                .setSourceSnapshotHash("source-hash");
        MesPqcReleaseDossierWriteResult dossierWrite = new MesPqcReleaseDossierWriteResult()
                .setBatchRecordEvidenceIds(List.of(101L))
                .setProcessInspectionEvidenceIds(List.of())
                .setProcessInspectionFormCenterInstanceIds(List.of(202L))
                .setLossReportEvidenceIds(List.of())
                .setLossReportStatus("NOT_REQUIRED")
                .setHasActualLoss(false)
                .setLossQuantity(java.math.BigDecimal.ZERO);
        when(dossierPort.plan(any(), eq(PQC_USER_ID))).thenReturn(dossierPlan);
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(dossierPort.write(dossierPlan, BATCH_EXECUTION_ID)).thenReturn(dossierWrite);
        when(reportStageInitializer.initializeRequiredReportStage(any()))
                .thenReturn(new MesProductionReleaseReportStageInitializationResult()
                        .setReportUploadTasks(reportTasks())
                        .setReportSnapshotHash("report-hash"));
        when(applicationMapper.approveFromPending(eq(APPLICATION_ID), eq(VERSION), eq(BATCH_EXECUTION_ID),
                eq(PQC_USER_ID), any(), eq("report-hash"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("APPROVE"))).thenReturn(1);
        when(signatureService.recordPqcReleaseSignature(
                eq(PQC_USER_ID), eq(BATCH_EXECUTION_ID), eq("signature-password"), eq("正式来源核对通过")))
                .thenReturn(9901L);

        MesPqcProductionReleaseDecisionResult result = service.approve(PQC_USER_ID,
                approveCommand("pqc-approve-dynamic-process-inspection")
                        .setApprovalOpinion("正式来源核对通过"));

        assertEquals(List.of(), result.getProcessInspectionEvidenceIds());
        assertEquals(List.of(202L), result.getProcessInspectionFormCenterInstanceIds());
        assertEquals(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING, result.getStatus());
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
        assertTrue(result.getProcessInspectionFormCenterInstanceIds().isEmpty());
        assertTrue(result.getLossReportEvidenceIds().isEmpty());
        assertTrue(result.getReportUploadTasks().isEmpty());
        verify(batchExecutionPort, never()).openOrCreate(any());
        verify(dossierPort, never()).plan(any(), any());
        verify(dossierPort, never()).write(any(), any());
        verify(reportStageInitializer, never()).initializeRequiredReportStage(any());
    }

    @Test
    void pqcApproveRequiresElectronicSignatureBeforeDownstreamWork() {
        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(PQC_USER_ID, approveCommand("pqc-approve-no-signature")
                        .setSignaturePassword("")));

        assertEquals(MesReleaseFlowBlockerType.UNSUPPORTED_RELEASE_ACTION,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionPort, never()).openOrCreate(any());
        verify(dossierPort, never()).plan(any(), any());
    }

    @Test
    void pqcReleasePageSeparatesFiveBusinessViews() {
        List<MesProcessPoolActiveOrderReleaseApplicationDO> applications = List.of(
                application().setId(7001L).setPqcReleaseWorkTaskId(8001L),
                application().setId(7002L).setPqcReleaseWorkTaskId(8002L)
                        .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                        .setBatchExecutionId(9002L),
                application().setId(7003L).setPqcReleaseWorkTaskId(8003L),
                application().setId(7004L).setPqcReleaseWorkTaskId(8004L),
                application().setId(7005L).setPqcReleaseWorkTaskId(8005L)
                        .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                        .setBatchExecutionId(9005L));
        stubPage("PENDING", List.of(applications.get(0)));
        stubPage("RELEASED", List.of(applications.get(1)));
        stubPage("VOIDED", List.of(applications.get(2)));
        stubPage("REWORKED", List.of(applications.get(3)));
        stubPage("CONCESSION_RELEASED", List.of(applications.get(4)));
        when(nonconformanceReviewMapper.selectLatestBySourceIds(eq("PQC_RELEASE"), any(), eq(TENANT_ID))).thenReturn(List.of(
                closedReview(103L, 7003L, "void"),
                closedReview(104L, 7004L, "rework"),
                closedReview(105L, 7005L, "concession_release")));

        assertEquals(1L, page("PENDING").getTotal());
        assertEquals(7001L, page("PENDING").getList().get(0).getApplicationId());
        assertEquals(7002L, page("RELEASED").getList().get(0).getApplicationId());
        assertEquals(7003L, page("VOIDED").getList().get(0).getApplicationId());
        assertEquals(7004L, page("REWORKED").getList().get(0).getApplicationId());
        assertEquals(7005L, page("CONCESSION_RELEASED").getList().get(0).getApplicationId());
    }

    @Test
    void pqcReleasePageSkipsLegacyApplicationsWithoutPqcWorkTask() {
        stubPage("PENDING", List.of());

        assertEquals(0L, page("PENDING").getTotal());
    }

    @Test
    void pqcReleasePageShowsFrozenCandidateTasksAfterRoleMembershipChanges() {
        stubPage("PENDING", List.of(application().setId(APPLICATION_ID).setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)));
        when(nonconformanceReviewMapper.selectLatestBySourceIds(eq("PQC_RELEASE"), any(), eq(TENANT_ID))).thenReturn(List.of());

        cn.iocoder.yudao.framework.common.pojo.PageResult<MesPqcProductionReleasePageItem> result =
                service.getPqcReleasePage(PQC_USER_ID, new MesPqcProductionReleasePageQuery()
                        .setPageNo(1).setPageSize(10).setViewStatus("PENDING"));

        assertEquals(1L, result.getTotal());
        assertEquals(APPLICATION_ID, result.getList().get(0).getApplicationId());
    }

    @Test
    void pendingPageProjectsDossierBlockerOnlyForCurrentPage() {
        MesProcessPoolActiveOrderReleaseApplicationDO first = application()
                .setId(7001L).setPqcReleaseWorkTaskId(8001L);
        MesProcessPoolActiveOrderReleaseApplicationDO second = application()
                .setId(7002L).setPqcReleaseWorkTaskId(8002L);
        stubPage("PENDING", List.of(first, second));

        cn.iocoder.yudao.framework.common.pojo.PageResult<MesPqcProductionReleasePageItem> page =
                service.getPqcReleasePage(PQC_USER_ID, new MesPqcProductionReleasePageQuery()
                        .setPageNo(1).setPageSize(1).setViewStatus("PENDING"));

        assertEquals(2L, page.getTotal());
        assertEquals(null, page.getList().get(0).getApprovalReady());
        assertEquals(null, page.getList().get(0).getApprovalBlockerReason());
        verify(dossierPort, never()).readiness(any(), any());
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
    void frozenPqcCandidateCanRejectAfterRoleMembershipChanges() {
        when(applicationMapper.rejectFromPending(eq(APPLICATION_ID), eq(VERSION), eq(PQC_USER_ID),
                any(), eq("角色调整后按冻结候选处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(PQC_WORK_TASK_ID), any(), eq("REJECT"))).thenReturn(1);

        MesPqcProductionReleaseDecisionResult result = service.reject(PQC_USER_ID,
                new MesPqcProductionReleaseRejectCommand()
                        .setApplicationId(APPLICATION_ID)
                        .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                        .setExpectedVersion(VERSION)
                        .setIdempotencyKey("pqc-reject-after-role-change")
                        .setRejectReason("角色调整后按冻结候选处理"));

        assertEquals("REJECT", result.getDecision());
        assertEquals(MesReleaseFlowStatus.PQC_RELEASE_REJECTED, result.getStatus());
    }

    @Test
    void dynamicFormCannotReplaceFormalProductionReleaseDocuments() {
        when(dossierPort.plan(any(), eq(PQC_USER_ID))).thenThrow(blocker(
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
        when(dossierPort.plan(any(), eq(PQC_USER_ID))).thenReturn(
                new MesPqcReleaseDossierPlan().setSourceSnapshotHash("source-hash"));
        when(batchExecutionPort.openOrCreate(any())).thenThrow(blocker(
                MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED,
                "legacy batch execution lacks the release application association"));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(PQC_USER_ID, approveCommand("pqc-approve-legacy-batch")));

        assertEquals(MesReleaseFlowBlockerType.LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(dossierPort, never()).write(any(), any());
    }

    @Test
    void sameIdempotencyKeyAndPayloadReturnsStoredDecisionReceipt() {
        MesPqcProductionReleaseApproveCommand command = approveCommand("pqc-approve-replay");
        String payloadHash = MesReleaseFlowIdempotency.payloadHash(
                "APPROVE", String.valueOf(APPLICATION_ID), String.valueOf(PQC_WORK_TASK_ID),
                String.valueOf(VERSION), String.valueOf(PQC_USER_ID), null);
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
    void sameIdempotencyKeyAndPayloadRequiresFrozenCandidateForStoredDecisionReplay() {
        MesPqcProductionReleaseApproveCommand command = approveCommand("pqc-approve-replay-forbidden");
        String payloadHash = MesReleaseFlowIdempotency.payloadHash(
                "APPROVE", String.valueOf(APPLICATION_ID), String.valueOf(PQC_WORK_TASK_ID),
                String.valueOf(VERSION), String.valueOf(PQC_USER_ID), null);
        MesPqcProductionReleaseDecisionResult stored = new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(APPLICATION_ID)
                .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                .setDecision("APPROVE")
                .setStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setDecisionIdempotencyKey(command.getIdempotencyKey())
                .setDecisionPayloadHash(payloadHash);
        when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(application()
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(2)
                .setDossierSummaryJson(JSON.toJSONString(stored)));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(7999L, command));

        assertEquals(MesReleaseFlowBlockerType.WORK_TASK_NOT_PROCESSABLE,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(batchExecutionPort, never()).openOrCreate(any());
        verify(auditRecorder, never()).record(any());
    }

    @Test
    void sameIdempotencyKeyCannotReplayStoredDecisionForAnotherFrozenCandidate() {
        MesPqcProductionReleaseApproveCommand command = approveCommand("pqc-approve-replay-actor-bound");
        String payloadHash = MesReleaseFlowIdempotency.payloadHash(
                "APPROVE", String.valueOf(APPLICATION_ID), String.valueOf(PQC_WORK_TASK_ID),
                String.valueOf(VERSION), String.valueOf(PQC_USER_ID), null);
        MesPqcProductionReleaseDecisionResult stored = new MesPqcProductionReleaseDecisionResult()
                .setApplicationId(APPLICATION_ID)
                .setPqcReleaseWorkTaskId(PQC_WORK_TASK_ID)
                .setDecision("APPROVE")
                .setStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setDecisionIdempotencyKey(command.getIdempotencyKey())
                .setDecisionPayloadHash(payloadHash);
        when(applicationMapper.selectByIdForUpdate(APPLICATION_ID)).thenReturn(application()
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(2)
                .setDossierSummaryJson(JSON.toJSONString(stored)));

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> service.approve(7102L, command));

        assertEquals(MesReleaseFlowBlockerType.IDEMPOTENCY_PAYLOAD_CONFLICT,
                failure.getFailure().getBlockers().get(0).getBlockerType());
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
        MesPqcReleaseDossierPlan dossierPlan = new MesPqcReleaseDossierPlan()
                .setSourceSnapshotHash("source-hash");
        when(dossierPort.plan(any(), eq(PQC_USER_ID))).thenReturn(dossierPlan);
        when(batchExecutionPort.openOrCreate(any())).thenReturn(BATCH_EXECUTION_ID);
        when(dossierPort.write(dossierPlan, BATCH_EXECUTION_ID)).thenReturn(
                new MesPqcReleaseDossierWriteResult()
                        .setBatchRecordEvidenceIds(List.of(101L))
                        .setProcessInspectionEvidenceIds(List.of(201L))
                        .setLossReportEvidenceIds(List.of())
                        .setLossReportStatus("NOT_REQUIRED")
                        .setHasActualLoss(false)
                        .setLossQuantity(java.math.BigDecimal.ZERO));
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
                .setSignaturePassword("signature-password")
                .setIdempotencyKey(key);
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
        return workTask(PQC_WORK_TASK_ID, APPLICATION_ID);
    }

    private MesProEdhrWorkTaskDO workTask(Long taskId, Long applicationId) {
        return new MesProEdhrWorkTaskDO()
                .setId(taskId)
                .setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(applicationId)
                .setCandidateUserSnapshot("7101,7102")
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
    }

    private MesProEdhrNonconformanceReviewDO closedReview(Long id, Long applicationId, String disposition) {
        return new MesProEdhrNonconformanceReviewDO()
                .setId(id)
                .setSourceType("PQC_RELEASE")
                .setSourceId(applicationId)
                .setReviewStatus("closed")
                .setDisposition(disposition)
                .setNonconformanceReason("评审原因");
    }

    private void stubPage(String viewStatus,
                           List<MesProcessPoolActiveOrderReleaseApplicationDO> applications) {
        when(applicationMapper.selectPqcReleasePage(
                any(PageParam.class), eq(TENANT_ID), eq(PQC_USER_ID), eq(viewStatus),
                isNull(String.class), isNull(String.class)))
                .thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(
                        applications, (long) applications.size()));
    }

    private cn.iocoder.yudao.framework.common.pojo.PageResult<MesPqcProductionReleasePageItem> page(
            String viewStatus) {
        return service.getPqcReleasePage(PQC_USER_ID, new MesPqcProductionReleasePageQuery()
                .setPageNo(1)
                .setPageSize(10)
                .setViewStatus(viewStatus));
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
