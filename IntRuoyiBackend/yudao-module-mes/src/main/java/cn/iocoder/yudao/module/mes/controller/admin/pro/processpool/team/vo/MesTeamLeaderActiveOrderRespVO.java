package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 班组长活跃订单 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRespVO {

    @Schema(description = "活跃订单记录编号", example = "8101")
    private Long id;

    @Schema(description = "生产订单编号", example = "9001")
    private Long workOrderId;

    @Schema(description = "生产工单号", example = "881MO090889")
    private String workOrderCode;

    @Schema(description = "产品名称", example = "球囊扩张压力泵")
    private String productName;

    @Schema(description = "产品编码", example = "AW.107.02.01.2010")
    private String productCode;

    @Schema(description = "生产订单数量", example = "2248")
    private BigDecimal quantity;

    @Schema(description = "正式工艺路线编号", example = "922119")
    private Long routeId;

    @Schema(description = "正式工艺路线名称", example = "按压式球囊扩充压力泵工艺路线")
    private String routeName;

    @Schema(description = "正式工艺路线版本编号", example = "448")
    private Long routeVersionId;

    @Schema(description = "正式工艺路线版本号", example = "V1")
    private String routeVersionNo;

    @Schema(description = "ERP固定生产数量快照", example = "200")
    private BigDecimal erpFixedQuantitySnapshot;

    @Schema(description = "各工序剩余可分配生产数量")
    private List<ProcessRemainingQuantity> processRemainingQuantities = List.of();

    @Schema(description = "生产进度百分比", example = "10.000000")
    private BigDecimal productionProgressPercent;

    @Schema(description = "检验进度百分比", example = "10.000000")
    private BigDecimal inspectionProgressPercent;

    @Schema(description = "活跃状态", example = "ACTIVE")
    private String activeStatus;

    @Schema(description = "跨角色业务状态", example = "ACTIVE")
    private String businessStatus;

    @Schema(description = "加入活跃池时间")
    private LocalDateTime joinedAt;

    @Schema(description = "移除活跃池时间")
    private LocalDateTime removedAt;

    @Schema(description = "乐观锁版本", example = "0")
    private Integer version;

    @Schema(description = "是否存在未关闭异常", example = "true")
    private Boolean abnormal;

    @Schema(description = "最近未关闭异常原因", example = "设备停机，影响生产")
    private String abnormalReason;

    @Schema(description = "最近未关闭异常上报时间")
    private LocalDateTime abnormalReportedAt;

    @Schema(description = "生产放行申请编号", example = "7001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long releaseApplicationId;

    @Schema(description = "PQC生产放行待办编号", example = "8001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pqcReleaseWorkTaskId;

    @Schema(description = "生产放行申请状态", example = "PQC_RELEASE_PENDING")
    private String releaseApplicationStatus;

    @Schema(description = "生产放行正式来源快照哈希")
    private String releaseSourceSnapshotHash;

    @Schema(description = "生产放行申请版本", example = "1")
    private Integer releaseApplicationVersion;

    @Schema(description = "是否存在生产数量冲突", example = "true")
    private Boolean quantityConflict;

    @Schema(description = "是否存在生产数量冲突", example = "true")
    private Boolean hasQuantityConflict;

    @Schema(description = "存在数量冲突的工序数", example = "1")
    private Integer quantityConflictProcessCount;

    @Schema(description = "累计超出工序目标数量", example = "1500")
    private BigDecimal overageQuantity;

    @Schema(description = "管理后台 - MES 班组长活跃订单工序剩余量 Response VO")
    @Data
    @Accessors(chain = true)
    public static class ProcessRemainingQuantity {

        @Schema(description = "路线工序编号", example = "5001")
        private Long routeProcessId;

        @Schema(description = "工序编号", example = "6001")
        private Long processId;

        @Schema(description = "当前工序计划生产数量", example = "100")
        private BigDecimal plannedQuantity;

        @Schema(description = "当前工序已分配生产数量", example = "70")
        private BigDecimal allocatedQuantity;

        @Schema(description = "当前工序剩余可分配生产数量", example = "30")
        private BigDecimal remainingQuantity;

        @Schema(description = "当前工序是否存在生产数量冲突", example = "true")
        private Boolean quantityConflict;

        @Schema(description = "当前工序超出目标数量", example = "1500")
        private BigDecimal overageQuantity;
    }
}
