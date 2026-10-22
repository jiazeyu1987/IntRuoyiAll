package cn.iocoder.yudao.module.ai.controller.admin.codex;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.ai.controller.admin.codex.vo.CodexWebStatusRespVO;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli.CodexCliChatModel;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - Codex Web")
@RestController
@RequestMapping("/ai/codex-web")
public class CodexWebController {

    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private YudaoAiProperties yudaoAiProperties;
    @Resource
    private CodexCliChatModel codexCliChatModel;

    @GetMapping("/status")
    @Operation(summary = "获得 Codex Web 状态")
    public CommonResult<CodexWebStatusRespVO> getStatus() {
        YudaoAiProperties.CodexCli properties = yudaoAiProperties.getCodexCli();
        boolean enabled = properties == null || properties.isEnable();
        boolean mcpConfigured = codexCliChatModel.isMcpConfigured();

        CodexWebStatusRespVO status = new CodexWebStatusRespVO()
                .setEnabled(enabled)
                .setMcpServerEnabled(codexCliChatModel.isMcpServerEnabled())
                .setMcpConfigured(mcpConfigured)
                .setMcpServerName(codexCliChatModel.getMcpServerName())
                .setMcpServerUrl(codexCliChatModel.getMcpServerUrl())
                .setMessage(!enabled ? "Codex CLI 已禁用"
                        : !codexCliChatModel.isMcpServerEnabled() ? "未启用 IntRuoyi MCP 服务"
                        : !mcpConfigured ? "未配置 IntRuoyi MCP endpoint"
                        : "Codex CLI 与 IntRuoyi MCP 已配置");
        return success(status);
    }

    @PostMapping("/conversation/create")
    @Operation(summary = "创建 Codex Web 对话")
    public CommonResult<Long> createConversation() {
        return success(chatConversationService.createCodexChatConversationMy(getLoginUserId()));
    }

}
