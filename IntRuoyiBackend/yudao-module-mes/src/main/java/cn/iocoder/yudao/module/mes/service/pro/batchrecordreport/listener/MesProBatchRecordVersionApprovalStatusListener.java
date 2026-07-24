package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class MesProBatchRecordVersionApprovalStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String BUSINESS_APPROVAL_BUSINESS_KEY_PREFIX = "BUSINESS_APPROVAL:";

    @Resource
    private MesProBatchRecordReportService batchRecordReportService;

    @Override
    protected String getProcessDefinitionKey() {
        return MesProBatchRecordReportServiceImpl.BATCH_RECORD_VERSION_APPROVAL_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        if (StrUtil.startWith(event.getBusinessKey(), BUSINESS_APPROVAL_BUSINESS_KEY_PREFIX)) {
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
            handleFinalStatus(event, "REJECTED", event.getReason());
        }
    }

    private void handleFinalStatus(BpmProcessInstanceStatusEvent event, String approvalResult, String rejectReason) {
        batchRecordReportService.handleBatchRecordVersionApprovalCallback(
                event.getId(),
                "BPM-" + event.getId() + "-" + event.getStatus(),
                approvalResult,
                rejectReason,
                null);
    }
}
