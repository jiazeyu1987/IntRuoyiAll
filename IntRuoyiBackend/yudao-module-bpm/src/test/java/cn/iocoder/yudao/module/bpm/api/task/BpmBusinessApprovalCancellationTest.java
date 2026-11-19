package cn.iocoder.yudao.module.bpm.api.task;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.*;
import cn.iocoder.yudao.module.bpm.businessapproval.service.BusinessApprovalRequestStore;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BpmBusinessApprovalCancellationTest {
    private static final String EXECUTOR = "MES_ROUTE_VERSION_PUBLISH";
    private final BpmProcessInstanceApiImpl api = new BpmProcessInstanceApiImpl();
    private final BusinessApprovalRequestStore store = mock(BusinessApprovalRequestStore.class);
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final ProcessInstance instance = mock(ProcessInstance.class);
    private final BpmTaskService tasks = mock(BpmTaskService.class);
    private BusinessApprovalRequest request;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        var service = new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "taskService", tasks);
        ReflectionTestUtils.setField(api, "processInstanceService", service);
        ReflectionTestUtils.setField(api, "businessApprovalRequestStore", store);
        var query = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        when(runtime.createProcessInstanceQuery()).thenReturn(query);
        when(query.singleResult()).thenReturn(instance);
        when(query.list()).thenReturn(List.of());
        when(instance.getTenantId()).thenReturn("1");
        when(instance.getBusinessKey()).thenReturn("BUSINESS_APPROVAL:11");
        when(instance.getProcessDefinitionKey()).thenReturn("mes-route-version-approval-v1");
        when(instance.getStartUserId()).thenReturn("7");
        request = BusinessApprovalRequest.builder().requestId(11L).tenantId(1L)
                .effectExecutorCode(EXECUTOR).processInstanceId("bpm-11")
                .processDefinitionKey("mes-route-version-approval-v1").status(BusinessApprovalRequestStatus.PENDING_BPM)
                .context(BusinessApprovalContext.builder().tenantId(1L).objectId("101")
                        .objectType("ROUTE_VERSION").actionCode("PUBLISH").applicantUserId(7L).build()).build();
        when(store.findByProcessInstanceId("bpm-11")).thenAnswer(c -> Optional.of(request));
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @Test void anotherReviewerCanInvalidateLinkedApprovalAndKeepsTheirOwnActorIdentity() {
        cancel();
        verify(runtime).setVariable("bpm-11", BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.CANCEL.getStatus());
        verify(runtime).setVariable("bpm-11", BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID, 9L);
        verify(tasks).moveTaskToEnd("bpm-11", "业务联动作废：工艺修改整批作废");
        verify(runtime, never()).setVariable(anyString(), eq(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID), eq(7L));
    }

    @ParameterizedTest @ValueSource(strings = {"tenant", "contextTenant", "executor", "object", "process", "terminal"})
    void mismatchedApprovalRequestCannotCancelAnyProcess(String field) {
        request = switch (field) {
            case "tenant" -> request.toBuilder().tenantId(2L).build();
            case "contextTenant" -> request.toBuilder().context(BusinessApprovalContext.builder().tenantId(2L).objectId("101").build()).build();
            case "executor" -> request.toBuilder().effectExecutorCode("OTHER").build();
            case "object" -> request.toBuilder().context(BusinessApprovalContext.builder().tenantId(1L).objectId("999").build()).build();
            case "process" -> request.toBuilder().processInstanceId("another-process").build();
            case "terminal" -> request.toBuilder().status(BusinessApprovalRequestStatus.APPROVED).build();
            default -> throw new AssertionError(field);
        };
        assertThrows(IllegalStateException.class, this::cancel);
        verifyNoInteractions(runtime, tasks);
    }

    @ParameterizedTest @ValueSource(strings = {"tenant", "businessKey", "definition", "child"})
    void mismatchedRunningInstanceCannotBeInvalidated(String field) {
        switch (field) {
            case "tenant" -> when(instance.getTenantId()).thenReturn("2");
            case "businessKey" -> when(instance.getBusinessKey()).thenReturn("BUSINESS_APPROVAL:999");
            case "definition" -> when(instance.getProcessDefinitionKey()).thenReturn("different-model");
            case "child" -> when(instance.getSuperExecutionId()).thenReturn("parent-execution");
            default -> throw new AssertionError(field);
        }
        assertThrows(IllegalStateException.class, this::cancel);
        verify(runtime, never()).setVariable(anyString(), anyString(), any());
        verifyNoInteractions(tasks);
    }

    @Test void engineFailureIsPropagatedToTheGroupTransaction() {
        doThrow(new IllegalStateException("engine failure")).when(tasks).moveTaskToEnd(anyString(), anyString());
        assertEquals("engine failure", assertThrows(IllegalStateException.class, this::cancel).getMessage());
    }
    private void cancel() { api.cancelBusinessApprovalProcess(9L, "bpm-11", EXECUTOR, "101", "工艺修改整批作废"); }
}
