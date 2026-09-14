# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: after a successful manual replan, highlight only the schedule-order product codes that actually participated in the just-applied replan.
- Non-goals: do not change backend replan algorithms, request contracts, preview behavior, gantt query logic, permissions, or failure/cancel handling.

## Requirements And Scope

- Acceptance b: only the two schedule orders sourced from `881MO093613` and `881MO093615` display orange product codes after a successful manual replan.
- Owned component: `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`.
- Regression contract: `IntRuoyiFronted/tests/e2e/mes-replan-product-code-current-selection-static.spec.js`.

## UI And API Contract

- UI entry: 排产工单 -> 手动重排 -> 开始重排 -> 确认应用重排.
- Route: existing MES 排产工单 page.
- API: `ProTaskAutoScheduleApi.replanApply`; the existing `freshPreview` result supplies the actual participant IDs.
- State: `lastReplanParticipatingScheduleOrderIds` is replaced only after `replanApply` succeeds.

## BDD Scenarios

- `BDD: 成功手动重排后仅参与工单的产品编号变橙 -> Given` 用户在排产工单页签选择 `881MO093613` 与 `881MO093615` 的两个工单；`When` 手动重排被成功应用；`Then` 成功分支基于本次 `freshPreview` 更新参与工单集合，且仅这两个工单的产品编号显示橙色。
- `BDD: 重排未成功时不提前标橙 -> Given` 用户取消重排或应用调用失败；`When` 页面返回；`Then` 不更新最近一次成功重排参与集合。

## TDD Evidence

- RED: `node tests/e2e/mes-replan-product-code-current-selection-static.spec.js` -> FAIL, expected reason: apply success did not update `lastReplanParticipatingScheduleOrderIds`.
- GREEN: `node tests/e2e/mes-replan-product-code-current-selection-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check:schedule` -> PASS.
- GREEN: `node doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs` -> PASS, real UI flow verified a/b/c/d at 2026-07-24 17:32.

## UX Checks

- Responsive: existing table layout and product code classes are unchanged.
- Accessibility: no interactive control or accessible label changes.
- Loading/error/empty/permission: existing loading, error, empty, and permission paths are unchanged; participant state changes only after success.
- E2E: reran real UI flow for the two authorized source work orders; only `881MO093613` and `881MO093615` product codes used the orange scheduled class/color, latest success time matched `2026-07-24 17:32`, and the production gantt contained only those two work orders.

## Verification

- `node tests/e2e/mes-replan-product-code-current-selection-static.spec.js` -> PASS.
- `pnpm ts:check:schedule` -> PASS.
- `node doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs` -> PASS.

## Blockers And Follow-up

- No current blocker.
