package cn.iocoder.yudao.module.dcc.service.download;

public record DccDownloadEncryptionResult(
        String status,
        String artifactId,
        String cipherFileRef,
        String plainSha256,
        String cipherSha256,
        String encryptionPolicyVersion,
        String cipherFileName,
        String contentType,
        byte[] cipherBytes
) {
}
