package cn.iocoder.yudao.module.dcc.service.upload;

public record DccUploadTicketMarkBoundCommand(String uploadTicket,
                                              Long userId,
                                              String sessionId,
                                              String purpose,
                                              Long controlledFileId) {
}
