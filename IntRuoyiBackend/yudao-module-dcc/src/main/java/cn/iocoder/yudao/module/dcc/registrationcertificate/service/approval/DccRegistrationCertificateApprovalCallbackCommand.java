package cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval;

import java.time.LocalDateTime;

public record DccRegistrationCertificateApprovalCallbackCommand(
        String bpmProcessInstanceId,
        String approvalKey,
        String rejectReason,
        LocalDateTime decidedAt) {
}
