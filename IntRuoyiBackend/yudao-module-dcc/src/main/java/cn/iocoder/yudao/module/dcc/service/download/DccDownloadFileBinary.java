package cn.iocoder.yudao.module.dcc.service.download;

public record DccDownloadFileBinary(
        String fileName,
        String contentType,
        byte[] bytes,
        String downloadRequestId,
        String accessEventCode,
        String plainSha256
) {
}
