# Bug Regression Evidence

## Bug Summary

The MES 系统 / eDHR 批记录 / 批记录表单 page could show a global `系统异常` alert even after the real batch-record form list loaded successfully. The screenshot showed table rows and the right-side preview visible, so the failure was not the primary `/mes/pro/batch-record-report/page` list request.

## Expected Behavior

The global list alert is reserved for primary list-load failures. Deferred secondary requests, such as per-report filler permission metadata, must not pollute the global list error. Their real API error must remain visible at the affected row instead of being hidden or converted to mock success.

## Reproduction Command

```powershell
cd E:\IntRuoyi\IntRuoyiFronted
node tests/e2e/edhr-batch-record-form-list-secondary-error-static.spec.js
```

## Root Cause

`loadRecordFormSecondaryData` caught errors from delayed secondary loaders and wrote them to `listErrorMessage`. Because `deferRecordFormSecondaryLoad` runs after the list renders, a failure from `EdhrProcessFormPermissionRuleApi.getByReport()` could display as a global `系统异常` while the list and preview were otherwise usable.

## Regression Test

Added `IntRuoyiFronted/tests/e2e/edhr-batch-record-form-list-secondary-error-static.spec.js`. The contract proves:

- Primary `getList` failures still write the global list alert.
- Deferred secondary data failures no longer assign `listErrorMessage`.
- Filler permission failures are stored as `permissionRuleErrorMessage`.
- The filler column displays a row-level `加载失败` state and keeps the real error text in tooltip/title.

## RED

`node tests/e2e/edhr-batch-record-form-list-secondary-error-static.spec.js` -> FAIL, expected reason: `loadRecordFormSecondaryData` assigned `listErrorMessage.value`.

## GREEN

`node tests/e2e/edhr-batch-record-form-list-secondary-error-static.spec.js` -> PASS.

## Risk And Regression Scope

The change is limited to `batchrecordformlist/index.vue` and one focused static contract. It does not change backend permission semantics, primary list loading, preview loading, or any fallback behavior. Real permission API errors remain visible on the affected row.

## Blockers And Follow-Up

`node tests/e2e/edhr-batch-record-form-list-static.spec.js` still fails an unrelated legacy assertion expecting the batch-delete button template to contain `@click="handleBatchDelete"`. This task did not touch batch-delete UI or handlers.
