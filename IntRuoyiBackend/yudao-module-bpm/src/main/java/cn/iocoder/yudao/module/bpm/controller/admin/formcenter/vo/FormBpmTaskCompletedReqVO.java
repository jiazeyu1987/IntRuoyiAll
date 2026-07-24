package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 表单中心 BPM 当前任务完成 Request VO")
@Data
public class FormBpmTaskCompletedReqVO {

    @Schema(description = "BPM 流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String processInstanceId;

    @Schema(description = "BPM 当前任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String taskId;

}
