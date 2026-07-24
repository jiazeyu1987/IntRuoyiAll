package cn.iocoder.yudao.module.dcc.service.upload;

import java.time.LocalDateTime;

public record DccUploadTemporaryFileStatus(String requestId,
                                           int temporaryFileCount,
                                           boolean bindable,
                                           String sessionId,
                                           String purpose,
                                           String status,
                                           LocalDateTime expireTime,
                                           String cleanupStatus,
                                           String cleanupReason,
                                           LocalDateTime cleanupTime) {
}
