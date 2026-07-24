package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES 排产日资源调整 Response VO")
@Data
public class MesProScheduleResourceAdjustmentRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "工艺路线ID", example = "922046")
    private Long routeId;

    @Schema(description = "工艺路线工序ID", example = "922339")
    private Long routeProcessId;

    @Schema(description = "生效日期")
    private LocalDate calendarDate;

    @Schema(description = "资源类型", example = "MACHINE")
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
