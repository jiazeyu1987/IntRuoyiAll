package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - SRM 来料检验 Request VO")
@Data
public class SrmOutsourceExecutionInspectReqVO {

    @Schema(description = "委外执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "委外执行单编号不能为空")
    private Long id;

    @Schema(description = "合格数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "合格数量不能为空")
    private BigDecimal qualifiedQuantity;

    @Schema(description = "检验说明", example = "模拟检验合格")
    private String inspectRemark;
}
