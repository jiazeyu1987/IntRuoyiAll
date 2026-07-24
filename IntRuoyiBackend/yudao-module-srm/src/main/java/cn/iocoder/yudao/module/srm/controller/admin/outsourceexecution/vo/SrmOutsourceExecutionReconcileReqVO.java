package cn.iocoder.yudao.module.srm.controller.admin.outsourceexecution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - SRM 对账确认 Request VO")
@Data
public class SrmOutsourceExecutionReconcileReqVO {

    @Schema(description = "委外执行单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "委外执行单编号不能为空")
    private Long id;

    @Schema(description = "对账说明", example = "模拟对账确认")
    private String confirmRemark;
}
