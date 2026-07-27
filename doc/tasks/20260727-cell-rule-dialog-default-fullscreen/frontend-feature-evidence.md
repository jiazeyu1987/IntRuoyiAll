# Frontend Feature Evidence

## Feature Goal

- 让批记录表单列表的“单元格规则”弹窗打开时默认全屏。

## Non-Goals

- 不改变弹窗内容、保存逻辑、字段类型选择逻辑或后端 API。
- 不新增全屏回退逻辑、兼容分支或无关视觉重设计。

## Requirements And Acceptance

- REQ-1: 打开“单元格规则”弹窗时默认全屏。
- AC-1: 组件模板对 `Dialog` 显式传入默认全屏配置。
- AC-2: 原有标题、宽度、内容布局、底部按钮与保存动作不被移除。

## UI Entry Points And Owned Files

- Entry: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue` 中的 `BatchRecordCellRulesConfirmDialog`。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`。
- Test: pending.

## API Contracts And Data States

- 不变更 API 契约、请求 payload、加载态、错误态或保存状态。

## BDD Scenarios

- Given 用户在批记录表单列表打开“单元格规则”弹窗; When 弹窗首次显示; Then 弹窗应默认处于全屏状态并保留原有内容、右侧配置面板和底部操作按钮。

## Verification

- RED command: pending.
- GREEN command: pending.
- Responsive/accessibility/error/loading/permission checks: no behavior change beyond default dialog display mode; static contract will verify content and buttons remain present.

## Blockers

- None currently.
