package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 排程日历产能生成 Request VO")
@Data
public class MesProScheduleCalendarCapacityGenerateReqVO {

    @Schema(description = "开始日期，格式 YYYY-MM-DD", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-06-10")
    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    @Schema(description = "生成天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "生成天数不能为空")
    @Min(value = 1, message = "生成天数必须大于 0")
    @Max(value = 366, message = "生成天数不能超过 366")
    private Integer days;

    @Schema(description = "产线编号列表；为空时生成当前租户下所有已绑定排班计划的启用产线")
    private List<Long> lineIds;

}
