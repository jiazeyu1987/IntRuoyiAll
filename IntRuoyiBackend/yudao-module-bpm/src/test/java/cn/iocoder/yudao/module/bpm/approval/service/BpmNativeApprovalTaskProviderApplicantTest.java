package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceCopyService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmNativeApprovalTaskProviderApplicantTest {

    private BpmProcessInstanceService processInstanceService;
    private BpmTaskService taskService;
    private BpmNativeApprovalTaskProvider provider;

    @BeforeEach
    void setUp() {
        processInstanceService = mock(BpmProcessInstanceService.class);
        taskService = mock(BpmTaskService.class);
        provider = new BpmNativeApprovalTaskProvider(
                processInstanceService,
                mock(BpmProcessInstanceCopyService.class),
                taskService,
                mock(org.flowable.engine.TaskService.class),
                mock(PermissionApi.class),
                mock(RoleApi.class));
    }

    @Test
    void pageTodoUsesRuntimeProcessInitiator() {
        Task task = todoTask("task-todo-applicant", "pi-todo-applicant");
        when(taskService.getTaskTodoPage(eq(100L), any()))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn("151");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-todo-applicant")))
                .thenReturn(Map.of("pi-todo-applicant", processInstance));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals(151L, summary.getInitiatorUserId());
    }

    @Test
    void pageTodoUsesFormalProcessStartUserVariableWhenRuntimeStartUserIsBlank() {
        Task task = todoTask("task-todo-applicant-variable", "pi-todo-applicant-variable");
        when(taskService.getTaskTodoPage(eq(100L), any()))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getStartUserId()).thenReturn(null);
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, 151L));
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-todo-applicant-variable")))
                .thenReturn(Map.of("pi-todo-applicant-variable", processInstance));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals(151L, summary.getInitiatorUserId());
    }

    @Test
    void pageDoneUsesHistoricProcessInitiator() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("task-done-applicant");
        when(task.getName()).thenReturn("已办审批");
        when(task.getTaskDefinitionKey()).thenReturn("approveTask");
        when(task.getProcessInstanceId()).thenReturn("pi-done-applicant");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));
        when(taskService.getTaskDonePage(eq(100L), any()))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getId()).thenReturn("pi-done-applicant");
        when(processInstance.getName()).thenReturn("已办审批");
        when(processInstance.getStartUserId()).thenReturn("151");
        when(processInstance.getProcessVariables()).thenReturn(Map.of());
        when(processInstanceService.getHistoricProcessInstances(Set.of("pi-done-applicant")))
                .thenReturn(List.of(processInstance));

        ApprovalTaskSummary summary = provider.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.BPM, null, 1, 10)).getList().get(0);

        assertEquals(151L, summary.getInitiatorUserId());
    }

    @Test
    void pageTodoFailsWhenRuntimeProcessInstanceIsMissing() {
        Task task = todoTask("task-todo-missing", "pi-todo-missing");
        when(taskService.getTaskTodoPage(eq(100L), any()))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-todo-missing")))
                .thenReturn(Map.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.page(ApprovalTaskQueryContext.of(100L,
                        ApprovalTaskViewType.TODO, ApprovalModuleCode.BPM, null, 1, 10)));

        assertEquals("APPROVAL_PROCESS_INSTANCE_REQUIRED: BPM todo pi-todo-missing", exception.getMessage());
    }

    private static Task todoTask(String taskId, String processInstanceId) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(taskId);
        when(task.getName()).thenReturn("待办审批");
        when(task.getTaskDefinitionKey()).thenReturn("approveTask");
        when(task.getProcessInstanceId()).thenReturn(processInstanceId);
        when(task.getAssignee()).thenReturn("910272");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getProcessVariables()).thenReturn(Map.of());
        return task;
    }
}
