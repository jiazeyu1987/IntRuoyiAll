package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccApprovalRouteNodeRespVO {
    private Long id;
    private Long routeId;
    private Integer stageNo;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String candidateSourceType;
    private Long candidateSourceId;
    private List<Long> candidateSourceIds;
    private String approveMethod;
    private Integer approveRatio;
    private Boolean requireAllApprovals;
    private Boolean required;
    private Integer sort;
    private String stageType;
    private String subjectLabel;
    private String marker;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String subjectDepartmentPath;
    private String ruleRemark;
}
