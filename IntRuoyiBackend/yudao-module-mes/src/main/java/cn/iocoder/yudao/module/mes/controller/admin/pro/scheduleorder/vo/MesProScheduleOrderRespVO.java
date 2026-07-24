package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - MES 排产工单 Response VO")
@Data
public class MesProScheduleOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "排产工单编码", example = "SCH-20260610-0001")
    private String code;

    @Schema(description = "生产工单编号", example = "100")
    private Long workOrderId;

    @Schema(description = "ERP 工单编码", example = "881MO090880")
    private String erpWorkOrderCode;

    @Schema(description = "生产用料清单数量", example = "2")
    private Integer productionMaterialListCount;

    @Schema(description = "生产用料清单摘要", example = "PPBOM-001、PPBOM-002")
    private String productionMaterialListSummary;

    @Schema(description = "产品编号", example = "200")
    private Long productId;

    @Schema(description = "产品编码", example = "YXN.037.011.1007")
    private String productCode;

    @Schema(description = "产品名称", example = "球囊扩张导管")
    private String productName;

    @Schema(description = "规格型号", example = "S020015-4")
    private String productSpecification;

    @Schema(description = "排产数量", example = "432.00")
    private BigDecimal quantity;

    @Schema(description = "总数量", example = "432.000000")
    private BigDecimal totalQuantity;

    @Schema(description = "已完成数量", example = "120.000000")
    private BigDecimal completedQuantity;

    @Schema(description = "真实完工数量（超报按计划量封顶）", example = "120.000000")
    private BigDecimal effectiveCompletedQuantity;

    @Schema(description = "待审批数量", example = "10.000000")
    private BigDecimal pendingApprovalQuantity;

    @Schema(description = "待检数量", example = "5.000000")
    private BigDecimal pendingInspectionQuantity;

    @Schema(description = "超报数量", example = "2.000000")
    private BigDecimal overReportedQuantity;

    @Schema(description = "未完成数量", example = "312.000000")
    private BigDecimal uncompletedQuantity;

    @Schema(description = "承诺交期")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate promiseDate;

    @Schema(description = "优先级排序", example = "10")
    private Integer priorityNo;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "进度百分比", example = "50.000000")
    private BigDecimal progressPercent;

    @Schema(description = "差异状态", example = "0")
    private Integer diffStatus;

    @Schema(description = "风险状态", example = "0")
    private Integer riskStatus;

    @Schema(description = "是否冻结", example = "false")
    private Boolean frozen;

    @Schema(description = "冻结时间")
    private LocalDateTime frozenTime;

    @Schema(description = "冻结人", example = "1")
    private Long frozenBy;

    @Schema(description = "冻结原因")
    private String freezeReason;

    @Schema(description = "是否人工完成", example = "false")
    private Boolean manualFinished;

    @Schema(description = "人工完成时间")
    private LocalDateTime manualFinishedTime;

    @Schema(description = "人工完成人", example = "1")
    private Long manualFinishedBy;

    @Schema(description = "人工完成原因")
    private String manualFinishedReason;

    @Schema(description = "最晚开工时间")
    private LocalDateTime latestStartTime;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndTime;

    @Schema(description = "工艺路线编号", example = "300")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "ROUTE-XLSX-00001")
    private String routeCode;

    @Schema(description = "工艺路线名称", example = "球囊扩张导管工艺路线")
    private String routeName;

    @Schema(description = "工艺路线快照版本", example = "ROUTE-ROUTE-A-20260610-0001")
    private String routeVersion;

    @Schema(description = "排产配置版本", example = "ROUTE-ROUTE-A-20260610-0001")
    private String scheduleConfigVersion;

    @Schema(description = "当前进行工序编号", example = "300")
    private Long currentProcessId;

    @Schema(description = "当前进行路线工序编号", example = "301")
    private Long currentRouteProcessId;

    @Schema(description = "当前进行工序编码", example = "B010")
    private String currentProcessCode;

    @Schema(description = "当前进行工序名称", example = "吹球囊成型")
    private String currentProcessName;

    @Schema(description = "当前进行工序进度百分比", example = "50.000000")
    private BigDecimal currentProcessProgressPercent;

    @Schema(description = "来源生产工单快照 JSON")
    private String sourceSnapshotJson;

    @Schema(description = "产能快照 JSON")
    private String capacitySnapshotJson;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
