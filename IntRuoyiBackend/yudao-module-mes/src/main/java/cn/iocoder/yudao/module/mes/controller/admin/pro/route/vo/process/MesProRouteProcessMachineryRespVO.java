package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 工艺路线工序设备 Response VO")
@Data
public class MesProRouteProcessMachineryRespVO {

    @Schema(description = "工作站设备绑定编号", example = "1")
    private Long workstationMachineId;

    @Schema(description = "工作站编号", example = "1")
    private Long workstationId;

    @Schema(description = "工作站编码", example = "WS-B010")
    private String workstationCode;

    @Schema(description = "工作站名称")
    private String workstationName;

    @Schema(description = "设备编号", example = "1")
    private Long machineryId;

    @Schema(description = "设备编码", example = "M001")
    private String machineryCode;

    @Schema(description = "设备名称")
    private String machineryName;

    @Schema(description = "设备数量", example = "2")
    private Integer quantity;

    @Schema(description = "设备工序单台标准小时产能", example = "25.714286")
    private BigDecimal machineryStandardHourlyCapacity;

    @Schema(description = "设备工序总标准小时产能", example = "51.428572")
    private BigDecimal machineryHourlyCapacityTotal;

    @Schema(description = "今日可用设备数量", example = "1")
    private Integer availableQuantity;

    @Schema(description = "今日可用设备小时产能", example = "25.714286")
    private BigDecimal availableHourlyCapacityTotal;

    @Schema(description = "今日可用设备班次产能", example = "270.000003")
    private BigDecimal availableShiftCapacityTotal;

    @Schema(description = "是否维修中", example = "false")
    private Boolean underRepair;

    @Schema(description = "设备可用状态：NORMAL/REPAIR")
    private String availabilityStatus;

    @Schema(description = "设备可用原因")
    private String availabilityReason;

}
