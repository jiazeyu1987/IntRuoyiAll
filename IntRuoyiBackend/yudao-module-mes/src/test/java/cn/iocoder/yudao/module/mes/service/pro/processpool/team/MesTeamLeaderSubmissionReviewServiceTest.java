package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamLeaderSubmissionReviewServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock
    private MesPqcProcessInspectionAggregationService processInspectionAggregationService;
    @Mock
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock
    private MesReportAllocationCommandService reportAllocationCommandService;

    private MesTeamLeaderSubmissionReviewService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderSubmissionReviewServiceImpl(scopeService, eventMapper, reviewMapper,
                processInspectionAggregationService);
        ReflectionTestUtils.setField(service, "signatureService", signatureService);
        ReflectionTestUtils.setField(service, "reportAllocationCommandService", reportAllocationCommandService);
        lenient().when(signatureService.recordTeamLeaderReviewSignature(any(), any(), any())).thenReturn(9101L);
    }

    @Test
    void shouldReviewResponsibleEmployeeSubmissionWithoutChangingRawEvent() {
        MesProProcessPoolEventDO event = event();
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event);
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7001L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(reviewReq());

        assertEquals(7001L, reviewId);
        verify(scopeService).assertCanAccessEmployee(3001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 2001L);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        MesProcessPoolSubmissionReviewDO review = reviewCaptor.getValue();
        assertEquals(1001L, review.getEventId());
        assertEquals(3001L, review.getLeaderUserId());
        assertEquals(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, review.getLeaderType());
        assertEquals(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED, review.getReviewStatus());
        assertEquals("数据和签名一致", review.getReviewRemark());
        assertNotNull(review.getReviewedAt());
        verify(eventMapper, never()).updateById(any(MesProProcessPoolEventDO.class));
        assertEquals("{\"outputQuantity\":10}", event.getRawPayload());
        assertEquals(9001L, event.getSignatureId());
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7001L);
    }

    @Test
    void shouldNotAggregateRejectedPqcSubmission() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7002L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(rejectedReviewReq());

        assertEquals(7002L, reviewId);
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldRejectApprovedProductionSubmissionThroughGenericReview() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(productionEvent());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED.getCode(),
                ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldDelegateProductionRejectionToAllocationRollbackService() {
        when(reportAllocationCommandService.rejectProductionSubmission(1001L, 3001L,
                "数量错误", "review-pass")).thenReturn(7401L);

        Long reviewId = service.reviewSubmission(rejectedProductionReviewReq());

        assertEquals(7401L, reviewId);
        verify(reportAllocationCommandService).rejectProductionSubmission(1001L, 3001L,
                "数量错误", "review-pass");
        verify(eventMapper, never()).selectByIdForUpdate(any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectProductionLeaderReviewingPqcSubmission() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reviewSubmission(productionReviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_PQC_LEADER_REQUIRED.getCode(),
                ex.getCode());
        verify(scopeService, never()).assertCanAccessEmployee(any(), any(), any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldRejectRejectedPqcReviewWithoutReason() {
        MesTeamLeaderSubmissionReviewReqBO req = rejectedReviewReq().setReviewRemark("  ");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_REJECT_REMARK_REQUIRED.getCode(),
                ex.getCode());
        verify(eventMapper, never()).selectByIdForUpdate(any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectReviewForOutOfScopeEmployee() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "员工"))
                .when(scopeService).assertCanAccessEmployee(3001L,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED.getCode(), ex.getCode());
        assertEquals("班组长不在该员工的负责范围内", ex.getMessage());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectAdditionalPqcReviewWhenPreviousPqcReviewExists() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(anyLong(), anyLong());
    }

    @Test
    void shouldReplaySameTerminalPqcReviewWithoutNewSignatureOrAggregate() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingApprovedReview());

        Long reviewId = service.reviewSubmission(reviewReq());

        assertEquals(7004L, reviewId);
        verify(signatureService, never()).recordTeamLeaderReviewSignature(any(), any(), any());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldPropagateAggregationFailureSoTransactionCanRollbackReviewAndSignature() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7005L);
            return 1;
        });
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                "pqcProcessInspectionAggregateDetail"))
                .when(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7005L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        verify(reviewMapper).insert(any(MesProcessPoolSubmissionReviewDO.class));
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7005L);
    }

    @Test
    void shouldAllowPqcReviewWhenLeaderIsActualInspector() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(eventWithActualEmployee(3001L));
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7003L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(reviewReq());

        assertEquals(7003L, reviewId);
        verify(scopeService).assertCanAccessEmployee(3001L,
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC, 3001L);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        assertEquals(3001L, reviewCaptor.getValue().getLeaderUserId());
        assertEquals(9101L, reviewCaptor.getValue().getReviewSignatureId());
        verify(processInspectionAggregationService).aggregateApprovedPqcSubmission(1001L, 7003L);
    }

    private static MesTeamLeaderSubmissionReviewReqBO reviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .signaturePassword("review-pass")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"review\"}")
                .build();
    }

    private static MesTeamLeaderSubmissionReviewReqBO productionReviewReq() {
        return reviewReq()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION);
    }

    private static MesTeamLeaderSubmissionReviewReqBO rejectedProductionReviewReq() {
        return reviewReq()
                .setLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .setReviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .setReviewRemark("数量错误");
    }

    private static MesTeamLeaderSubmissionReviewReqBO rejectedReviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark("压力曲线异常，退回补正")
                .signaturePassword("review-pass")
                .reviewSignatureId(9102L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"reject\"}")
                .build();
    }

    private static MesProProcessPoolEventDO event() {
        return eventWithActualEmployee(2001L);
    }

    private static MesProProcessPoolEventDO productionEvent() {
        return eventWithActualEmployee(2001L)
                .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT);
    }

    private static MesProProcessPoolEventDO eventWithActualEmployee(Long actualEmployeeId) {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .actualEmployeeId(actualEmployeeId)
                .rawPayload("{\"outputQuantity\":10}")
                .serverSubmitTime(LocalDateTime.of(2026, 7, 30, 9, 10))
                .signatureId(9001L)
                .signatureUserId(actualEmployeeId)
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO existingReview() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(7000L)
                .eventId(1001L)
                .leaderUserId(3002L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark("压力曲线异常，已退回")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 10, 30))
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO existingApprovedReview() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(7004L)
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 10, 30))
                .build();
    }
}
