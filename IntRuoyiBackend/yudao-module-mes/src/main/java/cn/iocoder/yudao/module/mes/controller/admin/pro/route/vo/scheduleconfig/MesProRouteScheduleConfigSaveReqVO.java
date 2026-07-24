package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 路线排产配置保存 Request VO")
@Data
public class MesProRouteScheduleConfigSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "路线版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "路线版本编号不能为空")
    private Long routeVersionId;

    @Schema(description = "历史产品物料编号，不再参与排产配置唯一性", example = "300")
    private Long itemId;

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "路线工序编号不能为空")
    private Long routeProcessId;

    @Schema(description = "产能模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "RESOURCE_CALCULATED")
    @NotNull(message = "产能模式不能为空")
    private String capacityMode;

    @Schema(description = "每小时产能")
    private BigDecimal hourlyCapacity;

    @Schema(description = "无限产能公式数量系数 a")
    private BigDecimal infiniteDurationQuantityFactor;

    @Schema(description = "无限产能公式基础分钟 b")
    private BigDecimal infiniteDurationBaseMinutes;

    @Schema(description = "是否启用夜班", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "夜班开关不能为空")
    private Boolean nightShiftEnabled;

    @Schema(description = "日历规则编号")
    private Long calendarRuleId;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "备注")
    private String remark;

}
