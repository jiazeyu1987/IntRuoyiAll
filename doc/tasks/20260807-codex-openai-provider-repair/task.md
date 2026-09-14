# 修复 Codex OpenAI Provider 配置

## Task Goal

修复 `C:\Users\BJB110\.codex\config.toml` 中导致当前对话串无法继续加载的错误：`Model provider "OpenAI" not found`，并在保存后重新打开当前对话串。

## Milestones

- [x] 建立任务记录并核对同类历史修复证据。
- [ ] 复现配置缺失并记录 RED。
- [ ] 最小化补齐 `[model_providers.OpenAI]`。
- [ ] 验证 TOML、provider 映射和 Codex CLI 加载。
- [ ] 重新打开当前对话串并完成收尾。

## Expected Verification

- `tomllib` 可解析 `config.toml`。
- `model_providers` 包含精确名称 `OpenAI`。
- 当前默认 `model_provider = "asxs"` 保持不变。
- Codex CLI 可读取配置并输出版本。
- 当前对话串重新打开。

## Current Status

in_progress

## Applicable Experience Gate

- `docs/experience-index.md` 已存在。
- 同类历史任务 `doc/tasks/fix-codex-openai-provider-config-20260730/` 已验证最小修复方案。
- 只补齐 `[model_providers.OpenAI]`，不得改动当前默认 provider、模型、插件、项目 trust、MCP 或认证配置。
- 使用 `python -X utf8` 和 `tomllib` 验证 UTF-8 与 TOML 结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，补齐线程引用的精确 provider 定义。
- `是否存在临时补丁或绕过`：否。
