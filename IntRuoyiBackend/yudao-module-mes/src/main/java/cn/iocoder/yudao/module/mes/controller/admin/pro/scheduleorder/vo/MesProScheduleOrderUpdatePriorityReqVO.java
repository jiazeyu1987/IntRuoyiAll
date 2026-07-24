package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 排产工单优先级更新 Request VO")
@Data
public class MesProScheduleOrderUpdatePriorityReqVO {

    @Schema(description = "排产工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "排产工单编号不能为空")
    private Long id;

    @Schema(description = "优先级排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级必须大于等于 1")
    private Integer priorityNo;

}
