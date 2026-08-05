# Frontend Feature Evidence

## Scope

AC-M23 release owner actions in `BatchExecutionDetailPage.vue`.

## Contract

- 放行按钮继续调用 `submitEdhrRelease` 并要求负责人电子签名密码。
- 放行退回按钮调用 `rejectEdhrRelease`，使用 `mes:pro-edhr-release:reject` 权限，和质量拒收保持独立。
- 放行追溯页保持只读，不新增写入口。

## BDD

See `execution-log.md`.

- BDD: release owner return entry -> Given release stage is visible and transaction exists When the owner clicks `退回` Then page opens a release-return dialog and calls `rejectEdhrRelease`.
- BDD: quality reject separation -> Given release stage is visible When user chooses quality rejection Then page keeps using `qualityRejectEdhrBatchExecution` and does not call release reject.

## Acceptance

- Release-return action key is `release-return`.
- Release-return permission is `mes:pro-edhr-release:reject`.
- Quality rejection remains a separate `quality-reject` action and keeps its existing API.

## RED

- RED: `node tests\e2e\edhr-release-owner-return-static.spec.js` -> FAIL before implementation.
- RED command: `node tests\e2e\edhr-release-owner-return-static.spec.js`.
- Result: FAIL as expected before implementation: `批次详情页必须导入正式放行退回 API rejectEdhrRelease。`

## GREEN

- GREEN: release-return and direct-submit static contracts PASS.
- Implemented `BatchExecutionDetailPage.vue` release-owner return path:
  - imports and calls `rejectEdhrRelease`
  - adds independent `放行退回` dialog with required reason
  - adds `release-return` action using `mes:pro-edhr-release:reject`
  - keeps `quality-reject` action separate on `qualityRejectEdhrBatchExecution`
- GREEN commands:
  - `node tests\e2e\edhr-release-owner-return-static.spec.js` -> PASS
  - `node tests\e2e\edhr-release-direct-submit-button-static.spec.js` -> PASS
  - `git diff --check -- <task-owned files>` -> PASS, only existing LF-to-CRLF warnings

## Verification

- Verified by focused static contracts because real E2E/login/runtime preconditions were not established in this turn.

## Blockers

- Full frontend typecheck and real Playwright E2E were not run in this pass; no local E2E/login/runtime preconditions were established for this task.
