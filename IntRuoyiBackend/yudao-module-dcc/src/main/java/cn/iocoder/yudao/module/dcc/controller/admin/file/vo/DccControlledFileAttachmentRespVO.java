package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileAttachmentRespVO {

    private Long attachmentId;
    private Long controlledFileId;
    private String fileName;
    private String contentType;
    private String previewKind;
    private Long fileSize;
    private Integer sortNo;

}
