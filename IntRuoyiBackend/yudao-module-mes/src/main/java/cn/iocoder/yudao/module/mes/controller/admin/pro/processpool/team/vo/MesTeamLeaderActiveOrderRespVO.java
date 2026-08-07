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
}
