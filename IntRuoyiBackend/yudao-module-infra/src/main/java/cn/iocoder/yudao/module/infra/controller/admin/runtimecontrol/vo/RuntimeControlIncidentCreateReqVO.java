package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台事故创建 Request VO")
@Data
public class RuntimeControlIncidentCreateReqVO {

    @Schema(description = "环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "prod")
    @NotBlank(message = "环境不能为空")
    private String environment;

    @Schema(description = "动作或异常类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "动作或异常类型不能为空")
    private String action;

    @Schema(description = "严重级别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "严重级别不能为空")
    private String severity;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "描述不能为空")
    private String description;

    @Schema(description = "来源类型：DIRECT/ALERT/HIGH_RISK_OPERATION", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "来源编号")
    private String sourceId;
}
