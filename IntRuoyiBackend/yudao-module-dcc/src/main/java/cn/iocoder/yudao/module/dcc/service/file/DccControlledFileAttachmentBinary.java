package cn.iocoder.yudao.module.dcc.service.file;

public record DccControlledFileAttachmentBinary(Long attachmentId,
                                                Long storageFileId,
                                                String fileName,
                                                String contentType,
                                                Long fileSize) {
}
