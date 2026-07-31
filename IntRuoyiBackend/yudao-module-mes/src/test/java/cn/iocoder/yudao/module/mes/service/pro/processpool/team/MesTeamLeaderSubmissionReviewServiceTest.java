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

    private MesTeamLeaderSubmissionReviewService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderSubmissionReviewServiceImpl(scopeService, eventMapper, reviewMapper);
    }

    @Test
    void shouldReviewResponsibleEmployeeSubmissionWithoutChangingRawEvent() {
        MesProProcessPoolEventDO event = event();
        when(eventMapper.selectById(1001L)).thenReturn(event);
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
    }

    @Test
    void shouldRejectReviewForOutOfScopeEmployee() {
        when(eventMapper.selectById(1001L)).thenReturn(event());
        doThrow(exception(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED))
                .when(scopeService).assertCanAccessEmployee(3001L,
                        MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION, 2001L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.reviewSubmission(reviewReq()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_DENIED.getCode(), ex.getCode());
        verify(reviewMapper, never()).insert(any(MesProcessPoolSubmissionReviewDO.class));
    }

    private static MesTeamLeaderSubmissionReviewReqBO reviewReq() {
        return MesTeamLeaderSubmissionReviewReqBO.builder()
                .eventId(1001L)
                .leaderUserId(3001L)
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark("数据和签名一致")
                .build();
    }

    private static MesProProcessPoolEventDO event() {
        return MesProProcessPoolEventDO.builder()
                .id(1001L)
                .actualEmployeeId(2001L)
                .rawPayload("{\"outputQuantity\":10}")
                .serverSubmitTime(LocalDateTime.of(2026, 7, 30, 9, 10))
                .signatureId(9001L)
                .signatureUserId(2001L)
                .build();
    }
}
