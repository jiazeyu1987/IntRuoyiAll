package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFilePreviewMetadataRespVO {

    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long controlledFileId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long distributionId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private Long recipientId;
    private String versionNo;
    private String approvalProcessInstanceId;

    private String previewKind;
    private String fileName;
    private String contentType;
    private String viewerToken;
    private String viewerTokenId;
    private String viewerTokenNonce;
    private String accessEventCode;
    /** Verified PREVIEW event occurredAt in UTC, with an explicit ISO-8601 offset; never a reading duration. */
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String previewRegisteredAt;
    private String watermarkTraceCode;
    private String onlyofficeBaseUrl;
    private String onlyofficeDocumentUrl;
    private String previewUnavailableReason;
    private DccControlledPreviewWatermarkRespVO watermark;
}
