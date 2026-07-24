package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - MES 排程日历规则保存 Request VO")
@Data
public class MesProScheduleCalendarRulesSaveReqVO {

    @Schema(description = "是否跳过法定节假日", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否跳过法定节假日不能为空")
    private Boolean skipStatutoryHolidays;

    @Schema(description = "周末休息模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "DOUBLE")
    @NotNull(message = "周末休息模式不能为空")
    private String weekendRestMode;

    @Schema(description = "按日期覆盖的班次模式，key 为 yyyy-MM-dd，value 为 REST/DAY；夜班仅由工序排产配置决定")
    private Map<String, String> dateShiftModeByDate;

}
