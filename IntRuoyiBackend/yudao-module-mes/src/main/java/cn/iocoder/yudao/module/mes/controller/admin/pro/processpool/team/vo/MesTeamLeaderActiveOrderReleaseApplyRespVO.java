package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES 生产组长活跃订单申请放行 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseApplyRespVO {

    @Schema(description = "申请记录编号", example = "10001")
    private Long applicationId;

    @Schema(description = "活跃订单记录编号", example = "8101")
    private Long activeOrderId;

    @Schema(description = "生产订单编号", example = "9001")
    private Long workOrderId;

    @Schema(description = "生产工单号", example = "MO20260808001")
    private String workOrderCode;

    @Schema(description = "eDHR批次执行编号", example = "7001")
    private Long batchExecutionId;

    @Schema(description = "放行事务编号", example = "8001")
    private Long releaseTransactionId;

    @Schema(description = "生产负责人放行待办编号", example = "9001")
    private Long releaseApprovalWorkTaskId;

    @Schema(description = "申请状态", example = "PENDING_RELEASE_APPROVAL")
    private String status;

    @Schema(description = "申请状态名称", example = "待生产负责人放行")
    private String statusName;

    @Schema(description = "资料摘要")
    private MesTeamLeaderActiveOrderReleaseDossierSummaryRespVO dossierSummary;

    @Schema(description = "阻塞项")
    private List<MesTeamLeaderActiveOrderReleaseBlockerRespVO> blockers;

    @Schema(description = "申请时间")
    private LocalDateTime appliedAt;
}
