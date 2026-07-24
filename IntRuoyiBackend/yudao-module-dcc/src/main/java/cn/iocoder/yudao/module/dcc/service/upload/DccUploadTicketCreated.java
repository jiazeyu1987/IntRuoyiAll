package cn.iocoder.yudao.module.dcc.service.upload;

import java.time.LocalDateTime;

public record DccUploadTicketCreated(String uploadTicket,
                                     String sessionId,
                                     String purpose,
                                     String status,
                                     LocalDateTime expireTime) {
}
