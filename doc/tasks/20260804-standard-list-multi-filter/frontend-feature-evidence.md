# Feature

- Goal: 标准列表模板多维筛选改为可复用条件 Tab 交互，在红框区域通过加号新增条件、减号删除当前条件，查询时把所有已填写 Tab 条件作为交集提交。
- Non-goals: 不改变后端接口契约，不引入页面特例筛选、不做前端本地过滤、不新增 mock 或 fallback。
- Owned files: `TableMultiFilter`、`MultiFilterField`、`useTableMultiFilter`、`UnifiedListTemplate`、排产工单 pilot 接入文件、任务专用静态合同和真实 E2E 脚本。

## Acceptance

- AC1: 标准列表模板显示条件 Tab 行，支持加号新增和减号删除当前条件。
- AC2: 当前 Tab 可选择筛选字段、操作符和值，条件 `id` 稳定保留。
- AC3: 查询时所有已填写 Tab 映射为正式 query 参数；排产工单不得发送临时 `multiFilters`。
- AC4: 排产工单真实页面保留动作栏、快速过滤、表格和分页，目标写请求数为 0。

## BDD:

- BDD: 条件 Tab 动态增删 -> Given 标准列表模板启用多维筛选 / When 用户点击加号和减号 / Then 组件新增或删除条件 Tab，且字段选择器仍在当前 Tab 内可操作。
- BDD: 条件 Tab 交集查询 -> Given 用户填写完成状态、排产工单号和来源生产工单号三个条件 Tab / When 点击查询 / Then 列表请求携带 `completionFilter`、`code`、`erpWorkOrderCode` 正式参数且不携带 `multiFilters`。

## RED:

- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: 旧固定筛选栏缺少条件 Tab、加减号和当前 Tab 字段选择器。
- RED: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: 排产工单仍存在页面级 inline filter 特例且默认条件缺少稳定 id。

## GREEN:

- GREEN: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> PASS.
- GREEN: `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.

## Verification

- Static contracts: unified list template multi-filter, schedule order main multi-filter, unified list template, schedule order sync tab, schedule order replan visible filter all passed.
- Type checks: `pnpm ts:check:schedule` passed; latest full `pnpm ts:check` is blocked by unrelated concurrent `BatchPqcLeaderWorkbenchPage.vue` type error.
- Real E2E: 排产工单页面 filtered params were `completionFilter=ALL`, `code=SCH-CODEX-FACTOR-20260708093210-20260710-0001`, and `erpWorkOrderCode=CODEX-FACTOR-20260708093210`; reset cleared formal params and `multiFilters`; target write requests were `0`.

## Blockers

- Full `pnpm ts:check` currently fails outside this task at `src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue(3,26)`.
- Commit/push remains blocked by shared branch dirty/ahead concurrent changes; no broad staging or push was performed.
