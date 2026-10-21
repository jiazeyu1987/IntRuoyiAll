package cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;

public record DccPreviewReadingSessionStartReqVO(
        @NotBlank @Size(max = 64) String clientSessionId,
        @JsonIgnore String viewerToken,
        @NotBlank @Size(max = 64) String accessEventCode,
        @NotBlank @Size(max = 64) String watermarkTraceCode,
        @NotBlank @Size(max = 64) String viewerTokenId,
        @NotBlank @Size(max = 64) String viewerTokenNonce) implements DccPreviewReadingSessionEvidence {
    public DccPreviewReadingSessionStartReqVO withViewerToken(String token) {
        return new DccPreviewReadingSessionStartReqVO(clientSessionId, token, accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce);
    }
    @Override public String toString() { return "DccPreviewReadingSessionStartReqVO[capability redacted]"; }
}
