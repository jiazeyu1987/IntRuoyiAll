package cn.iocoder.yudao.module.dcc.service.upload;

public record DccUploadTicketPreflightCommand(Long userId,
                                              Long categoryId,
                                              String sessionId,
                                              String purpose,
                                              byte[] content) {
}
