package cn.iocoder.yudao.module.mes.service.pro.route.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionApprovalService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionWorkflowServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class MesProRouteVersionApprovalStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String BUSINESS_APPROVAL_BUSINESS_KEY_PREFIX = "BUSINESS_APPROVAL:";

    @Resource
    private MesProRouteVersionApprovalService approvalService;

    @Override
    protected String getProcessDefinitionKey() {
        return MesProRouteVersionWorkflowServiceImpl.ROUTE_VERSION_APPROVAL_PROCESS_DEFINITION_KEY;
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
            handleFinalStatus(event, "CANCELED", event.getReason());
        }
    }

    private void handleFinalStatus(BpmProcessInstanceStatusEvent event, String approvalResult, String rejectReason) {
        approvalService.handleApprovalCallback(
                event.getId(),
                "BPM-" + event.getId() + "-" + event.getStatus(),
                approvalResult,
                rejectReason,
                event.getActorUserId());
    }
}
