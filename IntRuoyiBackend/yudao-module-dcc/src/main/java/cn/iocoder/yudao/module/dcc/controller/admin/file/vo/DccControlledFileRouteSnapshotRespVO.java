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
    private List<String> candidateSourceNames;
    private String approveMethod;
    private Integer approveRatio;
    private Boolean requireAllApprovals;
    private List<Long> resolvedUserIds;
    private List<DepartmentObligationRespVO> departmentObligations;

    /**
     * The immutable department responsibility captured when the approval task was created.
     */
    @Data
    public static class DepartmentObligationRespVO {

        private Long snapshotId;
        private Long departmentId;
        private String departmentName;
        private Long assigneeUserId;
        private String assigneeName;
        private String leaderConfigDigest;
        private String obligationId;
        private String bpmTaskId;
    }
}
