# Task: 生产工单列表用产品编号替换工单来源列

## Goal

在 `MES 生产工单` 列表页中显示工单对应产品的编号，不再显示 `工单来源` 列，并让该位置改为 `产品编号` 列。

## Scope

- 检查前一个前端任务状态，并在开始前明确是否完成或阻塞。
- 先创建本任务文档、执行日志、前端证据和 Playwright 校验脚本，再修改生产代码。
- 仅修改 `生产工单` 列表页的列表列显示，不改后端接口、不改表单、不改其他页面。
- 保持现有路由、权限、查询条件和数据契约不变。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-pro-workorder-freeze-display-order/task.md`
- Status before this task: blocked.
- Impact: the older task was deferred because a newer user request changed the required work-order list behavior, and continuing that older scope would introduce unrelated UI changes.

## Milestones

- [x] M1: Check the previous frontend task state and create this task directory and documents.
- [x] M2: Record BDD and RED evidence for the real-page work-order header check.
- [x] M3: Implement the minimal frontend change on the production work-order list page.
- [x] M4: Complete GREEN verification with Playwright and targeted ESLint.
- [x] M5: Update evidence, mark the task completed, and create a scoped frontend commit.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-product-number-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-workorder-product-number-column\scripts\verify-workorder-product-number-column.mjs`
- `pnpm exec eslint src/views/mes/pro/workorder/index.vue`

## Current Status

Completed. The production work-order list now replaces the visible `工单来源` column with `产品编号` and passes the real-page header verification.

## Blocker And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-product-number-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-workorder-product-number-column\scripts\verify-workorder-product-number-column.mjs` -> PASS
- `pnpm exec eslint src/views/mes/pro/workorder/index.vue` -> PASS
- Verified behavior:
  - the real page `http://127.0.0.1:8081/mes/pro/work-order` no longer shows `工单来源`
  - the headers now include `工单类型 | 产品编号 | 产品名称`
  - existing route access and data contract remain unchanged
