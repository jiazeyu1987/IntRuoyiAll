package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "供应商门户 - SRM 送收货回传 Request VO")
@Data
public class SrmOutsourceExecutionReceiveReqVO {

    @Schema(description = "委外执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "委外执行单编号不能为空")
    private Long id;

    @Schema(description = "收货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "收货数量不能为空")
    private BigDecimal receivedQuantity;

    @Schema(description = "送收货说明", example = "模拟收货回传")
    private String receiveRemark;
}
