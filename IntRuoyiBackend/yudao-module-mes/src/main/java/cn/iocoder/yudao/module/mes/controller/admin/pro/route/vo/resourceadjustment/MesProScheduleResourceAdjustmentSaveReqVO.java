package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES 排产日资源调整保存 Request VO")
@Data
public class MesProScheduleResourceAdjustmentSaveReqVO {

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "922046")
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "工艺路线工序ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "922339")
    @NotNull(message = "工艺路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效日期不能为空")
    private LocalDate calendarDate;

    @Schema(description = "资源类型：MACHINE/WORKER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @Schema(description = "工位ID")
    private Long workstationId;

    @Schema(description = "工位设备绑定ID")
    private Long workstationMachineId;

    @Schema(description = "设备ID")
    private Long machineryId;

    @Schema(description = "设备可用数量覆盖")
    private Integer availableQuantityOverride;

    @Schema(description = "人工人数覆盖")
    private Integer workerQuantityOverride;

    @Schema(description = "单人小时产能覆盖")
    private BigDecimal singleHourlyCapacityOverride;

    @Schema(description = "班次小时覆盖")
    private BigDecimal shiftHoursOverride;

    @Schema(description = "调整原因")
    private String reason;

}
