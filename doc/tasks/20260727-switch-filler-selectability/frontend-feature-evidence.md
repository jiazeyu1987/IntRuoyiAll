# Frontend Feature Evidence

## Feature Goal

Allow the eDHR auxiliary fill mode “切换填写人” dialog to keep other current-process fillable candidates selectable for users with golden-finger/delegate-fill permission.

## Non-goals

- No backend contract change.
- No mock data or API-only behavior replacement.
- No unrelated visual redesign of the switch dialog.

## Requirements And Acceptance

- BDD: 金手指可选择其他填写人 -> Given current process returns multiple `fillableUsers` and current user has golden-finger/delegate-fill permission / When the filler switch dialog renders / Then candidates for other users are not disabled by the current login user ID.
- BDD: 普通用户仍保留后端校验 -> Given a non-golden user opens a task they cannot process / When `openTask` rejects / Then the real backend error remains visible in the dialog.

## UI Entry Points And Owned Files

- Entry: `src/views/mes/pro/edhr/ExecutionPage.vue`, auxiliary fill mode `切换填写人` dialog.
- Test: `tests/e2e/edhr-switch-filler-selectability-static.spec.js`.

## API Contracts And Data States

- Data source remains `getEdhrBatchExecution(batchExecutionId)` and task-level `fillableUsers`.
- Navigation still calls `openEdhrBatchTask` through `navigateToAssistBatchTask`.
- The frontend keeps `isAssistBatchTaskOpenable(item.task)` before enabling candidates.

## RED

- RED: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> FAIL because the old predicate did not include `hasGoldenFingerPermission.value`.

## GREEN

- GREEN: `node --check tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- No layout or empty/loading state changes.
- Permission behavior is explicit: golden-finger can select other candidates; non-golden users still see a direct error when selecting a candidate they cannot process.
- Backend/API failures are not swallowed; `navigateToAssistBatchTask` continues to show real errors in the switch dialog.

## E2E Or Component Verification Path

- Focused static verification passed.
- Real E2E was not run in this turn because the current reported defect is covered by deterministic source-level selectable-state logic, and the wide real path requires live tenant/runtime prerequisites.

## Blockers And Follow-up Skills

- Existing broad assist-fill static contract is currently blocked by an unrelated redbox cleanup assertion and should be reconciled in a separate task.
