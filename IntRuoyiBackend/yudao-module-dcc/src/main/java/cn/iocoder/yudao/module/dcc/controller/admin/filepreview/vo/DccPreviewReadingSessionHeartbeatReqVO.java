package cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DccPreviewReadingSessionHeartbeatReqVO(
        @NotNull @Positive Long sessionId,
        @JsonIgnore String viewerToken,
        @NotBlank @Size(max = 64) String accessEventCode,
        @NotBlank @Size(max = 64) String watermarkTraceCode,
        @NotBlank @Size(max = 64) String viewerTokenId,
        @NotBlank @Size(max = 64) String viewerTokenNonce) implements DccPreviewReadingSessionEvidence {
    public DccPreviewReadingSessionHeartbeatReqVO withViewerToken(String token) {
        return new DccPreviewReadingSessionHeartbeatReqVO(sessionId, token, accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce);
    }
    @Override public String toString() { return "DccPreviewReadingSessionHeartbeatReqVO[capability redacted]"; }
}
