package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - eDHR OQ/PQ 步骤提交 Request VO")
@Data
public class MesProEdhrOqPqStepSubmitReqVO {

    @Schema(description = "执行记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行记录ID不能为空")
    private Long runId;

    @Schema(description = "实际结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "实际结果不能为空")
    private String actualResult;

    @Schema(description = "步骤结果：PASS、FAIL、BLOCKED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "步骤结果不能为空")
    private String stepResult;

    @Schema(description = "附件或证据标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "附件或证据标识不能为空")
    private String attachmentEvidence;

    @Schema(description = "证据校验值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证据校验值不能为空")
    private String evidenceChecksum;

    @Schema(description = "备注")
    private String remark;
}
