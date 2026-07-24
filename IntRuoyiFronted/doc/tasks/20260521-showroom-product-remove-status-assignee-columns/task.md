# 任务：删除展厅产品管理列表中的资料状态与指派对象列

## Goal

按用户最新要求，删除 `展厅 -> 产品管理` 列表中的 `资料状态` 与 `指派对象` 两列，不再为这两列做首屏压缩或可见性适配；保持其余列表行为、真实接口和数据契约不变。

## Scope

- 删除 `ProductListTable` 中 `资料状态` 与 `指派对象` 两个表格列。
- 先补一条会失败的回归测试，再做最小修复。
- 用真实前端入口 `http://localhost:8081` 验证列表不再渲染这两列。
- 更新任务文档、执行日志、变更申请与 bug regression evidence。

## Non-Scope

- 不改动资料状态/指派数据的后端计算逻辑、接口字段或产品详情页展示。
- 不删除筛选区中的 `资料状态` 查询条件，除非用户后续单独要求。
- 不顺带改动其他列表列、审批逻辑或产品详情行为。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-viewport-regression\task.md`
- Status before this task: `Blocked on 2026-05-21`
- Impact: 上一任务因用户改需求被中止；本任务承接新的明确实现方向。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的在途文档改动。
- Impact: 本任务只修改产品管理列表相关源码、测试和当前任务目录，避免覆盖无关变更。

## Milestones

- [x] M1: 记录变更决策并创建新任务文档。
- [ ] M2: 补充“这两列必须不存在”的 BDD/RED 证据。
- [ ] M3: 做最小代码修复并更新回归测试。
- [ ] M4: 运行源码级回归与真实页面验证，记录 GREEN。
- [ ] M5: 更新证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-remove-status-assignee-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\scripts\verify-showroom-product-columns-removed.mjs`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-remove-status-assignee-columns --mode preview`

## Current Status

Completed on 2026-05-21.

已按用户要求从 `展厅 -> 产品管理` 列表中删除 `资料状态` 与 `指派对象` 两列；源码级回归、ESLint、真实页面验证、变更校验与 closeout preview 均已通过。

## Milestone Status

### M1

- Status: Completed
- Completed work:
  - 记录用户对当前实现方向的变更请求，并将“首屏可见性”旧任务标记为 blocked。
  - 创建新的删列任务文档与执行日志。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\changes\20260521-showroom-product-remove-status-assignee-columns.md`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-viewport-regression\task.md`
- Remaining blockers:
  - None.

### M2

- Status: Completed
- Completed work:
  - 先将源码测试和真实页面验证改为“这两列必须不存在”的新验收标准。
  - 在未改源码前拿到了 RED 证据，确认当前模板和真实页面仍在渲染这两列。
- Verification evidence:
  - `node --test scripts/showroom-admin-product-list.test.mjs`
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-remove-status-assignee-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\scripts\verify-showroom-product-columns-removed.mjs`
- Remaining blockers:
  - None.

### M3

- Status: Completed
- Completed work:
  - 从 `ProductListTable.vue` 删除了 `资料状态` 与 `指派对象` 的表格列定义。
  - 保持筛选区、审批状态列、真实接口契约与其他列表行为不变。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- Remaining blockers:
  - None.

### M4

- Status: Completed
- Completed work:
  - 通过源码级测试、ESLint 和真实页面验证，确认列表已不再渲染这两列。
- Verification evidence:
  - PASS: `node --test scripts/showroom-admin-product-list.test.mjs`
  - PASS: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
  - PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-remove-status-assignee-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\scripts\verify-showroom-product-columns-removed.mjs`
- Remaining blockers:
  - None.

### M5

- Status: Completed
- Completed work:
  - 通过 change-request 校验、bug regression 校验与 closeout preview。
  - 已执行 task-closeout cleanup apply，清理 task 目录下的一次性 helper，仅保留任务记录。
- Verification evidence:
  - PASS: `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\changes\20260521-showroom-product-remove-status-assignee-columns.md`
  - PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\bug-regression-evidence.md`
  - PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-remove-status-assignee-columns --mode preview`
  - PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-remove-status-assignee-columns --mode apply`
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-list.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-remove-status-assignee-columns run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\scripts\verify-showroom-product-columns-removed.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\changes\20260521-showroom-product-remove-status-assignee-columns.md`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\bug-regression-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-remove-status-assignee-columns --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-remove-status-assignee-columns --mode apply`
