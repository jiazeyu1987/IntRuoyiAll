# Task: 生产工单冻结编码高亮与排序调整

## Goal

调整生产工单列表的展示规则：冻结工单的工单编码使用红色显示，非冻结工单在列表同层级内优先显示在前。

## Scope

- 检查上一条前端任务状态，确认没有未闭环任务阻塞本次小范围展示调整。
- 创建当前任务文档、执行日志与前端证据文件。
- 仅修改 `生产工单` 列表页展示层，不改动临时冻结业务逻辑或后端契约。
- 保持树形父子结构，仅在同层级内调整未冻结/冻结显示顺序。
- 完成针对性静态检查与必要的真实页面验证。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-pro-schedule-calendar-direct-route-e2e/task.md`
- Status before this task: completed.
- Impact: 排程日历直达路由任务已闭环，本次可以直接继续调整工单列表展示。

## Milestones

- [x] M1: 检查前序任务状态并创建当前任务目录、文档和证据文件。
- [x] M2: 调整工单编码冻结高亮与同层级排序规则。
- [x] M3: 完成静态检查/验证并提交当前任务相关改动。

## Expected Verification

- 冻结工单编码在 `生产工单` 列中为红色。
- 同层级的非冻结工单排在冻结工单前面。
- `npx.cmd eslint --ext .vue src/views/mes/pro/workorder/index.vue`

## Current Status

Completed. 前端高亮与同层级排序已实现，并已通过真实页面验证。

## Blocker And Impact

- Blocker: none for the frontend slice itself.
- Impact:
  - 冻结工单编码已改为红色显示。
  - 同层级内未冻结工单已优先显示在前。
  - 由于第一页数据切分页发生在后端，最终用户可见顺序还依赖后端分页排序支持；该部分已单独拆到后端任务 `20260516-pro-workorder-freeze-page-order`。

## Final Verification Result

- `npx.cmd eslint --ext .vue src/views/mes/pro/workorder/index.vue`
  - PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-freeze-display-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-workorder-freeze-display-order\scripts\verify-workorder-freeze-display.mjs`
  - PASS
  - result:
    - `url = http://127.0.0.1:8081/mes/pro/work-order`
    - `firstFrozenIndex = 2`
    - `firstNormalCode = 881MO090863`
    - `firstNormalColor = rgb(64, 158, 255)`
    - `firstFrozenCode = 881MO090756`
    - `firstFrozenColor = rgb(245, 108, 108)`
    - `visibleRows = 10`
