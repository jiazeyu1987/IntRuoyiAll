package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "清除指定排产工单单道工序的手工产能覆盖")
public class MesProScheduleOrderProcessCapacityResetReqVO extends MesProScheduleOrderActionReqVO {
    @NotNull(message = "排产工序不能为空")
    @Schema(description = "排产工序快照编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long scheduleOrderProcessId;
}
