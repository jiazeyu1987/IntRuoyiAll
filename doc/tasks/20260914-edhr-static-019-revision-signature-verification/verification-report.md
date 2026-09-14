# Verification Report - EDHR-STATIC-019

## Summary

Status: ready_for_closeout.

Implementation and required targeted verification are complete. Git commit and local `int_main` fusion are now authorized by the user's 2026-09-14 follow-up; Git push remains outside this turn's explicit authorization.

## Bug Summary

EDHR-STATIC-019 allowed the original-record revision path to accept client-submitted modifier identity, signature user, signature ID, and signature snapshot. A caller with revision permission could therefore attribute a revision to another user or attach a nonexistent/foreign signature as evidence.

## Expected Behavior

`update-original` accepts only business revision fields, change reason, signature password, and field diff. The authenticated user is injected server-side, the signature service reauthenticates the current account, and the revision stores only generated signature ID, actor, snapshot, signed time, before/after payload, reason, and diff evidence.

## Reproduction

- RED: `node tests\e2e\process-pool-event-revision-api-static.spec.js` from `IntRuoyiFronted` -> FAIL before fix because request/page still exposed client-owned modifier/signature fields and lacked `signaturePassword`.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProcessPoolEventRevisionControllerContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` from `IntRuoyiBackend` -> FAIL before fix because controller did not inject `getLoginUserId()` and request VO lacked `signaturePassword`.

## Root Cause

The `update-original` request VO mapped client-owned audit/signature fields straight into `MesProcessPoolEventRevisionUpdateReqBO`. `MesProcessPoolEventRevisionServiceImpl` then validated only signature uniqueness and user-ID equality, without proving the signature belonged to the current session actor or was bound to the event, reason, server time, and after-payload snapshot.

## Verification Commands

| Command | Result | Notes |
| --- | --- | --- |
| `node tests\e2e\process-pool-event-revision-api-static.spec.js` from `IntRuoyiFronted` | PASS | Frontend request/page contract includes `signaturePassword`; client-owned modifier/signature fields are absent. |
| `mvn -pl yudao-module-mes -am '-Dtest=MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` from `IntRuoyiBackend` | PASS | 15 tests; focused EDHR-STATIC-019 controller/service/FIFO contracts pass. |
| `mvn -pl yudao-module-mes -am '-Dtest=MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolProductionReportRevisionPolicyTest,MesProcessPoolProductionReportCorrectionServiceTest,MesProcessPoolPqcInspectionCorrectionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` from `IntRuoyiBackend` | PASS | 30 tests; adjacent production-report/PQC correction policies remain green after stale Mockito stub cleanup. |
| `git diff --check` from repository root | PASS | No whitespace errors; LF-to-CRLF normalization warnings only. |
| `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260914-edhr-static-019-revision-signature-verification/verification-report.md` | PASS | Bug regression evidence contract is valid. |
| `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-019-revision-signature-verification --mode preview` | BLOCKED | Earlier preview kept task.md/execution-log.md/verification-report.md and had an empty delete list; linked worktree current branch could not yet be resolved. |
| `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` | PASS | Pre-commit guard passed for current branch profile `int_main`, frontend 8100, backend 48100. |
| `node tests\e2e\process-pool-event-revision-api-static.spec.js` from `IntRuoyiFronted` | PASS | Re-run before implementation commit. |
| `mvn -pl yudao-module-mes -am '-Dtest=MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesProcessPoolProductionReportRevisionPolicyTest,MesProcessPoolProductionReportCorrectionServiceTest,MesProcessPoolPqcInspectionCorrectionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` from `IntRuoyiBackend` | PASS | Re-run before implementation commit; 30 tests, BUILD SUCCESS. |
| `git commit -m "fix(mes): verify original revision signatures server-side"` | PASS | Implementation/test commit `14a804053`, 11 files. |
| `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-019-revision-signature-verification --mode preview` | BLOCKED | Current branch cannot fast-forward into `int_main`; unrelated pending DCC/runtime/doc changes remain in the linked worktree and were not modified. |

## GREEN Evidence

- GREEN: Frontend static contract PASS.
- GREEN: Focused Maven EDHR-STATIC-019 regression PASS, 15 tests.
- GREEN: Extended Maven adjacent policy/correction regression PASS, 30 tests.
- GREEN: `git diff --check` PASS.
- GREEN: Bug regression evidence validator PASS.
- GREEN: Pre-commit focused frontend static contract and extended Maven regression re-run PASS.

## Changed Paths

- `doc/tasks/20260914-edhr-static-019-revision-signature-verification/task.md`
- `doc/tasks/20260914-edhr-static-019-revision-signature-verification/execution-log.md`
- `doc/tasks/20260914-edhr-static-019-revision-signature-verification/verification-report.md`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolEventRevisionUpdateReqVO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionUpdateReqBO.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProcessPoolEventRevisionControllerContractTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionFifoLockTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportRevisionPolicyTest.java`
- `IntRuoyiFronted/src/api/mes/pro/processpool/eventRevision.ts`
- `IntRuoyiFronted/src/views/mes/pro/processpool/EventRevisionPage.vue`
- `IntRuoyiFronted/tests/e2e/process-pool-event-revision-api-static.spec.js`

## Verification Scope

- Verified the static frontend request/page contract, controller contract, service signing behavior, FIFO lock preservation, production-report correction policy compatibility, PQC correction compatibility, and diff whitespace.
- Did not run Playwright/E2E, database writes, service lifecycle commands, remote operations, or Git push because they remain outside this turn's explicit authorization.

## Risks And Blockers

- Blockers: Project closeout rules require push before `completed`, but Git push has not been explicitly authorized in this turn.
- Blockers: `task-closeout-cleanup` preview is blocked in this linked worktree by non-fast-forward `int_main` ancestry and unrelated pending changes outside EDHR-STATIC-019.
- Risk: The fix is verified by static contracts and unit tests only; no runtime UI/E2E/database path was executed by scope.
