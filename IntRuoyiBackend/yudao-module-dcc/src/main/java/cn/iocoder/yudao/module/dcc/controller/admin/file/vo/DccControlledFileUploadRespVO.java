package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileUploadRespVO {

    private String uploadTicket;
    private String sessionId;
    private String purpose;
    private String status;
    private java.time.LocalDateTime expireTime;
    private String requestId;
    private String fileName;
    private String contentType;
    private String previewKind;
    private String onlyofficeBaseUrl;
    private String onlyofficeDocumentUrl;
    private String previewUnavailableReason;
    private Long fileSize;
    private DccControlledPreviewWatermarkRespVO watermark;
}
