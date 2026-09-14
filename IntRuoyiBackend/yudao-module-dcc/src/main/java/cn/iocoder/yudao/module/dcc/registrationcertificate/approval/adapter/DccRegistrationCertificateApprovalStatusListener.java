package cn.iocoder.yudao.module.dcc.registrationcertificate.approval.adapter;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalCallbackCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import org.springframework.stereotype.Component;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.BUSINESS_KEY_PREFIX;
import static cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalContract.PROCESS_DEFINITION_KEY;

@Component
public class DccRegistrationCertificateApprovalStatusListener extends BpmProcessInstanceStatusEventListener {

    private final DccRegistrationCertificateApprovalService approvalService;

    public DccRegistrationCertificateApprovalStatusListener(
            DccRegistrationCertificateApprovalService approvalService) {
        if (approvalService == null) {
            throw new IllegalArgumentException("注册证审批服务不能为空");
        }
        this.approvalService = approvalService;
    }

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        if (event == null || isBlank(event.getId()) || event.getActorUserId() == null
                || isBlank(event.getBusinessKey()) || !event.getBusinessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
        }
        String approvalKey = "BPM:" + event.getId() + ":" + event.getStatus();
        DccRegistrationCertificateApprovalCallbackCommand command =
                new DccRegistrationCertificateApprovalCallbackCommand(
                        event.getId(), approvalKey, event.getReason(), null);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            approvalService.approve(tenantId, event.getActorUserId(), command);
            return;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
            approvalService.reject(tenantId, event.getActorUserId(), command);
            return;
        }
        if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
            approvalService.cancelFromNative(tenantId, event.getActorUserId(), command);
            return;
        }
        throw new ServiceException(REGISTRATION_CERTIFICATE_ACCESS_BPM_BINDING_CONFLICT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
