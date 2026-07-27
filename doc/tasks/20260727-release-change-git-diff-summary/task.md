# 发布变更说明改为 Git 差异摘要

## Task Goal

- 将前端“版本变更说明”弹窗从发布包元信息展示，调整为只展示当前版本与上一个版本相比的 Git 变更内容。
- 每次最多展示 10 条变更，不展示版本号、构建时间、发布范围、组件、摘要、变更项和源码提交区块。

## Milestones

- [x] 创建任务记录并记录既有脏工作区基线。
- [x] 定位 ReleaseInfoDock 当前数据契约和 UI 展示逻辑。
- [x] 先补充静态契约 RED，覆盖隐藏旧区块与最多 10 条 Git 变更。
- [x] 实施最小前端修改和发布脚本 Git 差异生成。
- [x] 运行目标验证并记录 GREEN/REGRESSION。
- [x] 完成经验沉淀。
- [x] 完成 cleanup 和实现提交。
- [x] 完成任务证据提交、推送和最终状态记录。

## Expected Verification

- 目标静态契约先 RED 后 GREEN。
- 受影响前端测试通过；若全量验证被无关历史问题阻塞，记录首个无关 blocker。
- `git diff --check` 通过。

## 经验门禁

- `运行控制台版本说明与 source commit 分层验收门禁`：页面验收聚焦用户可见版本说明；source commit、dirty=false、publishScope 等机器字段以 `release-info.json`/manifest 为权威，不因 UI 不展示 commit 跳过机器校验。
- `release-info CRLF-safe 解析门禁`：运行态解析必须使用 JSON parser 或 CRLF 安全处理；本任务不改解析器，只改前端展示。
- `前端静态契约隔离门禁`：若全量 `pnpm ts:check` 或既有大契约先失败在无关历史问题上，使用任务专用最小静态契约覆盖本需求 RED/GREEN，并记录全量回归 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整发布说明展示口径和静态契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260727-release-change-git-diff-summary/frontend-feature-evidence.md
