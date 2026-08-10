# Verification Report

## Scope

- Backend authoritative filtering for one-line PQC active order list.
- Frontend empty-state and stale-selection behavior for one-line PQC order picker.
- Long-term gate consolidation for future PQC pending-task and picker empty-state regressions.

## Implementation Summary

- Backend `MesFrontlinePqcContextServiceImpl#listActiveOrders()` now returns an empty list when no active orders exist, selects the latest active order per work order + route, filters candidates by formal `PENDING` PQC task status, and only then loads work order/route/product summaries.
- Backend `MesPqcInspectionTaskMapper#selectActiveOrderIdsByTaskStatus()` provides the task-status read model used by the active-order list.
- Frontend `frontlineDeviceEmployeeContext.ts` now defines the formal no-pending-task text and clears stale selected active order, process, employee, runtime config, and template when the refreshed active-order list no longer contains the selection.
- Frontend `FrontlineFixedTemplatePanel.vue` now distinguishes no pending tasks, keyword no match, loading, and request errors in the order picker and submit status text.

## Verification Evidence

- `node tests\e2e\frontline-pqc-pending-order-empty-state-static.spec.js` -> PASS.
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS via latest Surefire report: `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`.
- `node tests\e2e\mes-frontline-pqc-order-picker-summary-static.spec.cjs` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- <task-owned paths>` -> PASS, with CRLF normalization warnings only.

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。后端按正式 `PENDING` 任务过滤，接口异常仍由前端 `lastError` 展示。
- 是否从根因和长期维护角度解决：是。工单列表源头不再暴露无待检任务工单，前端只表达真实空态。
- 是否存在临时补丁或绕过：否。

## Remaining Blockers

- None.

## Closeout

- `task_closeout.py --mode preview` -> PASS，delete/blocked/warnings 均为空。
- `task_closeout.py --mode apply` -> PASS，未删除任务文件。
- Final task status: completed.
