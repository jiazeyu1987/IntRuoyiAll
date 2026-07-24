package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class DccCategoryReviewMatrixRowRespVO {

    private Long categoryId;
    private String code;
    private String name;
    private String lifecycleStage;
    private Boolean active;
    private Boolean configured;
    private Integer routeVersionNo;
    private LocalDateTime effectiveTime;
    private String remark;
    private List<Long> signoffPositionIds;
    private List<Long> approvalPositionIds;
    private List<String> signoffPositionNames;
    private List<String> approvalPositionNames;
    private List<Rule> rules;
    private String viewRuleSummary;
    private List<Subject> viewSubjects;
    private String pendingPreviewRuleSummary;
    private List<String> downloadRuleSubjects;
    private String downloadRuleSummary;
    private List<Risk> risks;

    @Data
    @Accessors(chain = true)
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
    @Accessors(chain = true)
    public static class Risk {
        private String code;
        private String message;
        private String severity;
        private Boolean blocking;
    }

    @Data
    @Accessors(chain = true)
    public static class Rule {
        private String stageType;
        private Boolean active;
        private String subjectLabel;
        private String marker;
        private String subjectType;
        private Long subjectId;
        private String subjectName;
        private String subjectDepartmentPath;
        private String remark;
    }
}
