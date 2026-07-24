package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台运维动作命令预览 Response VO")
@Data
public class RuntimeControlActionPreviewRespVO {

    @Schema(description = "动作", example = "publish-test")
    private String action;

    @Schema(description = "动作名称", example = "部署发布包到测试服")
    private String actionLabel;

    @Schema(description = "环境", example = "test")
    private String environment;

    @Schema(description = "组件", example = "ops")
    private String component;

    @Schema(description = "脚本路径")
    private String scriptPath;

    @Schema(description = "命令参数")
    private List<String> arguments;

    @Schema(description = "安全操作参数")
    private Map<String, String> parameters;

    @Schema(description = "是否启用 Smart Release Phase 1 report-only")
    private Boolean enableSmartReleaseReport;

    @Schema(description = "摘要")
    private String summary;
}
