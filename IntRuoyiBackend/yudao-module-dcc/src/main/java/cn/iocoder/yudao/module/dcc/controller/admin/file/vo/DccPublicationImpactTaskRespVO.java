package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccPublicationImpactTaskRespVO {
    private String id;
    private String batchId;
    private String publishedControlledFileId;
    private String relatedMasterId;
    private String relatedActiveControlledFileId;
    private String relatedFileNumber;
    private String relatedFileName;
    private String relatedVersionNo;
    private String assigneeUserId;
    private String assigneeUserName;
    private String taskStatus;
    private String decision;
    private String decisionReason;
    private String revisionTrackingStatus;
    private String linkedRevisionControlledFileId;
    private String linkedRevisionVersion;
    private Integer rowVersion;
    private List<String> relationDirections;
}
