# Task: DCC 流程详情阶段名称问号占位修复

## Goal

修复 DCC 流程详情里右侧审批阶段名称显示为 `???` 的问题，确保当接口返回占位问号时，页面仍显示规范中文阶段名。

## Scope

- 先创建当前前端任务文档，再开始生产代码修改。
- 严格按 BDD + TDD 先补失败验证，再做最小实现。
- 修复 DCC 受控文件详情页的阶段进度显示。
- 修复 BPM 流程详情时间线中 DCC 审批节点的阶段名称显示。
- 必要时同步修复审批路线快照中的阶段名称展示。
- 不修改接口契约，不引入兜底分支去掩盖真实错误。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-workorder-status-column-and-kingdee-confirmed/task.md`
- Status before this task: completed for code delivery.
- Impact: no unfinished latest frontend task blocks this DCC stage-name display fix.

## Milestones

- [x] M1: Create task package and record the bug target before code edits.
- [x] M2: Add a failing regression check for placeholder stage names.
- [x] M3: Implement the smallest display normalization fix.
- [x] M4: Run targeted verification and update evidence.
- [x] M5: Commit only task-scoped files if the repo state allows a clean task-only commit.

## Expected Verification

- `node --experimental-strip-types D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-process-detail-stage-name-normalization\scripts\verify-dcc-process-detail-stage-name-normalization.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`

## Current Status

Completed. Code fix, verification, and task-scoped commit are complete.

## Blockers And Impact

- Blocker: local MySQL credentials from `application-local.yaml` could not be used to inspect live `stage_name` / `activity_name` data (`Access denied for user 'root'@'localhost'`).
- Impact: database-level confirmation is blocked, so this task verifies the display fix through source-level regression checks and targeted frontend build validation.

## Final Verification Result

- `node --experimental-strip-types D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-process-detail-stage-name-normalization\scripts\verify-dcc-process-detail-stage-name-normalization.mjs` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
