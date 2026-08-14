package cn.iocoder.yudao.module.dcc.service.upload;

import java.time.LocalDateTime;

public record DccUploadTicketCreated(String uploadTicket,
                                     String sessionId,
                                     String purpose,
                                     String status,
                                     LocalDateTime expireTime,
                                     Long storageFileId,
                                     String fileName,
                                     String contentType,
                                     Long fileSize) {

    public DccUploadTicketCreated(String uploadTicket,
                                  String sessionId,
                                  String purpose,
                                  String status,
                                  LocalDateTime expireTime) {
        this(uploadTicket, sessionId, purpose, status, expireTime, null, null, null, null);
    }
}
