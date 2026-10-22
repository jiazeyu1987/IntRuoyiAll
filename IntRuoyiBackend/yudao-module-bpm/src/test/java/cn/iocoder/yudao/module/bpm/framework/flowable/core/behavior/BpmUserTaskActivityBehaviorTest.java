package cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmUserTaskActivityBehaviorTest {

    @Test
    void recordDccTaskObligationLocalVariables_usesMultiInstanceLoopCounter() {
        BpmUserTaskActivityBehavior behavior = new BpmUserTaskActivityBehavior(new UserTask());
        TaskEntity task = mock(TaskEntity.class);
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getCurrentActivityId()).thenReturn("MATRIX_REVIEW");
        when(execution.getVariableLocal("loopCounter", Integer.class)).thenReturn(1);
        when(execution.getVariable(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS,
                Map.class)).thenReturn(Map.of("MATRIX_REVIEW", List.of(
                "900:MATRIX_REVIEW:51",
                "900:MATRIX_REVIEW:52")));

        behavior.recordDccTaskObligationLocalVariables(task, execution);

        verify(task).setVariableLocal(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,
                "900:MATRIX_REVIEW:52");
        verify(task).setVariableLocal(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_INDEX, 1);
    }
}
