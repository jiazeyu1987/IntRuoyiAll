# EDHR-STATIC-018 Remark Must Not Change Material Source

## Task Goal

Fix EDHR-STATIC-018 only: order remarks must remain explanatory text and must not redirect formal material pick-list or batch source resolution to another order. Material and batch sources must be decided by formal order, work-order, document, or controlled relation identity.

## Milestones

- [x] Read required repository rules and related eDHR/material-source documentation.
- [x] Record BDD scenarios before implementation.
- [x] Add RED static contract evidence for the remark-driven source switch.
- [x] Implement the minimal source-resolution fix.
- [x] Run targeted non-E2E verification and record GREEN evidence.
- [x] Prepare closeout records without git commit, push, service restart, E2E, remote operation, or database writes.
- [x] Consolidate reusable project experience into an existing experience index.

## Expected Verification

- Static contract test proves `MesTeamLeaderActiveOrderPickListCompletionSourceService` does not parse `workOrder.remark` or `[sourceActiveOrderId=...]` when resolving formal material pick-list sources.
- Targeted non-E2E verification confirms formal source resolution still uses the current active order's work-order code / production order number and existing controlled Stage1 logic remains outside ordinary remark parsing.
- No Playwright/E2E, database writes, service start/stop/restart, remote operation, git commit, or git push.

## Design Constraints Check

- No fallback, graceful degradation, compatibility shim, default success, or swallowed exception.
- Ordinary production orders must always resolve material and batch source from their own formal work-order identity.
- Cross-order source use requires explicit controlled relation plus permission/audit evidence; free-text remark is not a controlled relation.
- Scope is limited to EDHR-STATIC-018. Do not edit shared defect summary tables or unrelated EDHR defects.
- Preserve existing unrelated worktree changes.

## Current Status

ready_for_closeout - Implementation and required targeted verification passed; user has authorized baseline commit plus fusion into `int_main`, and final cleanup/merge evidence is pending.
