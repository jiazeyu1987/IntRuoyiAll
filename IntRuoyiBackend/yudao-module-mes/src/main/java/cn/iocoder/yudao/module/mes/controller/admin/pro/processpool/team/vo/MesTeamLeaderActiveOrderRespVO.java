package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Schema(description = "放行资料申请状态", example = "PENDING_RELEASE_APPROVAL")
    private String releaseApplicationStatus;

    @Schema(description = "放行资料申请阻塞摘要", example = "缺少过程检验汇集明细")
    private String releaseApplicationBlockerSummary;

    @Schema(description = "放行审批待办编号", example = "7001")
    private Long releaseApprovalWorkTaskId;
}
