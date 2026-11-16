package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalReceiptQueryReqVO {
    @NotNull
    private ApprovalModuleCode moduleCode;
    @NotBlank @Size(max = 128)
    private String idempotencyKey;
    @NotBlank @Size(max = 64)
    private String sourceTaskType;
    @NotBlank @Size(max = 128)
    private String sourceTaskId;
    @Size(min = 1, max = 128)
    private String prepareId;
}
