package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.List;

/** Immutable raw public source; runtime participants and mutable enablement are separate facts. */
@Data
public class DccApprovalMatrixBaselineVO {
    private String formatVersion;
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long tenantId;
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long categoryId;
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long fileTypeTaxonomyId;
    @JsonFormat(shape=JsonFormat.Shape.STRING) private Long routeId;
    private Integer routeVersionNo;
    private String actionType;
    /** Exact LocalDateTime source text, fixed nine fractional digits; no epoch conversion. */
    private String effectiveTime;
    private String routeRemark;
    private List<RawNode> nodes;
    private String matrixDigest;

    @Data
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
    public static class RawNode {
        @JsonFormat(shape=JsonFormat.Shape.STRING) private Long id;
        private Integer stageNo;
        private String stageCode;
        private String stageName;
        private Integer stageOrder;
        private Integer sort;
        private String candidateSourceType;
        @JsonFormat(shape=JsonFormat.Shape.STRING) private Long candidateSourceId;
        private String candidateSourceIds;
        private String approveMethod;
        private Integer approveRatio;
        private Boolean requireAllApprovals;
        private Boolean required;
        private String stageType;
        private String subjectLabel;
        private String marker;
        private String subjectType;
        @JsonFormat(shape=JsonFormat.Shape.STRING) private Long subjectId;
        private String subjectName;
        private String subjectDepartmentPath;
        private String ruleRemark;
    }
}
