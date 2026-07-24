package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileProjectCodeRecognitionRespVO {

    private Long controlledFileId;
    private String recognitionStatus;
    private Long dccProjectCodeId;
    private String projectName;
    private String projectCode;
    private String matchType;
    private String matchText;
    private String recognitionMethod;
    private String recognitionVersion;
    private Long matchedProjectAliasId;
    private String matchedProjectAliasText;
    private String matchedProjectAliasSource;
}
