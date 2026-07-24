package cn.iocoder.yudao.module.bpm.convert.task;

import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmTaskConvertTest {

    @Test
    void convert_taskAssignedMessageCarriesProcessDefinitionKey() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceId()).thenReturn("proc-1");
        when(processInstance.getName()).thenReturn("DCC流程");
        when(processInstance.getProcessDefinitionKey()).thenReturn("dcc-controlled-file-approval");
        AdminUserRespDTO startUser = new AdminUserRespDTO()
                .setId(99L)
                .setNickname("提交人");
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("文控审核");
        when(task.getAssignee()).thenReturn("100");

        BpmMessageSendWhenTaskCreatedReqDTO reqDTO = BpmTaskConvert.INSTANCE.convert(processInstance, startUser, task);

        assertEquals("dcc-controlled-file-approval", reqDTO.getProcessDefinitionKey());
        assertEquals(100L, reqDTO.getAssigneeUserId());
    }
}
