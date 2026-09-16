package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES AI E2E 确定性创建金蝶生产订单 Request VO")
@Data
public class MesKingdeeProductionOrderDeterministicCreateReqVO {

    @Schema(description = "本轮唯一运行编号", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "AI-EDHR-20260915T120000-A1B2")
    @NotBlank(message = "runId不能为空")
    private String runId;

    @Schema(description = "AI E2E订单槽位", requiredMode = Schema.RequiredMode.REQUIRED, example = "O01")
    @NotBlank(message = "slot不能为空")
    private String slot;

    @Schema(description = "固定生产数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "quantity不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "quantity必须为正数")
    private BigDecimal quantity;

    @Schema(description = "本轮生产批号", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "AI-EDHR-20260915T120000-A1B2-B01")
    @NotBlank(message = "batchNumber不能为空")
    private String batchNumber;
}
