# OfficeCLI 全局 Codex 接入

## Task Goal

将已下载的 OfficeCLI 接入全局 Codex 环境，使以后处理 Office 文档时优先使用 OfficeCLI；安装范围限定为 Codex，不安装到其它 AI 工具。

## Milestones

- [x] 核对 OfficeCLI README 和本机安装帮助。
- [x] 建立安装前基线：`officecli` 不在 PATH，Codex 未安装 `officecli` skill。
- [x] 执行 `officecli install codex`。
- [x] 验证 Codex skill 与 PATH 配置已生效；MCP 当前不支持 Codex target。
- [x] 记录收尾验证与阻塞项。

## Expected Verification

- `officecli install codex` 执行成功。
- `Get-Command officecli` 能定位到全局命令，或安装输出明确说明只完成 Codex skill/MCP 接入。
- `C:\Users\BJB110\.codex\skills\officecli` 存在且包含 OfficeCLI 使用说明。
- `officecli mcp list` / `officecli mcp codex --help` 确认当前 OfficeCLI MCP target 不含 Codex；Codex 接入以 PATH + skill + 全局 AGENTS 为准。
- 后续 Office 文档任务由全局 Codex guidance/skill 明确要求优先使用 OfficeCLI。

## README Findings

- OfficeCLI README 描述 `officecli install` 可自动安装 binary、skills 和 MCP。
- 本地 `officecli install --help` 显示可用精确目标 `codex`，因此本任务使用 `officecli install codex`，不使用 `all`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按官方 CLI 的 Codex 目标安装路径接入。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

OfficeCLI 已接入全局 Codex：二进制安装到 C:\Users\BJB110\AppData\Local\OfficeCli\officecli.exe，用户 PATH 已包含该目录，官方 skill 已同步到 .agents 和 .codex，全局 .codex\AGENTS.md 已加入 OfficeCLI 优先规则。Cleanup preview/apply 已通过并删除临时 smoke 文件；仓库提交/推送仍受既有非本任务脏改动、Git scan warnings 和 ahead 状态阻塞。

## Cleanup Candidates

- doc/tasks/officecli-global-codex-20260803/officecli-smoke-20260803.xlsx
