# 任务：修复展厅产品导入表单缺失导致的编译失败

## Goal

修复 `src/views/showroom-admin/index.vue` 对 `@/views/showroom-admin/product/ShowroomProductImportForm.vue` 的导入失效问题，恢复展厅产品管理页的编译通过状态，并保持产品 Excel 导入入口继续使用真实接口，不引入 fallback、mock 或静默降级。

## Scope

- 复现当前 `ShowroomProductImportForm.vue` 缺失导致的 Vite 编译失败。
- 先补失败回归证据，再做最小修复。
- 修复 `showroom-admin` 产品 Excel 导入表单组件缺失或引用失效问题。
- 运行定向源码/编译验证，并更新任务文档、执行日志与 bug regression evidence。

## Non-Scope

- 不改动产品列表的其他列、详情页、审批流或导出 Excel 行为。
- 不改动后端导入接口契约，除非发现前端无法对接的真实阻塞。
- 不顺带处理仓库中的其他未提交任务改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓任务已闭环，不阻塞本次编译回归修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的在途文档与未跟踪改动。
- Impact: 本任务只修改展厅产品导入表单相关源码、测试与当前任务目录，避免覆盖无关变更。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [ ] M2: 复现缺失组件导致的编译失败，记录 BDD/RED 证据。
- [ ] M3: 做最小修复并补充回归测试。
- [ ] M4: 运行源码/编译验证，记录 GREEN。
- [ ] M5: 更新证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/product/ShowroomProductImportForm.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- `pnpm exec vite build --mode env.local`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-import-form-missing --mode preview`

## Current Status

Completed on 2026-05-21.

已确认 `ShowroomProductImportForm.vue` 导入链路恢复可用：源码级回归、ESLint 与真实页面验证均通过；全量 `vue-tsc` / `vite build` 在当前仓库仍受 Node 内存限制阻塞，但不再表现为本缺陷中的缺失 import 错误。

## Milestone Status

### M1

- Status: Completed
- Completed work:
  - 核对上一同仓任务已完成，并创建本次导入表单缺失回归任务文档与执行日志。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\task.md`
- Remaining blockers:
  - None.

### M2

- Status: Completed
- Completed work:
  - 根据用户提供的 Vite overlay 报错与初次文件扫描，确认 `ShowroomProductImportForm.vue` 在当时缺失，属于真实 import 解析失败。
  - 在后续检查中发现当前工作树已经出现未跟踪的导入表单组件与 `index.vue` 接线改动，因此不再重复构造“人为删除组件”的二次 RED。
- Verification evidence:
  - 用户现场报错：`Failed to resolve import "@/views/showroom-admin/product/ShowroomProductImportForm.vue" from "src/views/showroom-admin/index.vue"`
  - `rg --files src/views/showroom-admin | rg "ShowroomProductImportForm\\.vue|index\\.vue|ProductDetailDialog\\.vue|ProductWholeAssignmentDialog\\.vue"`
- Remaining blockers:
  - None.

### M3

- Status: Completed
- Completed work:
  - 补充 `scripts/showroom-admin-product-import-form.test.mjs`，锁定 `index.vue` 对导入表单的接线契约，以及导入表单对真实 API 的调用方式。
  - 复核并保留当前工作树中的 `ShowroomProductImportForm.vue`、`index.vue`、`ProductListTable.vue` 与 `src/api/showroom-admin/index.ts` 导入链路改动，作为本次缺陷修复的最小实现。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-import-form.test.mjs`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ShowroomProductImportForm.vue`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- Remaining blockers:
  - None.

### M4

- Status: Completed
- Completed work:
  - 通过源码级测试、ESLint 和真实产品页验证，确认导入表单组件已被成功解析，产品管理页不再出现 `Failed to resolve import` 覆盖层。
  - 额外尝试全量 `vue-tsc` 与 `vite build`，确认当前阻塞点已转为仓库级内存不足，而非导入表单缺失。
- Verification evidence:
  - PASS: `node --test scripts/showroom-admin-product-import-form.test.mjs scripts/showroom-admin-product-list.test.mjs`
  - PASS: `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/product/ShowroomProductImportForm.vue scripts/showroom-admin-product-import-form.test.mjs --format stylish`
  - PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-import-form-check run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\scripts\verify-showroom-product-import-form-live.mjs`
  - BLOCKED: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> Node heap out of memory
  - BLOCKED: `pnpm exec vite build --mode env.local` -> `VirtualAlloc failed`
- Remaining blockers:
  - 当前仓库全量类型/构建验证受 Node 内存限制阻塞，但与本缺陷中的缺失 import 无关。

### M5

- Status: Completed
- Completed work:
  - 通过 bug regression evidence 校验与 closeout preview。
  - 已执行 task-closeout cleanup apply，清理任务目录中的一次性证据脚本与 bug evidence 文件，仅保留核心任务记录。
- Verification evidence:
  - PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\bug-regression-evidence.md`
  - PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-import-form-missing --mode preview`
  - PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-import-form-missing --mode apply`
- Remaining blockers:
  - 待完成任务范围提交。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-import-form.test.mjs scripts/showroom-admin-product-list.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/product/ShowroomProductImportForm.vue scripts/showroom-admin-product-import-form.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-import-form-check run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\scripts\verify-showroom-product-import-form-live.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-import-form-missing\bug-regression-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-import-form-missing --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-import-form-missing --mode apply`
- BLOCKED: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> Node heap out of memory
- BLOCKED: `pnpm exec vite build --mode env.local` -> `VirtualAlloc failed`
