package cn.iocoder.yudao.module.ai.controller.admin.codex.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - Codex Web 状态 Response VO")
@Data
public class CodexWebStatusRespVO {

    @Schema(description = "Codex CLI 是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    @Schema(description = "IntRuoyi MCP 是否已配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean mcpConfigured;

    @Schema(description = "IntRuoyi MCP 服务是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean mcpServerEnabled;

    @Schema(description = "MCP 服务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mcpServerName;

    @Schema(description = "MCP endpoint")
    private String mcpServerUrl;

    @Schema(description = "状态说明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

}
