package cn.iocoder.yudao.module.dcc.service.upload;

public record DccUploadTicketCreateCommand(Long userId,
                                           Long categoryId,
                                           String sessionId,
                                           String purpose,
                                           Long storageFileId,
                                           String originalFileName,
                                           String contentType,
                                           Long fileSize,
                                           byte[] content,
                                           String requestId) {
}
