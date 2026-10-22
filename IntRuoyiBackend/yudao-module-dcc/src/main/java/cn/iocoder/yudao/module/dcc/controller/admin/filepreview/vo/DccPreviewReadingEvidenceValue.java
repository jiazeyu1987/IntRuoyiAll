package cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo;

/** Header-only capability, never serialized to request JSON. */
public record DccPreviewReadingEvidenceValue(String viewerToken, String accessEventCode,
        String watermarkTraceCode, String viewerTokenId, String viewerTokenNonce) implements DccPreviewReadingSessionEvidence {
    @Override public String toString() { return "DccPreviewReadingEvidenceValue[capability redacted]"; }
}
