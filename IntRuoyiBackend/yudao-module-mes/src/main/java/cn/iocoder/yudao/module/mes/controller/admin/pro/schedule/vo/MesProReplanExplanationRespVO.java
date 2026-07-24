package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 最近一次成功重排说明 Response VO")
@Data
public class MesProReplanExplanationRespVO {

    private Boolean hasData;
    private String requestId;
    private String triggerSource;
    private String capacityMode;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime requestStartTime;
    private LocalDateTime appliedAt;
    private Summary summary;
    private List<OrderItem> orders;
    private List<WorkOrderItem> workOrders;
    private List<DailyExplanationItem> dailyExplanations;
    private List<MaterialItem> materials;
    private ProtectionSummary protectionSummary;
    private List<MesProAutoScheduleProtectedTaskRespVO> protectedTasks;
    private List<MesProAutoScheduleIssueRespVO> issues;

    @Data
    public static class Summary {
        private Integer scheduleOrderCount;
        private Integer workOrderCount;
        private Integer routeCount;
        private Integer processCount;
        private Integer generatedTaskCount;
        private Integer deletedTaskCount;
        private Integer preservedTaskCount;
        private Integer blockingIssueCount;
        private Integer warningIssueCount;
        private Integer shortageCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Data
    public static class OrderItem {
        private Integer rank;
        private Long scheduleOrderId;
        private String scheduleOrderCode;
        private Long workOrderId;
        private String workOrderCode;
        private Long productId;
        private String productCode;
        private String productName;
        private BigDecimal quantity;
        private LocalDate promiseDate;
        private Integer priorityNo;
        private Long routeId;
        private String routeCode;
        private String routeName;
        private Integer processCount;
    }

    @Data
    public static class WorkOrderItem {
        private Long workOrderId;
        private String workOrderCode;
        private Long productId;
        private String productCode;
        private String productName;
        private BigDecimal quantity;
        private Long routeId;
        private String routeCode;
        private String routeName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long bottleneckProcessId;
        private String bottleneckProcessName;
        private BigDecimal bottleneckHourlyCapacity;
        private List<ProcessItem> processes;
    }

    @Data
    public static class ProcessItem {
        private Long processId;
        private String processName;
        private Integer processSort;
        private BigDecimal scheduledQuantity;
        private String capacitySource;
        private List<String> shiftNames;
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

    @Data
    public static class DailyExplanationItem {
        private LocalDate planDate;
        private Long workOrderId;
        private String workOrderCode;
        private Long scheduleOrderProcessId;
        private Long processId;
        private String processName;
        private BigDecimal plannedQuantity;
        private Integer generatedTaskCount;
        private Integer availableWindowMinutes;
        private Integer usedWindowMinutes;
        private Integer protectedOccupiedMinutes;
        private LocalDateTime dependencyReleasedAt;
        private BigDecimal remainingQuantityBefore;
        private BigDecimal remainingQuantityAfter;
        private String reasonCode;
    }

    @Data
    public static class MaterialItem {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requiredQty;
        private BigDecimal availableQty;
        private BigDecimal shortageQty;
        private List<MaterialContribution> orderContributions;
    }

    @Data
    public static class MaterialContribution {
        private Long scheduleOrderId;
        private String scheduleOrderCode;
        private Long workOrderId;
        private String workOrderCode;
        private BigDecimal requiredQty;
    }

    @Data
    public static class ProtectionSummary {
        private Integer totalCount;
        private Integer feedbackCount;
        private Integer inProgressCount;
        private Integer finishedCount;
        private Integer lockedCount;
        private Integer manualCount;
        private Integer otherCount;
    }
}
