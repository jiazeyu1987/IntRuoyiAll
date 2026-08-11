package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFilePreparedSource(Long sourceFileId,
                                              Long originSourceFileId,
                                              String sourceSha256,
                                              boolean isolatedCopy) {
}
