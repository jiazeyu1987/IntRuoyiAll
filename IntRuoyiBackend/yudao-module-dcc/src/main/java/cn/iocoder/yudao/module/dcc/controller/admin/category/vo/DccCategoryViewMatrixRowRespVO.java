package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccCategoryViewMatrixRowRespVO {

    private Long categoryId;
    private String code;
    private String name;
    private Boolean active;
    private Integer sort;
    private Boolean configured;
    private String viewRuleSummary;
    private List<Rule> rules;
    private List<Subject> viewSubjects;
    private String pendingPreviewRuleSummary;
    private List<String> downloadRuleSubjects;
    private String downloadRuleSummary;
    private List<Risk> risks;

    @Data
    public static class Rule {
        private Long id;
        private String excelFileName;
        private Integer excelRowNo;
        private String excelColumnLetter;
        private String subjectLabel;
        private String subjectTopHeader;
        private String subjectSubHeader;
        private String marker;
        private String scopeType;
        private String subjectType;
        private Long subjectId;
        private String subjectName;
        private String subjectDepartmentPath;
        private Boolean active;
        private String remark;
    }

    @Data
    public static class Subject {
        private Long userId;
        private String userName;
        private String source;
        private String excelFileName;
        private Integer excelRowNo;
        private String excelColumnLetter;
        private String subjectLabel;
        private String marker;
        private String scopeType;
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
