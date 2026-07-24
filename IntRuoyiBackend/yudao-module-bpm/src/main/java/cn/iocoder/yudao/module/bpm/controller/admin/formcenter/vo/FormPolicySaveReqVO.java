package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单中心策略保存 Request VO")
@Data
public class FormPolicySaveReqVO {

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

    @Schema(description = "策略类型")
    @NotBlank
    private String policyType;

    @Schema(description = "审批模式：BPM_REQUIRED/DIRECT")
    private String approvalMode;

    @Schema(description = "BPM 流程 key")
    private String bpmProcessKey;

    @Schema(description = "生效执行器编码")
    @NotBlank
    private String effectExecutorCode;

    @Schema(description = "表单槽位")
    @Valid
    @NotEmpty
    private List<FormPolicySlotReqVO> slots;

    @Schema(description = "备注")
    private String remark;

}
