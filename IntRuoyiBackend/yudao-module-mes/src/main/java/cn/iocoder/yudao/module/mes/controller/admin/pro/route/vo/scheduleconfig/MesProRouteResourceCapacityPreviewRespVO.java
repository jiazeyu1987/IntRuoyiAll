package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - MES 路线工序资源计算预览 Response VO")
@Data
public class MesProRouteResourceCapacityPreviewRespVO {

    @Schema(description = "路线工序编号")
    private Long routeProcessId;

    @Schema(description = "工序编号")
    private Long processId;

    @Schema(description = "资源计算小时产能")
    private BigDecimal resourceCapacityHourly = BigDecimal.ZERO;

    @Schema(description = "产能来源：MACHINE / WORKER / UNCONFIGURED")
    private String capacitySource;

    @Schema(description = "工作站资源明细")
    private List<WorkstationRow> workstationRows = new ArrayList<>();

    @Schema(description = "阻断项")
    private List<BlockingIssue> blockingIssues = new ArrayList<>();

    @Schema(description = "资源预览工作站行")
    @Data
    public static class WorkstationRow {

        private Long workstationId;

        private String workstationCode;

        private String workstationName;

        private Long productionLineId;

        private String productionLineName;

        private BigDecimal shiftHours;

        private String resourceType;

        private BigDecimal hourlyCapacity = BigDecimal.ZERO;

        private Integer workerQuantity;

        private BigDecimal singleStandardHourlyCapacity;

        private List<MachineRow> machineRows = new ArrayList<>();

        private List<BlockingIssue> blockingIssues = new ArrayList<>();
    }

    @Schema(description = "资源预览设备行")
    @Data
    public static class MachineRow {

        private Long workstationMachineId;

        private Long machineryId;

        private String machineryCode;

        private String machineryName;

        private Integer quantity;

        private BigDecimal standardHourlyCapacity;

        private BigDecimal hourlyCapacity = BigDecimal.ZERO;
    }

    @Schema(description = "资源预览阻断项")
    @Data
    public static class BlockingIssue {

        private String code;

        private String message;

        private Long routeProcessId;

        private Long workstationId;

        private String workstationCode;

        private Long machineryId;

        private String machineryCode;
    }
}
