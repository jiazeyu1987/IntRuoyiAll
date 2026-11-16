package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesReverseTraceOperationLog;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesReverseTraceOperationLoggingControllerTest {
    private final MesProEdhrReverseTraceService service = mock(MesProEdhrReverseTraceService.class);
    private final MesReverseTraceOperationLog log = mock(MesReverseTraceOperationLog.class);
    private final MesProEdhrReverseTraceController controller = new MesProEdhrReverseTraceController();

    MesReverseTraceOperationLoggingControllerTest() {
        ReflectionTestUtils.setField(controller, "reverseTraceService", service);
        ReflectionTestUtils.setField(controller, "operationLog", log);
    }

    @Test
    void blockedSourceResponsesRemainVisibleWithoutInventingAnOrderLog() {
        QueryRequest query = new QueryRequest();
        QueryResponse queryResult = new QueryResponse().setQueryStatus("BLOCKED").setReasonCode("SOURCE_MISSING");
        when(service.query(query)).thenReturn(queryResult);
        assertSame(queryResult, controller.query(query).getData());
        EvidenceRequest evidence = new EvidenceRequest();
        EvidenceResponse evidenceResult = new EvidenceResponse().setEvidenceStatus("BLOCKED").setReasonCode("BATCH_SCOPE_INVALID");
        when(service.evidence(evidence)).thenReturn(evidenceResult);
        assertSame(evidenceResult, controller.evidence(evidence).getData());
        verifyNoInteractions(log);
    }

    @Test
    void successfulReadsRecordTheOriginalQueryAndEvidenceSeparately() {
        QueryRequest query = new QueryRequest().setAnchorBatchExecutionId("81").setConditions(List.of(new Condition()));
        when(service.query(query)).thenReturn(new QueryResponse().setQueryStatus("MATCHED")
                .setCoverageStatus("COMPLETE").setQueryHash("source-hash"));
        controller.query(query);
        EvidenceRequest evidence = new EvidenceRequest().setAnchorBatchExecutionId("81").setTargetBatchExecutionId("82");
        when(service.evidence(evidence)).thenReturn(new EvidenceResponse().setEvidenceStatus("MATCHED").setQueryHash("source-hash"));
        controller.evidence(evidence);
        verify(log).record(eq("81"), eq("REVERSE_TRACE_QUERY"), argThat(m -> m.get("conditionCount").equals(1)));
        verify(log).record(eq("81"), eq("REVERSE_TRACE_EVIDENCE"), argThat(m -> m.get("targetBatchExecutionId").equals("82")));
        verifyNoMoreInteractions(log);
    }
}
