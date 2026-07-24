package cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES 排产员工作台汇总 Response VO")
@Data
public class MesProSchedulerWorkbenchSummaryRespVO {

    @Schema(description = "统计日期")
    private LocalDate date;

    private Long pendingScheduleOrderCount;
    private Long todayScheduledTaskCount;
    private BigDecimal todayPlannedCapacity;
    private Long todayFeedbackCount;
    private BigDecimal todayFeedbackQuantity;
    private Long pendingApprovalFeedbackCount;
    private BigDecimal currentSchedulePlannedQuantity;
    private BigDecimal currentScheduleReportedQuantity;
    private BigDecimal reportedDeviationQuantity;
    private String reportedDeviationText;
    private BigDecimal todayAvailableCapacity;
    private Long repairingMachineryCount;
    private Long resourceUnconfiguredCount;
    private Long blockingIssueCount;
    private Long materialShortageCount;
    private String nightlyReplanText;
    private String todayActionSuggestion;
    private String currentScheduleScopeText;
    private String globalRiskScopeText;

    private List<Step> steps;
    private List<Bottleneck> bottlenecks;
    private List<ReportedDeviationDetail> reportedDeviationDetails;
    private List<RouteActiveOrder> routeActiveOrders;

    @Data
    public static class Step {
        private Integer sort;
        private String name;
        private String description;
        private String primaryPath;
        private String primaryMetricName;
        private String primaryMetricValue;
    }

    @Data
    public static class Bottleneck {
        private Long scheduleOrderProcessId;
        private Long routeId;
        private Long routeProcessId;
        private String scheduleOrderCode;
        private String workOrderCode;
        private String processCode;
        private String processName;
        private String workstationName;
        private String resourceType;
        private BigDecimal todayCapacity;
        private BigDecimal demandQuantity;
        private BigDecimal scheduledQuantity;
        private BigDecimal gapQuantity;
        private String reason;
        private String targetPath;
    }

    @Data
    public static class ReportedDeviationDetail {
        private Long scheduleOrderId;
        private Long scheduleOrderProcessId;
        private String scheduleOrderCode;
        private String workOrderCode;
        private String processCode;
        private String processName;
        private BigDecimal plannedQuantity;
        private BigDecimal reportedQuantity;
        private BigDecimal deviationQuantity;
        private String processStatus;
    }

    @Data
    public static class RouteActiveOrder {
        private Long routeId;
        private String routeCode;
        private String routeName;
        private Long wipOrderCount;
        private List<RouteActiveProduct> products;
    }

    @Data
    public static class RouteActiveProduct {
        private Long productId;
        private String productCode;
        private String productName;
        private Long wipOrderCount;
    }

}
