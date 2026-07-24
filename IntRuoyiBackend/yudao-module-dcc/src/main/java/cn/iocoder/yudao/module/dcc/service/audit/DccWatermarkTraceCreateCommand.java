package cn.iocoder.yudao.module.dcc.service.audit;

import java.time.LocalDateTime;

public record DccWatermarkTraceCreateCommand(String traceCode,
                                             Long accessEventId,
                                             String accessEventCode,
                                             Long fileId,
                                             String fileNumber,
                                             String versionId,
                                             Long userId,
                                             String userIdentifier,
                                             String userDisplayName,
                                             Long deptId,
                                             String deptName,
                                             String tenantName,
                                             String privacyMode,
                                             String watermarkPayloadJson,
                                             LocalDateTime issuedAt,
                                             LocalDateTime expiresAt) {
}
