package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台事故动作 Request VO")
@Data
public class RuntimeControlIncidentActionReqVO {

    @Schema(description = "处置动作", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处置动作不能为空")
    private String action;

    @Schema(description = "验证结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证结果不能为空")
    private String verificationResult;

    @Schema(description = "验证证据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证证据不能为空")
    private String evidence;
}
