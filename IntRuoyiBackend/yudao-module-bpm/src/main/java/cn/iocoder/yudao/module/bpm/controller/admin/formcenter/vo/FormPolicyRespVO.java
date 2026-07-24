package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 表单中心策略 Response VO")
@Data
public class FormPolicyRespVO {

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

    @Schema(description = "策略类型")
    private String policyType;

    @Schema(description = "审批模式：BPM_REQUIRED/DIRECT")
    private String approvalMode;

    @Schema(description = "BPM 流程 key")
    private String bpmProcessKey;

    @Schema(description = "生效执行器编码")
    private String effectExecutorCode;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "表单槽位")
    private List<FormPolicySlot> slots;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

}
