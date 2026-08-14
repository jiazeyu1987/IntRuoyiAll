package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 生产报工分配调整审计 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationAuditRespVO {

    private Long id;
    private Long eventId;
    private Integer allocationVersion;
    private Long sourceAllocationId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private BigDecimal beforeQuantity;
    private BigDecimal afterQuantity;
    private BigDecimal deltaQuantity;
    private Long actorUserId;
    private String adjustmentReason;
    private String allocationMode;
    private String changeSource;
    private LocalDateTime occurredAt;
}
