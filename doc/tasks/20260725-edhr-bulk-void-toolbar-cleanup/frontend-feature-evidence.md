# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: Clean up the eDHR batch execution toolbar by removing the screenshot red-box items and renaming the blue-box action to `批量作废`.
- Non-goal: Change backend bulk void API behavior, permissions, batch selection rules, or table data loading.

## Requirements And Acceptance

- Acceptance: `金手指一键作废` toolbar item is removed.
- Acceptance: `临时状态样本` toolbar item is removed.
- Acceptance: The remaining permitted bulk action is labeled `批量作废` and opens the existing bulk void dialog.
- Acceptance: Table checkbox selection remains available, and selected IDs still feed `buildGoldenFingerBulkVoidFilter`.

## UI Entry Points And Owned Files

- Route/page: eDHR batch execution list.
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`.
- Static contracts: `IntRuoyiFronted/tests/e2e/edhr-batch-execution-golden-finger-bulk-void-static.spec.js`, `IntRuoyiFronted/tests/e2e/edhr-batch-local-state-sample-static.spec.js`.

## API Contracts And Data States

- No backend API contract changed.
- `goldenFingerBulkVoidEdhrBatchExecutions` remains the batch void submit API.
- Selected rows still populate `batchExecutionIds`; empty selection still uses current filters.

## BDD Scenarios

- BDD: Toolbar cleanup -> Given a permitted user opens the eDHR batch execution list, When the toolbar renders, Then the red-box entries `金手指一键作废` and `临时状态样本` are not shown and the blue-box action is labeled `批量作废`.

## RED

- RED:
- `node tests/e2e/edhr-batch-execution-golden-finger-bulk-void-static.spec.js` -> FAIL before implementation because old `金手指一键作废` / `选择当前页可作废批次` copy still existed.
- `node tests/e2e/edhr-batch-local-state-sample-static.spec.js` -> FAIL before implementation because old `临时状态样本` entry still existed.

## GREEN

- GREEN:
- `node tests/e2e/edhr-batch-execution-golden-finger-bulk-void-static.spec.js` -> PASS.
- `node tests/e2e/edhr-batch-local-state-sample-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check` -> PASS; CRLF warnings only.

## Responsive Accessibility Loading Empty Error Permission

- Accessibility: the remaining toolbar button has `aria-label="批量作废"`.
- Loading: the remaining toolbar button uses `goldenFingerBulkVoidLoading`.
- Empty/disabled: the button remains disabled when the current page has no selectable voidable rows.
- Error: existing dialog validation and backend error display are preserved with `批量作废` wording.
- Permission: the button remains gated by `hasGoldenFingerPermission` and `GOLDEN_FINGER_PERMISSION`.

## E2E Or Component Verification Path

- Static contract verification was used for this toolbar-only change.
- Real write-type E2E was not run because the request only changes toolbar visibility/copy and would require signed write data.

## Blockers And Follow-Up Skills

- No blockers.
- `project-experience-consolidation`: no new durable experience entry needed; existing static contract sync rules apply.
