package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台重启 Request VO")
@Data
public class RuntimeControlRestartReqVO {

    @Schema(description = "环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "test")
    @NotBlank(message = "环境不能为空")
    private String environment;

    @Schema(description = "组件", requiredMode = Schema.RequiredMode.REQUIRED, example = "website-frontend")
    @NotBlank(message = "组件不能为空")
    private String component;

    @Schema(description = "重启原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "生产确认文本", example = "PROD")
    private String prodConfirmText;
}
