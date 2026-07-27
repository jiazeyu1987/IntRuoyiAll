# Frontend Feature Evidence

## Scope

- Component: `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`.
- API type: `src/api/mes/pro/edhr/batchExecution.ts`.
- Static contracts: `tests/e2e/edhr-release-owner-label-static.spec.js`, `tests/e2e/edhr-release-screenshot-action-buttons-static.spec.js`.

## Contract

- Workbench `releaseSummary` type exposes `releaseOwnerConfigured`, `releaseOwnerSourceType`, and `releaseOwnerLabel`.
- Release precheck and release approval stages use `releaseSummary.releaseOwnerLabel`.
- Release-stage owner display no longer silently falls back to `stageOwnerRole`.
- Missing or blank release-owner data displays the explicit state `放行责任人未配置`.
- Non-release stages keep the existing `stageOwnerRole` based display path.

## Acceptance

- Acceptance requires release precheck and release approval UI to display the backend release owner label and avoid `stageOwnerRole` fallback in release-specific owner computation.

## BDD

- BDD: Release stage owner label -> Given workbench returns `releaseSummary.releaseOwnerLabel` When the user views release precheck or release approval Then the right rail displays that label.
- BDD: No stageOwnerRole fallback -> Given release owner is missing When release stage is displayed Then the UI does not display generic `执行人` from `stageOwnerRole`.

## RED

- RED: `node tests\e2e\edhr-release-owner-label-static.spec.js` -> FAIL, expected because the API type lacked `releaseOwnerConfigured`.

## GREEN

- GREEN: `node tests\e2e\edhr-release-owner-label-static.spec.js; node tests\e2e\edhr-release-screenshot-action-buttons-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: final static rerun after the explicit missing-owner state refinement -> PASS.

## Verification

- Verification confirms the API type, release stage view model, right-rail owner label, and TypeScript project all accept the new contract.

## UX States

- Loading/empty state remains unchanged.
- Missing release owner displays `放行责任人未配置`; the frontend does not substitute `stageOwnerRole` or a generic owner role.
- Error behavior is unchanged; no API errors are hidden.

## Blockers

- Real browser verification is blocked until the shared local backend process is safely reloaded with the backend changes.
- Final `pnpm ts:check` retry after the last label-only refinement timed out while unrelated `vue-tsc` processes were active; the task static contracts passed.
