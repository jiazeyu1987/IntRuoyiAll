package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "供应商门户 - SRM 委外进度回传 Request VO")
@Data
public class SrmOutsourceExecutionProgressReqVO {

    @Schema(description = "委外执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "委外执行单编号不能为空")
    private Long id;

    @Schema(description = "进度百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "55")
    @NotNull(message = "进度百分比不能为空")
    private BigDecimal progressPercent;

    @Schema(description = "进度阶段", requiredMode = Schema.RequiredMode.REQUIRED, example = "加工中")
    @NotNull(message = "进度阶段不能为空")
    private String progressStage;

    @Schema(description = "进度说明", example = "模拟进度回传")
    private String progressRemark;
}
