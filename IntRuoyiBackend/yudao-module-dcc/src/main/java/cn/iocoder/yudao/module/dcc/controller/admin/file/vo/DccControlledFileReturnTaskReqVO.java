package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 受控文件任务回退 Request VO")
@Data
public class DccControlledFileReturnTaskReqVO {

    @Schema(description = "BPM 任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "task-1")
    @NotBlank(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "回退目标任务 Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "MATRIX_REVIEW")
    @NotBlank(message = "回退目标不能为空")
    private String targetTaskDefinitionKey;

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "回退原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "补充会签意见")
    @NotBlank(message = "回退原因不能为空")
    private String reason;

}
