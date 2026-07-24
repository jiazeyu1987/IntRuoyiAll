package cn.iocoder.yudao.module.dcc.service.download;

public record DccDownloadPolicyContext(
        Long controlledFileId,
        String status,
        Long publishedFileId,
        boolean categoryDownloadAllowed,
        boolean directoryDownloadAllowed
) {
}
