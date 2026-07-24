package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccCategoryPermissionRuleSaveReqVO {

    @NotBlank(message = "权限动作不能为空")
    private String actionType;

    @NotBlank(message = "授权主体类型不能为空")
    private String subjectType;

    @NotNull(message = "授权主体编号不能为空")
    private Long subjectId;

    @NotNull(message = "权限规则启用状态不能为空")
    private Boolean active;

    private String scopeType;

    private String remark;
}
