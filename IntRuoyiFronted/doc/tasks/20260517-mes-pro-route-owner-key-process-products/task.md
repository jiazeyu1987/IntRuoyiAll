# Task: MES 工艺流程负责人编辑与关键工序产品列

## Goal

调整 `MES / 生产管理 / 工艺流程` 前端：

- 列表将 `末道工序` 改为 `关键工序`
- 列表新增 `关联产品` 列，显示关联的所有产品编号
- 编辑工艺流程表单中可修改 `负责人`

## Scope

- 只修改工艺流程列表页、路由表单页和对应前端验证
- 依赖后端接口提供 `ownerName`、`keyProcessName`、`productCodes`
- 不改导入、状态切换、删除流程

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-mes-pro-route-list-owner-last-process/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this follow-up route enhancement.

## BDD

BDD: route list shows key process and product codes -> Given the MES 工艺流程列表加载完成, When the user查看表头和行内容, Then 列表显示 `负责人`、`关键工序`、`关联产品` 三列，并不再显示 `末道工序`。

BDD: route edit form exposes owner field -> Given the operator opens an existing MES 工艺流程 for edit, When the route form renders, Then the form contains an editable `负责人` 输入框 bound to `formData.ownerName`.

## Milestones

- [x] M1: Add a failing frontend/source regression for key process, product codes, and owner edit field.
- [x] M2: Update the route list columns and the route edit form owner field.
- [x] M3: Run targeted frontend verification.
- [x] M4: Record RED/GREEN evidence and finalize status.

## Expected Verification

- `node tests/e2e/mes-pro-route-columns.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session mes-pro-route-owner-key-process-products run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-mes-pro-route-owner-key-process-products\scripts\verify-mes-pro-route-owner-key-process-products.mjs`

## Current Status

Completed.

## Final Verification

- `node tests/e2e/mes-pro-route-columns.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS
- real browser verification script -> PASS

## Blockers

None.
