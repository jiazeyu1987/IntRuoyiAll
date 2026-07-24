package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

import java.util.List;

@Data
public class DccCategoryViewMatrixEffectivePreviewRespVO {

    private Long categoryId;
    private String viewRuleSummary;
    private String pendingPreviewRuleSummary;
    private List<String> downloadRuleSubjects;
    private String downloadRuleSummary;
    private Boolean blocking;
    private List<DccCategoryViewMatrixRowRespVO.Rule> rules;
    private List<DccCategoryViewMatrixRowRespVO.Subject> viewSubjects;
    private List<DccCategoryViewMatrixRowRespVO.Risk> risks;

}
