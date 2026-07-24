package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccApprovalRoutePreviewRespVO {
    private Integer stageNo;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private Integer approvalMode;
    private String candidateSourceType;
    private List<Long> candidateSourceIds;
    private Boolean requireAllApprovals;
    private List<Long> resolvedUserIds;
}
