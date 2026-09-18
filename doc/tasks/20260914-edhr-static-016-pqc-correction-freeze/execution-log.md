# EDHR-STATIC-016 Execution Log

## Preflight

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/backend-development.md`.
- Read `docs/frontend-development.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/test-release-preflight.md`.
- Read `docs/bugs/20260913-edhr-additional-logic-audit.md`.
- Read `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md`.
- Read `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`.
- Existing worktree has unrelated dirty changes before this task; this task will only touch EDHR-STATIC-016 owned files.

## BDD

- BDD: frozen PQC correction is rejected before writes -> Given a PQC submission has entered pending nonconformance review and the work order is frozen, When a PQC leader invokes the normal correction endpoint, Then the service rejects with the formal freeze error before signature, revision, piece-detail rebuild, PQC record update, or aggregation.
- BDD: unfrozen PQC correction still works -> Given the same submitted PQC event has no pending nonconformance review and no work-order freeze, When a PQC leader signs a valid correction, Then the existing correction flow updates the formal task, piece details, record payload, and revision.

## TDD Evidence

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-016-pqc-correction-freeze-static.spec.cjs` -> FAIL, expected reason: `MesProcessPoolPqcInspectionCorrectionService` did not depend on `MesProEdhrNonconformanceReviewService` and did not call the formal work-order freeze gate before correction writes.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-016-pqc-correction-freeze-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolEventFreezeGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS; Tests run: 7, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `git diff --check` -> PASS.

## Implementation Notes

- Root cause: PQC correction validated the event/task and final release state, then proceeded to signature/revision/formal table updates without invoking the existing nonconformance review work-order freeze gate used by new production/PQC submissions.
- `MesProcessPoolPqcInspectionCorrectionService.correct` now invokes `nonconformanceReviewService.ensureWorkOrderNotFrozen(task.getWorkOrderId(), "PQC更正")` immediately after locked task validation and before signature/revision/formal table writes.
- `MesProcessPoolPqcInspectionCorrectionServiceTest.rejectsFrozenWorkOrderBeforeCorrectionWrites` verifies the frozen path rejects with the formal freeze error and makes zero calls to signature, revision, detail rebuild, record update or aggregation.
- Updated `docs/bugs/20260913-edhr-additional-logic-audit.md` to mark EDHR-STATIC-016 as `FIXED_STATIC_VERIFIED` with this task evidence.
- Consolidated the reusable correction/freeze lesson into `docs/backend-development.md` under the business freeze gate.

## Verification

- Static contract PASS: freeze gate dependency and call order before signature, revision and formal PQC writes.
- Maven targeted PASS: PQC correction behavior and existing event freeze gates.
- Bug evidence validator PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-016-pqc-correction-freeze\verification-report.md` -> PASS.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-016-pqc-correction-freeze --mode preview --worktree-closeout off --json` -> PASS; keep task.md/execution-log.md/verification-report.md; delete=[], blocked=[], warnings=[]; linked worktree main branch detected as int_main.
- Final closeout blocker: project closeout rules require commit/push before `completed`, but this turn does not explicitly authorize git commit/push; task status set to `blocked`.
- Not run by scope: Playwright/E2E, database writes, service start/stop/restart, remote operations, git commit and git push.
