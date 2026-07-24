package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - DCC 受控文件任务转交 Request VO")
@Data
public class DccControlledFileTransferTaskReqVO {

    @Schema(description = "BPM 任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "task-1")
    @NotBlank(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "新处理人用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "新处理人不能为空")
    private Long assigneeUserId;

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "转交原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "请代为处理")
    @NotBlank(message = "转交原因不能为空")
    private String reason;

}
