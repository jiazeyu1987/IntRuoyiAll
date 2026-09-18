const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(root, '../IntRuoyiBackend')
const readFrontend = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const page = readFrontend('src/views/ai/chat/index/index.vue')
const conversationList = readFrontend(
  'src/views/ai/chat/index/components/conversation/ConversationList.vue'
)
const messageApi = readFrontend('src/api/ai/chat/message/index.ts')
const codexApi = readFrontend('src/api/ai/codex/index.ts')
const routes = readFrontend('src/router/modules/remaining.ts')
const backendController = readBackend(
  'yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/codex/CodexWebController.java'
)
const backendRequest = readBackend(
  'yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/controller/admin/chat/vo/message/AiChatMessageSendReqVO.java'
)
const backendProperties = readBackend(
  'yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/framework/ai/config/YudaoAiProperties.java'
)
const backendAutoConfiguration = readBackend(
  'yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/framework/ai/config/AiAutoConfiguration.java'
)
const backendReadOnlyTools = readBackend(
  'yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/tool/IntruoyiMcpReadOnlyTools.java'
)
const backendYaml = readBackend('yudao-server/src/main/resources/application.yaml')
const codexModel = readBackend(
  'yudao-module-ai/src/main/java/cn/iocoder/yudao/module/ai/framework/ai/core/model/codexcli/CodexCliChatModel.java'
)

assert.match(routes, /path:\s*['"]\/ai\/codex['"]/, '必须注册 Codex Web 路由。')
assert.match(page, /isCodexWeb/, '聊天页必须识别 Codex Web 模式。')
assert.match(page, /Codex Web/, 'Codex Web 页面必须展示明确标题。')
assert.match(page, /IntRuoyi MCP/, 'Codex Web 页面必须展示 IntRuoyi MCP 状态。')
assert.match(page, /CodexWebApi\.getStatus/, '页面必须读取真实 MCP 状态接口。')
assert.match(page, /CodexWebApi\.createConversation/, 'Codex Web 新建会话必须使用专用接口。')
assert.match(conversationList, /createConversation\?:/, '会话列表必须允许注入 Codex 会话创建器。')
assert.match(messageApi, /useIntRuoyiMcp/, '消息 API 必须显式发送 IntRuoyi MCP 开关。')
assert.match(codexApi, /\/ai\/codex-web\/status/, '前端必须调用 Codex Web 状态接口。')
assert.match(codexApi, /\/ai\/codex-web\/conversation\/create/, '前端必须调用 Codex Web 会话创建接口。')
assert.match(backendController, /@RequestMapping\("\/ai\/codex-web"\)/, '后端必须提供 Codex Web 控制器。')
assert.match(backendController, /@GetMapping\("\/status"\)/, '后端必须提供 MCP 状态接口。')
assert.match(backendController, /@PostMapping\("\/conversation\/create"\)/, '后端必须提供 Codex 会话创建接口。')
assert.match(backendRequest, /useIntRuoyiMcp/, '后端发送请求必须承载 IntRuoyi MCP 开关。')
assert.match(backendProperties, /mcpServerUrl/, 'Codex CLI 配置必须声明 MCP URL。')
assert.match(backendProperties, /mcpServerName/, 'Codex CLI 配置必须声明 MCP 名称。')
assert.match(backendProperties, /mcpServerEnabled/, 'Codex CLI 配置必须声明 MCP 启用状态。')
assert.match(backendAutoConfiguration, /IntruoyiMcpReadOnlyTools/, 'MCP 必须注册 IntRuoyi 只读工具。')
assert.doesNotMatch(backendAutoConfiguration, /PersonService/, 'MCP 不得继续注册示例 PersonService 工具。')
assert.match(backendReadOnlyTools, /intruoyi_get_system_summary/, '必须暴露 IntRuoyi 系统汇总只读工具。')
assert.match(backendReadOnlyTools, /intruoyi_get_tenant_summary/, '必须暴露 IntRuoyi 租户摘要只读工具。')
assert.doesNotMatch(backendReadOnlyTools, /create|update|delete/i, 'IntRuoyi MCP 工具不得暴露写入动作。')
assert.match(codexModel, /mcp_servers\./, 'Codex CLI 命令必须注入 MCP server 配置。')
assert.match(codexModel, /isMcpConfigured/, 'Codex CLI 必须暴露 MCP 配置状态。')
assert.match(backendYaml, /INTRUOYI_CODEX_MCP_SERVER_URL/, '运行配置必须支持显式配置 IntRuoyi MCP URL。')
assert.match(backendYaml, /INTRUOYI_MCP_SERVER_ENABLED/, '运行配置必须支持显式启用 IntRuoyi MCP 服务。')
assert.doesNotMatch(page, /mock|placeholder data|fallback data|降级|吞异常/i)

console.log('PASS: Codex Web IntRuoyi MCP static contract')
