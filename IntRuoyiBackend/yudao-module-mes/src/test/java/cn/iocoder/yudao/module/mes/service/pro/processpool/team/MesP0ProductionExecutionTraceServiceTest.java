package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0ProductionExecutionTraceServiceTest {

    private static final Long EVENT_ID = 1001L;
    private static final Long WORK_ORDER_ID = 9001L;
    private static final Long ROUTE_PROCESS_ID = 5001L;
    private static final Long PROCESS_ID = 6001L;

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
    void productionExecutionTraceReturnsBlockedSectionsInsteadOfThrowingWhenDownstreamLinksMissing() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent())).thenReturn(List.of());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of());
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of());
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(null);

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertEquals(EVENT_ID, trace.getProcessPoolEventId());
        assertFalse(trace.getComplete());
        assertTrue(trace.getCandidateEvents().isEmpty());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("COMPLETE", sections.get("submitEvent").getStatus());
        assertEquals(EVENT_ID, sections.get("submitEvent").getSourceIds().get("processPoolEventId"));
        assertEquals(101L, sections.get("submitEvent").getSourceIds().get("feedbackId"));
        assertEquals(201L, sections.get("submitEvent").getSourceIds().get("recordbookEntryId"));
        assertEquals("BLOCKED", sections.get("quality").getStatus());
        assertEquals("PQC_EVENT_MISSING", sections.get("quality").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("review").getStatus());
        assertEquals("REVIEW_MISSING", sections.get("review").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("allocation").getStatus());
        assertEquals("ALLOCATION_MISSING", sections.get("allocation").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("completion").getStatus());
        assertEquals("ORDER_PROCESS_NOT_COMPLETE", sections.get("completion").getBlockers().get(0).getCode());
        assertEquals("BLOCKED", sections.get("batchRecord").getStatus());
        assertEquals("FIELD_AUDIT_MISSING", sections.get("batchRecord").getBlockers().get(0).getCode());
        assertEquals(5, trace.getBlockers().size());
        verifyNoInteractions(executionMapper, auditItemMapper);
    }

    @Test
    void productionExecutionTraceReturnsCompleteOnlyWhenEveryFormalSourceIdExists() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent())).thenReturn(List.of(pqcEvent()));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(pqcRecord());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(review()));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocation()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertTrue(trace.getComplete());
        assertTrue(trace.getBlockers().isEmpty());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("COMPLETE", sections.get("quality").getStatus());
        assertEquals(1101L, sections.get("quality").getSourceIds().get("pqcEventId"));
        assertEquals("COMPLETE", sections.get("review").getStatus());
        assertEquals(7001L, sections.get("review").getSourceIds().get("reviewId"));
        assertEquals(9101L, sections.get("review").getSourceIds().get("reviewSignatureId"));
        assertEquals("COMPLETE", sections.get("allocation").getStatus());
        assertEquals(7101L, sections.get("allocation").getSourceIds().get("allocationId"));
        assertEquals("COMPLETE", sections.get("completion").getStatus());
        assertEquals(8301L, sections.get("completion").getSourceIds().get("orderProcessCompletionId"));
        assertEquals("COMPLETE", sections.get("batchRecord").getStatus());
        assertEquals(8801L, sections.get("batchRecord").getSourceIds().get("batchRecordExecutionId"));
        assertEquals(9901L, sections.get("batchRecord").getSourceIds().get("fieldAuditBatchId"));
        assertEquals(99011L, sections.get("batchRecord").getSourceIds().get("fieldAuditItemId"));
    }

    private static Map<String, MesProductionExecutionTraceRespVO.Section> sections(
            MesProductionExecutionTraceRespVO trace) {
        return trace.getSections().stream()
                .collect(Collectors.toMap(MesProductionExecutionTraceRespVO.Section::getSectionKey,
                        Function.identity()));
    }

    private static MesProProcessPoolEventDO submitEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(EVENT_ID)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(WORK_ORDER_ID)
                .routeId(4001L)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2001L)
                .deviceAccountId(3001L)
                .deviceId(4001L)
                .workstationId(5001L)
                .feedbackSourceId(101L)
                .recordbookEntryId(201L)
                .recordbookSourceId(202L)
                .signatureId(901L)
                .signatureUserId(2001L)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 0))
                .rawPayload("{\"outputQuantity\":80}")
                .build();
    }

    private static MesProProcessPoolEventDO pqcEvent() {
        return MesProProcessPoolEventDO.builder()
                .id(1101L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .workOrderId(WORK_ORDER_ID)
                .routeProcessId(ROUTE_PROCESS_ID)
                .processId(PROCESS_ID)
                .actualEmployeeId(2101L)
                .deviceAccountId(3001L)
                .deviceId(4001L)
                .workstationId(5001L)
                .feedbackSourceId(6101L)
                .signatureId(902L)
                .signatureUserId(2101L)
                .serverSubmitTime(LocalDateTime.of(2026, 8, 3, 9, 5))
                .rawPayload("{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")
                .build();
    }

    private static MesProcessPoolSubmissionReviewDO review() {
        return MesProcessPoolSubmissionReviewDO.builder()
                .id(7001L)
                .eventId(EVENT_ID)
                .leaderUserId(3001L)
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewSignatureId(9101L)
                .reviewSignatureUserId(3001L)
                .reviewSignatureSnapshotJson("{\"signature\":\"review\"}")
                .reviewedAt(LocalDateTime.of(2026, 8, 3, 9, 10))
                .build();
    }

    private static MesProProcessPoolPqcRecordDO pqcRecord() {
        return MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build();
    }

    private static MesProcessPoolReportAllocationDO allocation() {
        return MesProcessPoolReportAllocationDO.builder()
                .id(7101L)
                .eventId(EVENT_ID)
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

    private static MesProcessPoolOrderProcessCompletionDO completion() {
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
                .lastEventId(EVENT_ID)
                .lastReviewId(7001L)
                .completedAt(LocalDateTime.of(2026, 8, 3, 9, 20))
                .build();
    }

    private static MesProBatchRecordExecutionDO execution() {
        return MesProBatchRecordExecutionDO.builder()
                .id(8801L)
                .batchRecordReportId("BR-FORM-A")
                .batchRecordDefinitionId(400L)
                .batchRecordVersionId(401L)
                .fieldAuditLastBatchId(9901L)
                .build();
    }

    private static MesProBatchRecordExecutionFieldAuditItemDO auditItem() {
        return MesProBatchRecordExecutionFieldAuditItemDO.builder()
                .id(99011L)
                .auditBatchId(9901L)
                .executionId(8801L)
                .fieldPath("report.pressure")
                .fieldKey("pressure")
                .rowIndex(6)
                .columnIndex(2)
                .newValueDisplay("15")
                .build();
    }
}
