package cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit;

import lombok.Builder;

@Builder
public record DccRegistrationCertificateReadAuditCommand(
        Long tenantId,
        Long ownerCompanyId,
        Long certificateId,
        Long requestedOwnerCompanyId,
        Long requestedCertificateId,
        Long versionId,
        Long snapshotId,
        Long businessFileId,
        String operation,
        Long actorId,
        String result,
        String resultCode,
        String requestTraceId,
        String detailJson) {
}