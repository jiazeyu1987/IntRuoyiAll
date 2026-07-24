package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DccControlledFileStatusListenerTest {

    @Mock
    private DccControlledFileFinalizationService finalizationService;
    @InjectMocks
    private DccControlledFileStatusListener listener;

    @Test
    void onApplicationEventIgnoresFormCenterBusinessActionProcess() {
        BpmProcessInstanceStatusEvent event = event("FORM_ACTION:FCI-122-1");

        listener.onApplicationEvent(event);

        verify(finalizationService, never()).handleProcessInstanceStatusChanged(event);
    }

    @Test
    void onApplicationEventDelegatesNativeDccControlledFileProcess() {
        BpmProcessInstanceStatusEvent event = event("2054545668044046252");

        listener.onApplicationEvent(event);

        verify(finalizationService).handleProcessInstanceStatusChanged(event);
    }

    private static BpmProcessInstanceStatusEvent event(String businessKey) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent("test");
        event.setProcessDefinitionKey(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        event.setBusinessKey(businessKey);
        event.setId("process-1");
        return event;
    }
}
