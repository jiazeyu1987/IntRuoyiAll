package cn.iocoder.yudao.module.dcc.registrationcertificate.service.history;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DccRegistrationCertificateHistoryItem(
        String eventType,
        String itemType,
        String beforeValueJson,
        String afterValueJson,
        Long actorId,
        Long businessFileId,
        String fileKind,
        Long targetVersionId,
        Integer versionNo,
        LocalDate approvalDate,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Boolean categoryChanged,
        String certificateNo,
        String classification,
        String originalFileName,
        String fileStatus,
        LocalDateTime occurredAt,
        String renewalOperatorName,
        LocalDateTime renewalOperatedAt,
        String renewalApproverName,
        LocalDateTime renewalApprovedAt) {
}
