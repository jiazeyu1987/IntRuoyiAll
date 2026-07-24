# Task: 生产工单列表客户编码改工单状态前端

## Goal

在 MES 生产工单列表中不再显示 `客户编码` 列，并把该列位置替换为 `工单状态` 列，避免状态重复显示在两个位置。

## Scope

- 先创建当前前端任务文档，再开始生产代码修改。
- 严格按 BDD + TDD 先补失败验证，再做最小实现。
- 保留 `客户名称` 列。
- 将现有右侧 `工单状态` 列迁移到 `客户编码` 的位置，不保留重复列。
- 不改接口、不改筛选条件、不做无关视觉重构。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-dcc-paper-distribution-audit-fields-frontend/task.md`
- Status before this task: completed for code delivery.
- Impact: no unfinished latest frontend task blocks this list-column change.

## Milestones

- [x] M1: Create frontend task directory, task doc, execution log, and evidence file.
- [x] M2: Record BDD scenarios and RED verification for the current redundant status placement.
- [x] M3: Implement the list column replacement.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only frontend files produced by this task.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-status-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-workorder-status-column-and-kingdee-confirmed\scripts\verify-workorder-status-column.mjs`

## Current Status

Completed for code delivery. The work-order list no longer shows `客户编码`; `工单状态` occupies that slot and remains the only visible status column.
