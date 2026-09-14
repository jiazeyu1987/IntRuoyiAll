# EDHR-STATIC-018 Remark Must Not Change Material Source

## Task Goal

Fix EDHR-STATIC-018 only: order remarks must remain explanatory text and must not redirect formal material pick-list or batch source resolution to another order. Material and batch sources must be decided by formal order, work-order, document, or controlled relation identity.

## Milestones

- [x] Read required repository rules and related eDHR/material-source documentation.
- [x] Record BDD scenarios before implementation.
- [x] Add RED static contract evidence for the remark-driven source switch.
- [x] Implement the minimal source-resolution fix.
- [x] Run targeted non-E2E verification and record GREEN evidence.
- [x] Prepare closeout records; after explicit user authorization, commit and fuse into `int_main`.
- [x] Consolidate reusable project experience into an existing experience index.

## Expected Verification

- Static contract test proves `MesTeamLeaderActiveOrderPickListCompletionSourceService` does not parse `workOrder.remark` or `[sourceActiveOrderId=...]` when resolving formal material pick-list sources.
- Targeted non-E2E verification confirms formal source resolution still uses the current active order's work-order code / production order number and existing controlled Stage1 logic remains outside ordinary remark parsing.
- No Playwright/E2E, database writes, service start/stop/restart, or remote operation. Git commit/push is authorized only for the `int_main` baseline, EDHR-STATIC-018 implementation, and final closeout records.

## Design Constraints Check

- No fallback, graceful degradation, compatibility shim, default success, or swallowed exception.
- Ordinary production orders must always resolve material and batch source from their own formal work-order identity.
- Cross-order source use requires explicit controlled relation plus permission/audit evidence; free-text remark is not a controlled relation.
- Scope is limited to EDHR-STATIC-018. Do not edit shared defect summary tables or unrelated EDHR defects.
- Preserve existing unrelated worktree changes.

## Current Status

completed - EDHR-STATIC-018 implementation was committed, fused into `int_main`, re-verified on `int_main`, and cleanup preview/apply completed with no deletes or blockers.
