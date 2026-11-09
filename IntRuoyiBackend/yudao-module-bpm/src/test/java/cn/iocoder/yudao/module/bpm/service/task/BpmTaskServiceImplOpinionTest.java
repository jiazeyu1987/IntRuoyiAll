package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmCommentTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import jakarta.validation.Validation;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmTaskServiceImplOpinionTest {

    @ParameterizedTest
    @MethodSource("approvalOpinions")
    void oldRequiredOpinionModelCompletesApprovalAndPersistsNormalizedOpinion(String reason, String expected) {
        Fixture f = new Fixture(false);
        assertTrue(BpmnModelUtils.parseReasonRequire(f.model, "CURRENT"));
        BpmTaskApproveReqVO request = approval(reason);

        f.service.approveTask(1L, request);

        assertEquals(expected, request.getReason());
        verify(f.tasks).setVariableLocal("task", TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus());
        verify(f.tasks).setVariableLocal("task", TASK_VARIABLE_REASON, expected);
        verify(f.tasks).addComment("task", "instance", BpmCommentTypeEnum.APPROVE.getType(),
                BpmCommentTypeEnum.APPROVE.formatComment(expected));
        verify(f.tasks).complete(eq("task"), argThat(variables ->
                Long.valueOf(1L).equals(variables.get(PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID))
                        && Map.of("NEXT", List.of(2L)).equals(variables.get(
                        PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES))), eq(true));
    }

    @Test
    void optionalOpinionDoesNotBypassRequiredSignatureImage() {
        Fixture f = new Fixture(true);
        assertServiceException(() -> f.service.approveTask(1L, approval(null)), TASK_SIGNATURE_NOT_EXISTS);
        verify(f.tasks, never()).setVariableLocal(anyString(), anyString(), any());
        verify(f.tasks, never()).complete(anyString(), anyMap(), anyBoolean());
        verifyNoInteractions(f.runtime);
    }

    @Test
    void optionalOpinionWithSignaturePersistsSignatureAndCompletes() {
        Fixture f = new Fixture(true);
        BpmTaskApproveReqVO request = approval(null);
        request.setSignPicUrl("https://example.test/signature.png");
        f.service.approveTask(1L, request);
        verify(f.tasks).setVariableLocal("task", TASK_SIGN_PIC_URL, request.getSignPicUrl());
        verify(f.tasks).setVariableLocal("task", TASK_VARIABLE_REASON, "");
        verify(f.tasks).complete(eq("task"), anyMap(), eq(true));
    }

    @Test
    void optionalOpinionDoesNotBypassRequiredNextAssignee() {
        Fixture f = new Fixture(false);
        BpmTaskApproveReqVO request = approval("");
        request.setNextAssignees(Map.of());
        assertServiceException(() -> f.service.approveTask(1L, request),
                PROCESS_INSTANCE_APPROVE_USER_SELECT_ASSIGNEES_NOT_CONFIG, "Next approver");
        verify(f.tasks, never()).complete(anyString(), anyMap(), anyBoolean());
        verify(f.runtime, never()).setVariables(anyString(), anyMap());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n", "\u3000", "\u00a0", "\u2007\u202f", "\uFEFF", "\u0000\u001f"})
    void blankRejectReasonFailsBeforeTaskLookupOrAnyWrite(String reason) {
        Fixture f = new Fixture(false);
        BpmTaskRejectReqVO request = rejection(reason);
        assertServiceException(() -> f.service.rejectTask(1L, request), TASK_REASON_REQUIRE);
        verifyNoInteractions(f.tasks, f.runtime, f.instances, f.models);
    }

    @Test
    void nonemptyRejectReasonPersistsAndContinuesRejection() {
        Fixture f = new Fixture(false);
        when(f.query.list()).thenReturn(List.of());
        f.service.rejectTask(1L, rejection("需要修订"));
        verify(f.tasks).setVariableLocal("task", TASK_VARIABLE_STATUS, BpmTaskStatusEnum.REJECT.getStatus());
        verify(f.tasks).setVariableLocal("task", TASK_VARIABLE_REASON, "需要修订");
        verify(f.tasks).addComment("task", "instance", BpmCommentTypeEnum.REJECT.getType(),
                BpmCommentTypeEnum.REJECT.formatComment("需要修订"));
        verify(f.instances).updateProcessInstanceReject(f.instance, "需要修订");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n", "\u3000", "\u00a0", "\u2007\u202f", "\uFEFF", "\u0000\u001f"})
    void requestValidationAllowsBlankApproveButRejectsBlankReject(String reason) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(approval(reason)).isEmpty());
            assertTrue(validator.validate(rejection(reason)).stream().anyMatch(
                    violation -> violation.getPropertyPath().toString().equals("reason")));
            assertTrue(validator.validate(rejection("需要修订")).isEmpty());
        }
    }

    private static Stream<Arguments> approvalOpinions() {
        return Stream.of(Arguments.of(null, ""), Arguments.of("", ""),
                Arguments.of(" \t\n\u3000\u00a0\uFEFF", ""),
                Arguments.of("\u3000 同意 \u00a0", "同意"));
    }

    private static BpmTaskApproveReqVO approval(String reason) {
        BpmTaskApproveReqVO request = new BpmTaskApproveReqVO();
        request.setId("task");
        request.setReason(reason);
        request.setNextAssignees(Map.of("NEXT", List.of(2L)));
        return request;
    }

    private static BpmTaskRejectReqVO rejection(String reason) {
        BpmTaskRejectReqVO request = new BpmTaskRejectReqVO();
        request.setId("task");
        request.setReason(reason);
        return request;
    }

    private static class Fixture {
        final BpmTaskServiceImpl service = new BpmTaskServiceImpl();
        final TaskService tasks = mock(TaskService.class);
        final TaskQuery query = mock(TaskQuery.class, RETURNS_SELF);
        final RuntimeService runtime = mock(RuntimeService.class);
        final BpmProcessInstanceService instances = mock(BpmProcessInstanceService.class);
        final BpmModelService models = mock(BpmModelService.class);
        final ProcessInstance instance = mock(ProcessInstance.class);
        final BpmnModel model;

        Fixture(boolean signatureRequired) {
            Task task = mock(Task.class);
            when(task.getId()).thenReturn("task");
            when(task.getAssignee()).thenReturn("1");
            when(task.getProcessInstanceId()).thenReturn("instance");
            when(task.getProcessDefinitionId()).thenReturn("ordinary-process:1:definition");
            when(task.getTaskDefinitionKey()).thenReturn("CURRENT");
            when(tasks.createTaskQuery()).thenReturn(query);
            when(query.singleResult()).thenReturn(task);
            when(instances.getProcessInstance("instance")).thenReturn(instance);
            when(instance.getProcessVariables()).thenReturn(Map.of());
            String xml = """
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                        xmlns:flowable="http://flowable.org/bpmn" targetNamespace="test">
                      <process id="ordinary-process" isExecutable="true">
                        <userTask id="CURRENT"><extensionElements>
                          <flowable:reasonRequire>true</flowable:reasonRequire>
                          <flowable:signEnable>%s</flowable:signEnable>
                        </extensionElements></userTask>
                        <userTask id="NEXT" name="Next approver"><extensionElements>
                          <flowable:candidateStrategy>34</flowable:candidateStrategy>
                        </extensionElements></userTask>
                        <sequenceFlow id="toNext" sourceRef="CURRENT" targetRef="NEXT"/>
                      </process>
                    </definitions>
                    """.formatted(signatureRequired);
            model = BpmnModelUtils.getBpmnModel(xml.getBytes(StandardCharsets.UTF_8));
            when(models.getBpmnModelByDefinitionId("ordinary-process:1:definition")).thenReturn(model);
            ReflectionTestUtils.setField(service, "taskService", tasks);
            ReflectionTestUtils.setField(service, "runtimeService", runtime);
            ReflectionTestUtils.setField(service, "processInstanceService", instances);
            ReflectionTestUtils.setField(service, "modelService", models);
            clearInvocations(tasks, query, runtime, instances, models);
        }
    }
}
