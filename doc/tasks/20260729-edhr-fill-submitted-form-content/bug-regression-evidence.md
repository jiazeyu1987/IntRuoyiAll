# Bug Regression Evidence

## Bug Summary

批次详情红框主区域在没有已提交批记录内容时显示空态占位，未展示用户需要的空表单；存在已提交内容时必须确保展示 submitted execution 的表单单元内容。

## Expected Behavior

- 无已提交内容：主区域显示空白表单。
- 有已提交内容：主区域显示已提交表单里对应单元格内容。
- 草稿或预览单元值不得冒充已提交内容。

## Reproduction

- `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` 当前失败，证明页面还没有 `selectedEmptyTaskPreviewFormViewModel`，无法在无已提交内容时显示空表单。

## Root Cause

- `BatchExecutionDetailPage.vue` 主区域只在 selected submitted execution 存在时渲染只读表单；无 submitted execution 时直接显示 `暂无已提交批记录内容`，没有使用正式预览模板展示空表单。旧 task preview 合同又禁止主区域使用 preview，导致空表单壳缺失。

## Regression Test

- `IntRuoyiFronted/tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js`

## RED/GREEN

- RED: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> FAIL，缺少空表单壳数据源。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix.e2e.js` -> PASS.

## Verification

- `node tests/e2e/edhr-batch-first-screen-detail-defer-static.spec.js` -> PASS.
- `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS.
- `node tests/e2e/edhr-batch-detail-assist-preview-switch-static.spec.js` -> PASS.
- `node tests/e2e/edhr-batch-detail-preview-scroll-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.

## Risk And Regression Scope

- 风险集中在批次详情主区域只读渲染，不改变写入链路。

## Blockers

- 暂无。
