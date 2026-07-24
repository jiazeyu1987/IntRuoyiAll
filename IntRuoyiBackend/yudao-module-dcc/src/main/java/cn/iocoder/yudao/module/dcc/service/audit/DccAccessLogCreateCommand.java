package cn.iocoder.yudao.module.dcc.service.audit;

public record DccAccessLogCreateCommand(Long fileId,
                                        Long accessEventId,
                                        String accessEventCode,
                                        String watermarkTraceCode,
                                        String versionId,
                                        Long userId,
                                        String actionType,
                                        String purpose,
                                        String result,
                                        String failureCode,
                                        String reason,
                                        String sourceIp,
                                        String requestId,
                                        String userAgent) {
}
