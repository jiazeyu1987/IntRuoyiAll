package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

public record DccRegistrationCertificateFileDownloadResult(
        String fileName,
        String contentType,
        byte[] bytes,
        Long grantId,
        Long businessFileId) {
}
