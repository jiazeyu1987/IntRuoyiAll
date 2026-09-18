# EDHR-STATIC-022 Inventory Detail Dedup

## Task Goal

Fix EDHR-STATIC-022 only: the final release inventory consistency check must not treat legal multi-material, multi-batch, or multi-transfer inventory trace details as duplicate merely because they share the same source type. Real duplicate writes for the same formal source identity must still be blocked.

## Milestones

- [x] Read project rules and relevant eDHR / inventory traceability / pick-list / transfer / final release validation docs.
- [x] Record BDD scenarios before production code changes.
- [x] Add focused regression coverage for legal distinct inventory details and true duplicate source details.
- [x] Implement the minimal final release inventory dedup fix.
- [x] Run targeted non-E2E/static verification only.
- [x] Mark ready_for_closeout, run cleanup preview, and record closeout blocker caused by user-forbidden commit/push / detached worktree branch.

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceImplTest#evaluateInventoryConsistency_allowsDistinctTransferDetails test` must fail before the fix for the expected duplicate-source reason.
- `mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceImplTest#evaluateInventoryConsistency_allowsDistinctTransferDetails,MesOrderReleaseCompletenessServiceImplTest#evaluateInventoryConsistency_blocksDuplicateTransferDetailIdentity test` must pass after the fix.
- Static review confirms duplicate identity uses formal source keys including source type, source document/line/detail identity, material identity, batch, and idempotency/fact key instead of only `sourceType`.
- No Playwright/E2E, database writes, service start/stop/restart, remote server operation, git commit, or git push.

## Current Status

blocked

Implementation and targeted verification are complete, but final closeout cannot be marked completed. The user explicitly forbids git commit/push, project rules require commit/push before completion, and cleanup preview also reports the linked worktree branch cannot be resolved because the current checkout is detached HEAD.

## Design Constraints Check

- Scope is limited to EDHR-STATIC-022.
- No fallback, silent downgrade, mock success, or default PASS values.
- Inventory trace details are not deduplicated by source type alone.
- Legal distinct details remain preserved for release readiness.
- True duplicate persisted source identity is rejected fail-fast.
- Source identity now uses source type, direction, source object type/id, transfer document/line/detail IDs, material stock, material item, and batch.
- Existing unrelated dirty worktree changes must not be reverted or included.
- Shared defect summary files under `docs/bugs/` must not be edited; final response will propose update text instead.
- Cleanup apply was not run because closeout is blocked and this turn forbids commit/push.

## Cleanup Candidates
