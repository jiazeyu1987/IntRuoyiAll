package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
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

    @Test
    void onApplicationEventDelegatesUploadAndRevisionProcesses() {
        BpmProcessInstanceStatusEvent uploadEvent = event(
                DccControlledFileProcessDefinitionKeys.UPLOAD, "2054545668044046252");
        BpmProcessInstanceStatusEvent revisionEvent = event(
                DccControlledFileProcessDefinitionKeys.REVISION, "2054545668044046253");

        listener.onApplicationEvent(uploadEvent);
        listener.onApplicationEvent(revisionEvent);

        verify(finalizationService).handleProcessInstanceStatusChanged(uploadEvent);
        verify(finalizationService).handleProcessInstanceStatusChanged(revisionEvent);
    }

    @Test
    void onApplicationEventIgnoresNativeObsoleteProcessBecauseFormCenterOwnsTheEffect() {
        BpmProcessInstanceStatusEvent event = event(
                DccControlledFileProcessDefinitionKeys.OBSOLETE, "2054545668044046254");

        listener.onApplicationEvent(event);

        verify(finalizationService, never()).handleProcessInstanceStatusChanged(event);
    }

    @Test
    void onApplicationEventIgnoresObsoleteFormCenterProcess() {
        BpmProcessInstanceStatusEvent event = event(
                DccControlledFileProcessDefinitionKeys.OBSOLETE, "FORM_ACTION:FCI-122-2");

        listener.onApplicationEvent(event);

        verify(finalizationService, never()).handleProcessInstanceStatusChanged(event);
    }

    @Test
    void onApplicationEventIgnoresUnrelatedProcess() {
        BpmProcessInstanceStatusEvent event = event("unrelated-process", "2054545668044046252");

        listener.onApplicationEvent(event);

        verify(finalizationService, never()).handleProcessInstanceStatusChanged(event);
    }

    private static BpmProcessInstanceStatusEvent event(String businessKey) {
        return event(DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY, businessKey);
    }

    private static BpmProcessInstanceStatusEvent event(String processDefinitionKey, String businessKey) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent("test");
        event.setProcessDefinitionKey(processDefinitionKey);
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        event.setBusinessKey(businessKey);
        event.setId("process-1");
        return event;
    }
}
