package cn.iocoder.yudao.module.dcc.controller.admin.route.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccApprovalRoutePreviewReqVO {
    @NotNull(message = "类别编号不能为空")
    private Long categoryId;
}
