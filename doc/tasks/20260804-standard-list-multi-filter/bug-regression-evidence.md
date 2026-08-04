# Bug

- 用户截图反馈排产工单页面同时出现左侧旧 quick filter 和右侧条件 Tab 多维筛选两块筛选区域。
- 影响：用户会看到重复筛选入口，且旧 quick filter 与新 Tab 交互并存，违背“只保留右边”的设计决策。

## Expected

- 排产工单主列表启用右侧条件 Tab 多维筛选时，左侧旧 quick filter 区域不可见。
- 右侧条件 Tab 仍支持完成状态、排产工单号、来源生产工单号交集查询和重置。

## Reproduction

- 打开本机真实页面 `http://127.0.0.1:8081/mes/pro/schedule-order`。
- RED: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: `排产工单启用右侧条件 Tab 多维筛选时，必须关闭左侧旧 quick filter 区域。`

## Root Cause

- `ScheduleOrderMainList.vue` 启用 `showMultiFilter` 后仍未向 `UnifiedListTemplate` 传递 `showQuickFilter=false`。
- `UnifiedListTemplate` 默认 `showQuickFilter=true`，因此旧 `TableQuickFilter` 和新 `TableMultiFilter` 同时渲染。

## Regression Test

- 更新 `tests/e2e/schedule-order-main-multi-filter-static.spec.js`，静态锁定 `:show-quick-filter="!showMultiFilter"`。
- 更新 `schedule-order-multi-filter-real.e2e.cjs`，真实页面断言 `.unified-list-template__quick-filter:visible` 数量为 `0`。

## RED:

- RED: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> FAIL, expected reason: wrapper 未关闭旧 quick filter。

## GREEN:

- GREEN: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> PASS.
- GREEN: `node doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS, `legacyQuickFilterVisibleCount=0`.

## Verification

- `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> PASS.
- `node tests\e2e\unified-list-template-static.spec.js` -> PASS.
- `node tests\e2e\mes-schedule-order-sync-tab-static.spec.js` -> PASS.
- `node tests\e2e\mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS.
- `pnpm ts:check:schedule` -> PASS.
- `pnpm ts:check` -> PASS.

## Blockers

- Commit/push remains blocked by shared branch dirty/ahead concurrent changes; no broad staging or push was performed.
