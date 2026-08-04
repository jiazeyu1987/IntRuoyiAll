package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesPqcProcessInspectionAggregationServiceTest {

    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;

    private MesPqcProcessInspectionAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MesPqcProcessInspectionAggregationServiceImpl(pqcRecordMapper);
    }

    @Test
    void shouldMarkPendingPqcRecordAggregatedByApprovedReview() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(pendingRecord());
        when(pqcRecordMapper.updateProcessInspectionAggregatedIfPending(eq(1001L), eq(7001L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(1);

        service.aggregateApprovedPqcSubmission(1001L, 7001L);

        ArgumentCaptor<LocalDateTime> aggregatedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(pqcRecordMapper).updateProcessInspectionAggregatedIfPending(eq(1001L), eq(7001L),
                aggregatedAtCaptor.capture());
        assertNotNull(aggregatedAtCaptor.getValue());
    }

    @Test
    void shouldFailFastWhenApprovedSubmissionHasNoPqcRecord() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RECORD_REQUIRED.getCode(), ex.getCode());
        verify(pqcRecordMapper, never()).updateProcessInspectionAggregatedIfPending(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectAlreadyAggregatedPqcRecord() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(aggregatedRecord());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED.getCode(),
                ex.getCode());
        verify(pqcRecordMapper, never()).updateProcessInspectionAggregatedIfPending(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectConcurrentDuplicateAggregationWhenPendingWasConsumed() {
        when(pqcRecordMapper.selectByEventId(1001L)).thenReturn(pendingRecord());
        when(pqcRecordMapper.updateProcessInspectionAggregatedIfPending(eq(1001L), eq(7001L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.aggregateApprovedPqcSubmission(1001L, 7001L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED.getCode(),
                ex.getCode());
    }

    private static MesProProcessPoolPqcRecordDO pendingRecord() {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(9001L)
                .eventId(1001L)
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_PENDING)
                .build();
    }

    private static MesProProcessPoolPqcRecordDO aggregatedRecord() {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(9001L)
                .eventId(1001L)
                .processInspectionAggregationStatus(
                        MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_AGGREGATED)
                .processInspectionReviewId(7000L)
                .processInspectionAggregatedAt(LocalDateTime.of(2026, 8, 3, 18, 0))
                .build();
    }
}
