package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledContentAdapter;
import cn.iocoder.yudao.module.dcc.service.file.DccExternalFileReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccExternalFileReviewStatusListenerTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccExternalFileReviewServiceImpl externalFileReviewService;
    @Mock
    private DccControlledContentAdapter platformAdapter;
    @InjectMocks
    private DccExternalFileReviewStatusListener listener;

    @Test
    void approvedExternalReviewEntersReadyToPublishInsteadOfLegacyApproved() {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent("test");
        event.setProcessDefinitionKey(DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        event.setBusinessKey("2054545668044083986");
        event.setId("external-process-1");
        event.setActorUserId(1L);
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(2054545668044083986L)
                .masterId(2054545668044083000L)
                .status(DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus())
                .processInstanceId("external-process-1")
                .build();
        when(controlledFileMapper.selectById(2054545668044083986L)).thenReturn(file);

        listener.onApplicationEvent(event);

        ArgumentCaptor<DccControlledFileDO> updateCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(updateCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.READY_TO_PUBLISH.getStatus(),
                updateCaptor.getValue().getStatus());
        verify(platformAdapter).recordApprovedReadyToPublish(file, 1L, "external-process-1");
        verify(externalFileReviewService).closeExternalReview(2054545668044083986L);
    }

    @Test
    void approvedExternalReviewMissingFileDoesNotEmitPlatformTransition() {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent("test");
        event.setProcessDefinitionKey(DccExternalFileReviewServiceImpl.BPM_PROCESS_DEFINITION_KEY);
        event.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());
        event.setBusinessKey("2054545668044083986");
        event.setId("external-process-1");
        event.setActorUserId(1L);
        when(controlledFileMapper.selectById(2054545668044083986L)).thenReturn(null);

        listener.onApplicationEvent(event);

        verifyNoInteractions(platformAdapter);
        verifyNoInteractions(externalFileReviewService);
    }
}
