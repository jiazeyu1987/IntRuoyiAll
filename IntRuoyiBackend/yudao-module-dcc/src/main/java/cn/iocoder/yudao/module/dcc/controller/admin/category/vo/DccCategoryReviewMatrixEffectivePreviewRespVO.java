package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccCategoryReviewMatrixEffectivePreviewRespVO {

    private Long categoryId;
    private Integer nextRouteVersionNo;
    private String viewRuleSummary;
    private String pendingPreviewRuleSummary;
    private List<String> downloadRuleSubjects;
    private String downloadRuleSummary;
    private Boolean blocking;
    private List<StagePreview> stages;
    private List<Subject> viewSubjects;
    private List<Risk> risks;
    private List<DccCategoryReviewMatrixRowRespVO.Rule> rules;

    @Data
    public static class StagePreview {
        private Integer stageNo;
        private String stageName;
        private String stageType;
        private String approveMethod;
        private List<Long> positionIds;
        private List<String> positionNames;
        private String sourceRule;
        private List<Subject> resolvedSubjects;
    }

    @Data
    public static class Subject {
        private Long userId;
        private String userName;
        private String source;
        private Integer stageNo;
        private String stageName;
        private String stageType;
        private Long positionId;
        private String positionName;
        private String subjectLabel;
        private String marker;
        private String subjectType;
        private Long subjectId;
        private String reason;
    }

    @Data
    public static class Risk {
        private String code;
        private String message;
        private String severity;
        private Boolean blocking;
    }
}
