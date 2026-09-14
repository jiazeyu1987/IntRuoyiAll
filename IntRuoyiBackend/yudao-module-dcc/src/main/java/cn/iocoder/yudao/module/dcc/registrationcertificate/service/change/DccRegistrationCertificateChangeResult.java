package cn.iocoder.yudao.module.dcc.registrationcertificate.service.change;

public record DccRegistrationCertificateChangeResult(
        Long certificateId,
        Long changeId,
        Long sourceSnapshotId,
        Long resultingSnapshotId,
        String status) {
}
