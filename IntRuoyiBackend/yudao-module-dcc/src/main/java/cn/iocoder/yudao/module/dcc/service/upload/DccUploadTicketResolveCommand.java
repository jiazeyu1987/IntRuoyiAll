package cn.iocoder.yudao.module.dcc.service.upload;

public record DccUploadTicketResolveCommand(String uploadTicket,
                                            Long userId,
                                            Long categoryId,
                                            String sessionId,
                                            String purpose) {
}
