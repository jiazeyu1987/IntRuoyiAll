package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import static cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ProductionExecutionTraceTestSupport.submitEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesP0ProductionExecutionClosureAuditTest {

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
    void closureEvidenceAnswersNineAuditQuestionsWithFormalSourcesAndReadonlyVerification() {
        mockCompleteTrace(List.of(allocation()));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertTrue(trace.getComplete());
        MesProductionExecutionTraceRespVO.ClosureEvidence closureEvidence = trace.getClosureEvidence();
        assertNotNull(closureEvidence);
        assertTrue(closureEvidence.getComplete());
        assertEquals(EVENT_ID, closureEvidence.getProcessPoolEventId());
        assertEquals(Set.of("who", "device", "process", "quantity", "quality", "signature",
                        "workOrder", "review", "batchRecord"),
                closureEvidence.getAnswers().keySet());
        closureEvidence.getAnswers().forEach((answerKey, answer) -> {
            assertNotNull(answer.getValue(), answerKey + " must expose a business value.");
            assertFalse(answer.getSourceIds().isEmpty(), answerKey + " must expose formal sourceIds.");
            assertTrue(answer.getSameSource(), answerKey + " must pass same-source checks.");
            assertFalse(answer.getReadOnlyVerificationEntries().isEmpty(),
                    answerKey + " must expose readonly verification entries.");
        });
        assertEquals(2001L, closureEvidence.getAnswers().get("who").getSourceIds().get("actualEmployeeId"));
        assertEquals(4001L, closureEvidence.getAnswers().get("device").getSourceIds().get("deviceId"));
        assertEquals(PROCESS_ID, closureEvidence.getAnswers().get("process").getSourceIds().get("processId"));
        assertEquals("SUCCESS", closureEvidence.getAnswers().get("quality").getSourceIds().get("inspectionResult"));
        assertEquals(9101L, closureEvidence.getAnswers().get("signature").getSourceIds().get("reviewSignatureId"));
        assertEquals(WORK_ORDER_ID, closureEvidence.getAnswers().get("workOrder").getSourceIds().get("targetWorkOrderId"));
        assertEquals(7001L, closureEvidence.getAnswers().get("review").getSourceIds().get("reviewId"));
        assertEquals(99011L, closureEvidence.getAnswers().get("batchRecord").getSourceIds().get("fieldAuditItemId"));
        assertTrue(closureEvidence.getSameSourceChecks().stream()
                .allMatch(MesProductionExecutionTraceRespVO.SameSourceCheck::getPassed));
        assertTrue(closureEvidence.getBlockers().isEmpty());
    }

    @Test
    void closureEvidenceKeepsTraceIncompleteWhenQuantityAnswerLacksFormalSource() {
        MesProcessPoolReportAllocationDO allocationWithoutQuantity = allocation().setAllocatedQuantity(null);
        mockCompleteTrace(List.of(allocationWithoutQuantity));

        MesProductionExecutionTraceRespVO trace = service.getProductionExecutionTrace(EVENT_ID);

        assertFalse(trace.getComplete());
        MesProductionExecutionTraceRespVO.EvidenceAnswer quantityAnswer =
                trace.getClosureEvidence().getAnswers().get("quantity");
        assertFalse(quantityAnswer.getSameSource());
        assertEquals("CLOSURE_EVIDENCE_MISSING_SOURCE", quantityAnswer.getBlockers().get(0).getCode());
        assertEquals("quantity", trace.getClosureEvidence().getBlockers().get(0).getMissingObjectType());
    }

    private void mockCompleteTrace(List<MesProcessPoolReportAllocationDO> allocations) {
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
        when(allocationMapper.selectListByEventId(EVENT_ID)).thenReturn(allocations);
        when(completionMapper.selectByWorkOrderAndProcess(WORK_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(completion());
        when(executionMapper.selectById(8801L)).thenReturn(execution());
        when(auditItemMapper.selectListByBatchId(9901L)).thenReturn(List.of(auditItem()));
    }
}
