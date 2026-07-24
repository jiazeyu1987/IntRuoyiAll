package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.module.bpm.formcenter.model.FormPolicySlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单中心业务动作解析 Response VO")
@Data
public class FormActionResolutionRespVO {

    private Long policyId;

    private String policyType;

    private String approvalMode;

    private Boolean requiresForm;

    private Boolean requiresBpm;

    private String bpmProcessKey;

    private List<FormPolicySlot> slots;

}
