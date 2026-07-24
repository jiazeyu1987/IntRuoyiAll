package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 撤回采购订单变更 Request VO")
@Data
public class SrmPurchaseOrderWithdrawChangeReqVO {

    @Schema(description = "采购订单变更单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购订单变更单编号不能为空")
    private Long changeId;

    @Schema(description = "撤回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "撤回原因不能为空")
    private String withdrawRemark;
}
