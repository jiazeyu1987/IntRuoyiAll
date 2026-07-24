package cn.iocoder.yudao.module.mes.service.pro.batchrecord.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrRecordChangeServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class MesProEdhrBatchExecutionVoidStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String BUSINESS_APPROVAL_BUSINESS_KEY_PREFIX = "BUSINESS_APPROVAL:";

    @Resource
    private MesProEdhrRecordChangeService recordChangeService;

    @Override
    protected String getProcessDefinitionKey() {
        return MesProEdhrRecordChangeServiceImpl.BATCH_EXECUTION_VOID_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        if (event.getBusinessKey() != null
                && event.getBusinessKey().startsWith(BUSINESS_APPROVAL_BUSINESS_KEY_PREFIX)) {
            return;
        }
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            handleFinalStatus(event, "APPROVED", null);
            return;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
            handleFinalStatus(event, "REJECTED", event.getReason());
            return;
        }
        if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
            handleFinalStatus(event, "CANCELLED", event.getReason());
        }
    }

    private void handleFinalStatus(BpmProcessInstanceStatusEvent event, String approvalResult, String rejectReason) {
        recordChangeService.handleVoidBatchExecutionApprovalCallback(
                event.getId(),
                null,
                approvalResult,
                rejectReason,
                null);
    }
}
