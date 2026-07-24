package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccCategoryDirectoryBindingSaveReqVO {

    @NotNull(message = "目录编号不能为空")
    private Long directoryId;

    @NotNull(message = "绑定启用状态不能为空")
    private Boolean active;
}
