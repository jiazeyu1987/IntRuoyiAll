package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 工单产线分析 Response VO")
@Data
@Builder
public class MesProScheduleCalendarWorkOrderAnalysisRespVO {

    private Long workOrderId;

    private String workOrderCode;

    private Long productId;

    private String productCode;

    private String productName;

    private BigDecimal quantity;

    private Boolean conflict;

    private String conflictMessage;

    private Long lineId;

    private String lineCode;

    private String lineName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long bottleneckProcessId;

    private String bottleneckProcessName;

    private BigDecimal bottleneckHourlyCapacity;

    private List<ProcessAnalysisItem> processes;

    @Data
    @Builder
    public static class ProcessAnalysisItem {
        private Long processId;
        private String processName;
        private Integer processSort;
        private BigDecimal scheduledQuantity;
        private String capacitySource;
        private Integer workstationCount;
        private List<String> workstationNames;
        private Integer machineCount;
        private Integer configuredWorkerCount;
        private Integer currentWorkerCount;
        private BigDecimal effectiveHourlyCapacity;
        private Integer plannedDurationMinutes;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean bottleneck;
    }
}
