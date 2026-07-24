package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - MES 排产工单修改 Request VO")
@Data
public class MesProScheduleOrderUpdateReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "排产工单不能为空")
    private Long id;

    @Schema(description = "承诺交期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "承诺交期不能为空")
    private LocalDate promiseDate;

    @Schema(description = "优先级排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "排产工单优先级必须大于等于 1")
    private Integer priorityNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "修改原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修改原因不能为空")
    private String reason;

}
