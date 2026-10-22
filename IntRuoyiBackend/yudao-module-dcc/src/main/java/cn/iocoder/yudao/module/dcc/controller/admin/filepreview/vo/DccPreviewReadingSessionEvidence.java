package cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo;

public interface DccPreviewReadingSessionEvidence {
    String viewerToken();
    String accessEventCode();
    String watermarkTraceCode();
    String viewerTokenId();
    String viewerTokenNonce();
}
