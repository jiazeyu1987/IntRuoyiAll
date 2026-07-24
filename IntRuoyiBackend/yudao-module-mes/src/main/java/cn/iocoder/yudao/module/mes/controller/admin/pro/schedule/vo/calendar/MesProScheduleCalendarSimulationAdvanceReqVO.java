package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 排程日历模拟日期推进 Request VO")
@Data
public class MesProScheduleCalendarSimulationAdvanceReqVO {

    @Schema(description = "推进天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "推进天数不能为空")
    @Min(value = 1, message = "推进天数必须大于 0")
    private Integer days;

}
