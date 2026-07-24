package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR OQ/PQ 步骤结果 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrOqPqStepResultRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "用例ID")
    private Long caseId;

    @Schema(description = "执行记录ID")
    private Long runId;

    @Schema(description = "步骤编号")
    private String stepNo;

    @Schema(description = "步骤标题")
    private String stepTitle;

    @Schema(description = "预期结果")
    private String expectedResult;

    @Schema(description = "实际结果")
    private String actualResult;

    @Schema(description = "步骤结果")
    private String stepResult;

    @Schema(description = "执行人")
    private String executorName;

    @Schema(description = "复核人")
    private String reviewerName;

    @Schema(description = "执行时间")
    private LocalDateTime executedAt;

    @Schema(description = "附件或证据标识")
    private String attachmentEvidence;

    @Schema(description = "证据校验值")
    private String evidenceChecksum;

    @Schema(description = "偏差ID")
    private Long deviationId;

    @Schema(description = "下一步动作")
    private String nextAction;
}
