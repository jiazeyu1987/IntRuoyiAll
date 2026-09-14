package cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit;

import java.time.LocalDateTime;

public record DccRegistrationCertificateOperationAudit(
        Long operatorId,
        String operatorName,
        LocalDateTime operatedAt,
        Long approverId,
        String approverName,
        LocalDateTime approvedAt) {
}
