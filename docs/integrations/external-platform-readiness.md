# Codex Web / IntRuoyi MCP Integration Readiness

## Scope

本记录覆盖 IntRuoyi 管理后台中的 Codex Web 页面、Windows 本机 Codex CLI 与 IntRuoyi MCP Streamable HTTP endpoint 的集成前置条件。

## Platform Inventory

| Platform | Purpose | Environment | Status | Evidence |
| --- | --- | --- | --- | --- |
| Codex CLI | 执行 Codex Web 对话请求 | Windows local/dev | CONFIRMED | `codex-cli 0.147.0` 已在本机可执行 |
| Spring AI MCP Server | 暴露 IntRuoyi 工具 | IntRuoyi local/dev | CONFIGURED, runtime pending | `spring.ai.mcp.server` 配置与 `/mcp` endpoint 已加入 |
| Codex CLI MCP client | 从 Codex CLI 连接 IntRuoyi | Windows local/dev | CONFIGURED, runtime pending | `INTRUOYI_CODEX_MCP_SERVER_URL` 注入 `mcp_servers.intruoyi.url` |

## Credentials And Ownership

- Codex CLI 登录凭据由运行 IntRuoyi 后端的 Windows 用户维护，不写入仓库。
- MCP endpoint 当前不新增静态密钥字段；若部署在非受信网络，必须在独立安全评审中补充认证和访问控制。
- 不在任务文档、日志或配置提交中记录 API key、token、cookie、私钥或密码。

## Domains

- Local/dev frontend: `http://127.0.0.1:8081`.
- Local/dev backend and MCP endpoint: `http://127.0.0.1:48081/mcp` when the default `int_main` runtime is used.
- Production domain, reverse proxy callback URL and public ingress: NOT APPLICABLE to this local-only implementation.

## Approval

- Local/dev approval: CONFIRMED by repository configuration and local Codex CLI availability.
- Production/public exposure approval: NOT APPROVED; requires separate security, authentication, TLS and operations review.

## Required Runtime Configuration

- `INTRUOYI_MCP_SERVER_ENABLED=true`
- `INTRUOYI_MCP_PROTOCOL=STREAMABLE`
- `INTRUOYI_MCP_STREAMABLE_ENDPOINT=/mcp`
- `INTRUOYI_CODEX_MCP_SERVER_NAME=intruoyi`
- `INTRUOYI_CODEX_MCP_SERVER_URL=<IntRuoyi 服务可访问的完整 MCP URL>`

本地同进程部署时，endpoint 通常应与实际后端端口保持一致，例如 `http://127.0.0.1:48081/mcp`；不得把该示例地址当作生产部署地址。

## MCP Tool Exposure Boundary

- MCP 工具注册必须使用显式的只读工具集合；不要把示例 CRUD 服务或带有创建、更新、删除能力的通用工具对象直接注册到 MCP。
- 每个对外工具都应返回经过裁剪的业务摘要，排除密码、手机号、令牌和其它不必要的敏感字段；工具调用日志只记录工具名和稳定业务标识。
- 工具集合变更必须同时补充单元测试和静态注册合同，验证真实工具名称存在且示例写入工具不存在。
- endpoint 未启用、地址未配置或 Codex CLI 不可用时必须失败并保留真实错误，不得切换到其它模型或伪造成功。

## Verification Procedure

1. 执行 `codex --version`，确认 Codex CLI 可执行。
2. 启动启用 MCP 的 IntRuoyi 后端。
3. 使用已登录账号打开 `/ai/codex`，确认状态栏显示 `IntRuoyi MCP`。
4. 发送一条需要调用 IntRuoyi 工具的真实任务，确认 Codex CLI 能完成 MCP 初始化并返回结果。
5. 故意移除 `INTRUOYI_CODEX_MCP_SERVER_URL`，确认页面显示未配置，发送请求明确失败。

## Blockers And Launch Impact

- 未完成第 4 步前，只能认定为代码与配置已就绪，不能认定真实 MCP 联调通过。
- 若 Codex CLI 未登录、MCP endpoint 未启用、端口不一致或 endpoint 不可达，Codex Web 发送会失败；系统不返回默认成功、不切换到其它模型。
- 本任务未覆盖公网暴露、OAuth、反向代理 TLS、跨租户授权或生产监控；这些属于单独的发布前安全与运维门禁。
