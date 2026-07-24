package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFilePreviewMetadataRespVO {

    private String previewKind;
    private String fileName;
    private String contentType;
    private String viewerToken;
    private String viewerTokenId;
    private String viewerTokenNonce;
    private String accessEventCode;
    private String watermarkTraceCode;
    private String onlyofficeBaseUrl;
    private String onlyofficeDocumentUrl;
    private String previewUnavailableReason;
    private DccControlledPreviewWatermarkRespVO watermark;
}
