package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

public record DccRegistrationCertificateCommandResult(
        Long certificateId,
        Long versionId,
        Long snapshotId,
        Long businessFileId) {
}
