# 任务：恢复展厅产品管理中的审批状态、资料状态、指派对象列

## Goal

修复 `展厅 -> 产品管理` 列表中 `审批状态`、`资料状态`、`指派对象` 三列未显示的问题，确保后台产品管理页继续使用真实产品列表数据渲染这三列，不引入 fallback、mock、兼容分支或静默降级。

## Scope

- 定位 `showroom-admin` 产品管理页与 `ProductListTable` 组件中导致三列未显示的根因。
- 先补一条会失败的定向回归测试，再做最小修复。
- 用真实前端入口 `http://localhost:8081` 做至少一次定向验证。
- 更新任务文档、执行日志与 closeout preview 证据。

## Non-Scope

- 不改动无关产品字段、审批状态机、后端接口契约或其他展厅页面。
- 不顺带调整产品管理页的视觉样式、列顺序或筛选项设计。
- 不为测试额外添加前端临时控件、演示数据或 mock 数据。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-hide-manufacturing-honors\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓任务已完成，不阻塞本次产品管理列表缺陷修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库中存在其他任务文档和在途改动。
- Impact: 本任务只修改产品管理列表相关源码、测试与当前任务目录，避免覆盖无关变更。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [x] M2: 复现“三列未显示”问题并记录 BDD/RED 证据。
- [x] M3: 做最小代码修复并补足回归测试。
- [x] M4: 运行定向测试与真实页面验证，记录 GREEN。
- [x] M5: 更新任务文档、执行 closeout preview，并在边界允许时准备提交。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-status-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-columns-restore\scripts\verify-showroom-product-status-columns.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-status-assignee-columns-restore --mode preview`

## Current Status

Completed on 2026-05-21.

已将 `资料状态`、`审批状态`、`指派对象` 前移到产品列表首屏可视区域，并压缩操作列宽度。源码级回归、真实页面回归与 closeout preview 均已通过。

## Blockers And Impact

- Blocker: none.
- Impact: pending closeout preview and task-scoped commit.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-list.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-status-columns-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-columns-restore\scripts\verify-showroom-product-status-columns.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-status-assignee-columns-restore --mode preview`
