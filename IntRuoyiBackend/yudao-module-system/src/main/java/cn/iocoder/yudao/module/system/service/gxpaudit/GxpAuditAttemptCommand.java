package cn.iocoder.yudao.module.system.service.gxpaudit;

import lombok.Builder;

@Builder
public record GxpAuditAttemptCommand(String attemptedOperationId, String serverAttemptId,
                                     String resultStatus, String errorCode, String reason,
                                     String subjectId, String subjectVersion, String requestId,
                                     String sourceType, String sourceLocator) {
}
