package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 平台业务审批策略 Response VO")
@Data
public class BusinessApprovalPolicyRespVO {

    @Schema(description = "策略编号")
    private Long id;

    @Schema(description = "数据域")
    private String dataDomain;

    @Schema(description = "系统编码")
    private String systemCode;

    @Schema(description = "对象类型")
    private String objectType;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "对象状态")
    private String objectState;

    @Schema(description = "策略模式")
    private String policyMode;

    @Schema(description = "BPM 流程定义 key")
    private String processDefinitionKey;

    @Schema(description = "业务生效执行器编码")
    private String effectExecutorCode;

    @Schema(description = "表单策略类型")
    private String formPolicyType;

    @Schema(description = "表单槽位 JSON")
    private String formSlotsJson;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

}
