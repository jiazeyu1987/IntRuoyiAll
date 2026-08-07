# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal：在一线 PQC 订单弹框内按订单号过滤全局 ACTIVE 候选，并支持确定性的回车快速选择。
- Non-goal：不改变订单、路线工序、PQC 人员、规程、任务生成或提交 API。

## Requirements And Acceptance IDs

- AC-1：订单候选继续来自 `/mes/pro/feedback/frontline/device-account/pqc/active-orders`。
- AC-2：输入订单号按大小写不敏感的包含关系过滤候选，清空恢复全部。
- AC-3：回车只选择订单号完全匹配项，或当前唯一过滤结果。
- AC-4：零结果提供明确空状态；多条模糊结果不自动猜测。

## UI Entry And Owned Files

- Route：`/mes/pro/feedback/edhr-batch-pqc-fill`。
- Component：`src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- Test：`tests/e2e/mes-frontline-pqc-all-active-orders-search-static.spec.cjs`。

## API Contract And Data States

- API contract unchanged：`GET /mes/pro/feedback/frontline/device-account/pqc/active-orders`。
- Loading/error/empty states retain the existing fail-fast context behavior。
- Search empty state applies only after the full formal candidate list has loaded。

## BDD And TDD Evidence

- BDD：见 `execution-log.md`。
- RED：pending。
- GREEN：pending。

## Responsive Accessibility And Permission Checks

- Search input will expose an order-number `aria-label` and use the existing fixed-format picker canvas。
- Existing `mes:pro-feedback:query` endpoint permission remains unchanged。
- Pending verification：layout regression, keyboard Enter path, zero-result state, `pnpm ts:check`。

## Blockers And Follow-up Skills

- 当前无 blocker。

