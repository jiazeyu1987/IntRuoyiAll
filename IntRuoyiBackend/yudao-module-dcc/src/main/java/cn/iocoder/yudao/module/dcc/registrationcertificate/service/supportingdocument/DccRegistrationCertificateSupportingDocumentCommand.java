package cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument;

public record DccRegistrationCertificateSupportingDocumentCommand(
        Long tenantId,
        Long actorId,
        String idempotencyKey,
        String requestTraceId,
        Long certificateId,
        Long versionId,
        Long businessFileId,
        Long supportingDocumentId,
        Integer expectedRowVersion,
        String documentType,
        String rejectReason) {
}
