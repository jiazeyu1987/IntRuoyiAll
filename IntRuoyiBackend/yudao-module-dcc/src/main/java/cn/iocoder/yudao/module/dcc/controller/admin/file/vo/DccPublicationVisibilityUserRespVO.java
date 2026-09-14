package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccPublicationVisibilityUserRespVO {
    private String userId;
    private String userName;
    private String deptId;
    private String deptName;
    private Integer userStatus;
    private String resolutionReason;
    private String assignmentScopeResult;
}
