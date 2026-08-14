package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceBpmnModelViewRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.flowable.bpmn.constants.BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW;

@ExtendWith(MockitoExtension.class)
class BpmProcessInstanceServiceImplTest {

    private static final String PROCESS_INSTANCE_ID = "route-version-process-122-v5";

    private BpmProcessInstanceServiceImpl processInstanceService;

    @Mock
    private RuntimeService runtimeService;
    @Mock
    private HistoryService historyService;
    @Mock
    private BpmMessageService messageService;
    @Mock
    private BpmProcessDefinitionService processDefinitionService;
    @Mock
    private BpmTaskService taskService;
    @Mock
    private BpmProcessInstanceEventPublisher processInstanceEventPublisher;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ProcessInstance processInstance;
    @Mock
    private ProcessInstanceQuery processInstanceQuery;
    @Mock
    private ProcessInstanceQuery childProcessInstanceQuery;
    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @BeforeEach
    void setUp() {
        processInstanceService = new BpmProcessInstanceServiceImpl();
        ReflectionTestUtils.setField(processInstanceService, "runtimeService", runtimeService);
        ReflectionTestUtils.setField(processInstanceService, "historyService", historyService);
        ReflectionTestUtils.setField(processInstanceService, "messageService", messageService);
        ReflectionTestUtils.setField(processInstanceService, "processDefinitionService", processDefinitionService);
        ReflectionTestUtils.setField(processInstanceService, "taskService", taskService);
        ReflectionTestUtils.setField(processInstanceService, "processInstanceEventPublisher", processInstanceEventPublisher);
        ReflectionTestUtils.setField(processInstanceService, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(processInstanceService, "deptApi", deptApi);
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

    @Test
    void getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel() {
        HistoricProcessInstance historicProcessInstance = mock(HistoricProcessInstance.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.includeProcessVariables()).thenReturn(historicProcessInstanceQuery);
        when(historicProcessInstanceQuery.singleResult()).thenReturn(historicProcessInstance);
        when(historicProcessInstance.getProcessDefinitionId()).thenReturn("definition-1");
        when(historicProcessInstance.getStartUserId()).thenReturn("1");
        when(historicProcessInstance.getProcessVariables()).thenReturn(Map.of(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.RUNNING.getStatus()));

        BpmnModel bpmnModel = buildVisibleBpmnModel();
        List<HistoricActivityInstance> activities = List.of(
                mockHistoricActivity("task_present", "userTask", true),
                mockHistoricActivity("task_missing_done", "userTask", true),
                mockHistoricActivity("task_missing_running", "userTask", false),
                mockHistoricActivity("flow_present", ELEMENT_SEQUENCE_FLOW, true),
                mockHistoricActivity("flow_missing_done", ELEMENT_SEQUENCE_FLOW, true));

        when(processDefinitionService.getProcessDefinitionBpmnModel("definition-1")).thenReturn(bpmnModel);
        when(taskService.getActivityListByProcessInstanceId(PROCESS_INSTANCE_ID)).thenReturn(activities);
        when(taskService.getTaskListByProcessInstanceId(PROCESS_INSTANCE_ID, true)).thenReturn(List.of());
        AdminUserRespDTO startUser = new AdminUserRespDTO();
        startUser.setId(1L);
        startUser.setDeptId(10L);
        when(adminUserApi.getUserMap(anySet())).thenReturn(Map.of(1L, startUser));
        when(deptApi.getDeptMap(anySet())).thenReturn(Map.of());

        BpmProcessInstanceBpmnModelViewRespVO result =
                processInstanceService.getProcessInstanceBpmnModelView(PROCESS_INSTANCE_ID);

        assertTrue(result.getFinishedTaskActivityIds().contains("task_present"));
        assertFalse(result.getFinishedTaskActivityIds().contains("task_missing_done"));
        assertFalse(result.getUnfinishedTaskActivityIds().contains("task_missing_running"));
        assertTrue(result.getFinishedSequenceFlowActivityIds().contains("flow_present"));
        assertFalse(result.getFinishedSequenceFlowActivityIds().contains("flow_missing_done"));
    }

    private static HistoricActivityInstance mockHistoricActivity(String activityId, String activityType,
                                                                 boolean finished) {
        HistoricActivityInstance activity = mock(HistoricActivityInstance.class);
        when(activity.getActivityId()).thenReturn(activityId);
        when(activity.getEndTime()).thenReturn(finished ? new java.util.Date(1704067200000L) : null);
        if (finished) {
            when(activity.getActivityType()).thenReturn(activityType);
        }
        return activity;
    }

    private static BpmnModel buildVisibleBpmnModel() {
        BpmnModel model = new BpmnModel();
        Process process = new Process();
        process.setId("process");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start_present");
        UserTask userTask = new UserTask();
        userTask.setId("task_present");
        EndEvent endEvent = new EndEvent();
        endEvent.setId("end_present");
        SequenceFlow sequenceFlow = new SequenceFlow();
        sequenceFlow.setId("flow_present");
        sequenceFlow.setSourceRef(startEvent.getId());
        sequenceFlow.setTargetRef(userTask.getId());

        process.addFlowElement(startEvent);
        process.addFlowElement(userTask);
        process.addFlowElement(endEvent);
        process.addFlowElement(sequenceFlow);
        process.setInitialFlowElement(startEvent);
        model.addProcess(process);
        return model;
    }
}
