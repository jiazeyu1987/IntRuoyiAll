package cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal;

public record DccRegistrationCertificateRenewalResult(
        Long certificateId,
        Long renewalVersionId,
        Long renewalSnapshotId,
        Long businessFileId,
        String masterStatus,
        String renewalVersionStatus,
        boolean renewalUploadMissing) {
}
