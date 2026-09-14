package cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization;

public record DccRegistrationCertificateFormalizationResult(
        Long certificateId,
        Long versionId,
        Long snapshotId,
        Long businessFileId) {
}
