package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchExecutionRejectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNonconformanceReviewApplicationScopeTest {

    @Mock private MesProEdhrNonconformanceReviewMapper reviewMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock private MesProProcessPoolEventMapper processPoolEventMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock private MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock private MesPqcInspectionTaskMapper pqcInspectionTaskMapper;
    @Mock private MesProEdhrOperationAuditService operationAuditService;

    private MesProEdhrNonconformanceReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MesProEdhrNonconformanceReviewServiceImpl();
        ReflectionTestUtils.setField(service, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        ReflectionTestUtils.setField(service, "releaseApplicationMapper", releaseApplicationMapper);
        ReflectionTestUtils.setField(service, "processPoolEventMapper", processPoolEventMapper);
        ReflectionTestUtils.setField(service, "workOrderMapper", workOrderMapper);
        ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        ReflectionTestUtils.setField(service, "signatureService", signatureService);
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", batchExecutionOriginMapper);
        ReflectionTestUtils.setField(service, "pqcInspectionTaskMapper", pqcInspectionTaskMapper);
        ReflectionTestUtils.setField(service, "operationAuditService", operationAuditService);
    }

    @Test
    void creatingApplicationReviewRecordsActiveOrderOperationFact() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001"));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });

        service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setSourceType("PQC_RELEASE")
                .setSourceId(7001L)
                .setNonconformanceReason("检验结论需要评审"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("NONCONFORMANCE_REVIEW_CREATE", auditCaptor.getValue().getOperationType());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":8101"));
    }

    @Test
    void disposingApplicationReviewRecordsActiveOrderOperationFact() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                isNull(), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("NONCONFORMANCE_REVIEW_DISPOSE", auditCaptor.getValue().getOperationType());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":8101"));
    }

    @Test
    void concessionReleaseDispositionRecordsActiveOrderOperationFactWithBusinessActionName() {
        stubPendingReview("concession_release");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(disposeRequest("concession_release"));

        ArgumentCaptor<MesProEdhrOperationAuditCommand> auditCaptor =
                ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(operationAuditService).recordInCallerTransaction(auditCaptor.capture());
        assertEquals("NONCONFORMANCE_REVIEW_DISPOSE", auditCaptor.getValue().getOperationType());
        assertEquals("让步放行", auditCaptor.getValue().getActionName());
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"activeOrderId\":8101"));
        assertTrue(auditCaptor.getValue().getMetadataJson().contains("\"disposition\":\"concession_release\""));
    }

    @Test
    void pqcReleaseApplicationCanStartReviewBeforeBatchCreation() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setWorkOrderCode("WO-001")
                        .setBatchCode("BATCH-001"));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1001L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_RELEASE")
                        .setSourceId(7001L)
                        .setNonconformanceReason("检验结论需要评审"));

        assertEquals(1001L, result.getId());
        assertEquals(3001L, result.getWorkOrderId());
        assertEquals("WO-001", result.getWorkOrderCode());
        assertNull(result.getBatchExecutionId());
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), true);
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void pqcSubmissionCanStartReviewWithoutBatchExecution() {
        when(processPoolEventMapper.selectByIdForUpdate(160L)).thenReturn(
                new MesProProcessPoolEventDO()
                        .setId(160L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setWorkOrderId(3003L));
        when(workOrderMapper.selectByIdForUpdate(3003L)).thenReturn(
                new MesProWorkOrderDO()
                        .setId(3003L)
                        .setCode("WO-PQC-001")
                        .setBatchCode("BATCH-PQC-001")
                        .setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3003L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1003L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_SUBMISSION")
                        .setSourceId(160L)
                        .setNonconformanceReason("PQC提交不合格"));

        assertEquals(1003L, result.getId());
        assertEquals("PQC_SUBMISSION", result.getSourceType());
        assertEquals(160L, result.getSourceId());
        assertEquals(3003L, result.getWorkOrderId());
        assertEquals("WO-PQC-001", result.getWorkOrderCode());
        assertEquals("BATCH-PQC-001", result.getBatchCode());
        assertNull(result.getBatchExecutionId());
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3003L), true);
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void pqcSubmissionUsesSourceEventWhenBatchExecutionIdIsStale() {
        when(processPoolEventMapper.selectByIdForUpdate(161L)).thenReturn(
                new MesProProcessPoolEventDO()
                        .setId(161L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setWorkOrderId(3004L));
        when(workOrderMapper.selectByIdForUpdate(3004L)).thenReturn(
                new MesProWorkOrderDO()
                        .setId(3004L)
                        .setCode("WO-PQC-002")
                        .setBatchCode("BATCH-PQC-002")
                        .setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3004L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1004L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.create(
                new MesProEdhrNonconformanceReviewCreateReqVO()
                        .setSourceType("PQC_SUBMISSION")
                        .setSourceId(161L)
                        .setBatchExecutionId(999_999L)
                        .setNonconformanceReason("PQC提交不合格"));

        assertEquals(1004L, result.getId());
        assertEquals("PQC_SUBMISSION", result.getSourceType());
        assertEquals(161L, result.getSourceId());
        assertEquals(3004L, result.getWorkOrderId());
        assertNull(result.getBatchExecutionId());
        verify(batchExecutionMapper, never()).selectById(999_999L);
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void batchReviewFreezesWorkOrderAndCapturesOriginalState() {
        when(batchExecutionMapper.selectById(9001L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9001L)
                .setBatchExecutionCode("BE-9001")
                .setWorkOrderId(3002L)
                .setWorkOrderCode("WO-002")
                .setBatchCode("BATCH-002")
                .setStatus(20));
        when(workOrderMapper.selectByIdForUpdate(3002L)).thenReturn(
                new MesProWorkOrderDO().setId(3002L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3002L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1002L);
            return 1;
        });

        service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setSourceType("PQC_RELEASE")
                .setBatchExecutionId(9001L)
                .setNonconformanceReason("批次不合格"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(false, reviewCaptor.getValue().getPreviousWorkOrderTemporaryFrozen());
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3002L), true);
        verify(batchExecutionMapper).updateById(any(MesProEdhrBatchExecutionDO.class));
    }


    @Test
    void releaseOwnerRejectCreatesSignedPendingReviewAndFreezesBatch() {
        when(batchExecutionMapper.selectByIdForUpdate(9002L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9002L)
                .setBatchExecutionCode("BE-9002")
                .setWorkOrderId(3008L)
                .setWorkOrderCode("WO-008")
                .setBatchCode("BATCH-008")
                .setStatus(20));
        when(workOrderMapper.selectByIdForUpdate(3008L)).thenReturn(
                new MesProWorkOrderDO().setId(3008L).setTemporaryFrozen(false));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3008L), true)).thenReturn(1);
        when(signatureService.recordBatchActionSignature(isNull(), eq(9002L), eq("release-password"),
                eq("末检结果不合格"), eq(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT),
                eq("eDHR不合格评审发起"), any())).thenReturn(9202L);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(1202L);
            return 1;
        });

        MesProEdhrNonconformanceReviewRespVO result = service.rejectBatch(
                new MesProEdhrBatchExecutionRejectReqVO()
                        .setBatchExecutionId(9002L)
                        .setNonconformanceReason("末检结果不合格")
                        .setSignaturePassword("release-password"));

        assertEquals(1202L, result.getId());
        assertEquals("PQC_RELEASE", result.getSourceType());
        assertEquals(9002L, result.getBatchExecutionId());
        assertEquals("pending_review", result.getReviewStatus());
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertTrue(reviewCaptor.getValue().getRemark().contains("9202"));
        verify(signatureService).recordBatchActionSignature(isNull(), eq(9002L), eq("release-password"),
                eq("末检结果不合格"), eq(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT),
                eq("eDHR不合格评审发起"), any());
        verify(batchExecutionMapper).updateById(argThat((MesProEdhrBatchExecutionDO batch) ->
                batch.getId().equals(9002L) && batch.getStatus().equals(15)));
        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3008L), true);
    }

    @Test
    void releaseOwnerRejectWithInvalidSignatureDoesNotCreateReviewOrFreezeBatch() {
        when(batchExecutionMapper.selectByIdForUpdate(9003L)).thenReturn(new MesProEdhrBatchExecutionDO()
                .setId(9003L)
                .setWorkOrderId(3009L)
                .setStatus(20));
        when(signatureService.recordBatchActionSignature(isNull(), eq(9003L), eq("wrong-password"),
                eq("末检结果不合格"), eq(MesProBatchRecordExecutionSignatureService.ACTION_NONCONFORMANCE_REJECT),
                eq("eDHR不合格评审发起"), any()))
                .thenThrow(exception(PRO_BATCH_RECORD_EXECUTION_SIGNATURE_PASSWORD_INVALID));

        assertThrows(ServiceException.class, () -> service.rejectBatch(
                new MesProEdhrBatchExecutionRejectReqVO()
                        .setBatchExecutionId(9003L)
                        .setNonconformanceReason("末检结果不合格")
                        .setSignaturePassword("wrong-password")));

        verify(reviewMapper, never()).insert(any(MesProEdhrNonconformanceReviewDO.class));
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
        verify(workOrderMapper, never()).updateTemporaryFrozenByIds(any(), any());
    }

    @Test
    void workOrderPendingReviewFreezeErrorIncludesBranchDetail() {
        when(reviewMapper.selectFirstBlockingByWorkOrderId(3005L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(1101L)
                        .reviewCode("NCR-1101")
                        .reviewStatus("pending_review")
                        .workOrderId(3005L)
                        .sourceType("PQC_SUBMISSION")
                        .sourceId(160L)
                        .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureWorkOrderNotFrozen(3005L, "报工"));

        assertTrue(exception.getMessage().contains("冻结分支=待处置不合格评审"));
        assertTrue(exception.getMessage().contains("action=报工"));
        assertTrue(exception.getMessage().contains("workOrderId=3005"));
        assertTrue(exception.getMessage().contains("reviewId=1101"));
        assertTrue(exception.getMessage().contains("sourceType=PQC_SUBMISSION"));
    }

    @Test
    void workOrderVoidDispositionFreezeErrorIncludesBranchDetail() {
        when(reviewMapper.selectFirstBlockingByWorkOrderId(3006L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(1102L)
                        .reviewCode("NCR-1102")
                        .reviewStatus("closed")
                        .disposition("void")
                        .workOrderId(3006L)
                        .sourceType("PQC_RELEASE")
                        .sourceId(7001L)
                        .build());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureWorkOrderNotFrozen(3006L, "报工"));

        assertTrue(exception.getMessage().contains("冻结分支=作废处置不合格评审"));
        assertTrue(exception.getMessage().contains("action=报工"));
        assertTrue(exception.getMessage().contains("workOrderId=3006"));
        assertTrue(exception.getMessage().contains("reviewId=1102"));
        assertTrue(exception.getMessage().contains("sourceType=PQC_RELEASE"));
    }

    @Test
    void workOrderTemporaryFrozenErrorIncludesBranchDetail() {
        when(workOrderMapper.selectByIdForUpdate(3007L)).thenReturn(
                new MesProWorkOrderDO().setId(3007L).setTemporaryFrozen(true));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.ensureWorkOrderNotFrozen(3007L, "报工"));

        assertTrue(exception.getMessage().contains("冻结分支=工单临时冻结"));
        assertTrue(exception.getMessage().contains("action=报工"));
        assertTrue(exception.getMessage().contains("workOrderId=3007"));
    }

    @Test
    void batchVoidKeepsWorkOrderFrozen() {
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .id(1002L)
                .sourceType("PQC_RELEASE")
                .batchExecutionId(9001L)
                .workOrderId(3002L)
                .reviewStatus("pending_review")
                .previousBatchStatus(20)
                .previousWorkOrderTemporaryFrozen(false)
                .nonconformanceReason("批次不合格")
                .build();
        when(reviewMapper.selectByIdForUpdate(1002L)).thenReturn(review);
        when(reviewMapper.selectById(1002L)).thenReturn(review.setDisposition("void"));
        when(batchExecutionMapper.selectById(9001L)).thenReturn(
                new MesProEdhrBatchExecutionDO().setId(9001L).setStatus(15));
        when(workOrderMapper.selectByIdForUpdate(3002L)).thenReturn(
                new MesProWorkOrderDO().setId(3002L).setTemporaryFrozen(true));
        when(signatureService.recordQaDispositionSignature(isNull(), eq(1002L), eq("qa-signature-password"),
                eq("作废处理"), any())).thenReturn(9102L);
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3002L), true)).thenReturn(1);

        service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1002L)
                .setDisposition("void")
                .setReviewMaterialUrl("https://example.invalid/review.pdf")
                .setReviewOpinion("作废处理")
                .setSignaturePassword("qa-signature-password"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3002L), true);
        verify(batchExecutionMapper).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void concessionRestoresOriginalWorkOrderStateAndKeepsPqcTaskActive() {
        stubPendingReview("concession_release");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);

        service.dispose(disposeRequest("concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), false);
        verify(releaseApplicationMapper, never()).closeFromNonconformance(
                any(), any(), any(), any(), any(), any(), any());
        verify(workTaskMapper, never()).completePqcDecisionTask(any(), any(), any());
    }

    @Test
    void reworkClosesApplicationAndPqcTaskAndRestoresOriginalWorkOrderState() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                isNull(), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), false);
        verify(workTaskMapper).completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK"));
    }

    @Test
    void voidClosesApplicationAndPqcTaskAndKeepsWorkOrderFrozen() {
        stubPendingReview("void");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), true)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_VOID"),
                isNull(), any(), eq("作废处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_VOID")))
                .thenReturn(1);

        service.dispose(disposeRequest("void"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3001L), true);
        verify(workTaskMapper).completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_VOID"));
    }

    @Test
    void disposeRecordsQaElectronicSignatureSnapshot() {
        stubPendingReview("rework");
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3001L), false)).thenReturn(1);
        when(releaseApplicationMapper.closeFromNonconformance(eq(7001L), eq(1), eq("NONCONFORMANCE_REWORK"),
                isNull(), any(), eq("返工处理"), any())).thenReturn(1);
        when(workTaskMapper.completePqcDecisionTask(eq(8001L), any(), eq("NONCONFORMANCE_REWORK")))
                .thenReturn(1);

        service.dispose(disposeRequest("rework"));

        verify(signatureService).recordQaDispositionSignature(isNull(), eq(1001L), eq("qa-signature-password"),
                eq("返工处理"), any());
        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> updateCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).updateById(updateCaptor.capture());
        assertEquals("QA电子签名#9101", updateCaptor.getValue().getQaSignature());
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"qaSignatureSnapshotJson\""));
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"signatureId\":9101"));
        assertTrue(updateCaptor.getValue().getTraceSnapshotJson().contains("\"actionType\":\"QA_DISPOSITION\""));
    }

    @Test
    void disposeWithoutSignaturePasswordFailsBeforeMutation() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                        .setId(1001L)
                        .setDisposition("rework")
                        .setReviewMaterialUrl("https://example.invalid/review.pdf")
                        .setReviewOpinion("返工处理")
                        .setSignaturePassword(" ")));

        assertEquals(PRO_EDHR_NONCONFORMANCE_REVIEW_REQUIRED.getCode(), exception.getCode());
        verifyNoInteractions(reviewMapper, batchExecutionMapper, releaseApplicationMapper, workOrderMapper,
                workTaskMapper, signatureService);
    }

    @Test
    void overlappingSecondReviewCapturesReviewIntroducedFreezeAsNonExternal() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        MesProEdhrNonconformanceReviewDO firstReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2101L)
                .workOrderId(3010L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(firstFrozenAt)
                .build();
        when(processPoolEventMapper.selectByIdForUpdate(171L)).thenReturn(
                new MesProProcessPoolEventDO()
                        .setId(171L)
                        .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .setWorkOrderId(3010L));
        when(workOrderMapper.selectByIdForUpdate(3010L)).thenReturn(
                new MesProWorkOrderDO()
                        .setId(3010L)
                        .setCode("WO-CYCLE")
                        .setBatchCode("BATCH-CYCLE")
                        .setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3010L)).thenReturn(
                java.util.List.of(firstReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3010L), true)).thenReturn(1);
        when(reviewMapper.insert(any(MesProEdhrNonconformanceReviewDO.class))).thenAnswer(invocation -> {
            invocation.<MesProEdhrNonconformanceReviewDO>getArgument(0).setId(2102L);
            return 1;
        });

        service.create(new MesProEdhrNonconformanceReviewCreateReqVO()
                .setSourceType("PQC_SUBMISSION")
                .setSourceId(171L)
                .setNonconformanceReason("第二份同轮评审"));

        ArgumentCaptor<MesProEdhrNonconformanceReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProEdhrNonconformanceReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(false, reviewCaptor.getValue().getPreviousWorkOrderTemporaryFrozen());
    }

    @Test
    void closingFirstOverlappingReviewKeepsFreezeUntilRemainingReviewCloses() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 5);
        MesProEdhrNonconformanceReviewDO firstReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2201L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(181L)
                .workOrderId(3020L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(firstFrozenAt)
                .nonconformanceReason("第一份同轮评审")
                .build();
        MesProEdhrNonconformanceReviewDO secondReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2202L)
                .workOrderId(3020L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(secondFrozenAt)
                .build();
        when(reviewMapper.selectByIdForUpdate(2201L)).thenReturn(firstReview);
        when(reviewMapper.selectById(2201L)).thenReturn(firstReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3020L)).thenReturn(
                new MesProWorkOrderDO().setId(3020L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3020L)).thenReturn(
                java.util.List.of(firstReview, secondReview));
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3020L), false)).thenReturn(1);
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3020L), true)).thenReturn(1);

        service.dispose(disposeRequest(2201L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3020L), true);
    }

    @Test
    void closingLaterOverlappingReviewDoesNotKeepReviewIntroducedFreezeAfterCycleEnds() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 5);
        LocalDateTime firstClosedAt = LocalDateTime.of(2026, 9, 13, 9, 10);
        MesProEdhrNonconformanceReviewDO firstReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2301L)
                .workOrderId(3030L)
                .reviewStatus("closed")
                .disposition("concession_release")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(firstFrozenAt)
                .closedAt(firstClosedAt)
                .build();
        MesProEdhrNonconformanceReviewDO laterReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2302L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(191L)
                .workOrderId(3030L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(true)
                .frozenAt(secondFrozenAt)
                .nonconformanceReason("第二份同轮评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(2302L)).thenReturn(laterReview);
        when(reviewMapper.selectById(2302L)).thenReturn(laterReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3030L)).thenReturn(
                new MesProWorkOrderDO().setId(3030L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3030L)).thenReturn(
                java.util.List.of(firstReview, laterReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3030L), false)).thenReturn(1);
        lenient().when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3030L), true)).thenReturn(1);

        service.dispose(disposeRequest(2302L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3030L), false);
    }

    @Test
    void secondRoundManualFreezeRemainsAfterLaterReviewCloses() {
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 10, 0);
        MesProEdhrNonconformanceReviewDO laterReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2402L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(201L)
                .workOrderId(3040L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(true)
                .frozenAt(secondFrozenAt)
                .nonconformanceReason("第二轮评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(2402L)).thenReturn(laterReview);
        when(reviewMapper.selectById(2402L)).thenReturn(laterReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3040L)).thenReturn(
                new MesProWorkOrderDO().setId(3040L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3040L)).thenReturn(
                java.util.List.of(laterReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3040L), true)).thenReturn(1);

        service.dispose(disposeRequest(2402L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3040L), true);
    }

    @Test
    void closedHistoricalExternalFreezeDoesNotRefreezeLaterLifecycle() {
        LocalDateTime firstFrozenAt = LocalDateTime.of(2026, 9, 13, 9, 0);
        LocalDateTime firstClosedAt = LocalDateTime.of(2026, 9, 13, 9, 10);
        LocalDateTime secondFrozenAt = LocalDateTime.of(2026, 9, 13, 10, 0);
        MesProEdhrNonconformanceReviewDO historicalReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2501L)
                .workOrderId(3050L)
                .reviewStatus("closed")
                .disposition("concession_release")
                .previousWorkOrderTemporaryFrozen(true)
                .frozenAt(firstFrozenAt)
                .closedAt(firstClosedAt)
                .build();
        MesProEdhrNonconformanceReviewDO laterReview = MesProEdhrNonconformanceReviewDO.builder()
                .id(2502L)
                .sourceType("PQC_SUBMISSION")
                .sourceId(211L)
                .workOrderId(3050L)
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(secondFrozenAt)
                .nonconformanceReason("第二轮评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(2502L)).thenReturn(laterReview);
        when(reviewMapper.selectById(2502L)).thenReturn(laterReview.setDisposition("concession_release"));
        when(workOrderMapper.selectByIdForUpdate(3050L)).thenReturn(
                new MesProWorkOrderDO().setId(3050L).setTemporaryFrozen(true));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3050L)).thenReturn(
                java.util.List.of(historicalReview, laterReview));
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3050L), false)).thenReturn(1);

        service.dispose(disposeRequest(2502L, "concession_release"));

        verify(workOrderMapper).updateTemporaryFrozenByIds(java.util.List.of(3050L), false);
    }

    private void stubPendingReview(String disposition) {
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .id(1001L)
                .sourceType("PQC_RELEASE")
                .sourceId(7001L)
                .workOrderId(3001L)
                .workOrderCode("WO-001")
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .frozenAt(LocalDateTime.of(2026, 9, 13, 8, 0))
                .nonconformanceReason("检验结论需要评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(review);
        when(reviewMapper.selectById(1001L)).thenReturn(review.setDisposition(disposition));
        lenient().when(reviewMapper.selectFreezeLifecycleByWorkOrderId(3001L)).thenReturn(
                java.util.List.of(review));
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setActiveOrderId(8101L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setPqcReleaseWorkTaskId(8001L));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(true));
        when(signatureService.recordQaDispositionSignature(isNull(), eq(1001L), eq("qa-signature-password"),
                eq("void".equals(disposition) ? "作废处理" :
                        "rework".equals(disposition) ? "返工处理" : "让步放行"), any()))
                .thenReturn(9101L);
        if (!"concession_release".equals(disposition)) {
            when(workTaskMapper.selectByIdForUpdate(8001L)).thenReturn(new MesProEdhrWorkTaskDO()
                    .setId(8001L)
                    .setTaskType("PQC_PRODUCTION_RELEASE")
                    .setBusinessScopeType("RELEASE_APPLICATION")
                    .setBusinessScopeId(7001L)
                    .setStatus(MesProEdhrWorkTaskStatus.TODO));
        }
    }

    private MesProEdhrNonconformanceReviewDisposeReqVO disposeRequest(String disposition) {
        return new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1001L)
                .setDisposition(disposition)
                .setReviewMaterialUrl("https://example.invalid/review.pdf")
                .setReviewOpinion("void".equals(disposition) ? "作废处理" :
                        "rework".equals(disposition) ? "返工处理" : "让步放行")
                .setSignaturePassword("qa-signature-password");
    }

    private MesProEdhrNonconformanceReviewDisposeReqVO disposeRequest(Long reviewId, String disposition) {
        return disposeRequest(disposition).setId(reviewId);
    }
}
