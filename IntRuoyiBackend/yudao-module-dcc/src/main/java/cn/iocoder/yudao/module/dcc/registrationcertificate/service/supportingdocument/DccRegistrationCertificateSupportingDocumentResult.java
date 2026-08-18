package cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument;

public record DccRegistrationCertificateSupportingDocumentResult(
        Long supportingDocumentId,
        Long certificateId,
        Long versionId,
        String documentType,
        String status,
        boolean lightRequired) {
}
