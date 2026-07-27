# Frontend Feature Evidence

## Feature Goal

在现有 `BatchRecordCellRulesConfirmDialog.vue` 中提供单元格控件类型切换和下拉选项编辑能力。

## Requirements And Acceptance

- AC-1：字段类型可选择文本、数字、日期、日期时间、勾选、签名和下拉框。
- AC-2：选择类型后，选择框显示值和条件配置区域必须立即同步。
- AC-3：下拉框至少配置两个选项；数字最小值不得大于最大值。
- AC-4：电子签名缺少 enabled `edhrSignature` marker 时保存失败，不降级。

## Non-Goals

- 不改造 Jimu iframe 原生设计器。
- 不新增右键菜单。
- 不改运行时权限或菜单。

## Entry Points

- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
- `IntRuoyiFronted/src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts`
- `IntRuoyiFronted/src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue`
- Route/menu: 复用批记录表单列表既有入口，不新增路由或权限。

## API Contracts

- `GET /mes/pro/batch-record-report/cell-rules`
- `PUT /mes/pro/batch-record-report/cell-rules`
- Data states: loading 使用既有 `v-loading`；读取/保存错误显示 `el-alert` 和真实错误消息；无选中单元格显示 `el-empty`。

## BDD:

- Given 字段类型当前为文本 / When 用户选择数字 / Then 选择框立即显示数字、控件类型同步为 `input-number`、字段范围立即显示。
- Given 用户选择下拉框 / When 类型切换完成 / Then 立即显示下拉选项配置区域。
- Given 用户选择日期、日期时间、勾选或签名 / When 类型切换完成 / Then 选择框和控件类型立即同步。

## RED:

- `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` 初始失败，证明规则弹窗缺少 select 控件入口、下拉选项编辑、保存 constraints options、模板 select 渲染和 API options 类型。
- 对 `dd5e6f59^` 源码执行显示同步契约断言 -> FAIL；旧版本缺少显式 `@change` 和 `replaceSelectedRule`。

## GREEN:

- `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` -> PASS。
- 前端实现点：规则弹窗支持控件类型切换、下拉选项 textarea、select 保存前校验、SIGNATURE marker 提示、模板内 select 下拉渲染。
- 显示同步实现点：字段类型使用显式 `:model-value + @change`，类型切换替换当前规则行。

## UX And State Checks

- Responsive: 复用现有全屏规则弹窗和固定右侧面板，不新增断点或溢出布局。
- Accessibility: 保留原有表单 label、选择框键盘行为和单元格 `aria-pressed`。
- Loading: 读取和保存继续使用既有 loading 状态。
- Empty: 无选中单元格和无布局继续使用既有 `el-empty`。
- Error: API 错误、下拉选项不足、数字范围非法继续显示真实错误。
- Permission: 不新增菜单、路由或权限判断。

## Verification Path

- Static contract: `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js`
- Manual path: 批记录表单列表 -> 规则 -> 选中可填写单元格 -> 字段类型 -> 选择数字/下拉框。
- Expected: 选择框显示和条件区域立即更新。

## Blockers And Follow-Up

- 无实现 blocker。
- 真实页面 E2E 未运行；用户需在刷新最新前端后按 Verification Path 复验。
- Follow-up skill: 如真实页面仍不同步，使用 Playwright 和 `bug-regression-fix-loop` 采集浏览器事件与组件状态。
