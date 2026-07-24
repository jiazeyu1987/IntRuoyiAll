package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 统一审批中心审核 Request VO")
@Data
@Accessors(chain = true)
public class ApprovalTaskReviewReqVO {

    @Schema(description = "模块编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "MES_FEEDBACK")
    @NotNull(message = "模块编码不能为空")
    private ApprovalModuleCode moduleCode;

    @Schema(description = "来源任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "MES_PRO_FEEDBACK")
    @NotBlank(message = "来源任务类型不能为空")
    private String sourceTaskType;

    @Schema(description = "来源任务编号")
    private String sourceTaskId;

    @Schema(description = "业务键")
    private String businessKey;

    @Schema(description = "流程实例编号")
    private String processInstanceId;

    @Schema(description = "审核结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVE")
    @NotNull(message = "审核结果不能为空")
    private ApprovalTaskReviewResult result;

    @Schema(description = "审核意见或驳回原因")
    private String reason;

    @Schema(description = "电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "电子签名密码不能为空")
    private String signaturePassword;

    public ApprovalTaskReviewCommand toCommand() {
        return new ApprovalTaskReviewCommand()
                .setModuleCode(moduleCode)
                .setSourceTaskType(sourceTaskType)
                .setSourceTaskId(sourceTaskId)
                .setBusinessKey(businessKey)
                .setProcessInstanceId(processInstanceId)
                .setResult(result)
                .setReason(reason)
                .setSignaturePassword(signaturePassword);
    }
}
