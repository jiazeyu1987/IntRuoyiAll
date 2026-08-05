# Bug

- 用户截图反馈排产工单页面同时出现左侧旧 quick filter 和右侧条件 Tab 多维筛选两块筛选区域。
- 用户继续反馈同步工单同样是标准列表模板，但没有切换为右侧条件 Tab 多维筛选。
- 用户截图反馈条件为空时第二行“点击右侧加号新增筛选条件。”红框提示不应显示。
- 影响：用户会看到重复筛选入口，且旧 quick filter 与新 Tab 交互并存，违背“只保留右边”的设计决策。

## Expected

- 排产工单主列表启用右侧条件 Tab 多维筛选时，左侧旧 quick filter 区域不可见。
- 右侧条件 Tab 仍支持完成状态、排产工单号、来源生产工单号交集查询和重置。
- 同步工单页签也启用同一右侧条件 Tab，入池状态由稳定条件 Tab 表达，不再保留旧 quick filter、显示已入池开关或重复重置按钮。
- 条件为空时只保留 Tab 行内“暂无筛选条件”，不显示第二行新增提示。

## Reproduction

- 打开本机真实页面 `http://127.0.0.1:8081/mes/pro/schedule-order`。
- RED: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: `排产工单启用右侧条件 Tab 多维筛选时，必须关闭左侧旧 quick filter 区域。`
- RED: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> FAIL, expected reason: `同步工单启用条件 Tab 后必须关闭旧 quick filter。`
- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: `TableMultiFilter` 仍包含“点击右侧加号新增筛选条件。”空状态提示。

## Root Cause

- `ScheduleOrderMainList.vue` 启用 `showMultiFilter` 后仍未向 `UnifiedListTemplate` 传递 `showQuickFilter=false`。
- `UnifiedListTemplate` 默认 `showQuickFilter=true`，因此旧 `TableQuickFilter` 和新 `TableMultiFilter` 同时渲染。
- 同步工单页签直接使用 `UnifiedListTemplate`，但只显式接入了旧 `useTableQuickFilter` 和状态开关，没有传入 `showMultiFilter`、`workOrderAdmissionMultiFilterDefinitions` 和多维筛选事件；标准列表模板不会自动替换未显式启用的列表。
- `TableMultiFilter` 在没有 active condition 时额外渲染 `.table-multi-filter__condition-empty`，与 Tab 行内“暂无筛选条件”形成重复空状态提示。

## Regression Test

- 更新 `tests/e2e/schedule-order-main-multi-filter-static.spec.js`，静态锁定 `:show-quick-filter="!showMultiFilter"`。
- 更新 `tests/e2e/mes-schedule-order-sync-tab-static.spec.js`，静态锁定同步工单 `:show-quick-filter="false"`、`:show-multi-filter="true"`、正式参数定义和多维 hook。
- 更新 `schedule-order-multi-filter-real.e2e.cjs`，真实页面断言 `.unified-list-template__quick-filter:visible` 数量为 `0`。
- 更新 `tests/e2e/unified-list-template-multi-filter-static.spec.js` 和真实 E2E，锁定第二行新增提示 DOM/文本计数为 `0`。

## RED:

- RED: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: wrapper 未关闭旧 quick filter。
- RED: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> FAIL, expected reason: sync tab 未关闭旧 quick filter。
- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: empty condition prompt still rendered.

## GREEN:

- GREEN: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS.
- GREEN: `node doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS, `legacyQuickFilterVisibleCount=0`.
- GREEN: `node doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS, `admissionLegacyQuickFilterVisibleCount=0` and sync tab submitted only formal params.
- GREEN: `node doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS, `conditionEmptyPromptVisibleCount=0` and `conditionEmptyPromptTextCount=0`.

## Verification

- `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> PASS.
- `node tests\e2e\unified-list-template-static.spec.js` -> PASS.
- `node tests\e2e\mes-schedule-order-sync-tab-static.spec.js` -> PASS.
- `node tests\e2e\mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS.
- `pnpm ts:check:schedule` -> PASS.
- `pnpm ts:check` -> PASS.
- Real E2E sync tab result: `workOrderCode=SMART-SCHED-20260630-RERUN5-MO`, `productCode=AW.106.03.08.1007`, `admissionStatus=READY_TO_ADMIT`, `quickFilter` absent, `multiFilters` absent, target writes `0`.
- Real E2E empty prompt result: `conditionEmptyPromptVisibleCount=0`, `conditionEmptyPromptTextCount=0`, target writes `0`.

## Blockers

- Commit/push remains blocked by shared branch dirty/ahead concurrent changes; no broad staging or push was performed.
