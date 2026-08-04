# Feature

- Goal: add configuration-driven multi-dimensional filtering support to the standard list template.
- Non-goals: no backend contract changes, no page-specific migration, no frontend-only data filtering, no localStorage/sessionStorage fallback, and no mock/default-success path.
- Owned files: `src/components/UnifiedListTemplate/index.vue`, `src/components/TableMultiFilter/index.vue`, `src/components/TableMultiFilter/MultiFilterField.vue`, `src/hooks/web/useTableMultiFilter.ts`, and `tests/e2e/unified-list-template-multi-filter-static.spec.js`.

## Acceptance

- Standard list template exposes an optional `showMultiFilter` mode with `multiFilterDefinitions`, `multiFilterState`, and multi-filter query/reset/remove events.
- Multi-filter definitions support text, select, multiSelect, dateRange, autocomplete, and numberRange fields.
- Multi-filter state is condition-array based and maps only configured fields into query params, resetting `pageNo` to 1 on query/reset.
- Existing quick filter, extra filters, table slot, pagination, sorting, and column settings remain intact.

## BDD

- BDD: 多条件筛选配置渲染 -> Given 页面提供多个筛选定义 / When 用户打开标准列表模板 / Then 模板按配置渲染默认可见筛选项和更多筛选入口，不要求页面手写额外筛选布局。
- BDD: 多条件筛选参数提交 -> Given 用户同时设置文本、下拉和日期范围筛选 / When 点击查询 / Then 前端只提交正式配置映射出的 query params，并将 `pageNo` 重置为 1。
- BDD: 多条件筛选重置 -> Given 用户已经设置多个筛选条件 / When 点击重置或清除条件 / Then 前端清空所有配置驱动筛选参数并重新加载第一页。

## RED

- RED: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> FAIL, expected reason: `多维度筛选组件必须存在。`

## GREEN

- GREEN: `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/unified-list-template-static.spec.js` -> PASS.
- GREEN: target TypeScript syntax transpile check -> PASS for touched hook and SFC script blocks.

## Verification

- Static contract verifies template props/events, multi-filter components, condition chips, more-filter popover, field types, range query mapping, `pageNo = 1`, explicit `multiFilters`, and no browser storage fallback.
- Regression contract verifies the existing unified list template still provides quick filter, extra filters, table slot, pagination, sorting helpers, and column settings.
- Full `pnpm ts:check` was attempted and failed on unrelated existing QA template export errors before current task files.
- Target ESLint command was attempted but hung with no output; the task-owned lint process was stopped and recorded as a blocker.
- Real browser regression was run against `/system/user` and passed login plus standard list quick-filter visibility.
- New multi-filter interaction E2E is blocked because no business page currently enables `showMultiFilter` or provides `multiFilterDefinitions`.

## Blockers

- Full repository typecheck is blocked by unrelated existing errors in `src/views/mes/qc/template/index.vue` and `@/api/mes/qc/template`.
- True multi-filter E2E is blocked by the missing real business-page entry; adding a pilot page would be a separate approved migration, not a verification-only action.
- Formal commit/push closeout is blocked by the repository's pre-existing dirty/ahead state with many unrelated task changes.
