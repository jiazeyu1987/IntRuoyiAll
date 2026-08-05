# Feature

- Goal: 标准列表模板多维筛选改为可复用条件 Tab 交互，在红框区域通过加号新增条件、减号删除当前条件，查询时把所有已填写 Tab 条件作为交集提交，并在排产工单与同步工单两个真实列表复用。
- Non-goals: 不改变后端接口契约，不引入页面特例筛选、不做前端本地过滤、不新增 mock 或 fallback。
- Owned files: `TableMultiFilter`、`MultiFilterField`、`useTableMultiFilter`、`UnifiedListTemplate`、排产工单 pilot 接入文件、任务专用静态合同和真实 E2E 脚本。

## Acceptance

- AC1: 标准列表模板显示条件 Tab 行，支持加号新增和减号删除当前条件。
- AC2: 当前 Tab 可选择筛选字段、操作符和值，条件 `id` 稳定保留。
- AC3: 查询时所有已填写 Tab 映射为正式 query 参数；排产工单不得发送临时 `multiFilters`。
- AC4: 排产工单真实页面保留动作栏、右侧条件 Tab 筛选、表格和分页，不再显示左侧旧 quick filter，目标写请求数为 0。
- AC5: 同步工单真实页签复用同一条件 Tab，旧 quick filter、显示已入池开关和重复重置按钮不可见，筛选只提交后端正式参数。
- AC6: 条件为空时仅保留 Tab 行内“暂无筛选条件”，不再显示第二行“点击右侧加号新增筛选条件。”提示。

## BDD:

- BDD: 条件 Tab 动态增删 -> Given 标准列表模板启用多维筛选 / When 用户点击加号和减号 / Then 组件新增或删除条件 Tab，且字段选择器仍在当前 Tab 内可操作。
- BDD: 条件 Tab 交集查询 -> Given 用户填写完成状态、排产工单号和来源生产工单号三个条件 Tab / When 点击查询 / Then 列表请求携带 `completionFilter`、`code`、`erpWorkOrderCode` 正式参数且不携带 `multiFilters`。
- BDD: 只保留右侧条件 Tab 筛选 -> Given 排产工单主列表启用多维筛选 / When 用户进入排产工单页面 / Then 左侧旧 quick filter 不可见，右侧条件 Tab 筛选可见且可查询。
- BDD: 同步工单条件 Tab 复用 -> Given 用户切换到同步工单页签 / When 条件 Tab 渲染并查询 / Then 工单编码、产品编号和入池状态作为交集映射为 `workOrderCode`、`productCode`、`admissionStatus`，且不发送 `quickFilter` 或 `multiFilters`。
- BDD: 条件为空不显示第二行提示 -> Given 用户重置所有条件 Tab / When 多维筛选区域处于空条件状态 / Then 页面只显示“暂无筛选条件”和加号入口，不显示“点击右侧加号新增筛选条件。”。

## RED:

- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: 旧固定筛选栏缺少条件 Tab、加减号和当前 Tab 字段选择器。
- RED: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: 排产工单仍存在页面级 inline filter 特例且默认条件缺少稳定 id。
- RED: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> FAIL, expected reason: 同步工单仍绑定旧 quick filter，未启用条件 Tab。
- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: 条件为空时仍保留第二行新增条件提示。

## GREEN:

- GREEN: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> PASS.
- GREEN: `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- GREEN: duplicate-filter regression `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS, `legacyQuickFilterVisibleCount=0`.
- GREEN: sync-tab regression `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS, `admissionLegacyQuickFilterVisibleCount=0`, formal sync params submitted and reset cleared.
- GREEN: empty-condition prompt regression `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS, `conditionEmptyPromptVisibleCount=0` and `conditionEmptyPromptTextCount=0`.

## Verification

- Static contracts: unified list template multi-filter, schedule order main multi-filter, unified list template, schedule order sync tab, schedule order replan visible filter all passed.
- Type checks: `pnpm ts:check:schedule` and `pnpm ts:check` passed.
- Real E2E: 排产工单页面 `legacyQuickFilterVisibleCount=0`; filtered params were `completionFilter=ALL`, `code=SCH-CODEX-FACTOR-20260708093210-20260710-0001`, and `erpWorkOrderCode=CODEX-FACTOR-20260708093210`; reset cleared formal params and `multiFilters`; target write requests were `0`.
- Real E2E: 同步工单页签 `admissionLegacyQuickFilterVisibleCount=0`; filtered params were `admissionStatus=READY_TO_ADMIT`, `workOrderCode=SMART-SCHED-20260630-RERUN5-MO`, and `productCode=AW.106.03.08.1007`; reset cleared formal params; `quickFilter` and `multiFilters` were absent.
- Real E2E: 重置后空条件状态保留“暂无筛选条件”，第二行新增提示 DOM/文本计数均为 `0`。

## Blockers

- Commit/push remains blocked by shared branch dirty/ahead concurrent changes; no broad staging or push was performed.
