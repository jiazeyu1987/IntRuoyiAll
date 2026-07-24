package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

@Data
public class DccCategoryTrainingRuleRespVO {

    private Long id;
    private Long categoryId;
    private Long departmentId;
    private Boolean active;
}
