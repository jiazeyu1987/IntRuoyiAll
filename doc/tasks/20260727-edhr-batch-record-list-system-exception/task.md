# 20260727 eDHR Batch Record List System Exception

## Task Goal

Fix the `系统异常` shown on the MES 系统 / eDHR 批记录 / 批记录表单 page when loading the batch-record form list and right-side preview area, without adding fallback or hiding backend/API failures.

## Milestones

- [x] Preserve unrelated dirty worktree changes before task-owned edits.
- [x] Reproduce and isolate the failing route/API/contract.
- [x] Add a focused RED regression check for the observed failure.
- [x] Implement the smallest root-cause fix.
- [x] Run GREEN and relevant regression verification.
- [ ] Complete cleanup, experience consolidation, commit, and push.

## Expected Verification

- Focused regression test fails before the fix and passes after the fix.
- Affected frontend/backend targeted verification passes.
- No new fallback, silent downgrade, swallowed exception, mock success, or default-success behavior is introduced.
- Task-owned changes are committed separately after verification and pushed to `origin/int_main`.

## Current Status

ready_for_closeout

## Baseline Preservation

- `fc07fc8a` preserved existing dirty documentation/evidence changes before this task.
- `32df0a46` preserved a second concurrent documentation/evidence dirty set before this task.

## Experience Gate

### Frontend Static Contract Isolation

- Trigger: eDHR frontend page failure where broad `pnpm ts:check` may fail on unrelated historical issues.
- Preflight check: Run or create the closest task-focused static contract after isolating the failing route/API.
- Blocker: If the focused contract cannot prove RED/GREEN for the screenshot behavior, do not claim the fix is complete.
- Verification: Record RED/GREEN command and any unrelated broader typecheck blocker separately.
- Forbidden action: Do not edit unrelated broad contracts or hide API errors to make the page appear successful.
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`.

### eDHR Batch Record Source Boundary

- Trigger: eDHR batch-record list/config/version behavior, including route snapshots, current BATCH configs, Jimu JSON, or form bindings.
- Preflight check: Confirm whether the failure belongs to frontend rendering, batch-record report API, route/version source selection, or Jimu JSON content before editing.
- Blocker: Missing or corrupt current configuration must fail fast; do not silently fall back to stale snapshots, empty bindings, defaults, or hidden frontend success.
- Verification: Use focused backend/frontend regression coverage for the exact source boundary touched.
- Forbidden action: Do not use current config as a fallback for explicit draft snapshots; do not mask missing batch-record configuration with empty data.
- Evidence: `docs/backend-development.md#edhr-批次任务配置来源门禁`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在复现和回归测试基础上修复根因。
- `是否存在临时补丁或绕过`：否。
