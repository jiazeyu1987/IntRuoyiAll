package cn.iocoder.yudao.module.dcc.service.download;

public record DccDownloadFileBinary(
        String fileName,
        String contentType,
        byte[] bytes,
        String downloadRequestId,
        String accessEventCode,
        String encryptionPolicyVersion,
        String artifactId,
        String plainSha256,
        String cipherSha256
) {
}
