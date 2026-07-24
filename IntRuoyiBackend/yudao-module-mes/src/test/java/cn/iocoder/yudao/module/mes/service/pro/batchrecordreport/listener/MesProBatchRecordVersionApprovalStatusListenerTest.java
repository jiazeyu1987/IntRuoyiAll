package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordVersionApprovalStatusListenerTest {

    @Mock
    private MesProBatchRecordReportService batchRecordReportService;

    private MesProBatchRecordVersionApprovalStatusListener listener;

    @BeforeEach
    void setUp() {
        listener = new MesProBatchRecordVersionApprovalStatusListener();
        ReflectionTestUtils.setField(listener, "batchRecordReportService", batchRecordReportService);
    }

    @Test
    void approveEvent_shouldApproveBatchRecordVersion() {
        BpmProcessInstanceStatusEvent event = batchRecordVersionEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        listener.onApplicationEvent(event);

        verify(batchRecordReportService).handleBatchRecordVersionApprovalCallback(
                "batch-version-process-1001",
                "BPM-batch-version-process-1001-2",
                "APPROVED",
                null,
                null);
    }

    @Test
    void rejectEvent_shouldRejectBatchRecordVersionWithReason() {
        BpmProcessInstanceStatusEvent event = batchRecordVersionEvent(BpmProcessInstanceStatusEnum.REJECT.getStatus())
                .setReason("资料不完整");

        listener.onApplicationEvent(event);

        verify(batchRecordReportService).handleBatchRecordVersionApprovalCallback(
                "batch-version-process-1001",
                "BPM-batch-version-process-1001-3",
                "REJECTED",
                "资料不完整",
                null);
    }

    @Test
    void cancelEvent_shouldRejectBatchRecordVersionWithReason() {
        BpmProcessInstanceStatusEvent event = batchRecordVersionEvent(BpmProcessInstanceStatusEnum.CANCEL.getStatus())
                .setReason("撤回审批");

        listener.onApplicationEvent(event);

        verify(batchRecordReportService).handleBatchRecordVersionApprovalCallback(
                "batch-version-process-1001",
                "BPM-batch-version-process-1001-4",
                "REJECTED",
                "撤回审批",
                null);
    }

    @Test
    void otherProcessDefinitionKey_shouldBeIgnored() {
        BpmProcessInstanceStatusEvent event = batchRecordVersionEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .setProcessDefinitionKey("other-process");

        listener.onApplicationEvent(event);

        verify(batchRecordReportService, never()).handleBatchRecordVersionApprovalCallback(
                "batch-version-process-1001",
                "BPM-batch-version-process-1001-2",
                "APPROVED",
                null,
                null);
    }

    @Test
    void businessApprovalBusinessKey_shouldBeIgnoredByLegacyListener() {
        BpmProcessInstanceStatusEvent event = batchRecordVersionEvent(BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .setBusinessKey("BUSINESS_APPROVAL:3001");

        listener.onApplicationEvent(event);

        verify(batchRecordReportService, never()).handleBatchRecordVersionApprovalCallback(
                "batch-version-process-1001",
                "BPM-batch-version-process-1001-2",
                "APPROVED",
                null,
                null);
    }

    private static BpmProcessInstanceStatusEvent batchRecordVersionEvent(Integer status) {
        return new BpmProcessInstanceStatusEvent(new Object())
                .setId("batch-version-process-1001")
                .setProcessDefinitionKey(MesProBatchRecordReportServiceImpl
                        .BATCH_RECORD_VERSION_APPROVAL_PROCESS_DEFINITION_KEY)
                .setBusinessKey("77001")
                .setStatus(status);
    }
}
