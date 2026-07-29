# Frontend Feature Evidence

## Feature Goal

让批次详情辅助模式与配置页“辅助表单预览”保持网格结构一致。

## Non-goals

- 不修改辅助配置保存接口。
- 不修改批记录正式表单、表单槽位或工序开始配置。
- 不增加写入动作。

## Acceptance

- A1: 按责任主体分组。
- A2: 按配置行列坐标定位字段。
- A3: 保留未映射空单元格。
- A4: 显示字段名称、类型和当前值。
- A5: 保持只读且无写入入口。

## Entry And Owned Files

- Route: `/mes/pro/feedback/edhr-batch-execution/detail`
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- Test: `IntRuoyiFronted/tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js`

## API And Data Contract

- 使用现有 `selectedExecution.formViewModel.executionSnapshotJson` 中的 `assistRows` 和 `fields`。
- 不修改请求、响应或后端。

## BDD

见 `execution-log.md`。

## RED

`node tests/e2e/edhr-batch-detail-assist-grid-parity-static.spec.js` -> FAIL，当前详情页仍为扁平字段列表。

## GREEN

Pending.

## UI State Checks

- Responsive: pending.
- Accessibility: pending.
- Loading/empty/error: pending.
- Permissions/read-only: pending.
- Real browser: pending.
