package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Schema(description = "管理后台 - DCC 受控文件任务加签 Request VO")
@Data
public class DccControlledFileCreateSignTaskReqVO {

    @Schema(description = "BPM 任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "task-1")
    @NotBlank(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "加签用户编号集合", requiredMode = Schema.RequiredMode.REQUIRED, example = "[101,102]")
    @NotEmpty(message = "加签用户不能为空")
    private Set<Long> userIds;

    @Schema(description = "加签类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "before")
    @NotBlank(message = "加签类型不能为空")
    private String type;

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "加签原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "需要工艺确认")
    @NotBlank(message = "加签原因不能为空")
    private String reason;

}
