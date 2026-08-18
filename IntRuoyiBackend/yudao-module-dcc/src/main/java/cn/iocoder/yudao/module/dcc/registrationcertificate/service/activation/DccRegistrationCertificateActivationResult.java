package cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation;

public record DccRegistrationCertificateActivationResult(
        Long certificateId,
        Long oldVersionId,
        Long currentVersionId,
        Long currentSnapshotId,
        boolean activated) {
}