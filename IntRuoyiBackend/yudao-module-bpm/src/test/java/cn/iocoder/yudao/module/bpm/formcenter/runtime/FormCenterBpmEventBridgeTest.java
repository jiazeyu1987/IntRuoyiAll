package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessApprovedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessCancelledReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmProcessRejectedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCompletedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormBpmTaskCreatedReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormCenterBpmEventBridgeTest {

    @Mock
    private FormActionInstanceMapper actionInstanceMapper;
    @Mock
    private FormCenterRuntimeService runtimeService;
    @Mock
    private Task task;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void taskAssignedForFormCenterProcessPersistsDerivedPermission() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());
        when(task.getProcessInstanceId()).thenReturn("PI-1001");
        when(task.getId()).thenReturn("TASK-1");
        when(task.getAssignee()).thenReturn("101");

        bridge.onTaskAssigned(task);

        ArgumentCaptor<FormBpmTaskCreatedReqVO> captor = ArgumentCaptor.forClass(FormBpmTaskCreatedReqVO.class);
        verify(runtimeService).onBpmTaskCreated(captor.capture());
        assertEquals("PI-1001", captor.getValue().getProcessInstanceId());
        assertEquals("TASK-1", captor.getValue().getTaskId());
        assertEquals(List.of(101L), captor.getValue().getHandlerUserIds());
    }

    @Test
    void taskCreatedWithoutHandlerWaitsForAssignmentEvent() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());
        when(task.getProcessInstanceId()).thenReturn("PI-1001");

        bridge.onTaskCreated(task);

        verify(runtimeService, never()).onBpmTaskCreated(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void taskCompletedForFormCenterProcessRevokesCurrentTaskPermissions() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());
        when(task.getProcessInstanceId()).thenReturn("PI-1001");
        when(task.getId()).thenReturn("TASK-1");

        bridge.onTaskCompleted(task);

        ArgumentCaptor<FormBpmTaskCompletedReqVO> captor = ArgumentCaptor.forClass(FormBpmTaskCompletedReqVO.class);
        verify(runtimeService).onBpmTaskCompleted(captor.capture());
        assertEquals("PI-1001", captor.getValue().getProcessInstanceId());
        assertEquals("TASK-1", captor.getValue().getTaskId());
    }

    @Test
    void approvedProcessEventForFormCenterProcessAppliesBusinessEffect() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());

        bridge.onProcessInstanceStatusChanged(event(BpmProcessInstanceStatusEnum.APPROVE));

        ArgumentCaptor<FormBpmProcessApprovedReqVO> captor = ArgumentCaptor.forClass(FormBpmProcessApprovedReqVO.class);
        verify(runtimeService).onBpmProcessApproved(captor.capture());
        assertEquals("PI-1001", captor.getValue().getProcessInstanceId());
    }

    @Test
    void rejectedProcessEventForFormCenterProcessRejectsInstance() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());

        bridge.onProcessInstanceStatusChanged(event(BpmProcessInstanceStatusEnum.REJECT));

        ArgumentCaptor<FormBpmProcessRejectedReqVO> captor = ArgumentCaptor.forClass(FormBpmProcessRejectedReqVO.class);
        verify(runtimeService).onBpmProcessRejected(captor.capture());
        assertEquals("PI-1001", captor.getValue().getProcessInstanceId());
    }

    @Test
    void cancelledProcessEventForFormCenterProcessAbandonsInstance() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(instance());

        bridge.onProcessInstanceStatusChanged(event(BpmProcessInstanceStatusEnum.CANCEL));

        ArgumentCaptor<FormBpmProcessCancelledReqVO> captor =
                ArgumentCaptor.forClass(FormBpmProcessCancelledReqVO.class);
        verify(runtimeService).onBpmProcessCancelled(captor.capture());
        assertEquals("PI-1001", captor.getValue().getProcessInstanceId());
    }

    @Test
    void nonFormCenterProcessIsIgnored() {
        TenantContextHolder.setTenantId(122L);
        FormCenterBpmEventBridge bridge = new FormCenterBpmEventBridge(actionInstanceMapper, runtimeService);
        when(actionInstanceMapper.selectByProcessInstanceId(122L, "PI-1001")).thenReturn(null);
        when(task.getProcessInstanceId()).thenReturn("PI-1001");

        bridge.onTaskCompleted(task);

        verify(runtimeService, never()).onBpmTaskCompleted(org.mockito.ArgumentMatchers.any());
    }

    private static BpmProcessInstanceStatusEvent event(BpmProcessInstanceStatusEnum status) {
        return new BpmProcessInstanceStatusEvent("test")
                .setId("PI-1001")
                .setStatus(status.getStatus())
                .setProcessDefinitionKey("form-change-approval")
                .setBusinessKey("FORM_ACTION:10");
    }

    private static FormActionInstanceDO instance() {
        return FormActionInstanceDO.builder()
                .id(10L)
                .tenantId(122L)
                .bpmProcessInstanceId("PI-1001")
                .build();
    }
}
