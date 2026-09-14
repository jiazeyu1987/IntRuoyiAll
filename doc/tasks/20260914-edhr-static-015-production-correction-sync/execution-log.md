# EDHR-STATIC-015 Execution Log

## Preflight

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/bugs/20260913-edhr-additional-logic-audit.md`.
- Read `docs/bugs/20260912-edhr-90-step-static-audit.md`.
- Read `docs/product/edhr-urs-balloon-pressure-pump.md`.
- Read `docs/product/edhr-urs-template.md`.
- Read production/eDHR related change notes and targeted `docs/backend-development.md` sections for formal source, loss, completion, PQC aggregation and no-fallback rules.
- Read `docs/test-release-preflight.md` before any verification command.
- Existing worktree had unrelated dirty changes before this task; this task will only touch EDHR-STATIC-015 owned files.

## BDD

- BDD: corrected zero loss propagates to formal sources -> Given a formally submitted production record has loss quantity 0 and a production leader signs a correction to loss quantity 2 with complete loss reasons, When downstream completion/backfill reads the production loss source, Then event payload, formal feedback, material fact, completion loss condition and final loss record all use quantity 2 while original 0 remains auditable.
- BDD: corrected nonzero loss replaces stale formal quantity -> Given a formally submitted production record has loss quantity 5 and complete formal review evidence, When a production leader signs a correction to loss quantity 2, Then subsequent loss backfill and writer validation use 2 rather than stale 5.
- BDD: unsigned or incomplete correction has no formal effect -> Given a production correction has no valid signature or lacks required loss details, When the correction endpoint is invoked, Then formal feedback and material facts remain unchanged and downstream evidence still reads the last signed complete formal source.

## TDD Evidence

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-015-production-correction-sync-static.spec.cjs` -> FAIL, expected reason: `MesProcessPoolProductionReportCorrectionService` lacks the formal `MES_PRO_FEEDBACK` synchronization contract and does not lock/update linked formal feedback/material facts.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-015-production-correction-sync-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolProductionReportCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 14, Failures: 0, Errors: 0, Skipped: 0.
- REGRESSION: first Maven rerun failed because the service constructed `LambdaUpdateWrapper` directly in a pure Mockito unit test without MyBatis TableInfo cache; fix moved nullable wrapper updates behind Mapper business methods and reran the same command PASS.

## Implementation Notes

- Root cause: production correction updated signed event payload/revision summary but left `MES_PRO_FEEDBACK` and `mes_pro_feedback_material` formal facts at their stale quantities.
- `MesProcessPoolProductionReportCorrectionService.correct` now synchronizes the locked formal `MES_PRO_FEEDBACK` row after the signed revision is accepted and before refreshing management summary.
- `MesProFeedbackMapper.updateCorrectedProductionReport` explicitly sets corrected feedback total, qualified quantity, loss quantity, scrap buckets and nullable loss reason snapshots.
- `MesProFeedbackMaterialMapper.updateCorrectedMaterialFact` locks and updates formal material rows by feedback id/material id, including corrected loss/device JSON snapshots; missing formal material rows or incomplete loss details fail fast.
- Loss source reading/writer contracts now carry and validate `hasActualLoss`, `zeroLossConfirmed` and `lossDecision` against the corrected formal feedback loss quantity, preventing stale NO_LOSS facts after correction.
- Consolidated reusable MyBatis/Mockito lesson into `docs/backend-development.md` under the MyBatis Plus null update gate.

## Verification

- Static contract PASS: correction service owns formal feedback/material mappers, requires `MES_PRO_FEEDBACK`, locks the feedback row, updates formal quantities, and persists material JSON snapshots.
- Maven targeted PASS: production correction service and active-order loss source reader tests pass with the corrected formal synchronization path.
- Bug evidence validator PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-015-production-correction-sync\verification-report.md` -> PASS.
- Not run by scope: Playwright/E2E, database writes, service start/stop/restart, remote operations, git commit and git push.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-015-production-correction-sync --mode preview --worktree-closeout off --json` -> PASS; keep task.md/execution-log.md/verification-report.md; delete=[], blocked=[], warnings=[]; linked worktree main branch detected as int_main.
- Final closeout blocker: project closeout rules require commit/push before `completed`, but this task scope explicitly prohibits git commit/push; task status set to `blocked`.
