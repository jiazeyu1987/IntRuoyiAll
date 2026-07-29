# Frontend Feature Evidence

## Feature Goal

辅助表格映射预览中，已映射字段名单行省略显示，删除格内字段类型圆标和独立取消映射按钮，改为双击已映射格取消映射。

## Non-goals

- 不改变辅助表格行列配置、保存 payload、rowKey 协议或后端接口。
- 不改变原表格字段选择和映射建立流程。

## Requirements

- R1: 已映射辅助格字段名 `white-space: nowrap`，超出显示 `...`。
- R2: 预览格内不显示字段类型圆标。
- R3: 预览格下方不显示独立“取消映射”按钮。
- R4: 双击已映射辅助格调用既有 `removeAssistGridCellMapping(gridCell.key)`。

## UI Entry Points

- 批记录填写规则确认弹窗辅助映射模式。
- FormCenter 模板填写配置弹窗辅助映射模式。

## BDD Scenarios

- BDD: 映射格紧凑显示与双击取消 -> Given 用户在辅助映射模式中看到已映射辅助格 / When 字段名称超出格宽且用户需要取消映射 / Then 字段名称保持单行并用省略号截断，格内不显示字段类型圆标和独立取消映射按钮，双击已映射辅助格会调用取消映射逻辑释放原表单元格。

## Verification

- RED: pending
- GREEN: pending
- Responsive/accessibility: pending
- Type check: pending

## Blockers

- None currently.
