# Bug Regression Evidence

## Bug Summary

填写配置的辅助表单映射模式中，已经链接的原表单元格被 disabled，用户无法点击查看或重新选中对应映射；辅助表单中被链接格子也不会随原表单点击联动为绿色选中边框。

## Expected Behavior

已链接原表单元格仍可点击；点击后选中原表单单元格，并同步选中辅助表单里被链接的格子，两个位置都显示绿色边框。

## Reproduction

- 代码层复现：`BatchRecordCellRulesConfirmDialog.vue` 的原表单按钮存在 `:disabled="isSourceCellDisabledForAssistMapping(cell)"`。
- RED 命令：`node tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js`。

## Root Cause

旧实现把已分配原表单元格视为“不可重复分配”的禁用状态，同时在点击处理里直接提示先取消映射，导致用户无法通过点击已链接格子查看对应辅助表格格子。

## Regression Test Added

- `IntRuoyiFronted/tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js`

## RED

- RED: `node tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js` -> FAIL，断言旧 disabled 绑定仍存在。

## GREEN

- GREEN: `node tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification

- Verification: 静态合同和 `pnpm ts:check` 均通过；详见 `doc/tasks/20260729-linked-cell-click-green-border/verification-report.md`。

## Risk And Regression Scope

风险集中在辅助表单映射模式的单元格点击交互。保存数据结构未变，未链接单元格仍沿用原映射流程，已映射辅助格仍支持双击取消映射。

## Blockers And Follow-up Actions

- 无。
