# Verification Report

## Scope

- 修复一线 PQC 页面红框 tab：可见主标题显示正式检验方法 `目视检验`，不显示 `AO5 final inspection`。
- 同步修复检验方法弹窗：标题和正文都使用规范化后的检验方法。
- 保留 `itemCode`、`itemName` 和 `inspectionMethod` 的结构化提交身份，不把内部字段改写成中文文案。

## Result

- PASS: 红框 tab helper 改为 `formatPqcMethodSummary(item)`。
- PASS: 检验方法弹窗不再读取 `activePqcMethodItem.label`。
- PASS: `Visual inspection` 仍规范化显示为 `目视检验`。
- PASS: 提交 payload 仍包含 `itemCode: item.key`、`itemName: item.itemName` 和 `inspectionMethod: item.inspectionMethod`。

## Verification Commands

- PASS: `node tests/e2e/pqc-tab-method-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- PASS: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs doc/tasks/20260808-pqc-active-title-runtime-method-display`
