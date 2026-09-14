# Verification Report

## Scope

- 修复一线 PQC 页面底部红框检验项目 tab：可见标题显示正式 `itemName`，不显示内部 `itemCode`。
- 保留 `itemCode` 作为 `key`、tab 切换身份和正式提交身份。

## Result

- PASS: 红框 tab 模板使用 `formatPqcInspectionItemTabLabel(item)`，不再直接显示 `item.label`。
- PASS: 项目映射写入规范化后的 `itemName`，`label` 不再回退展示 `itemCode`。
- PASS: 提交明细继续传递 `itemCode: item.key`，并单独传递 `itemName: item.itemName`。

## Verification Commands

- PASS: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- PASS: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js`
- PASS: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js`
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs doc/tasks/20260808-pqc-tab-item-name-display`
