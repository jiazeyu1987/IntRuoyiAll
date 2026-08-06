# Bug Regression Evidence

## Bug

The team leader submission table still displayed the screenshot red-box columns `审核副本` and `复核判定`.

## Expected

The submission table must not render `审核副本` or `复核判定`, and these keys must not remain available in the submission column settings. Adjacent table content such as `损耗数量`, `设备参数`, and `操作` must remain visible, and the review action must remain available.

## Reproduction

RED: `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` -> FAIL, `red-box column 审核副本 must not render in the submission table`.

## Root Cause

`TeamLeaderWorkbenchPage.vue` rendered `auditCopyStatus` and `submissionReviewStatus` as production submission table columns and kept both in `productionSubmissionDefaultColumns`, so the list and user column settings could still expose the red-box content.

## Fix

Removed the two list column blocks and removed both keys from the production submission default column pool. Kept review workflow logic and exposed review log details in the row detail view instead of the main list.

## RED/GREEN

- RED: `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` -> FAIL before implementation.
- GREEN: `node tests/e2e/team-leader-hide-review-copy-columns-static.spec.cjs` -> PASS after implementation.

## Verification

- `node tests/e2e/pqc-leader-sample-values-detail-only-static.spec.cjs` -> PASS.
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- `node tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check` -> PASS.

## Blockers

Closeout commit/push is blocked by unrelated concurrent dirty paths and existing ahead commits on the shared branch. The implementation and required verification are complete.

