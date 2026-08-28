package cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload;

public record DccRegistrationCertificateUploadSubmitResult(
        Long requestId,
        Long certificateId,
        Long businessFileId) {
}
