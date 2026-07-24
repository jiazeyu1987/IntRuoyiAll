package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 运行控制台告警创建 Request VO")
@Data
public class RuntimeControlAlertCreateReqVO {

    @Schema(description = "环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "prod")
    @NotBlank(message = "环境不能为空")
    private String environment;

    @Schema(description = "动作或异常类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "backup-failed")
    @NotBlank(message = "动作或异常类型不能为空")
    private String action;

    @Schema(description = "严重级别", requiredMode = Schema.RequiredMode.REQUIRED, example = "WARN")
    @NotBlank(message = "严重级别不能为空")
    private String severity;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "站内信模板编码")
    private String notifyTemplateCode;

    @Schema(description = "站内信模板参数")
    private Map<String, Object> templateParams;
}
