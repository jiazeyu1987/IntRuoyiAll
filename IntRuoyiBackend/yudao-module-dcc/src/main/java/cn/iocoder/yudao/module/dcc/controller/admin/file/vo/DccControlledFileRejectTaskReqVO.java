package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - DCC 受控文件审批驳回 Request VO")
@Data
public class DccControlledFileRejectTaskReqVO {

    @Schema(description = "BPM 任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "task-1")
    @NotBlank(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "版本描述不完整")
    @NotBlank(message = "驳回原因不能为空")
    private String reason;

}
