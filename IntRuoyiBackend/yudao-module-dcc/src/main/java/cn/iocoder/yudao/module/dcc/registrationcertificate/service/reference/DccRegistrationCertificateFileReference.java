package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference;

public record DccRegistrationCertificateFileReference(
        Long tenantId,
        Long ownerCompanyId,
        Long certificateId,
        Long versionId,
        Integer versionNo,
        Long businessFileId,
        Long infraFileId,
        String versionStatus,
        String originalName,
        String mimeType) {

    public String versionKey() {
        return "CERTIFICATE:" + certificateId + ":VERSION:" + versionId
                + ":NO:" + versionNo + ":FILE:" + businessFileId;
    }
}
