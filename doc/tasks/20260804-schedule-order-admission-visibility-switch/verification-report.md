# Verification Report: 同步工单已入池显示开关

## Summary

- Implemented a Switch in the schedule order “同步工单” toolbar to show or hide orders already added to the schedule order pool.
- Default behavior remains hidden: direct query param `admissionStatus=READY_TO_ADMIT`.
- When enabled, the direct `admissionStatus` filter is cleared so `ALREADY_ADMITTED` rows can be returned by the existing backend admission-diff endpoint.
- No backend API, permission, admission submit, or local page filtering fallback was introduced.

## Commands

- `node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js` -> PASS
- `node tests/e2e/mes-schedule-order-admission-reason-options-static.spec.js` -> PASS
- `pnpm ts:check:schedule` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-schedule-order-admission-visibility-switch/frontend-feature-evidence.md` -> PASS
- `pnpm ts:check` -> FAIL due unrelated existing `src/views/mes/qc/template/index.vue` missing API exports and method.

## Blockers

- Full-project type check is not clean because `src/views/mes/qc/template/index.vue` imports `QaInspectionRegulationPublishedVersionVO` and `QaInspectionRuleVO` from `@/api/mes/qc/template`, and calls `MesQcTemplateApi.getPublishedQaRegulationVersion`, none of which currently exist in that API module.
- Repository closeout remains constrained by pre-existing dirty worktree and local ahead state unrelated to this task.
- Cleanup preview/apply passed and removed only `frontend-feature-evidence.md`; formal task completion remains `ready_for_closeout` until unrelated git and full type-check blockers are resolved.
