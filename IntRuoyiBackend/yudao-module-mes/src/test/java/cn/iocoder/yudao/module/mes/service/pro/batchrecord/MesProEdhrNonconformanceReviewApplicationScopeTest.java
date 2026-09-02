package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    }

    @Test
    void pqcReleaseApplicationCanStartReviewBeforeBatchCreation() {
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
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
        when(workOrderMapper.updateTemporaryFrozenByIds(java.util.List.of(3002L), true)).thenReturn(1);

        service.dispose(new MesProEdhrNonconformanceReviewDisposeReqVO()
                .setId(1002L)
                .setDisposition("void")
                .setReviewMaterialUrl("https://example.invalid/review.pdf")
                .setReviewOpinion("作废处理")
                .setQaSignature("QA-SIGNATURE"));

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

    private void stubPendingReview(String disposition) {
        MesProEdhrNonconformanceReviewDO review = MesProEdhrNonconformanceReviewDO.builder()
                .id(1001L)
                .sourceType("PQC_RELEASE")
                .sourceId(7001L)
                .workOrderId(3001L)
                .workOrderCode("WO-001")
                .reviewStatus("pending_review")
                .previousWorkOrderTemporaryFrozen(false)
                .nonconformanceReason("检验结论需要评审")
                .build();
        when(reviewMapper.selectByIdForUpdate(1001L)).thenReturn(review);
        when(reviewMapper.selectById(1001L)).thenReturn(review.setDisposition(disposition));
        when(releaseApplicationMapper.selectByIdForUpdate(7001L)).thenReturn(
                new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(7001L)
                        .setApplicationStatus(MesReleaseFlowStatus.PQC_RELEASE_PENDING)
                        .setVersion(1)
                        .setWorkOrderId(3001L)
                        .setPqcReleaseWorkTaskId(8001L));
        when(workOrderMapper.selectByIdForUpdate(3001L)).thenReturn(
                new MesProWorkOrderDO().setId(3001L).setTemporaryFrozen(true));
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
                .setQaSignature("QA-SIGNATURE");
    }
}
