# Bug Regression Evidence

## Bug Summary

- Symptom: eDHR 批次执行详情页右侧栏单据卡片下方额外显示独立“填写人 / 提交时间”元信息块。
- Expected behavior: 用户红框标注区域不显示；每张单据卡片自身的填写人、状态、门禁提示和打开入口保持可见。

## Reproduction

- Path: eDHR 批次执行详情页右侧工序单据栏。
- Reproduction command: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`。

## Root Cause

- `BatchExecutionDetailPage.vue` 在右侧栏表单卡片列表后仍渲染 `edhr-batch-detail__primary-fill-meta` 独立块。
- 该块通过 `primaryFormFillMetaItems` 额外展示“填写人 / 提交时间”，与用户期望的卡片级展示重复且造成红框区域多余显示。

## Regression Test

- Updated `IntRuoyiFronted/tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js`。
- The test asserts the detail page no longer contains the primary fill metadata block or its dead computed helpers while keeping per-card filler display.

## RED

- RED: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> FAIL。
- Expected reason: current source still contained `class="edhr-batch-detail__primary-fill-meta"` before the fix.

## GREEN

- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS。

## Verification

- Target regression test passed after the fix.
- Adjacent eDHR static regressions passed after the fix.

## Risk And Regression Scope

- Scope is limited to the eDHR batch detail right-side review rail.
- Risk is low because the fix removes only the independent red-box metadata block and does not alter task card rendering, action handlers, API calls, or form navigation.

## Blockers And Follow-Up

- `pnpm ts:check` -> FAIL due existing unrelated DCC controlled-file browser type mismatches in `src/views/dcc/controlled-file/browser/index.vue`; no eDHR primary-fill-meta residue remained in the touched source file.
