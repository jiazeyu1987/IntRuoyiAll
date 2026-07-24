package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccCategoryTrainingRuleSaveReqVO {

    @NotNull(message = "培训部门不能为空")
    private Long departmentId;

    @NotNull(message = "培训规则启用状态不能为空")
    private Boolean active;
}
