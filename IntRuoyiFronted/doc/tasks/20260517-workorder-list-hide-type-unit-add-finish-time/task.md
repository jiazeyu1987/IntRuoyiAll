# Task: 生产工单列表隐藏工单类型和单位并增加完成时间

## Goal

调整 MES 生产工单列表：

- 不显示 `工单类型`
- 不显示 `单位`
- 新增 `完成时间` 列
- 保持 `客户编码` 已替换成 `工单状态` 的结果不变

## Scope

- 仅修改生产工单列表列展示，不改筛选条件和接口。
- `客户编码` 位置继续显示 `工单状态`。
- `完成时间` 使用工单 `finishDate`。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-dcc-upload-route-position-name-display/task.md`
- Status before this task: completed.
- Impact: no unfinished latest frontend task blocks this list-column follow-up.

## Milestones

- [x] M1: Create task package before code edits.
- [x] M2: Record BDD scenarios and RED evidence for the current column layout.
- [x] M3: Implement the list column change.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only task-scoped files.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-list-hide-type-unit-add-finish-time run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-workorder-list-hide-type-unit-add-finish-time\scripts\verify-workorder-list-hide-type-unit-add-finish-time.mjs`

## Current Status

Completed for code delivery. The list now hides `工单类型` and `单位`, keeps `工单状态` in the former customer-code slot, and adds `完成时间`.
