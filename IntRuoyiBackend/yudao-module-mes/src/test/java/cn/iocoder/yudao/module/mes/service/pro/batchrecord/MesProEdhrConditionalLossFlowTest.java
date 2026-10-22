package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrConditionalLossFlowTest {
    private final MesProEdhrBatchExecutionServiceImpl service = new MesProEdhrBatchExecutionServiceImpl();
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProEdhrBatchExecutionTaskMapper tasks = mock(MesProEdhrBatchExecutionTaskMapper.class);

    private MesProEdhrBatchExecutionTaskDO loss() {
        return new MesProEdhrBatchExecutionTaskDO().setId(2L).setBatchExecutionId(1L)
                .setNodeType("ROUTE_FORM").setRouteProcessId(101L).setProcessId(201L)
                .setRootProcessFlag(true).setRouteProcessSort(1).setBatchRecordSort(2)
                .setBatchRecordReportId("LOSS").setExecutionMode("SEQUENTIAL")
                .setRequiredFlag(true).setRequiredPolicy("CONDITIONAL_REQUIRED").setFormSlotType("LOSS_REPORT")
                .setRequiredConditionJson("{\"type\":\"HAS_ACTUAL_LOSS\"}")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_WAITING);
    }

    private void formalDecision(Boolean hasLoss) {
        ReflectionTestUtils.setField(service, "batchExecutionOriginMapper", origins);
        when(origins.selectListByBatchExecutionId(1L)).thenReturn(hasLoss == null ? List.of()
                : List.of(new MesProEdhrBatchExecutionOriginDO().setHasActualLoss(hasLoss)));
    }

    @Test
    void anotherProcessLossDoesNotRequireThisProcessLossForm() {
        var task = loss();
        var receipt = new MesCompletionBackfillReceipt().setHasActualLoss(true)
                .setLossConditionFactsJson("[{\"processId\":201,\"routeProcessId\":101,\"hasActualLoss\":false},"
                        + "{\"processId\":201,\"routeProcessId\":102,\"hasActualLoss\":true}]");
        ReflectionTestUtils.invokeMethod(service, "applyFormalLossRequirement", task,
                new MesBatchExecutionProvisionCommand().setCompletionBackfillReceipt(receipt));
        assertEquals(false, task.getRequiredFlag());
    }

    @Test
    void creationFreezesNoLossBeforeFormAndFillTasksAreCreated() {
        var task = loss();
        var command = new MesBatchExecutionProvisionCommand().setCompletionBackfillReceipt(
                new MesCompletionBackfillReceipt().setHasActualLoss(false)
                        .setLossConditionFactsJson("[{\"processId\":201,\"routeProcessId\":101,\"hasActualLoss\":false}]"));
        ReflectionTestUtils.invokeMethod(service, "applyFormalLossRequirement", task, command);
        assertEquals(false, task.getRequiredFlag());
    }

    @Test
    void noLossTaskNeverCreatesACompanionFillTodo() {
        var workService = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(workService, "batchExecutionOriginMapper", origins);
        when(origins.selectListByBatchExecutionId(1L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setHasActualLoss(false)));
        // No routing, permission or assignment dependency is called for an inapplicable task.
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(workService, "createFillTask",
                new MesProEdhrBatchExecutionDO().setId(1L), loss(), null, null));
    }

    @Test
    void absentFormalDecisionStillBlocksNextRecord() {
        formalDecision(null);
        var loss = loss();
        var next = loss().setId(3L).setRequiredPolicy("REQUIRED").setFormSlotType("MAIN").setBatchRecordSort(3);
        Object gate = ReflectionTestUtils.invokeMethod(service, "resolveTaskGate", next, List.of(loss, next));
        assertEquals(false, ReflectionTestUtils.invokeMethod(gate, "available"));
    }

    @Test
    void noFormalLossDoesNotBlockNextRecord() {
        formalDecision(false);
        var loss = loss();
        var next = loss().setId(3L).setRequiredPolicy("REQUIRED").setFormSlotType("MAIN").setBatchRecordSort(3);
        Object gate = ReflectionTestUtils.invokeMethod(service, "resolveTaskGate", next, List.of(loss, next));
        assertEquals(true, ReflectionTestUtils.invokeMethod(gate, "available"));
    }

    @Test
    void noFormalLossDoesNotDiluteCompletedProgress() {
        formalDecision(false);
        ReflectionTestUtils.setField(service, "batchTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "batchExecutionMapper", mock(MesProEdhrBatchExecutionMapper.class));
        var main = loss().setId(3L).setRequiredPolicy("REQUIRED").setFormSlotType("MAIN")
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED);
        when(tasks.selectListByBatchExecutionId(1L)).thenReturn(List.of(loss(), main));
        var batch = new MesProEdhrBatchExecutionDO().setId(1L);
        ReflectionTestUtils.invokeMethod(service, "syncBatchStatus", batch);
        assertEquals(MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE, batch.getStatus());
        assertEquals(1, batch.getTaskTotal());
        assertEquals(1, batch.getTaskApprovedCount());
    }
}
