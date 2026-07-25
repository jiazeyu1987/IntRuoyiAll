# Frontend Feature Evidence - eDHR 金手指批量作废

## Feature Goal

- Add a `金手指一键作废` action on the eDHR batch execution list.
- Show it only when `hasGoldenFingerPermission` is true and `v-hasPermi` includes `mes:pro-batch-record-execution:golden-finger`.
- Submit current filters for cross-page bulk voiding, not Element Plus table selection state.

## Non-Goals

- Do not modify global BPM or void approval configuration.
- Do not route single-row void requests through the new gold-finger bulk API.
- Do not call approval resolution APIs from the bulk void submit flow.

## Owned Frontend Files

- `IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts`
- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-batch-execution-golden-finger-bulk-void-static.spec.js`

## API Contract

- New frontend API wrapper calls `/mes/pro/edhr-batch-execution/golden-finger/bulk-void`.
- Request includes `filter`, `reasonCategory`, `reasonText`, `password`, and optional `comment`.
- Successful response displays `voidedCount` and `skippedCount`, then refreshes the list.
- Failed response surfaces backend error text through `resolveErrorMessage`; no default success is shown.

## UI And State

- Toolbar button: `金手指一键作废`.
- Dialog title: `金手指一键作废批次执行`.
- Dialog warning states that the action applies to current filters, cross-page, all voidable batches and does not enter approval.
- Required fields: reason category, reason text, electronic signature password; remark is optional.

## BDD Scenarios

- `BDD: 金手指按钮可见性 -> Given 用户具备金手指权限 / When 打开批次执行列表 / Then 显示金手指一键作废按钮；非金手指不显示。`
- `BDD: 当前筛选跨页提交 -> Given 用户设置筛选条件 / When 提交金手指一键作废 / Then 前端提交筛选对象，不依赖当前页勾选。`
- `BDD: 审批路径隔离 -> Given 用户提交金手指一键作废 / When 前端调用接口 / Then 不调用审批解析接口；单条作废仍调用原 BPM 作废申请接口。`

## RED Evidence

- `RED: node tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js -> FAIL, 缺少金手指按钮、bulk API wrapper、当前筛选提交和审批隔离断言。`

## GREEN Evidence

- `GREEN: node --check tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js -> PASS。`
- `GREEN: node tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js -> PASS, PASS edhr batch execution golden finger bulk void static contract。`
- `GREEN: pnpm ts:check -> PASS。`

## Permission, Error, Loading, Empty State Checks

- Permission: button is controlled by both `hasGoldenFingerPermission` and `v-hasPermi`.
- Loading: button and confirm action use `goldenFingerBulkVoidLoading`.
- Error: dialog alert displays backend-specific error text.
- Empty/no-candidate handling is delegated to backend fail-fast response, then surfaced in dialog.

## E2E Status

- Optional real E2E not run in this turn because writable test tenant/account, gold-finger role and traceable candidate data were not confirmed.
## Acceptance

- 金手指权限用户可见 `金手指一键作废`，非金手指不展示入口。
- 弹窗明确说明按当前筛选条件跨页作废所有可作废批次。
- 提交 payload 使用当前筛选条件，不依赖当前页表格勾选。
- 批量直通不调用审批解析接口；单条作废仍走原 BPM 作废申请 API。

## Verification

- `node --check tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js` -> PASS
- `node tests\e2e\edhr-batch-execution-golden-finger-bulk-void-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Blockers

- Optional real E2E was not executed because local runtime, gold-finger test account and traceable writable batch execution data were not confirmed.