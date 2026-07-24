package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 路线排产配置 Response VO")
@Data
public class MesProRouteScheduleConfigRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "路线版本编号")
    private Long routeVersionId;

    @Schema(description = "路线工序编号")
    private Long routeProcessId;

    @Schema(description = "产能模式")
    private String capacityMode;

    @Schema(description = "每小时产能")
    private BigDecimal hourlyCapacity;

    @Schema(description = "班次小时数")
    private BigDecimal shiftHours;

    @Schema(description = "标准班次产能")
    private BigDecimal standardShiftCapacity;

    @Schema(description = "无限产能公式数量系数 a")
    private BigDecimal infiniteDurationQuantityFactor;

    @Schema(description = "无限产能公式基础分钟 b")
    private BigDecimal infiniteDurationBaseMinutes;

    @Schema(description = "是否启用夜班")
    private Boolean nightShiftEnabled;

    @Schema(description = "日历规则编号")
    private Long calendarRuleId;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "备注")
    private String remark;

}
