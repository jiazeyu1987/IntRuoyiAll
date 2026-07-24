# 20260527-edhr-int-main-integration

## 目标

将 eDHR 审批追踪关闭闭环与字段级不可篡改审计前端融合到 `int_main` 前端集成分支，确保执行、审批、追踪、签名、归档闸门和字段审计页面共同满足后端接口契约。

## 里程碑

- [x] M1 前端提交按审定顺序 cherry-pick 到 integration worktree。
- [x] M2 修正执行详情中旧模板字段的兼容展示文案，保持语义优先的 eDHR 详情页。
- [x] M3 验证 Node 契约测试、TypeScript relaxed 类型检查和本地构建。
- [ ] M4 与后端集成、主分支快进合并和合并后验证一起完成。

## 预期验证

- `node --test scripts\edhr-approval-page-contract.test.mjs scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-approval-archive-gate.test.mjs scripts\edhr-field-audit-api-contract.test.mjs scripts\edhr-field-audit-ui-contract.test.mjs scripts\edhr-execution-page.test.mjs scripts\edhr-execution-submit.test.mjs scripts\edhr-v1-feedback-entry.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=16384'; pnpm ts:check`
- `$env:NODE_OPTIONS='--max-old-space-size=16384'; pnpm build:local`

## 当前状态

前端 integration worktree 已完成融合与目标测试验证；等待快进合并到 `int_main` 并在合并结果上复验。

## 2026-05-28 阻塞记录

Blocked for new-task handoff.

Missing precondition: this isolated eDHR production-readiness worktree is not the original integration worktree and cannot complete the pending `int_main` fast-forward merge plus merged-result verification without leaving the current task boundary.

Impact: M4 remains not completed in this task record. New eDHR domain trace work may proceed only as a separate task and must not treat this integration task as completed.
