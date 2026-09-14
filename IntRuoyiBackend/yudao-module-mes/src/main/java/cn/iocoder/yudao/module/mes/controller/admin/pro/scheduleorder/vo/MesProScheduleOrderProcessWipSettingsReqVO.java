package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES 排产工单工序在制设置保存 Request VO")
@Data
public class MesProScheduleOrderProcessWipSettingsReqVO {

    @Schema(description = "工艺路线版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "工艺路线版本不能为空")
    private Long routeVersionId;

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "300")
    @NotNull(message = "路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "是否启用夜班")
    private Boolean nightShiftEnabled;

    @Schema(description = "开排日期")
    private LocalDate plannedStartDate;

    @Schema(description = "手工调整后的班次产能")
    private BigDecimal shiftCapacityTotal;

    @Schema(description = "操作原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

}
