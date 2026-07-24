package cn.iocoder.yudao.module.dcc.service.download;

public record DccDownloadEncryptionRequest(
        Long tenantId,
        Long userId,
        Long controlledFileId,
        String fileVersionNo,
        String downloadRequestId,
        Long accessEventId,
        String accessEventCode,
        String policyVersion,
        Long sourceFileId,
        Long sourceConfigId,
        String sourcePath,
        String sourceFileName,
        String sourceContentType
) {
}
