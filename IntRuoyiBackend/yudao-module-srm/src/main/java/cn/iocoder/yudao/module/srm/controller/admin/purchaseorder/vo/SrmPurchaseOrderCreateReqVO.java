package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 采购订单协同创建 Request VO")
@Data
public class SrmPurchaseOrderCreateReqVO {

    @Schema(description = "采购计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购计划编号不能为空")
    private Long sourcePlanId;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "108")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "订单备注", example = "请优先确认交期")
    private String orderRemark;
}
