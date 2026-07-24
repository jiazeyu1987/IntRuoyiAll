package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 供应商拒绝采购订单变更 Request VO")
@Data
public class SrmPurchaseOrderRejectChangeReqVO {

    @Schema(description = "采购订单变更单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购订单变更单编号不能为空")
    private Long changeId;

    @Schema(description = "拒绝原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "拒绝原因不能为空")
    private String rejectRemark;
}
