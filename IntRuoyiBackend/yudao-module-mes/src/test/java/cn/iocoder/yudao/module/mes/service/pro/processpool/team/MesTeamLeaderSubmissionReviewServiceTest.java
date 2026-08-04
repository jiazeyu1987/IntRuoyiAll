package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
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

    private MesTeamLeaderSubmissionReviewService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderSubmissionReviewServiceImpl(scopeService, eventMapper, reviewMapper,
                processInspectionAggregationService);
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
                MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 2001L);
        ArgumentCaptor<MesProcessPoolSubmissionReviewDO> reviewCaptor =
                ArgumentCaptor.forClass(MesProcessPoolSubmissionReviewDO.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        MesProcessPoolSubmissionReviewDO review = reviewCaptor.getValue();
        assertEquals(1001L, review.getEventId());
        assertEquals(3001L, review.getLeaderUserId());
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
    void shouldNotAggregateApprovedProductionSubmission() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(productionEvent());
        when(reviewMapper.insert(any(MesProcessPoolSubmissionReviewDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolSubmissionReviewDO.class).setId(7003L);
            return 1;
        });

        Long reviewId = service.reviewSubmission(reviewReq());

        assertEquals(7003L, reviewId);
        verify(processInspectionAggregationService, never()).aggregateApprovedPqcSubmission(any(), any());
    }

    @Test
    void shouldRejectReviewForOutOfScopeEmployee() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED))
                .when(scopeService).assertCanAccessEmployee(3001L,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectDuplicateTerminalReviewForSameSubmission() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(event());
        when(reviewMapper.selectLatestByEventIdForUpdate(1001L)).thenReturn(existingReview());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    @Test
    void shouldRejectSelfReviewWhenLeaderIsActualInspector() {
        when(eventMapper.selectByIdForUpdate(1001L)).thenReturn(eventWithActualEmployee(3001L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    private static MesTeamLeaderSubmissionReviewReqBO reviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"review\"}")
                .build();
    }

    private static MesTeamLeaderSubmissionReviewReqBO rejectedReviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_REJECTED)
                .reviewRemark("压力曲线异常，退回补正")
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
}
