package cn.iocoder.yudao.module.dcc.service.preview;

import java.time.LocalDateTime;

public record DccPreviewAccessResult(Long accessEventId,
                                     String accessEventCode,
                                     Long watermarkTraceId,
                                     String watermarkTraceCode,
                                     String viewerToken,
                                     String viewerTokenId,
                                     String viewerTokenNonce,
                                     LocalDateTime issuedAt,
                                     LocalDateTime expiresAt,
                                     String watermarkPayloadJson) {
}
