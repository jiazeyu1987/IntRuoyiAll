# Bug Regression Evidence

## Bug Summary

当前对话串无法继续加载，错误为 `Model provider "OpenAI" not found`。

## Expected Behavior

当对话串引用 provider `OpenAI` 时，`C:\Users\BJB110\.codex\config.toml` 必须存在精确大小写匹配的 `[model_providers.OpenAI]` 定义。

## Reproduction

使用 `python -X utf8` 和 `tomllib` 解析配置，并断言 `OpenAI` 存在于 `model_providers`。

## Root Cause

当前主配置保留了默认 provider `asxs`，但 `model_providers` 表中缺少线程引用的精确键 `OpenAI`，导致加载阶段无法解析该 provider。

## Regression Test

解析 TOML 后检查：

```text
OpenAI in config["model_providers"]
```

## RED / GREEN

- `RED: python -X utf8 -c "<tomllib provider assertion>" -> FAIL, ACTIVE_PROVIDER=asxs; DEFINED_PROVIDERS=asxs; AssertionError: OpenAI provider missing.`
- `GREEN: pending`

## Risk And Regression Scope

修复范围仅为新增官方 OpenAI Responses API provider 定义；不改变默认 provider、模型、沙箱、插件、项目 trust、MCP 或认证文件。

## Blockers And Follow-Up

- 无。
