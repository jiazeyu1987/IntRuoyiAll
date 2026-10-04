package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderReworkCycleService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementRevokeReqDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_REWORK;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.DISPOSITION_VOID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNcrManagerDispositionTest {
    @Mock
    private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService unifiedAudit;


    @Test
    void missingReleaseTransactionIdCancelsLinkedManagerTask() {
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(2001L)
                        .setReleaseApprovalWorkTaskId(6001L);

        ReflectionTestUtils.invokeMethod(service, "closeManagerReleaseForNonconformance",
                application, "NONCONFORMANCE_REWORK", 900L, LocalDateTime.now());

        verify(workTaskService).cancelReleaseApprovalTaskById(6001L, "NONCONFORMANCE_REWORK");
    }

    @Test
    void terminalTransactionStillCancelsResidualManagerTask() {
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(2001L)
                        .setReleaseTransactionId(5001L);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(5001L)
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_REJECTED)
                .setVersion(2);
        when(releaseTransactionMapper.selectByIdForUpdate(5001L)).thenReturn(transaction);

        ReflectionTestUtils.invokeMethod(service, "closeManagerReleaseForNonconformance",
                application, "NONCONFORMANCE_VOID", 900L, LocalDateTime.now());

        verify(workTaskService).cancelReleaseApprovalTask(5001L, "NONCONFORMANCE_VOID");
    }

    @Mock
    private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private MesProEdhrOperationAuditService operationAuditService;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper signatureRecordMapper;
    @Mock
    private cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService signatureQueryService;
    @Mock
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesActiveOrderReworkCycleService reworkCycleService;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock
    private PermissionApi permissionApi;

    @InjectMocks
    private MesProEdhrNonconformanceReviewServiceImpl service;

    @Test
    void disposeReworkClosesManagerApprovalAndTransactionWithoutApprove() {
        assertManagerReleaseDisposition(DISPOSITION_REWORK, "NONCONFORMANCE_REWORK");
    }

    @Test
    void disposeVoidClosesManagerApprovalAndTransactionWithoutApprove() {
        assertManagerReleaseDisposition(DISPOSITION_VOID, "NONCONFORMANCE_VOID");
    }

    @Test
    void disposeRejectsForgedFileIdAndUrl() {
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(new MesProEdhrNonconformanceReviewDO()
                .setId(1001L).setReviewStatus(STATUS_PENDING_REVIEW));
        MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO material =
                new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO()
                        .setFileId(70002L)
                        .setUrl("s3://bucket/mes/edhr-ncr/reviews/1001/20260929/ncr.pdf")
                        .setFileName("ncr.pdf")
                        .setSortNo(1);
        var request = new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L).setDisposition(DISPOSITION_VOID)
                .setReviewOpinion("QA disposition").setSignaturePassword("qa-password")
                .setReviewMaterials(List.of(material));
        var failure = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.dispose(request));
        assertEquals(MesProEdhrBatchExecutionErrorCodeConstants
                .PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED.getCode(), failure.getCode());
        verify(fileMapper).selectById(70002L);
        verify(reviewMapper, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
        verify(signatureService, never()).recordQaDispositionSignature(any(), any(), any(), any(), any());
        verify(reworkCycleService, never()).start(any(), any(), any(), any());
    }

    private void assertManagerReleaseDisposition(String disposition, String decision) {
        long activeOrderId = 7001L;
        long workOrderId = 8001L;
        long applicationId = 2001L;
        long pqcTaskId = 3001L;
        long transactionId = 5001L;
        long managerTaskId = 6001L;

        MesProEdhrNonconformanceReviewDO review = new MesProEdhrNonconformanceReviewDO()
                .setId(1001L).setReviewCode("BHGSP-202609-00000001")
                .setSourceType("ACTIVE_ORDER").setSourceId(activeOrderId).setActiveOrderId(activeOrderId)
                .setWorkOrderId(workOrderId).setReviewStatus(STATUS_PENDING_REVIEW)
                .setNonconformanceReason("manager release is still open")
                .setPreviousWorkOrderTemporaryFrozen(false)
                .setFrozenAt(LocalDateTime.of(2026, 9, 24, 8, 30));
        MesProcessPoolActiveOrderReleaseApplicationDO application =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(applicationId).setActiveOrderId(activeOrderId).setWorkOrderId(workOrderId)
                        .setBatchExecutionId(4101L).setReleaseTransactionId(transactionId)
                        .setReleaseApprovalWorkTaskId(managerTaskId).setPqcReleaseWorkTaskId(pqcTaskId)
                        .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING).setVersion(3)
                        .setAppliedAt(LocalDateTime.of(2026, 9, 20, 8, 0));
        MesProcessPoolActiveOrderReleaseApplicationDO laterApplication =
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(applicationId + 1).setActiveOrderId(activeOrderId).setWorkOrderId(workOrderId)
                        .setBatchExecutionId(4102L).setReleaseTransactionId(transactionId + 1)
                        .setReleaseApprovalWorkTaskId(managerTaskId + 1).setPqcReleaseWorkTaskId(pqcTaskId + 1)
                        .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING).setVersion(1)
                        .setAppliedAt(LocalDateTime.of(2026, 9, 25, 8, 0));
        MesProEdhrWorkTaskDO pqcTask = new MesProEdhrWorkTaskDO()
                .setId(pqcTaskId).setTaskType("PQC_PRODUCTION_RELEASE")
                .setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(applicationId)
                .setBatchExecutionId(4101L).setStatus(MesProEdhrWorkTaskStatus.DONE);
        MesProEdhrWorkTaskDO managerTask = new MesProEdhrWorkTaskDO()
                .setId(managerTaskId).setTaskType("RELEASE_APPROVE")
                .setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(transactionId)
                .setBatchExecutionId(4101L).setStatus(MesProEdhrWorkTaskStatus.TODO);
        MesProEdhrReleaseTransactionDO transaction = new MesProEdhrReleaseTransactionDO()
                .setId(transactionId).setBatchExecutionId(4101L)
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL).setVersion(4);

        assertEquals(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING, application.getApplicationStatus());
        assertEquals(3, application.getVersion());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, managerTask.getStatus());
        assertEquals("RELEASE_APPROVE", managerTask.getTaskType());
        assertEquals("RELEASE_TRANSACTION", managerTask.getBusinessScopeType());
        assertEquals(transactionId, managerTask.getBusinessScopeId());
        assertEquals(transaction.getBatchExecutionId(), managerTask.getBatchExecutionId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, transaction.getReleaseStatus());
        assertEquals(4, transaction.getVersion());

        when(reviewMapper.selectByIdForUpdate(review.getId())).thenReturn(review);
        when(reviewMapper.selectById(review.getId())).thenReturn(review);
        when(reviewMapper.updateById(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            var written = invocation.getArgument(0, MesProEdhrNonconformanceReviewDO.class);
            review.setDisposition(written.getDisposition()).setReviewStatus(written.getReviewStatus())
                    .setActiveOrderId(written.getActiveOrderId()).setReviewMaterialUrl(written.getReviewMaterialUrl())
                    .setReviewMaterialFileId(written.getReviewMaterialFileId()).setReviewMaterialsJson(written.getReviewMaterialsJson())
                    .setReviewOpinion(written.getReviewOpinion()).setQaSignature(written.getQaSignature())
                    .setQaUserId(written.getQaUserId()).setClosedAt(written.getClosedAt())
                    .setUnfrozenAt(written.getUnfrozenAt()).setVoidedAt(written.getVoidedAt())
                    .setTraceSnapshotJson(written.getTraceSnapshotJson());
            return 1;
        });
        if (DISPOSITION_REWORK.equals(disposition)) {
            when(reviewMapper.selectFreezeLifecycleByWorkOrderId(workOrderId)).thenReturn(List.of());
        }
        var sourceOrder = new MesProcessPoolActiveOrderDO().setId(activeOrderId).setWorkOrderId(workOrderId);
        when(activeOrderMapper.selectByIdForUpdate(activeOrderId)).thenReturn(sourceOrder);
        when(workTaskMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(managerTask);
        when(workTaskMapper.selectByIdForUpdate(managerTaskId)).thenReturn(managerTask);
        when(releaseApplicationMapper.selectByIdForUpdate(applicationId)).thenReturn(application);
        if (DISPOSITION_REWORK.equals(disposition)) {
            long cycleId = 8001L;
            when(reworkCycleService.start(eq(activeOrderId), eq(workOrderId), eq(review.getId()), any())).thenReturn(cycleId);
            when(activeOrderMapper.selectByIdForUpdate(cycleId)).thenReturn(new MesProcessPoolActiveOrderDO()
                    .setId(cycleId).setWorkOrderId(workOrderId).setReworkSourceActiveOrderId(activeOrderId)
                    .setReworkReviewId(review.getId()));
            when(processSnapshotMapper.selectListByActiveOrderIdForUpdate(cycleId)).thenReturn(List.of(
                    new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO()
                            .setId(8101L).setActiveOrderId(cycleId).setWorkOrderId(workOrderId)));
            when(pqcInspectionTaskMapper.selectListByActiveOrderIdForUpdate(cycleId)).thenReturn(List.of(
                    new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO()
                            .setId(8201L).setActiveOrderId(cycleId).setWorkOrderId(workOrderId)));
        }
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(activeOrderId)))
                .thenReturn(List.of(application, laterApplication));
        when(releaseApplicationMapper.closeFromNonconformance(eq(applicationId), eq(3), eq(decision),
                eq(900L), any(), eq("QA disposition"), any())).thenAnswer(invocation -> {
            application.setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_REJECTED).setVersion(4)
                    .setPqcDecision(decision).setPqcDecidedBy(900L).setPqcDecidedAt(invocation.getArgument(4))
                    .setPqcRejectReason("QA disposition").setDossierSummaryJson(invocation.getArgument(6));
            return 1;
        });
        when(workTaskMapper.selectByIdForUpdate(pqcTaskId)).thenReturn(pqcTask);
        MesProWorkOrderDO workOrder = new MesProWorkOrderDO();
        workOrder.setId(workOrderId);
        workOrder.setTemporaryFrozen(true);
        when(workOrderMapper.selectByIdForUpdate(workOrderId)).thenReturn(workOrder);
        when(workOrderMapper.updateTemporaryFrozenByIds(anyList(), anyBoolean())).thenAnswer(invocation -> {
            workOrder.setTemporaryFrozen(invocation.getArgument(1));
            return 1;
        });
        when(signatureService.recordQaDispositionSignature(eq(900L), eq(review.getId()), eq("qa-password"),
                eq("QA disposition"), any())).thenAnswer(call -> {
                    String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                            0L, "QA_DISPOSITION", null, null, null, null, null, null, null,
                            "EDHR_NONCONFORMANCE_REVIEW", review.getId(), "eDHR不合格评审处置",
                            "QA_DISPOSITION", null, null, call.getArgument(4), null);
                    var snapshot = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                            new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(
                                    900L, "MES", "QA_DISPOSITION", "MES_BATCH_RECORD", subject,
                                    MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), "QA disposition"));
                    String hash = cn.hutool.crypto.digest.DigestUtil.sha256Hex(snapshot.canonicalContentJson());
                    var signature = cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.builder()
                            .id(9000L).actorId(900L).moduleCode("MES").actionCode("QA_DISPOSITION")
                            .subjectType("MES_BATCH_RECORD").subjectId(subject).subjectVersion(snapshot.subjectVersion())
                            .reason("QA disposition").canonicalContentJson(snapshot.canonicalContentJson())
                            .contentHash(hash).evidenceHash("manager-disposition-evidence").verificationStatus("VALID").build();
                    signature.setTenantId(122L);
                    when(signatureRecordMapper.selectById(9000L)).thenReturn(signature);
                    when(signatureQueryService.verifyEvidence(9000L)).thenReturn(
                            new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO(
                                    9000L, "VALID", hash, hash, "manager-disposition-evidence",
                                    "manager-disposition-evidence", "SHA-256", null));
                    return 9000L;
                });
        FileDO file = new FileDO();
        file.setId(70001L);
        file.setConfigId(1L);
        file.setPath("mes/edhr-ncr/reviews/1001/upload-1/20260929/ncr.pdf");
        file.setName("ncr.pdf");
        file.setUrl("/admin-api/infra/file/1/get/ncr.pdf");
        when(fileMapper.selectById(70001L)).thenReturn(file);
        when(releaseTransactionMapper.selectByIdForUpdate(transactionId)).thenReturn(transaction);
        when(releaseTransactionMapper.updateById(any(MesProEdhrReleaseTransactionDO.class))).thenAnswer(invocation -> {
            var written = invocation.getArgument(0, MesProEdhrReleaseTransactionDO.class);
            transaction.setReleaseStatus(written.getReleaseStatus()).setRejectedBy(written.getRejectedBy())
                    .setRejectedAt(written.getRejectedAt()).setRejectReason(written.getRejectReason())
                    .setVersion(written.getVersion());
            return 1;
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            managerTask.setStatus(MesProEdhrWorkTaskStatus.CANCELED).setReason(decision);
            return null;
        }).when(workTaskService).cancelReleaseApprovalTask(transactionId, decision);

        MesProEdhrNonconformanceReviewDisposeReqVO req = new MesProEdhrNonconformanceReviewDisposeReqVO();
        req.setId(review.getId());
        req.setDisposition(disposition);
        req.setReviewOpinion("QA disposition");
        req.setSignaturePassword("qa-password");
        MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO material =
                new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO();
        material.setFileId(file.getId());
        material.setUrl("/admin-api/infra/file/1/get/ncr.pdf");
        material.setFileName("ncr.pdf");
        material.setSortNo(1);
        req.setReviewMaterials(List.of(material));

        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(900L);
            login.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("QA");
            service.dispose(req);
        } finally {
            TenantContextHolder.clear();
        }

        verify(releaseApplicationMapper).closeFromNonconformance(eq(applicationId), eq(3), eq(decision),
                eq(900L), any(), eq("QA disposition"), any());
        if (DISPOSITION_REWORK.equals(disposition)) {
            verify(reworkCycleService).start(eq(activeOrderId), eq(workOrderId), eq(review.getId()), any());
        }
        verify(workTaskService).cancelReleaseApprovalTask(eq(transactionId), eq(decision));
        verify(workTaskMapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
        verify(releaseTransactionMapper, org.mockito.Mockito.times(4)).selectByIdForUpdate(transactionId);
        verify(workTaskMapper, never()).completePqcDecisionTask(any(), any(), any());

        ArgumentCaptor<MesProEdhrReleaseTransactionDO> transactionUpdate =
                ArgumentCaptor.forClass(MesProEdhrReleaseTransactionDO.class);
        verify(releaseTransactionMapper, org.mockito.Mockito.atLeastOnce()).updateById(transactionUpdate.capture());
        MesProEdhrReleaseTransactionDO rejectedTransaction = transactionUpdate.getAllValues().stream()
                .filter(update -> MesProEdhrReleaseServiceImpl.STATUS_REJECTED.equals(update.getReleaseStatus()))
                .findFirst().orElseThrow();
        assertEquals(transactionId, rejectedTransaction.getId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_REJECTED, rejectedTransaction.getReleaseStatus());
        assertEquals(decision, rejectedTransaction.getRejectReason());
        assertEquals(900L, rejectedTransaction.getRejectedBy());
        assertEquals(5, rejectedTransaction.getVersion());

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewUpdate =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(reviewUpdate.capture());
        assertEquals("电子签名#9000", reviewUpdate.getValue().getQaSignature());
        assertEquals(disposition, reviewUpdate.getValue().getDisposition());
        assertEquals(MesProEdhrNonconformanceReviewService.STATUS_CLOSED, review.getReviewStatus());
        assertEquals(MesReleaseFlowStatus.PQC_RELEASE_REJECTED, application.getApplicationStatus());
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED, managerTask.getStatus());
        var unifiedCommand = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(unifiedAudit).append(unifiedCommand.capture());
        var affectedAfter = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                unifiedCommand.getValue().getAfterState().getCanonicalJson()).get("affectedState");
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED,
                affectedAfter.get("managerReleaseTask").get("status").asText());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_REJECTED,
                affectedAfter.get("managerReleaseTransaction").get("releaseStatus").asText());

        ArgumentCaptor<MesProEdhrOperationAuditCommand> audit =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(audit.capture());
        MesProEdhrOperationAuditCommand auditCommand = audit.getValue();
        assertEquals("NONCONFORMANCE_REVIEW_DISPOSE", auditCommand.getOperationType());
        assertEquals("NONCONFORMANCE_REVIEW", auditCommand.getObjectType());
        assertEquals(String.valueOf(review.getId()), auditCommand.getObjectId());
        assertEquals(900L, auditCommand.getActorUserId());
        assertEquals("QA", auditCommand.getActorUsername());
        assertEquals("ALLOW", auditCommand.getPermissionDecision());
        assertEquals("SUCCESS", auditCommand.getResultStatus());
        assertTrue(auditCommand.getMetadataJson().contains("\"activeOrderId\":7001"));
        assertTrue(auditCommand.getMetadataJson().contains("\"reviewId\":1001"));
        assertTrue(auditCommand.getMetadataJson().contains("\"qaSignature\":\"电子签名#9000\""));
        assertTrue(auditCommand.getMetadataJson().contains("\"qaUserId\":900"));
        assertTrue(auditCommand.getMetadataJson().contains("\"signatureId\":9000"));
    }

    @Test
    void cancelReleaseApprovalTaskById_cancelsTaskAndRevokesRuntimeEntitlement() {
        long transactionId = 5001L;
        long managerTaskId = 6001L;
        MesProEdhrWorkTaskDO managerTask = new MesProEdhrWorkTaskDO()
                .setId(managerTaskId)
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE)
                .setBusinessScopeType("RELEASE_TRANSACTION")
                .setBusinessScopeId(transactionId)
                .setCandidateUserSnapshot("188,189")
                .setStatus(MesProEdhrWorkTaskStatus.TODO);
        when(workTaskMapper.selectById(managerTaskId)).thenReturn(managerTask);
        when(workTaskMapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);

        MesProEdhrWorkTaskServiceImpl realWorkTaskService = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(realWorkTaskService, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(realWorkTaskService, "permissionApi", permissionApi);
        var auxiliaryAudit = org.mockito.Mockito.mock(MesWorkTaskAuxiliaryAudit.class);
        ReflectionTestUtils.setField(realWorkTaskService, "auxiliaryAudit", auxiliaryAudit);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(3, Runnable.class).run();
            return null;
        }).when(auxiliaryAudit).entitlements(eq(managerTask), any(), any(), any());


        TenantContextHolder.setTenantId(122L);
        try (MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(900L);
            login.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("QA");
            realWorkTaskService.cancelReleaseApprovalTaskById(managerTaskId, "NONCONFORMANCE_REWORK");
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<MesProEdhrWorkTaskDO> taskUpdate =
                ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(workTaskMapper).updateById(taskUpdate.capture());
        assertEquals(managerTaskId, taskUpdate.getValue().getId());
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED, taskUpdate.getValue().getStatus());
        assertEquals("NONCONFORMANCE_REWORK", taskUpdate.getValue().getReason());
        assertEquals("NONCONFORMANCE_REWORK", taskUpdate.getValue().getRemark());
        assertNotNull(taskUpdate.getValue().getCompletedAt());

        ArgumentCaptor<SystemEntitlementRevokeReqDTO> revoke =
                ArgumentCaptor.forClass(SystemEntitlementRevokeReqDTO.class);
        verify(permissionApi).revokeEntitlementSource(revoke.capture());
        assertEquals(122L, revoke.getValue().getTenantId());
        assertEquals("EDHR_WORK_TASK_ASSIGNEE", revoke.getValue().getSourceType());
        assertEquals("WORK_TASK|6001", revoke.getValue().getSourceKey());
        assertEquals("MES_EDHR_RELEASE_APPROVER_MINIMAL", revoke.getValue().getPolicyCode());
        assertEquals(900L, revoke.getValue().getOperatorUserId());
        assertEquals("QA", revoke.getValue().getOperatorUsername());
    }
}
