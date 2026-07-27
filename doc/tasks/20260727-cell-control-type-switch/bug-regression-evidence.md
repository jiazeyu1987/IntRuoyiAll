# Bug Regression Evidence

## Bug Summary

在“单元格规则”弹窗右侧选择字段类型后，选项对应的规则对象发生内部修改，但 Element Plus 选择框显示值和关联区域没有稳定地立即刷新。例如选择“数字”后，选择框仍显示“文本”。

## Expected Behavior

选择数字、日期、日期时间、勾选、签名或下拉框后，字段类型显示值、控件类型、数字范围或下拉选项区域必须立即同步，不等待保存、重新选择单元格或刷新页面。

## Reproduction

- User path: 批记录表单列表 -> 规则 -> 选择一个可填写单元格 -> 字段类型 -> 选择“数字”。
- Evidence: `C:\Users\BJB110\AppData\Local\Temp\codex-clipboard-444d6ff7-6619-4941-a4ea-0af6c104e3e6.png`。
- Observed: 下拉选项中已选择数字，但选择框显示值仍为文本。

## Root Cause

- 字段类型使用 writable computed 直接作为 `v-model`。
- 类型切换 handler 仅修改当前规则对象的内部字段，没有替换 `ruleRows` 中对应规则行。
- 该组合没有为 Element Plus 选择框和左侧预览提供稳定的引用变化，导致显示同步不可靠。

## Regression Test

- Updated: `IntRuoyiFronted/tests/e2e/edhr-cell-control-type-switch-static.spec.js`
- Contract: 字段类型选择器必须使用显式 `:model-value + @change`，并存在 `replaceSelectedRule` 规则行替换逻辑。

## RED:

- Command: 对 `dd5e6f59^` 版本的 `BatchRecordCellRulesConfirmDialog.vue` 执行源码契约断言。
- Result: FAIL，旧版本缺少显式 `@change` 和 `replaceSelectedRule`。
- Expected reason: 旧版本不能保证选择框显示和规则行立即同步。

## GREEN:

- Command: `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js`
- Result: PASS，输出 `PASS: eDHR cell control type switch static contract`。
- Command: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- Result: PASS；仅 Windows LF/CRLF 提示。

## Fix

- 字段类型选择器改为显式 `:model-value="selectedRuleEditorValueType"` 和 `@change="handleSelectedEditorValueTypeChange"`。
- 增加 `replaceSelectedRule`，通过替换 `ruleRows` 中当前规则行触发稳定重新渲染。
- 字段类型和控件类型切换统一使用规则行替换，不增加 fallback 或延迟同步分支。

## Verification

- 父版本源码契约稳定失败，证明旧实现缺少即时同步机制。
- 当前静态契约通过，覆盖显式 change 和规则行替换。
- 本任务路径 `git diff --check` 通过。

## Risk And Regression Scope

- Scope: `BatchRecordCellRulesConfirmDialog.vue` 的字段类型、控件类型、左侧预览标签和条件配置区域。
- Risk: 规则行替换必须保留 label、placeholder、helpText、required、constraints 等已有字段；通过对象展开和 `normalizeCellRule` 保持契约。
- Backend/API contract: 未改变。

## Blockers And Follow-Up

- Blocker: 未运行真实浏览器 E2E；当前完成静态契约验证。
- Follow-up: 用户刷新最新前端后，按原路径验证选择数字时显示立即变为数字，并出现字段范围。
