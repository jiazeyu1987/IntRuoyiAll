package cn.iocoder.yudao.module.dcc.service.audit;

import java.time.LocalDateTime;

public record DccAccessEventCreateCommand(String accessEventCode,
                                          Long fileId,
                                          String versionId,
                                          Long userId,
                                          String accessType,
                                          String purpose,
                                          String result,
                                          String failureCode,
                                          String failureReason,
                                          String sourceIp,
                                          String userAgent,
                                          String requestId,
                                          LocalDateTime occurredAt) {
}
