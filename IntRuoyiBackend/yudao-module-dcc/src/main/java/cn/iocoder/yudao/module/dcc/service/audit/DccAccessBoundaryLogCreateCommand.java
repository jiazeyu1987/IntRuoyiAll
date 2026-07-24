package cn.iocoder.yudao.module.dcc.service.audit;

public record DccAccessBoundaryLogCreateCommand(Long userId,
                                                String actionType,
                                                String purpose,
                                                String result,
                                                String failureCode,
                                                String reason,
                                                String sourceIp,
                                                String requestId,
                                                String userAgent) {
}
