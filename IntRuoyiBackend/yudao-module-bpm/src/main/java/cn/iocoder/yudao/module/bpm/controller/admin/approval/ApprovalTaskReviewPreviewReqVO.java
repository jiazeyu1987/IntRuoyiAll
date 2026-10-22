package cn.iocoder.yudao.module.bpm.controller.admin.approval;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class ApprovalTaskReviewPreviewReqVO {
    @NotNull private ApprovalModuleCode moduleCode;
    @NotBlank private String sourceTaskType;
    @NotBlank private String sourceTaskId;
    private String processInstanceId;
}
