package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BpmProcessInstanceServiceImplTest {

    private static final String PROCESS_INSTANCE_ID = "route-version-process-122-v5";

    private BpmProcessInstanceServiceImpl processInstanceService;

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private BpmMessageService messageService;
    @Mock
    private BpmProcessDefinitionService processDefinitionService;
    @Mock
    private BpmTaskService taskService;
    @Mock
    private BpmProcessInstanceEventPublisher processInstanceEventPublisher;
    @Mock
    private ProcessInstance processInstance;
    @Mock
    private ProcessInstanceQuery processInstanceQuery;
    @Mock
    private ProcessInstanceQuery childProcessInstanceQuery;

    @BeforeEach
    void setUp() {
        processInstanceService = new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(processInstanceService, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(processInstanceService, "messageService", messageService);
        ReflectionTestUtils.setField(processInstanceService, "processDefinitionService", processDefinitionService);
        ReflectionTestUtils.setField(processInstanceService, "taskService", taskService);
        ReflectionTestUtils.setField(processInstanceService, "processInstanceEventPublisher", processInstanceEventPublisher);
    }

    @Test
    void processProcessInstanceCompleted_shouldPublishLastApproverFromRuntimeVariables() {
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        when(processInstance.getProcessDefinitionKey()).thenReturn("mes_pro_route_version_approval");
        when(processInstance.getProcessDefinitionId()).thenReturn("route-version-def");
        when(processInstance.getBusinessKey()).thenReturn("122");
        when(processInstance.getName()).thenReturn("route version approval");
        when(processInstance.getStartUserId()).thenReturn("149");
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.RUNNING.getStatus()));
        when(runtimeService.getVariables(PROCESS_INSTANCE_ID)).thenReturn(Map.of(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.APPROVE.getStatus(),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,
                1L));

        processInstanceService.processProcessInstanceCompleted(processInstance);

        ArgumentCaptor<BpmProcessInstanceStatusEvent> eventCaptor =
                ArgumentCaptor.forClass(BpmProcessInstanceStatusEvent.class);
        verify(processInstanceEventPublisher).sendProcessInstanceResultEvent(eventCaptor.capture());
        BpmProcessInstanceStatusEvent event = eventCaptor.getValue();
        assertEquals(PROCESS_INSTANCE_ID, event.getId());
        assertEquals(BpmProcessInstanceStatusEnum.APPROVE.getStatus(), event.getStatus());
        assertEquals(1L, event.getActorUserId());
    }

    @Test
    void cancelProcessInstanceByStartUser_shouldPersistCancelActorForBusinessApprovalEvent() {
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery, childProcessInstanceQuery);
        when(processInstanceQuery.includeProcessVariables()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getStartUserId()).thenReturn("149");
        when(processInstance.getProcessDefinitionId()).thenReturn("route-version-def");
        when(processDefinitionService.getProcessDefinitionInfo("route-version-def"))
                .thenReturn(new BpmProcessDefinitionInfoDO());
        when(processInstance.getSuperExecutionId()).thenReturn(null);
        when(childProcessInstanceQuery.superProcessInstanceId(PROCESS_INSTANCE_ID))
                .thenReturn(childProcessInstanceQuery);
        when(childProcessInstanceQuery.list()).thenReturn(List.of());

        BpmProcessInstanceCancelReqVO reqVO = new BpmProcessInstanceCancelReqVO();
        reqVO.setId(PROCESS_INSTANCE_ID);
        reqVO.setReason("withdraw route publish");

        processInstanceService.cancelProcessInstanceByStartUser(149L, reqVO);

        verify(runtimeService).setVariable(PROCESS_INSTANCE_ID,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.CANCEL.getStatus());
        verify(runtimeService).setVariable(PROCESS_INSTANCE_ID,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID, 149L);
        verify(taskService).moveTaskToEnd(eq(PROCESS_INSTANCE_ID), anyString());
    }
}
