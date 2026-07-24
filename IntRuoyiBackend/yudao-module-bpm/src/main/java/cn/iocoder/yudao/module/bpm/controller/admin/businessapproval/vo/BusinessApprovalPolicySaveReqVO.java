package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 平台业务审批策略保存 Request VO")
@Data
public class BusinessApprovalPolicySaveReqVO {

    @Schema(description = "数据域")
    @NotBlank
    private String dataDomain;

    @Schema(description = "系统编码")
    @NotBlank
    private String systemCode;

    @Schema(description = "对象类型")
    @NotBlank
    private String objectType;

    @Schema(description = "动作编码")
    @NotBlank
    private String actionCode;

    @Schema(description = "对象状态")
    @NotBlank
    private String objectState;

    @Schema(description = "策略模式")
    @NotBlank
    private String policyMode;

    @Schema(description = "BPM 流程定义 key")
    private String processDefinitionKey;

    @Schema(description = "业务生效执行器编码")
    @NotBlank
    private String effectExecutorCode;

    @Schema(description = "备注")
    private String remark;

}
