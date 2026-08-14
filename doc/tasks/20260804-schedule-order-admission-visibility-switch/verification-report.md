# Verification Report: 同步工单已入池显示开关

## Summary

- Implemented a Switch in the schedule order “同步工单” toolbar to show or hide orders already added to the schedule order pool.
- Default behavior remains hidden: direct query param `admissionStatus=READY_TO_ADMIT`.
- When enabled, the direct `admissionStatus` filter is cleared so `ALREADY_ADMITTED` rows can be returned by the existing backend admission-diff endpoint.
- Quick-filter searches now preserve the Switch-driven `admissionStatus` state, so filtering by work order code no longer reintroduces already admitted rows while the Switch is off.
- No backend API, permission, admission submit, or local page filtering fallback was introduced.

## Commands

- `node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-admission-reason-options-static.spec.js` -> PASS
- `pnpm ts:check:schedule` -> PASS
- `node --check doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs` -> PASS
- `node doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-schedule-order-admission-visibility-switch/frontend-feature-evidence.md` -> PASS
- `pnpm ts:check` -> FAIL due unrelated existing `src/views/mes/qc/template/index.vue` missing API exports and method.

## Real E2E Evidence

- Environment: `http://127.0.0.1:8081` frontend and `http://127.0.0.1:48081` backend, tenant/user label `芋道源码/admin`.
- Sample: admitted work order `RRM-20260801-PP-MO-001` from read-only admission-diff query.
- Hidden default: initial request used `admissionStatus=READY_TO_ADMIT`; searching the admitted sample kept `workOrderCode=RRM-20260801-PP-MO-001&admissionStatus=READY_TO_ADMIT` and returned total `0`.
- Shown state: turning the Switch on cleared direct `admissionStatus`, kept `workOrderCode=RRM-20260801-PP-MO-001`, returned total `1`, and showed the row as `ALREADY_ADMITTED`.
- Hidden again: turning the Switch off restored `admissionStatus=READY_TO_ADMIT`, returned total `0`, and the sample row disappeared from the table.
- Safety: `targetWriteCount=0`, `targetBadResponseCount=0`, `pageErrorCount=0`, and `consoleErrorCount=0`.
- Result file retained: `doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch-result.json`.

## Blockers

- Full-project type check is not clean because `src/views/mes/qc/template/index.vue` imports `QaInspectionRegulationPublishedVersionVO` and `QaInspectionRuleVO` from `@/api/mes/qc/template`, and calls `MesQcTemplateApi.getPublishedQaRegulationVersion`, none of which currently exist in that API module.
- Repository closeout remains constrained by pre-existing dirty worktree and local ahead state unrelated to this task.
- Cleanup preview/apply previously removed only `frontend-feature-evidence.md`; current real E2E script and result are retained as task evidence. Formal task completion remains `ready_for_closeout` until unrelated git and full type-check blockers are resolved.
