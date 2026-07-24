package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心 BPM 流程取消 Request VO")
@Data
public class FormBpmProcessCancelledReqVO {

    @Schema(description = "BPM 流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String processInstanceId;

    @Schema(description = "取消原因")
    private String reason;

}
