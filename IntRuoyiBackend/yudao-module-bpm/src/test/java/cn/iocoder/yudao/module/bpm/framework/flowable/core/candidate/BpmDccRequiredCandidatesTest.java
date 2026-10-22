package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmDccRequiredCandidatesTest {
    private static final List<String> DCC_THREE_WORKFLOW_KEYS = List.of(
            "dcc-controlled-file-upload",
            "dcc-controlled-file-revision",
            "dcc-controlled-file-obsolete"
    );

    @Test
    void dccMultiInstanceCandidateListPreservesDepartmentObligationsWithSameLeader() {
        for (String key : DCC_THREE_WORKFLOW_KEYS) {
            var users = mock(AdminUserApi.class);
            var strategy = mock(BpmTaskCandidateStrategy.class);
            when(strategy.getStrategy()).thenReturn(BpmTaskCandidateStrategyEnum.START_USER_SELECT);
            var invoker = new BpmTaskCandidateInvoker(List.of(strategy), users);
            var execution = mock(DelegateExecution.class);
            var task = new UserTask();
            when(execution.getCurrentFlowElement()).thenReturn(task);
            when(execution.getCurrentActivityId()).thenReturn("MATRIX_REVIEW");
            when(execution.getProcessInstanceId()).thenReturn("instance-" + key);
            var processService = mock(BpmProcessInstanceService.class);
            var instance = mock(ProcessInstance.class);
            when(processService.getProcessInstance("instance-" + key)).thenReturn(instance);
            when(instance.getProcessDefinitionKey()).thenReturn(key);
            when(instance.getStartUserId()).thenReturn("9");
            when(instance.getProcessVariables()).thenReturn(Map.of(
                    BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                    Map.of("MATRIX_REVIEW", List.of(901L, 901L))
            ));
            Set<Long> confirmed = new LinkedHashSet<>(List.of(901L));
            when(strategy.calculateUsersByTask(execution, null)).thenReturn(confirmed);
            when(users.getUserMap(confirmed)).thenReturn(Map.of(901L,
                    new AdminUserRespDTO().setId(901L).setStatus(0)));
            try (var spring = mockStatic(SpringUtil.class); var model = mockStatic(BpmnModelUtils.class)) {
                spring.when(() -> SpringUtil.getBean(BpmProcessInstanceService.class)).thenReturn(processService);
                model.when(() -> BpmnModelUtils.parseCandidateStrategy(task))
                        .thenReturn(BpmTaskCandidateStrategyEnum.START_USER_SELECT.getStrategy());
                model.when(() -> BpmnModelUtils.parseCandidateParam(task)).thenReturn(null);
                assertEquals(List.of(901L, 901L), invoker.calculateUserListByTask(execution), key);
            }
        }
    }

    @Test
    void disabledRequiredUserBlocksTaskGenerationRatherThanShrinkingConfirmedList() {
        var users = mock(AdminUserApi.class);
        var strategy = mock(BpmTaskCandidateStrategy.class);
        when(strategy.getStrategy()).thenReturn(BpmTaskCandidateStrategyEnum.USER);
        var invoker = new BpmTaskCandidateInvoker(List.of(strategy), users);
        var execution = mock(DelegateExecution.class);
        var task = new UserTask();
        when(execution.getCurrentFlowElement()).thenReturn(task);
        when(execution.getProcessInstanceId()).thenReturn("instance-1");
        var processService = mock(BpmProcessInstanceService.class);
        var instance = mock(ProcessInstance.class);
        when(processService.getProcessInstance("instance-1")).thenReturn(instance);
        when(instance.getProcessDefinitionKey()).thenReturn("dcc-controlled-file-approval");
        when(instance.getStartUserId()).thenReturn("9");
        Set<Long> confirmed = new LinkedHashSet<>(List.of(1L, 2L));
        when(strategy.calculateUsersByTask(execution, "1,2")).thenReturn(confirmed);
        when(users.getUserMap(confirmed)).thenReturn(Map.of(1L, new AdminUserRespDTO().setId(1L).setStatus(0),
                2L, new AdminUserRespDTO().setId(2L).setStatus(1)));
        try (var spring = mockStatic(SpringUtil.class); var model = mockStatic(BpmnModelUtils.class)) {
            spring.when(() -> SpringUtil.getBean(BpmProcessInstanceService.class)).thenReturn(processService);
            model.when(() -> BpmnModelUtils.parseCandidateStrategy(task)).thenReturn(BpmTaskCandidateStrategyEnum.USER.getStrategy());
            model.when(() -> BpmnModelUtils.parseCandidateParam(task)).thenReturn("1,2");
            assertThrows(ServiceException.class, () -> invoker.calculateUsersByTask(execution));
            assertEquals(Set.of(1L, 2L), confirmed);
        }
    }

    @Test
    void disabledRequiredUserBlocksTaskGenerationForThreeWorkflowKeys() {
        for (String key : DCC_THREE_WORKFLOW_KEYS) {
            var users = mock(AdminUserApi.class);
            var strategy = mock(BpmTaskCandidateStrategy.class);
            when(strategy.getStrategy()).thenReturn(BpmTaskCandidateStrategyEnum.USER);
            var invoker = new BpmTaskCandidateInvoker(List.of(strategy), users);
            var execution = mock(DelegateExecution.class);
            var task = new UserTask();
            when(execution.getCurrentFlowElement()).thenReturn(task);
            when(execution.getProcessInstanceId()).thenReturn("instance-" + key);
            var processService = mock(BpmProcessInstanceService.class);
            var instance = mock(ProcessInstance.class);
            when(processService.getProcessInstance("instance-" + key)).thenReturn(instance);
            when(instance.getProcessDefinitionKey()).thenReturn(key);
            when(instance.getStartUserId()).thenReturn("9");
            Set<Long> confirmed = new LinkedHashSet<>(List.of(1L, 2L));
            when(strategy.calculateUsersByTask(execution, "1,2")).thenReturn(confirmed);
            when(users.getUserMap(confirmed)).thenReturn(Map.of(1L, new AdminUserRespDTO().setId(1L).setStatus(0),
                    2L, new AdminUserRespDTO().setId(2L).setStatus(1)));
            try (var spring = mockStatic(SpringUtil.class); var model = mockStatic(BpmnModelUtils.class)) {
                spring.when(() -> SpringUtil.getBean(BpmProcessInstanceService.class)).thenReturn(processService);
                model.when(() -> BpmnModelUtils.parseCandidateStrategy(task)).thenReturn(BpmTaskCandidateStrategyEnum.USER.getStrategy());
                model.when(() -> BpmnModelUtils.parseCandidateParam(task)).thenReturn("1,2");
                assertThrows(ServiceException.class, () -> invoker.calculateUsersByTask(execution), key);
                assertEquals(Set.of(1L, 2L), confirmed, key);
            }
        }
    }

    @Test
    void previewAlsoRejectsDisabledRequiredUser() {
        var users = mock(AdminUserApi.class);
        var strategy = mock(BpmTaskCandidateStrategy.class);
        when(strategy.getStrategy()).thenReturn(BpmTaskCandidateStrategyEnum.USER);
        var invoker = new BpmTaskCandidateInvoker(List.of(strategy), users);
        var model = new BpmnModel();
        var process = new org.flowable.bpmn.model.Process();
        process.setId("dcc-controlled-file-approval");
        var task = new UserTask(); task.setId("MATRIX_REVIEW"); process.addFlowElement(task); model.addProcess(process);
        Set<Long> confirmed = new LinkedHashSet<>(List.of(1L, 2L));
        when(strategy.calculateUsersByActivity(eq(model), eq("MATRIX_REVIEW"), eq("1,2"), eq(9L), eq("def"), anyMap()))
                .thenReturn(confirmed);
        when(users.getUserMap(confirmed)).thenReturn(Map.of(1L, new AdminUserRespDTO().setId(1L).setStatus(0),
                2L, new AdminUserRespDTO().setId(2L).setStatus(1)));
        try (var utils = mockStatic(BpmnModelUtils.class)) {
            utils.when(() -> BpmnModelUtils.getFlowElementById(model, "MATRIX_REVIEW")).thenReturn(task);
            utils.when(() -> BpmnModelUtils.parseCandidateStrategy(task)).thenReturn(BpmTaskCandidateStrategyEnum.USER.getStrategy());
            utils.when(() -> BpmnModelUtils.parseCandidateParam(task)).thenReturn("1,2");
            assertThrows(ServiceException.class, () -> invoker.calculateUsersByActivity(model, "MATRIX_REVIEW", 9L, "def", Map.of()));
            assertEquals(Set.of(1L, 2L), confirmed);
        }
    }

    @Test
    void previewAlsoRejectsDisabledRequiredUserForThreeWorkflowKeys() {
        for (String key : DCC_THREE_WORKFLOW_KEYS) {
            var users = mock(AdminUserApi.class);
            var strategy = mock(BpmTaskCandidateStrategy.class);
            when(strategy.getStrategy()).thenReturn(BpmTaskCandidateStrategyEnum.USER);
            var invoker = new BpmTaskCandidateInvoker(List.of(strategy), users);
            var model = new BpmnModel();
            var process = new org.flowable.bpmn.model.Process();
            process.setId(key);
            var task = new UserTask();
            task.setId("MATRIX_REVIEW");
            process.addFlowElement(task);
            model.addProcess(process);
            Set<Long> confirmed = new LinkedHashSet<>(List.of(1L, 2L));
            when(strategy.calculateUsersByActivity(eq(model), eq("MATRIX_REVIEW"), eq("1,2"), eq(9L), eq("def"), anyMap()))
                    .thenReturn(confirmed);
            when(users.getUserMap(confirmed)).thenReturn(Map.of(1L, new AdminUserRespDTO().setId(1L).setStatus(0),
                    2L, new AdminUserRespDTO().setId(2L).setStatus(1)));
            try (var utils = mockStatic(BpmnModelUtils.class)) {
                utils.when(() -> BpmnModelUtils.getFlowElementById(model, "MATRIX_REVIEW")).thenReturn(task);
                utils.when(() -> BpmnModelUtils.parseCandidateStrategy(task)).thenReturn(BpmTaskCandidateStrategyEnum.USER.getStrategy());
                utils.when(() -> BpmnModelUtils.parseCandidateParam(task)).thenReturn("1,2");
                assertThrows(ServiceException.class, () -> invoker.calculateUsersByActivity(model, "MATRIX_REVIEW", 9L, "def", Map.of()), key);
                assertEquals(Set.of(1L, 2L), confirmed, key);
            }
        }
    }
}
