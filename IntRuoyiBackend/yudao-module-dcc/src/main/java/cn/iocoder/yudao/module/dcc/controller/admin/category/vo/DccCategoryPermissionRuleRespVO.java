package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

@Data
public class DccCategoryPermissionRuleRespVO {

    private Long id;
    private Long categoryId;
    private String actionType;
    private String subjectType;
    private Long subjectId;
    private String scopeType;
    private Boolean active;
    private String remark;
}
