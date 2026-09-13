# Verification Report

## Bug

DCC-STATIC-018: DCC approval routes could save editable `approveMethod`, `approveRatio`, and `required` values even though actual DCC BPMN execution uses a fixed four-stage model.

## Expected

Configuration and execution must be consistent. Under the current fixed DCC four-stage BPMN strategy, unsupported approval method, ratio, or required-switch values must be rejected or made non-editable, and saved/executed evidence must not imply a rule the BPMN does not consume.

## Reproduction

- `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest#testSaveRoute_unsupportedFixedApprovalPolicy_throwsExplicitFailure test`
- `node scripts\dcc-route-fixed-approval-policy-static.test.mjs`

## Root Cause

Route save and route snapshots kept user-editable approval policy fields, but `createApprovalProcess` only sent file metadata and assignee maps to the fixed BPMN model. The BPMN model fixes matrix review to 100% completion and matrix approval to one completed approval, so editable policy values were not part of runtime execution.

## Fix

- Added `DccFixedApprovalRoutePolicy` as the shared backend authority for the fixed four-stage policy.
- Updated route save to reject unsupported `approveMethod`, `approveRatio`, and `required` values and persist only fixed policy metadata.
- Updated route runtime assignee resolution to fail fast on historical active routes whose saved policy no longer matches the fixed BPMN contract.
- Updated the frontend route form to display fixed rules instead of editable approval method, ratio, and required controls, and to submit normalized fixed policy fields.

## RED:

- Backend RED failed as expected: unsupported matrix approval `ALL/100%` was accepted before implementation.
- Frontend RED failed as expected: the form lacked fixed policy helpers and still rendered editable policy controls.

## GREEN:

- `node scripts\dcc-route-fixed-approval-policy-static.test.mjs` -> PASS, 2 tests.
- `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest" test` -> PASS before rebase, 21 tests; PASS after rebase, 24 tests.
- `git diff --check` -> PASS.
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-018-route-approval-policy\verification-report.md` -> PASS.
- UTF-8 task docs readback -> PASS.

## Authorized Closeout Verification

- User authorized Git closeout on 2026-09-13.
- Registered D worktree: `D:\IntRuoyiWorktree\20260913-dcc-static-018-route-approval-policy`.
- Runtime slot: `int_main` slot 8, frontend `8089`, backend `48089`.
- `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
- `node scripts\dcc-route-fixed-approval-policy-static.test.mjs` -> PASS, 2 tests.
- `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest" test` -> PASS, 24 tests.
- `git diff --check` -> PASS.
- Bug regression evidence validator -> PASS.

## Rebase Finding

- `origin/int_main` was ahead of the original task base, so the task branch was rebased to `origin/int_main`.
- The first post-rebase Maven rerun failed in two upstream-added future-effective-route tests because their fixtures still set matrix approval stage 3 to `ALL`; those fixtures were updated to the fixed policy value `ANY`.

## Verification

Verified the requested static/unit scope only. No E2E was run, no service was started or restarted, and no database write was performed. Git closeout was resumed only after explicit user authorization on 2026-09-13.

## Blockers

- Frontend lint was not available in this clean worktree: `pnpm exec eslint ...` failed because `eslint` was not installed/resolvable.
- Frontend `pnpm ts:check` did not run because `cross-env` was not installed/resolvable and pnpm reported missing local `node_modules`.
- The original Codex worktree at `C:\Users\BJB110\.codex\worktrees\e772\IntRuoyi` remains detached and cannot satisfy closeout commit gates directly; task-owned changes were migrated to the registered D worktree for compliant closeout.
