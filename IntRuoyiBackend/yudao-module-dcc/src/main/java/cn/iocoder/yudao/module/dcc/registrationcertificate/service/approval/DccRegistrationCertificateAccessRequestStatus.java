package cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval;

import java.time.LocalDateTime;
import java.util.List;

public record DccRegistrationCertificateAccessRequestStatus(
        Long requestId,
        Long certificateId,
        Long ownerCompanyId,
        Long requesterUserId,
        String requestType,
        String purpose,
        Long projectCodeId,
        String requestStatus,
        String bpmProcessInstanceId,
        String bpmBindingStatus,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        LocalDateTime withdrawnAt,
        String withdrawReason,
        String rejectReason,
        List<DccRegistrationCertificateGrantStatus> grants) {
}
