# Feature

Hide the team leader submission table red-box columns `审核副本` and `复核判定` without changing submission review actions, table data loading, or backend API contracts.

## Acceptance

- The submission table no longer renders `审核副本` or `复核判定`.
- The submission column settings no longer expose `auditCopyStatus` or `submissionReviewStatus`.
- Adjacent columns including `损耗数量`, `设备参数`, and `操作` remain available.
- Review actions remain available from the operation column, and review log details remain available in the detail view.

## UI Entry Points

- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Team leader submission report table marked by `data-team-leader-report-workbench`.

## API Contracts And Data States

No API contract changed. `submissionReviewStatus` remains in query/review flow for filtering, action gating, and daily close status, but it is no longer rendered as a main list column.

## BDD

BDD: hide review copy columns -> Given a user opens the affected loss/review table, When the table renders rows, Then the `审核副本` and `复核判定` columns are not present while adjacent columns such as `设备参数` and `操作` remain available.

## RED/GREEN

- RED: `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` -> FAIL before implementation because the table still rendered `审核副本`.
- GREEN: `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` -> PASS after implementation.

## Verification

- `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` -> PASS.
- `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS.
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- `node tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check` -> PASS.

## Blockers

No implementation blocker remains. Final closeout commit/push is blocked by unrelated concurrent dirty paths and existing ahead commits outside this task scope.

