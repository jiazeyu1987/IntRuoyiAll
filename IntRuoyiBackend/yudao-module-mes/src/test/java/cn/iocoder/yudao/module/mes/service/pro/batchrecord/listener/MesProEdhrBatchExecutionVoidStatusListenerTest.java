package cn.iocoder.yudao.module.mes.service.pro.batchrecord.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchExecutionVoidStatusListenerTest {

    @Mock
    private MesProEdhrRecordChangeService recordChangeService;

    private MesProEdhrBatchExecutionVoidStatusListener listener;

    @BeforeEach
    void setUp() {
        listener = new MesProEdhrBatchExecutionVoidStatusListener();
        ReflectionTestUtils.setField(listener, "recordChangeService", recordChangeService);
    }

    @Test
    void approveEvent_shouldMarkSubmittedBatchVoidRequestEffective() {
        BpmProcessInstanceStatusEvent event = batchVoidEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        listener.onApplicationEvent(event);

        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "void-process-1001", null, "APPROVED", null, null);
    }

    @Test
    void rejectEvent_shouldMarkSubmittedBatchVoidRequestRejected() {
        BpmProcessInstanceStatusEvent event = batchVoidEvent(BpmProcessInstanceStatusEnum.REJECT.getStatus())
                .setReason("不同意作废");

        listener.onApplicationEvent(event);

        verify(recordChangeService).handleVoidBatchExecutionApprovalCallback(
                "void-process-1001", null, "REJECTED", "不同意作废", null);
    }

    @Test
    void otherProcessDefinitionKey_shouldBeIgnored() {
        BpmProcessInstanceStatusEvent event = batchVoidEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .setProcessDefinitionKey("other-process");

        listener.onApplicationEvent(event);

        verify(recordChangeService, never()).handleVoidBatchExecutionApprovalCallback(
                "void-process-1001", null, "APPROVED", null, null);
    }

    private static BpmProcessInstanceStatusEvent batchVoidEvent(Integer status) {
        return new BpmProcessInstanceStatusEvent(new Object())
                .setId("void-process-1001")
                .setProcessDefinitionKey(MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY)
                .setBusinessKey("EDHR_BATCH_EXECUTION_VOID:7001")
                .setStatus(status);
    }
}
