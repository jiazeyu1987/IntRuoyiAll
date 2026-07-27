# 发布变更改为 Codex 通俗中文摘要

## Task Goal

将发布脚本生成的版本变更说明从原始 Git 提交列表改为 Codex 生成的普通人可读中文摘要。摘要只基于当前版本与上个版本的 Git 差异，每次最多 10 条；Codex 缺失、调用失败或输出不符合结构化契约时，发布必须直接失败。

## Milestones

- [ ] 完成变更请求记录和校验。
- [ ] 补充 BDD 场景与 Codex 摘要 RED 测试。
- [ ] 实现 Codex CLI 调用、结构化输出校验和发布信息写入。
- [ ] 完成聚焦验证、回归验证和证据记录。
- [ ] 完成经验沉淀、cleanup、提交并推送。

## Expected Verification

- 变更请求校验脚本通过。
- Codex 摘要相关 RED 测试先失败，实现后 GREEN。
- PowerShell 发布脚本语法解析通过。
- 发布脚本聚焦测试、前端静态契约和 `pnpm ts:check` 通过。
- `git diff --check` 通过；无真实服务器发布操作。

## 经验门禁

- `release-info 用户可见 Git 变更门禁`：继续使用上一发布包与当前版本的 `previousCommit..currentCommit` 差异；不可回退到发布包元信息或原始提交列表。
- `PowerShell 命令编排与 UTF-8 门禁`：Codex prompt 和 schema 使用明确 UTF-8 文件承载；命令退出码、标准错误和 JSON 解析错误必须显式处理。
- `发布链路 fail-fast 门禁`：Codex CLI、认证环境和结构化输出是本次发布摘要的必需前置条件，缺失时停止，不切换数据源。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；Codex 缺失、调用失败或输出非法均直接失败。
- `是否从根因和长期维护角度解决`：是；使用结构化 JSON 输出和严格校验，避免依赖模型自由文本格式。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
