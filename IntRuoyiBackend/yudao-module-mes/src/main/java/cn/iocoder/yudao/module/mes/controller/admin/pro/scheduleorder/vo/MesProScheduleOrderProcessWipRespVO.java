package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 排产工单当前工序在制订单统计 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProScheduleOrderProcessWipRespVO {

    @Schema(description = "工艺路线编号", example = "100")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "ROUTE-001")
    private String routeCode;

    @Schema(description = "工艺路线名称", example = "球囊生产路线")
    private String routeName;

    @Schema(description = "工艺路线版本编号", example = "200")
    private Long routeVersionId;

    @Schema(description = "工艺路线版本号", example = "V1")
    private String routeVersionNo;

    @Schema(description = "工艺路线版本状态", example = "ACTIVE")
    private String routeVersionStatus;

    @Schema(description = "路线工序编号", example = "300")
    private Long routeProcessId;

    @Schema(description = "工序编号", example = "300")
    private Long processId;

    @Schema(description = "工序编码", example = "B010")
    private String processCode;

    @Schema(description = "工序名称", example = "清洗")
    private String processName;

    @Schema(description = "当前在制订单数", example = "5")
    private Long wipOrderCount;

    @Schema(description = "当前班次产能", example = "500")
    private BigDecimal shiftCapacityTotal;

    @Schema(description = "班次产能模式", example = "RESOURCE_CALCULATED")
    private String capacityMode;

    @Schema(description = "班次产能来源", example = "MACHINE")
    private String capacitySource;

    @Schema(description = "资源配置状态", example = "CAPACITY_MISSING")
    private String resourceStatus;

    @Schema(description = "资源配置状态原因", example = "人工人数未配置")
    private String resourceStatusReason;

    @Schema(description = "班次状态", example = "白班")
    private String shiftStatus;

    @Schema(description = "夜班是否启用")
    private Boolean nightShiftEnabled;

    @Schema(description = "开排日期；混合状态时为空")
    private LocalDate plannedStartDate;

    @Schema(description = "当前工序在制快照开排日期是否混合", example = "false")
    private Boolean plannedStartDateMixed;

    @Schema(description = "未完成订单需求", example = "1500")
    private BigDecimal unfinishedDemandQuantity;

    @Schema(description = "按开排约束预计开始时间")
    private LocalDateTime estimatedStartTime;

    @Schema(description = "按班次产能预计完成时间")
    private LocalDateTime estimatedCompletionTime;

    @Schema(description = "今日历史报工数量", example = "320")
    private BigDecimal todayFeedbackQuantity;

    @Schema(description = "当前工序为该工序的排产工单编号列表")
    private List<Long> scheduleOrderIds;

}
