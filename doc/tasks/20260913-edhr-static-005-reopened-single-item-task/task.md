# EDHR-STATIC-005 Reopened: Single-Item PQC Task Backfill

## Task Goal

Fix reopened EDHR-STATIC-005 so PQC production-release process-inspection backfill validates each PQC task against its own frozen QA regulation version and its own task item/details, instead of requiring one item-scoped task to cover every item in the whole QA version.

## Milestones

- [x] Read project rules, closeout rules, backend rules, bug evidence, and independent audit evidence.
- [x] Record BDD scenarios before production-code changes.
- [x] Add RED regression/static contract that reproduces multi-item single-task mismatch.
- [x] Implement the minimal reader/writer fix for dedicated `MES_QA` and common `MES_QA_COMMON` tasks.
- [x] Run targeted non-E2E verification, `git diff --check`, and static scope review.
- [x] Complete task closeout records without Git commit/push per current task restriction.
- [x] Complete post-closeout Git commit and local `int_main` fusion after explicit user request.

## Expected Verification

- Targeted Java regression for process-inspection reader/writer logic.
- Targeted JS static contract for item-scoped PQC task validation.
- `git diff --check`.
- Static changed-scope review.
- Post-fusion reader/writer full-class Maven regression, both EDHR static JS contracts, `git diff --check`, and branch runtime port guard.
- No E2E, no Playwright, no service start, no database write, and no remote push.

## Current Status

completed

Implementation, approved non-E2E verification, project experience consolidation, cleanup, local task-branch commit, and local `int_main` fusion are complete. Remote push, service start, database write, and E2E remained out of scope for the current user request.

## Post-Completion Git Integration

- User request on 2026-09-13: "先提交,然后融合int_main".
- Registered `D:\IntRuoyiWorktree\20260913-edhr-static-005-reopened-single-item-task` as `int_main` slot 49 for branch-runtime guard compliance.
- Task branch commit: `7a05d8bf3 fix: scope PQC process inspection backfill by task`.
- Local `int_main` task merge commit: `37053c4d2 Merge branch 'codex/20260913-edhr-static-005-reopened-single-item-task' into int_main`.
- Local `int_main` remote-sync merge commit: `e3288631c Merge remote-tracking branch 'origin/int_main' into int_main`.

## Design Constraints Check

- No fallback, compatibility shim, exception swallowing, mock success, or default-success path.
- Each PQC task validates against its own frozen `regulationVersionId`.
- Dedicated `MES_QA` and common `MES_QA_COMMON` sources must both remain supported.
- Item-scoped tasks validate only their own `qaProcessId + qaItemCode + inspectionType` details.
- Multi-item records are backfilled through multiple single-item tasks, preserving each task version and provenance.
- EDHR-STATIC-008 readiness semantics are out of scope except for avoiding wider changes.
