package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR OQ/PQ 偏差 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrOqPqDeviationRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "用例ID")
    private Long caseId;

    @Schema(description = "执行记录ID")
    private Long runId;

    @Schema(description = "步骤结果ID")
    private Long stepResultId;

    @Schema(description = "偏差编号")
    private String deviationCode;

    @Schema(description = "偏差标题")
    private String deviationTitle;

    @Schema(description = "偏差状态")
    private String deviationStatus;

    @Schema(description = "失败实际结果")
    private String failedActualResult;

    @Schema(description = "原因分析")
    private String rootCause;

    @Schema(description = "整改措施")
    private String remediationAction;

    @Schema(description = "整改责任人")
    private String remediationOwnerName;

    @Schema(description = "复测结果")
    private String retestResult;

    @Schema(description = "复测证据")
    private String retestEvidence;

    @Schema(description = "复测复核人")
    private String retestReviewerName;

    @Schema(description = "关闭签核人")
    private String closeSignoffName;

    @Schema(description = "关闭时间")
    private LocalDateTime closedAt;

    @Schema(description = "阻断原因")
    private String blockedReason;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
