package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 排产工单工序快照 Response VO")
@Data
public class MesProScheduleOrderProcessRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "排产工单编号", example = "100")
    private Long scheduleOrderId;

    @Schema(description = "路线工序编号", example = "200")
    private Long routeProcessId;

    @Schema(description = "工序编号", example = "300")
    private Long processId;

    @Schema(description = "工序编码", example = "B010")
    private String processCode;

    @Schema(description = "工序名称", example = "吹球囊成型")
    private String processName;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "产能来源", example = "MACHINE")
    private String capacitySource;

    @Schema(description = "产能模式", example = "RESOURCE_CALCULATED")
    private String capacityMode;

    @Schema(description = "快照小时总产能", example = "42.00")
    private BigDecimal hourlyCapacityTotal;

    @Schema(description = "快照班次小时数", example = "10.5")
    private BigDecimal shiftHours;

    @Schema(description = "快照班次总产能", example = "441.00")
    private BigDecimal shiftCapacityTotal;

    @Schema(description = "是否夜班", example = "false")
    private Boolean nightShiftEnabled;

    @Schema(description = "生产系数", example = "3.000000")
    private BigDecimal productionQuantityFactor;

    @Schema(description = "工序资源快照 JSON")
    private String resourceSnapshotJson;

    @Schema(description = "计划数量", example = "432.00")
    private BigDecimal plannedQuantity;

    @Schema(description = "已报工数量", example = "0")
    private BigDecimal reportedQuantity;

    @Schema(description = "真实完工数量（超报按计划量封顶）", example = "0")
    private BigDecimal effectiveCompletedQuantity;

    @Schema(description = "待审批数量", example = "0")
    private BigDecimal pendingApprovalQuantity;

    @Schema(description = "待检数量", example = "0")
    private BigDecimal pendingInspectionQuantity;

    @Schema(description = "超报数量", example = "0")
    private BigDecimal overReportedQuantity;

    @Schema(description = "剩余数量", example = "432.00")
    private BigDecimal remainingQuantity;

    @Schema(description = "工序进度百分比", example = "50.000000")
    private BigDecimal progressPercent;

    @Schema(description = "报工次数", example = "3")
    private Integer feedbackCount;

    @Schema(description = "最近报工时间")
    private LocalDateTime latestFeedbackTime;

    @Schema(description = "历史报工明细")
    private List<FeedbackHistoryRespVO> feedbackHistoryList;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndTime;

    @Schema(description = "是否瓶颈", example = "false")
    private Boolean bottleneckFlag;

    @Schema(description = "是否关键工序", example = "true")
    private Boolean keyProcessFlag;

    @Schema(description = "管理后台 - MES 排产工单工序历史报工 Response VO")
    @Data
    public static class FeedbackHistoryRespVO {

        @Schema(description = "报工编号", example = "1001")
        private Long id;

        @Schema(description = "报工单号", example = "FB202607020001")
        private String code;

        @Schema(description = "报工时间")
        private LocalDateTime feedbackTime;

        @Schema(description = "本次报工数量", example = "25.000000")
        private BigDecimal feedbackQuantity;

        @Schema(description = "合格品数量", example = "24.000000")
        private BigDecimal qualifiedQuantity;

        @Schema(description = "不良品数量", example = "1.000000")
        private BigDecimal unqualifiedQuantity;

        @Schema(description = "待检数量", example = "0.000000")
        private BigDecimal uncheckQuantity;

        @Schema(description = "报工人编号", example = "1024")
        private Long feedbackUserId;

        @Schema(description = "报工人昵称", example = "张三")
        private String feedbackUserNickname;

        @Schema(description = "报工状态", example = "20")
        private Integer status;

        @Schema(description = "报工状态名称", example = "已完成")
        private String statusName;

        @Schema(description = "备注")
        private String remark;

    }

}
