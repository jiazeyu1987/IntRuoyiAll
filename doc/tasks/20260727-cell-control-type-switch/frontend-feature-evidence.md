# Frontend Feature Evidence

## Feature Goal

在现有 `BatchRecordCellRulesConfirmDialog.vue` 中提供单元格控件类型切换和下拉选项编辑能力。

## Non-Goals

- 不改造 Jimu iframe 原生设计器。
- 不新增右键菜单。
- 不改运行时权限或菜单。

## Entry Points

- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`

## API Contracts

- `GET /mes/pro/batch-record-report/cell-rules`
- `PUT /mes/pro/batch-record-report/cell-rules`

## BDD

见 `execution-log.md`。

## RED

- `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` 初始失败，证明规则弹窗缺少 select 控件入口、下拉选项编辑、保存 constraints options、模板 select 渲染和 API options 类型。

## GREEN

- `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` -> PASS。
- 前端实现点：规则弹窗支持控件类型切换、下拉选项 textarea、select 保存前校验、SIGNATURE marker 提示、模板内 select 下拉渲染。

## Blockers

- 无前端实现 blocker；真实页面 E2E 未运行，本任务按既定 Expected Verification 使用静态契约覆盖。
