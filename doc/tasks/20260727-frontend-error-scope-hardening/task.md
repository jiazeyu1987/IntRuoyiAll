# Frontend Error Scope Hardening

## Task Goal

Prevent successful primary content from being presented as a full-page load failure when a deferred auxiliary request, row-level request, detail-panel request, or user action fails.

## Milestones

- [x] Audit the identified frontend error-state sharing patterns.
- [ ] Add a focused failing static regression contract.
- [ ] Separate primary, auxiliary, row, detail, and action error ownership.
- [ ] Run focused and adjacent regression verification.
- [ ] Complete experience consolidation, cleanup, commit, and push.

## Expected Verification

- Focused static contract fails before implementation and passes after implementation.
- Primary list/detail failures continue to use the existing page-level load error.
- Deferred, row-level, secondary-panel, verification, export, and create-operation failures remain visible in their local scope.
- `pnpm ts:check` passes or any unrelated blocker is recorded precisely.

## Current Status

in_progress

## Scope

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`
- `IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/FieldAuditPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/FieldAuditDetailPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr/DomainTraceDetailPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-delivery/DeliveryPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/edhr-validation/ValidationPage.vue`

## Baseline Preservation

- The branch was already ahead of `origin/int_main` by two unrelated commits.
- The worktree contains unrelated concurrent changes.
- `BatchExecutionDetailPage.vue` contains a concurrent special-node filler display hunk around the template; this task must preserve it and only edit the non-overlapping error-scope logic.

## Applicable Experience Gate

- Trigger: Primary content succeeds, but a delayed row, preview, candidate, supplemental-status, or right-side detail request fails and shows a global error.
- Preflight check: Only primary queries may write the page-level load error; auxiliary failures must remain row, card, panel, preview, or action scoped and retain the real error text.
- Blocker: Auxiliary failure clears primary content, overwrites the primary error, returns default success, or hides the real error.
- Verification: Focused static contracts must prove primary errors remain global while local failures remain local.
- Evidence: `docs/frontend-development.md#前端延迟辅助加载错误归属门禁`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；所有失败必须保留真实错误文本并显示在正确作用域。
- `是否从根因和长期维护角度解决`：是；通过拆分错误状态消除跨区域污染。
- `是否存在临时补丁或绕过`：否。
