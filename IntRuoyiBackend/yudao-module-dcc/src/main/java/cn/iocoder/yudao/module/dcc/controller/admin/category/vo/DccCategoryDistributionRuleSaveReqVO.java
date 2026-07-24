package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccCategoryDistributionRuleSaveReqVO {

    @NotNull(message = "分发部门不能为空")
    private Long departmentId;

    private String distributionMedium;

    @NotNull(message = "分发规则启用状态不能为空")
    private Boolean active;
}
