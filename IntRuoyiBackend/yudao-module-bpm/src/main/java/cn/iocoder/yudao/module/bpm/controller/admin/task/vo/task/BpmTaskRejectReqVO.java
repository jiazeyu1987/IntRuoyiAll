package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "管理后台 - 不通过流程任务的 Request VO")
@Data
public class BpmTaskRejectReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotEmpty(message = "任务编号不能为空")
    private String id;

    @Schema(description = "审批意见", requiredMode = Schema.RequiredMode.REQUIRED, example = "不错不错！")
    @NotBlank(message = "驳回原因不能为空")
    @Pattern(regexp = "(?s).*[^\\p{javaWhitespace}\\p{javaSpaceChar}\\x00-\\x20\\uFEFF].*", message = "驳回原因不能为空")
    private String reason;

}
