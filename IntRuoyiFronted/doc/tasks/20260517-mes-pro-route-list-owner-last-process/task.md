# Task: MES 工艺流程列表列替换为负责人与末道工序

## Goal

调整 `MES / 生产管理 / 工艺流程` 列表列定义：隐藏 `路线说明` 和 `备注`，改为显示 `负责人` 和 `末道工序`。

## Scope

- 只修改工艺流程列表页列定义与必要前端验证。
- 依赖后端分页接口提供 `ownerName` 与 `lastProcessName`。
- 不改详情弹窗、导入按钮、状态开关和操作按钮流程。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-dcc-file-category-list-columns-actions/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this route-list column change.

## BDD

BDD: route list hides description and remark -> Given the MES 工艺流程列表加载完成, When the user查看表头, Then `路线说明` 与 `备注` 列不再显示。

BDD: route list shows owner and last process -> Given the MES 工艺流程列表加载完成, When the user查看表头和行内容, Then 列表显示 `负责人` 与 `末道工序` 两列，并绑定到后端返回字段。

## Milestones

- [x] M1: Add a failing frontend/source regression for the old columns.
- [x] M2: Replace the list columns with `负责人` and `末道工序`.
- [x] M3: Run targeted frontend verification.
- [x] M4: Update task evidence and status.

## Expected Verification

- `node tests/e2e/mes-pro-route-columns.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- live dev-server source probe

## Current Status

Completed. The MES 工艺流程列表 now hides `路线说明/备注` and shows `负责人/末道工序`.

## Final Verification

- `node tests/e2e/mes-pro-route-columns.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS
- live dev-server source probe -> PASS

## Blockers

None.
