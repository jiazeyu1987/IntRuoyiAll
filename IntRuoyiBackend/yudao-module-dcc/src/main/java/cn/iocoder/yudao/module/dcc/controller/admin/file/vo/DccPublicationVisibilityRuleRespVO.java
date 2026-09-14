package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccPublicationVisibilityRuleRespVO {
    private String id;
    private String sourceType;
    private String sourceRuleId;
    private String sourceScope;
    private String subjectType;
    private String subjectId;
    private String sourceSummary;
    private String resolutionStatus;
    private String resolutionMessage;
    private List<DccPublicationVisibilityUserRespVO> users;
}
