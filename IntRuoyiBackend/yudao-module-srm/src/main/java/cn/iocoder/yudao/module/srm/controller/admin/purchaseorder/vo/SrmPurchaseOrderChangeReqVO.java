package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - SRM 采购订单变更申请 Request VO")
@Data
public class SrmPurchaseOrderChangeReqVO {

    @Schema(description = "采购订单协同单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购订单协同单编号不能为空")
    private Long orderId;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "变更原因不能为空")
    private String changeReason;

    @Schema(description = "采购侧补充说明", example = "客户交付窗口调整")
    private String changeRemark;

    @Schema(description = "变更行", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "变更行不能为空")
    @Valid
    private List<Line> lines;

    @Schema(description = "管理后台 - SRM 采购订单变更申请行")
    @Data
    public static class Line {

        @Schema(description = "采购订单协同行编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "采购订单协同行编号不能为空")
        private Long orderLineId;

        @Schema(description = "变更数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "变更数量不能为空")
        private BigDecimal changedQuantity;

        @Schema(description = "变更交期", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "变更交期不能为空")
        private LocalDate changedDeliveryDate;

        @Schema(description = "变更备注")
        private String changedSupplierRemark;
    }
}
