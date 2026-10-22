package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.Execution;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_TARGET_NODE_NOT_EXISTS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmTaskServiceImplTriggerTaskTest extends BaseMockitoUnitTest {

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private ExecutionQuery executionQuery;
    @Mock
    private Execution execution;
    @InjectMocks
    private BpmTaskServiceImpl service;

    @Test
    void triggerTask_missingExecutionFailsWithTargetNodeError() {
        stubExecutionQuery(List.of());

        assertServiceException(() -> service.triggerTask("process-1", "DISTRIBUTION"),
                TASK_TARGET_NODE_NOT_EXISTS);

        verify(runtimeService, never()).trigger(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void triggerTask_multipleExecutionsFailsWithoutChoosingOne() {
        stubExecutionQuery(List.of(execution, org.mockito.Mockito.mock(Execution.class)));

        assertServiceException(() -> service.triggerTask("process-1", "DISTRIBUTION"),
                TASK_TARGET_NODE_NOT_EXISTS);

        verify(runtimeService, never()).trigger(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void triggerTask_triggersTheUniqueExecution() {
        stubExecutionQuery(List.of(execution));
        when(execution.getTenantId()).thenReturn(null);
        when(execution.getId()).thenReturn("execution-1");

        service.triggerTask("process-1", "DISTRIBUTION");

        verify(runtimeService).trigger("execution-1");
    }

    @Test
    void triggerTask_propagatesFlowableTriggerFailure() {
        stubExecutionQuery(List.of(execution));
        when(execution.getTenantId()).thenReturn(null);
        when(execution.getId()).thenReturn("execution-1");
        IllegalStateException failure = new IllegalStateException("Flowable trigger failed");
        doThrow(failure).when(runtimeService).trigger("execution-1");

        IllegalStateException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> service.triggerTask("process-1", "DISTRIBUTION"));

        org.junit.jupiter.api.Assertions.assertSame(failure, thrown);
    }

    private void stubExecutionQuery(List<Execution> executions) {
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId("process-1")).thenReturn(executionQuery);
        when(executionQuery.activityId("DISTRIBUTION")).thenReturn(executionQuery);
        when(executionQuery.list()).thenReturn(executions);
    }
}
