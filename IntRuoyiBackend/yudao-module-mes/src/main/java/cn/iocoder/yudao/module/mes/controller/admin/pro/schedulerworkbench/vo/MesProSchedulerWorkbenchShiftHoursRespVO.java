package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 排产员工作台班次小时 Response VO")
@Data
public class MesProSchedulerWorkbenchShiftHoursRespVO {

    @Schema(description = "当前统一班次小时；存在多个已配置值时为空")
    private BigDecimal shiftHours;

    @Schema(description = "工作站总数")
    private Long workstationCount;

    @Schema(description = "已配置班次小时的工作站数")
    private Long configuredWorkstationCount;

    @Schema(description = "未配置班次小时的工作站数")
    private Long missingWorkstationCount;

    @Schema(description = "不同班次小时值数量")
    private Long distinctShiftHoursCount;

    @Schema(description = "本次更新工作站数量")
    private Integer updatedWorkstationCount;

}
