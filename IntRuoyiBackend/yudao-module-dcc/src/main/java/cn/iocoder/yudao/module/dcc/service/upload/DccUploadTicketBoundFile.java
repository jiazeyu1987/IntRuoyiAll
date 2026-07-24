package cn.iocoder.yudao.module.dcc.service.upload;

public record DccUploadTicketBoundFile(String uploadTicket,
                                       Long storageFileId,
                                       String fileName,
                                       String contentType,
                                       Long fileSize) {
}
