# Task: DCC 电子签名管理页签与审批签名前端闭环

## Goal

在 `DCC文控中心` 下新增 `电子签名管理` 页签，前端展示能力参照 `IntAuth`
已有电子签名管理实现，并配合后端把 DCC 审批动作统一收口到电子签名路径。

## Scope

- 先检查本仓最近一个前端任务状态，并确认不阻塞本任务。
- 在当前前端仓库创建本任务文档和执行日志后再开始生产修改。
- 新增 DCC 电子签名管理页面，包含签名记录与签名授权两个视图。
- 页面风格遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 和现有 DCC 操作台风格。
- 新增前端 API 封装，接入 DCC 电子签名记录分页和授权分页/更新接口。
- 保持 DCC 审批主路径走文控中心详情页电子签名交互，不为测试新增绕行动作。
- 如共享 BPM 审批页仍会暴露 DCC 审批按钮，则补最小前端提示或限制，但不改动非 DCC 流程行为。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-training-rules-real-content-e2e/task.md`
- Status before this task: completed.
- Impact: no unfinished latest frontend task blocks this DCC signature-management
  delivery.

## Milestones

- [x] M1: Create this frontend task package after checking the latest frontend
  task state.
- [x] M2: Inspect current DCC pages and IntAuth signature-management UI
  patterns, then record BDD scenarios and RED scope.
- [x] M3: Implement the DCC electronic-signature management page and API wiring.
- [x] M4: Run focused frontend verification and update task evidence.
- [x] M5: Commit only this frontend task's files if verification fully passes.

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/signatures.ts src/views/dcc/controlled-file/signatures/index.vue`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-electronic-signature-management run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-electronic-signature-management\scripts\verify-dcc-electronic-signature-management.mjs`

## Current Status

Completed for implementation and verification. The new DCC signature-management
page, API wiring, shared BPM redirect hint, tracked declaration-file repair,
and the requested repo-wide frontend type-baseline cleanup are all complete.
