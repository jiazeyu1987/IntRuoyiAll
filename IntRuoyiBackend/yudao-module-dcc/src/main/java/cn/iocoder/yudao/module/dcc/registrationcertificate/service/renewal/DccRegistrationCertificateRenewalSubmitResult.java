package cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal;

public record DccRegistrationCertificateRenewalSubmitResult(
        Long requestId,
        Long certificateId,
        Long businessFileId,
        String requestStatus) {
}
