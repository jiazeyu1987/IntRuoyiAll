package cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "供应商门户 - SRM 采购订单确认 Request VO")
@Data
public class SrmPurchaseOrderConfirmReqVO {

    @Schema(description = "采购订单协同单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "采购订单协同单编号不能为空")
    private Long id;

    @Schema(description = "确认备注", example = "可按期交付")
    private String confirmRemark;

    @Schema(description = "确认行", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "采购订单确认行不能为空")
    private List<Line> lines;

    @Data
    public static class Line {

        @Schema(description = "订单行编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "订单行编号不能为空")
        private Long orderLineId;

        @Schema(description = "确认数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull(message = "确认数量不能为空")
        private BigDecimal confirmedQuantity;

        @Schema(description = "确认交期", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "确认交期不能为空")
        private LocalDate confirmedDeliveryDate;

        @Schema(description = "供应商行备注", example = "分两批交付")
        private String supplierRemark;
    }
}
