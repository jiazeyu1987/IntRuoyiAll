package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccControlledFileRouteSnapshotRespVO {

    private Long id;
    private Integer routeVersionNo;
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
    private List<Long> resolvedUserIds;
}
