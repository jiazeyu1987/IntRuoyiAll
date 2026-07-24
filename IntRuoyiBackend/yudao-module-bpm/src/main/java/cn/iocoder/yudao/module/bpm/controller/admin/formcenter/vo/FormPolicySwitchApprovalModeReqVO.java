package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心策略审批模式切换 Request VO")
@Data
public class FormPolicySwitchApprovalModeReqVO {

    @Schema(description = "审批模式：BPM_REQUIRED/DIRECT")
    @NotBlank
    private String approvalMode;

    @Schema(description = "BPM 流程 key；切换到 BPM_REQUIRED 时必填或沿用原策略")
    private String bpmProcessKey;

}
