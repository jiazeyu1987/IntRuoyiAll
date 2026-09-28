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
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesActiveOrderReworkCycleService reworkCycleService;
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
        if (DISPOSITION_REWORK.equals(disposition)) {
            when(reviewMapper.selectFreezeLifecycleByWorkOrderId(workOrderId)).thenReturn(List.of());
            when(activeOrderMapper.selectByIdForUpdate(activeOrderId)).thenReturn(new MesProcessPoolActiveOrderDO()
                    .setId(activeOrderId).setWorkOrderId(workOrderId));
        }
        when(releaseApplicationMapper.selectListByActiveOrderIdsForUpdate(List.of(activeOrderId)))
                .thenReturn(List.of(application, laterApplication));
        when(releaseApplicationMapper.closeFromNonconformance(eq(applicationId), eq(3), eq(decision),
                eq(900L), any(), eq("QA disposition"), any())).thenReturn(1);
        when(workTaskMapper.selectByIdForUpdate(pqcTaskId)).thenReturn(pqcTask);
        MesProWorkOrderDO workOrder = new MesProWorkOrderDO();
        workOrder.setId(workOrderId);
        workOrder.setTemporaryFrozen(true);
        when(workOrderMapper.selectByIdForUpdate(workOrderId)).thenReturn(workOrder);
        when(workOrderMapper.updateTemporaryFrozenByIds(anyList(), anyBoolean())).thenReturn(1);
        when(signatureService.recordQaDispositionSignature(eq(900L), eq(review.getId()), eq("qa-password"),
                eq("QA disposition"), any())).thenReturn(9000L);
        FileDO file = new FileDO();
        file.setId(70001L);
        file.setConfigId(1L);
        file.setPath("ncr.pdf");
        file.setName("ncr.pdf");
        when(fileMapper.selectList(any())).thenReturn(List.of(file));
        when(releaseTransactionMapper.selectByIdForUpdate(transactionId)).thenReturn(transaction);
        when(releaseTransactionMapper.updateById(any(MesProEdhrReleaseTransactionDO.class))).thenReturn(1);

        MesProEdhrNonconformanceReviewDisposeReqVO req = new MesProEdhrNonconformanceReviewDisposeReqVO();
        req.setId(review.getId());
        req.setDisposition(disposition);
        req.setReviewOpinion("QA disposition");
        req.setSignaturePassword("qa-password");
        MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO material =
                new MesProEdhrNonconformanceReviewDisposeReqVO.ReviewMaterialReqVO();
        material.setUrl("/admin-api/infra/file/1/get/ncr.pdf");
        material.setFileName("ncr.pdf");
        material.setSortNo(1);
        req.setReviewMaterials(List.of(material));

        try (MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(900L);
            login.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("QA");
            service.dispose(req);
        }

        verify(releaseApplicationMapper).closeFromNonconformance(eq(applicationId), eq(3), eq(decision),
                eq(900L), any(), eq("QA disposition"), any());
        if (DISPOSITION_REWORK.equals(disposition)) {
            verify(reworkCycleService).start(eq(activeOrderId), eq(workOrderId), eq(review.getId()), any());
        }
        verify(workTaskService).cancelReleaseApprovalTask(eq(transactionId), eq(decision));
        verify(workTaskMapper, never()).updateById(any(MesProEdhrWorkTaskDO.class));
        verify(releaseTransactionMapper).selectByIdForUpdate(transactionId);
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
