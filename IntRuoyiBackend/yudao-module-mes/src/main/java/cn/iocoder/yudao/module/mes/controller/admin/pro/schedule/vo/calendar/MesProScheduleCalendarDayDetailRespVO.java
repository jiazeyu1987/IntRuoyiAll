package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 排程日历单日详情 Response VO")
@Data
public class MesProScheduleCalendarDayDetailRespVO {

    private String date;

    private String simulationCurrentDate;

    private Boolean holiday;

    private String dateShiftMode;

    private Integer dayShiftTaskCount;

    private Integer nightShiftTaskCount;

    private List<WorkshopDetailItem> workshops;

    private MaterialShortageSummary materialShortageSummary;

    private MaterialDemandSummary materialDemandSummary;

    private ScheduleIssueSummary scheduleIssueSummary;

    private ProcessCapacitySummary processCapacitySummary;

    @Data
    @Builder
    public static class WorkshopDetailItem {
        private Long workshopId;
        private String workshopCode;
        private String workshopName;
        private Integer taskCount;
        private Integer orderCount;
        private Integer busyLineCount;
        private List<LineDetailItem> lines;
    }

    @Data
    @Builder
    public static class LineDetailItem {
        private Long lineId;
        private String lineCode;
        private String lineName;
        private Integer taskCount;
        private Integer orderCount;
        private List<TaskDetailItem> tasks;
    }

    @Data
    @Builder
    public static class TaskDetailItem {
        private Long taskId;
        private String taskCode;
        private Long workOrderId;
        private String workOrderCode;
        private Long routeId;
        private String routeName;
        private String processName;
        private String itemCode;
        private String itemName;
        private String shiftCode;
        private BigDecimal quantity;
        private BigDecimal dailyQuantity;
        private BigDecimal reportedQuantity;
        private BigDecimal pendingInspectionQuantity;
        private String executionStatus;
        private String startTime;
        private String endTime;
        private String scheduleSource;
        private Boolean locked;
        private String riskStatus;
        private Boolean scheduleOrderFrozen;
        private String scheduleOrderFreezeReason;
    }

    @Data
    @Builder
    public static class MaterialShortageSummary {
        private Integer shortageCount;
        private BigDecimal totalShortageQty;
        private List<MaterialShortageItem> items;
    }

    @Data
    @Builder
    public static class MaterialShortageItem {
        private Long issueId;
        private String severity;
        private Long workOrderId;
        private String workOrderCode;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal scheduledUsageQty;
        private BigDecimal remainingAvailableQty;
        private Integer affectedWorkOrderCount;
        private BigDecimal requiredQty;
        private BigDecimal availableQty;
        private BigDecimal shortageQty;
        private String message;
    }

    @Data
    @Builder
    public static class MaterialDemandSummary {
        private Integer materialCount;
        private Integer workOrderCount;
        private List<MaterialDemandTotalItem> totalItems;
        private List<MaterialDemandWorkOrderItem> workOrderItems;
    }

    @Data
    @Builder
    public static class MaterialDemandTotalItem {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requiredQty;
        private BigDecimal availableQty;
        private BigDecimal shortageQty;
        private Integer affectedWorkOrderCount;
    }

    @Data
    @Builder
    public static class MaterialDemandWorkOrderItem {
        private Long workOrderId;
        private String workOrderCode;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requiredQty;
        private BigDecimal availableQty;
        private BigDecimal shortageQty;
    }

    @Data
    @Builder
    public static class ScheduleIssueSummary {
        private Integer openIssueCount;
        private Integer blockingIssueCount;
        private List<ScheduleIssueItem> items;
    }

    @Data
    @Builder
    public static class ScheduleIssueItem {
        private Long issueId;
        private String issueType;
        private String severity;
        private Long workOrderId;
        private String workOrderCode;
        private Long taskId;
        private String message;
        private String status;
        private String sourceType;
        private Long sourceId;
    }

    @Data
    @Builder
    public static class ProcessCapacitySummary {
        private Integer processCount;
        private BigDecimal totalMaxCapacity;
        private BigDecimal totalScheduledQuantity;
        private BigDecimal totalRemainingCapacity;
        private List<ProcessCapacityItem> items;
    }

    @Data
    @Builder
    public static class ProcessCapacityItem {
        private Long processId;
        private String processName;
        private Integer taskCount;
        private Integer workOrderCount;
        private BigDecimal maxCapacity;
        private BigDecimal scheduledQuantity;
        private BigDecimal remainingCapacity;
        private BigDecimal overCapacity;
        private BigDecimal utilizationRate;
    }

}
