package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.EVENT_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.PROCESS_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.ROUTE_PROCESS_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.WORK_ORDER_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.allocation;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.auditItem;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.execution;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.pqcEvent;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.review;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.sections;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.submitEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0ProductionExecutionTraceFailureTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;
    @Mock
    private MesProProcessPoolPqcRecordMapper pqcRecordMapper;

    private MesTeamLeaderTraceService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamLeaderTraceServiceImpl(eventMapper, reviewMapper, allocationMapper, completionMapper,
                executionMapper, auditItemMapper, pqcRecordMapper);
    }

    @Test
    void batchRecordSectionStaysBlockedWhenCompletionLacksSourceEvent() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(review(7001L, 9101L, 3001L)));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocation()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completionWithoutSourceEvent());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("BLOCKED", sections.get("batchRecord").getStatus());
        assertEquals("BATCH_RECORD_SOURCE_MISSING", sections.get("batchRecord").getBlockers().get(0).getCode());
    }

    @Test
    void traceSectionsStayBlockedWhenAllocationAndCompletionPointToOtherEvent() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(review(7001L, 9101L, 3001L)));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocationForOtherEvent()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completionForOtherEvent());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("BLOCKED", sections.get("allocation").getStatus());
        assertEquals("ALLOCATION_SOURCE_MISSING", sections.get("allocation").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("completion").getStatus());
        assertEquals("COMPLETION_SOURCE_MISSING", sections.get("completion").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("batchRecord").getStatus());
        assertEquals("BATCH_RECORD_SOURCE_MISSING", sections.get("batchRecord").getBlockers().get(0).getCode());
    }

    @Test
    void traceSectionsStayBlockedWhenAllocationOrCompletionBelongsToOtherWorkOrderOrProcess() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(review(7001L, 9101L, 3001L)));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocationForOtherWorkOrder()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completionForOtherRouteProcess());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("BLOCKED", sections.get("allocation").getStatus());
        assertEquals("ALLOCATION_SCOPE_MISMATCH", sections.get("allocation").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("completion").getStatus());
        assertEquals("COMPLETION_SCOPE_MISMATCH", sections.get("completion").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("batchRecord").getStatus());
        assertEquals("BATCH_RECORD_SOURCE_MISSING", sections.get("batchRecord").getBlockers().get(0).getCode());
    }

    private static MesProcessPoolOrderProcessCompletionDO completionWithoutSourceEvent() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(8301L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(8801L)
                .lastReviewId(7001L)
                .completedAt(LocalDateTime.of(2026, 8, 3, 9, 20))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocationForOtherEvent() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(9999L)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 3, 9, 15))
                .build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completionForOtherEvent() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(8301L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(8801L)
                .lastEventId(9999L)
                .lastReviewId(7001L)
                .completedAt(LocalDateTime.of(2026, 8, 3, 9, 20))
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocationForOtherWorkOrder() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(EVENT_ID)
                .reviewId(7001L)
                .leaderUserId(3001L)
                .activeOrderId(8101L)
                .workOrderId(99901L)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .allocatedQuantity(new BigDecimal("80"))
                .allocationMode(MesProcessPoolReportAllocationDO.MODE_FIFO)
                .confirmedAt(LocalDateTime.of(2026, 8, 3, 9, 15))
                .build();
    }

    private static MesProcessPoolOrderProcessCompletionDO completionForOtherRouteProcess() {
        return MesProcessPoolOrderProcessCompletionDO.builder()
                .id(8301L)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(5999L)
                .processId(PROCESS_ID)
                .targetQuantity(new BigDecimal("80"))
                .confirmedQuantity(new BigDecimal("80"))
                .completionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .backfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .backfillExecutionId(8801L)
                .lastEventId(EVENT_ID)
                .lastReviewId(7001L)
                .completedAt(LocalDateTime.of(2026, 8, 3, 9, 20))
                .build();
    }
}
