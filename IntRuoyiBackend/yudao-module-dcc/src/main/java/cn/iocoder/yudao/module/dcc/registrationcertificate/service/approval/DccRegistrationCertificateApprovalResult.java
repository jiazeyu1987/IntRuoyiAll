package cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval;

import java.util.List;

public record DccRegistrationCertificateApprovalResult(
        Long requestId,
        String businessKey,
        String processInstanceId,
        String status,
        List<Long> grantIds) {
}
