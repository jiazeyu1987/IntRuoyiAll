package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 平台业务审批策略切换模式 Request VO")
@Data
public class BusinessApprovalPolicySwitchModeReqVO {

    @Schema(description = "目标策略模式")
    @NotBlank
    private String policyMode;

    @Schema(description = "电子签名密码")
    private String signaturePassword;

}
