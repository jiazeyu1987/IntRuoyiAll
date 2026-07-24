package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 第三方报工确认归属 Request VO")
@Data
@Accessors(chain = true)
public class MesProFeedbackImportAttributeReqVO {

    public static final String TARGET_TYPE_CURRENT_ORDER = "CURRENT_ORDER";
    public static final String TARGET_TYPE_EXTERNAL_OTHER_ORDER = "EXTERNAL_OTHER_ORDER";

    @NotNull(message = "待归属记录不能为空")
    private Long importRecordId;

    @Schema(description = "归属目标类型：CURRENT_ORDER 当前订单；EXTERNAL_OTHER_ORDER 其他订单", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetType;

    private Long scheduleOrderId;

    private Long scheduleOrderProcessId;

    @Schema(description = "本次报工数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal feedbackQuantity;

    @Schema(description = "多订单归属明细；为空时使用上方单目标字段")
    @Valid
    private List<Allocation> allocations;

    @Data
    @Accessors(chain = true)
    public static class Allocation {

        @Schema(description = "归属目标类型：CURRENT_ORDER 当前订单；EXTERNAL_OTHER_ORDER 其他订单", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "归属目标类型不能为空")
        private String targetType;

        private Long scheduleOrderId;

        private Long scheduleOrderProcessId;

        @Schema(description = "本次分配报工数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "报工数量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "报工数量必须大于 0")
        private BigDecimal feedbackQuantity;
    }
}
