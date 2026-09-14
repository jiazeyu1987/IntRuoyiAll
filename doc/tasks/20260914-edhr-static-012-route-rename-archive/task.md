# EDHR-STATIC-012 Route Rename Archive Fix

## Task Goal

Fix only EDHR-STATIC-012: after an active order freezes route V1, if route master data is renamed or republished to V2 before eDHR batch creation, batch creation and archive manifest generation must use one consistent historical route identity/name/version from the frozen order snapshot.

## Milestones

- [x] Read required project rules and EDHR route/archive documents.
- [x] Record BDD acceptance contract before code changes.
- [x] Reproduce the static contract failure with a targeted non-E2E test.
- [x] Implement the minimal route identity fix for batch creation and archive consistency.
- [x] Run targeted static/non-E2E verification.
- [x] Record verification evidence, changed paths, risks, and closeout blocker.

## Expected Verification

- Static contract test proving batch creation does not read current route code/name after an active order froze a historical route snapshot.
- Targeted Maven test for the affected MES service logic.
- No Playwright/E2E, database writes, service start/stop/restart, remote operation, or git commit/push.

## Current Status

blocked - Implementation commit `8e94cdb83` exists on `codex/20260914-edhr-static-012-route-rename-archive`, but merge into `int_main` is blocked because the task branch cannot be fast-forward merged into the current `int_main` head and the `E:\IntRuoyi` main worktree has unrelated dirty changes.

## Design Constraints Check

- Scope is limited to EDHR-STATIC-012; other EDHR defects remain untouched.
- No fallback, graceful degradation, silent downgrade, mock success, or current-route lookup to repair missing historical identity.
- Batch creation, batch persisted fields, and archive manifest must agree on frozen route ID, code, name, and version.
- Historical route identity must come from active-order/batch frozen snapshot data; current route master data is not authoritative for old orders.
- If required frozen identity is missing or inconsistent, fail fast with a deterministic blocker rather than using current route data.
- Shared defect summary docs must not be edited in this task.
- User later authorized commit and merge into `int_main`; no E2E, DB writes, service operations, remote operations, or push were requested.
