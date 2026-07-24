package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 表单中心 BPM 当前任务创建 Request VO")
@Data
public class FormBpmTaskCreatedReqVO {

    @Schema(description = "BPM 流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String processInstanceId;

    @Schema(description = "BPM 当前任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String taskId;

    @Schema(description = "当前任务处理人用户编号集合", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<Long> handlerUserIds;

}
