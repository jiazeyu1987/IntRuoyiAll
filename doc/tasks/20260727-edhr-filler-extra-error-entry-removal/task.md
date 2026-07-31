# 20260727 eDHR Filler Extra Error Entry Removal

## Task Goal

Remove the extra `查看错误` text entry from the batch-record form list `填写人` cell when the filler rule fails to load, while preserving the `加载失败` status and the real row-scoped error tooltip/title.

## Milestones

- [x] Preserve unrelated dirty worktree changes before task-owned implementation.
- [x] Record BDD and reproduce the extra error-state entry with a focused failing contract.
- [x] Implement the smallest frontend rendering fix.
- [x] Run focused and adjacent regression verification.
- [x] Complete cleanup, experience consolidation, commit, and push.

## Expected Verification

- Focused static contract fails before the fix because the error state renders `查看错误`.
- The same contract passes after the fix and proves the real error remains available through tooltip/title.
- Adjacent filler and deferred-secondary-error contracts pass.
- `pnpm ts:check` passes or an unrelated pre-existing blocker is recorded precisely.
- Task-owned commits are pushed to `origin/int_main`.

## Current Status

completed

## Baseline Preservation

- `40b7f7b9` preserved the pre-existing dirty workspace before task-owned implementation.

## Experience Gate

### Frontend Deferred Auxiliary Error Ownership

- Trigger: The primary list has loaded, but a row-level filler permission-rule request fails.
- Preflight check: Keep the failure on the affected row and retain the real error text.
- Blocker: Removing the extra text must not clear the row error, convert it to `未配置`, or pollute the global list error.
- Verification: A focused contract must prove `加载失败` remains and tooltip/title still exposes `permissionRuleErrorMessage`.
- Forbidden action: Do not swallow the API error, return default success, or hide the failure entirely.
- Evidence: `docs/frontend-development.md#前端延迟辅助加载错误归属门禁`.

### Frontend Static Contract Isolation

- Trigger: A small frontend rendering behavior needs RED/GREEN proof while broader contracts may contain unrelated failures.
- Preflight check: Use a task-focused static contract for the exact filler error-state rendering.
- Blocker: If the focused contract cannot fail for the current extra entry and pass after its removal, do not claim completion.
- Verification: Record the focused RED/GREEN command and adjacent regressions separately.
- Forbidden action: Do not modify unrelated broad assertions to manufacture a pass.
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`.

## Experience Consolidation

- Reused the existing `前端延迟辅助加载错误归属门禁` and `前端静态契约隔离门禁`.
- No new durable lesson or long-term experience document is needed for this narrow visual deduplication.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否，保留行级失败状态和真实错误信息。
- `是否从根因和长期维护角度解决`：是，直接收敛错误态单元格的重复视觉条目。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- IntRuoyiFronted/.playwright-cli/console-2026-07-27T11-48-46-363Z.log
- IntRuoyiFronted/.playwright-cli/element-2026-07-27T11-50-58-360Z.png
- IntRuoyiFronted/.playwright-cli/page-2026-07-27T11-48-47-793Z.yml
- IntRuoyiFronted/.playwright-cli/page-2026-07-27T11-49-22-975Z.yml
- IntRuoyiFronted/.playwright-cli/page-2026-07-27T11-50-36-465Z.yml
