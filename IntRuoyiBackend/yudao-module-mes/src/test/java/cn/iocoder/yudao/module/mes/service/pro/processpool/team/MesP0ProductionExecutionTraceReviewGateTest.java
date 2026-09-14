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
class MesP0ProductionExecutionTraceReviewGateTest {

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
    void unsignedReviewInReviewSetKeepsReviewBlockedAndReturnsAllReviewIds() {
        when(eventMapper.selectById(EVENT_ID)).thenReturn(submitEvent());
        when(eventMapper.selectPqcEventsForSubmit(submitEvent()))
                .thenReturn(List.of(pqcEvent(1101L, "{\"productionSubmitEventId\":1001,\"inspectionResult\":\"PASS\"}")));
        when(pqcRecordMapper.selectByEventId(1101L)).thenReturn(MesProProcessPoolPqcRecordDO.builder()
                .id(1201L)
                .eventId(1101L)
                .productionSubmitEventId(EVENT_ID)
                .inspectionResult(MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS)
                .build());
        when(reviewMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(
                review(7001L, 9101L, 3001L),
                review(7002L, null, null)));
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(List.of(allocation()));
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        Map<String, MesProductionExecutionTraceRespVO.Section> sections = sections(trace);
        assertEquals("BLOCKED", sections.get("review").getStatus());
        assertEquals(List.of(7001L, 7002L), sections.get("review").getSourceIds().get("reviewIds"));
        assertEquals("REVIEW_SIGNATURE_MISSING", sections.get("review").getBlockers().get(0).getCode());
    }
}
