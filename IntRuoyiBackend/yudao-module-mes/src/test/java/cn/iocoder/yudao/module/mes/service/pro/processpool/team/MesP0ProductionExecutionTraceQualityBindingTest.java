package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
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

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.EVENT_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.PROCESS_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.ROUTE_PROCESS_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.WORK_ORDER_ID;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.allocation;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.auditItem;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.completion;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.execution;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.pqcEvent;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.review;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.sections;
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.submitEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0ProductionExecutionTraceQualityBindingTest {

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
    void singlePqcEventWithoutFormalSubmitBindingKeepsQualityBlocked() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"inspectionResult\":\"PASS\"}")));
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(review(7001L, 9101L, 3001L)));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocation()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("BLOCKED", sections.get("quality").getStatus());
        assertEquals("PQC_BINDING_MISSING", sections.get("quality").getBlockers().get(0).getCode());
    }

    @Test
    void rawPayloadOnlyProductionSubmitIdKeepsQualityBlocked() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(review(7001L, 9101L, 3001L)));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocation()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("BLOCKED", sections.get("quality").getStatus());
        assertEquals("PQC_BINDING_MISSING", sections.get("quality").getBlockers().get(0).getCode());
    }

    @Test
    void structuredPqcProductionSubmitBindingCompletesQualitySection() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"productionSubmitEventId\":9999,\"inspectionResult\":\"PASS\"}")));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of());
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of());
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(null);

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("COMPLETE", sections.get("quality").getStatus());
        assertEquals(EVENT_ID, sections.get("quality").getSourceIds().get("productionSubmitEventId"));
        assertEquals(1201L, sections.get("quality").getSourceIds().get("pqcRecordId"));
    }

    @Test
    void multiplePqcCandidatesAreReturnedAndKeepTraceIncomplete() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent())).thenReturn(List.of(
                pqcEvent(1101L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}"),
                pqcEvent(1102L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")));
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of());
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of());
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(null);

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        assertEquals(2, trace.getCandidateEvents().size());
        assertEquals(1101L, trace.getCandidateEvents().get(0).getProcessPoolEventId());
        assertEquals("PQC_BINDING_AMBIGUOUS", sections(trace).get("quality").getBlockers().get(0).getCode());
    }
}
