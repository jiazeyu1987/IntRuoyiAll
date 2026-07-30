# Bug Regression Evidence

## Bug Summary

当前对话串无法继续加载，报错为 `Model provider "OpenAI" not found`。

## Expected Behavior

`C:\Users\BJB110\.codex\config.toml` 中任何被对话串或运行时引用的 provider 名称，都必须在 `[model_providers.<name>]` 下有明确配置。

## Reproduction

修复前只读检查 `config.toml` 的 provider 声明，发现仅存在 `model_provider = "asxs"` 和 `[model_providers.asxs]`，缺少 `[model_providers.OpenAI]`。

## Root Cause

主配置保留了当前默认 provider `asxs`，但线程或运行态仍可能引用精确大小写的 provider 名称 `OpenAI`；`model_providers` 表中缺少该键，导致配置加载时无法解析 provider。

## Fix

在 `C:\Users\BJB110\.codex\config.toml` 中补齐：

```toml
[model_providers.OpenAI]
name = "OpenAI"
base_url = "https://api.openai.com/v1"
wire_api = "responses"
requires_openai_auth = true
```

未修改当前默认 `model_provider = "asxs"`。

## RED / GREEN

- `RED: Select-String -Path C:\Users\BJB110\.codex\config.toml -Pattern '^model_provider\s*=|^\[model_providers\.' -> FAIL, pre-fix provider list lacked [model_providers.OpenAI].`
- `GREEN: python -X utf8 tomllib provider check -> PASS, TOML_OK; ACTIVE_PROVIDER=asxs; DEFINED_PROVIDERS=OpenAI,asxs.`

## Risk And Regression Scope

风险较低：只新增显式 provider 定义，不改变默认 provider、模型、沙箱、插件、项目 trust、MCP 或认证文件。

## Blockers

项目仓库存在大量非本任务脏改动；本任务未提交或推送，避免混入无关变更。
