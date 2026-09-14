# 修复 Codex OpenAI provider 配置

## Task Goal

修复 `C:\Users\BJB110\.codex\config.toml` 中导致当前对话串无法继续加载的模型供应商配置错误：`Model provider "OpenAI" not found`。

## Milestones

- [x] 建立任务记录并核对适用经验门禁。
- [x] 复现并定位 `config.toml` 中 provider 名称与定义不匹配的根因。
- [x] 最小化修复配置并验证 TOML 可解析、provider 可解析。
- [x] 记录验证证据与收尾状态。

## Expected Verification

- TOML 解析通过。
- 当前 `model_provider` 指向已定义的 `model_providers` 项。
- 不引入 fallback、降级、吞异常或默认成功路径。

## Current Status

ready_for_closeout

## Applicable Experience Gate

- `docs/experience-index.md` 已存在。
- `rg -n "config\.toml|Model provider|OpenAI|model_provider|model_providers" E:\IntRuoyi\docs E:\IntRuoyi\doc -g "*.md"` 命中 `docs/request-command-log.md` 中 2026-07-20 同类修复记录。
- 适用门禁：最小化补齐 `[model_providers.OpenAI]`；保留当前默认 `model_provider = "asxs"`；验证 `tomllib` 解析和 provider 映射。
- 已执行 `project-experience-consolidation` 检查：当前仅命中既有同类执行日志，未发现合适长期 memory 文档；未获用户授权前不新建长期经验文档。

## Closeout Evidence

- `task-closeout-cleanup` preview -> PASS，keep 4 个任务证据文件，delete/blocked/warnings 均为 none。
- `task-closeout-cleanup` apply -> PASS，deleted_paths 为 none。
- 正式提交/推送未执行：当前仓库存在大量非本任务脏改动，按任务所有权规则不得混入或回滚。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正 provider 配置名称与定义不一致。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/fix-codex-openai-provider-config-20260730/bug-regression-evidence.md
