# Execution Log

## BDD Scenarios

- BDD: No-loss conditional loss form is not required -> Given a route has a `CONDITIONAL_REQUIRED` loss/scrap form whose condition is "required only when loss/scrap exists" and the completed active order has no real production/PQC loss or scrap fact, When final release DHR readiness evaluates required forms, Then the loss/scrap form is recorded as not applicable and does not block release for missing fill evidence.
- BDD: Actual loss conditional loss form remains required -> Given the same route has a `CONDITIONAL_REQUIRED` loss/scrap form and the completed active order has real loss/scrap facts from formal production/PQC sources, When final release DHR readiness evaluates required forms, Then the form remains required and missing approved/formal evidence blocks release with a clear missing-form reason.
- BDD: Unconditional required form remains required -> Given a loss/scrap or normal route form is configured as unconditional `REQUIRED`, When final release DHR readiness evaluates required forms, Then missing approved/formal evidence still blocks release even if no loss/scrap fact exists.

## Evidence Log

- Read `AGENTS.md`, `docs/task-closeout-rules.md`, `docs/bugs/20260913-edhr-additional-logic-audit.md`, `docs/backend-development.md`, `docs/product/production-team-leader-daily-operations.md`, `docs/product/production-role-system-operations.md`, `docs/product/frontline-process-material-batch-record-mvp-prd.md`, `docs/acceptance/production-execution-main-loop/scope-contract.md`, and `docs/acceptance/production-execution-main-loop/traceability-matrix.md`.
- Read `bug-regression-fix-loop` skill and evidence contract before implementation.
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss+keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss+keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `MesProductionReleaseBusinessReadinessService` constructor did not accept `MesProEdhrBatchExecutionOriginMapper`; the new regression tests could not compile, proving DHR readiness did not read formal loss-origin facts.
- Root cause: `ordinaryProcessFillEvidenceComplete` treated `CONDITIONAL_REQUIRED` route forms as normal required forms and only excluded `SKIPPABLE_CONTROLLED`; it ignored `requiredConditionJson` and `MesProEdhrBatchExecutionOriginDO.hasActualLoss`.
- Fix: `MesProductionReleaseBusinessReadinessService` now reads batch origins only when conditional route forms exist, evaluates `CONDITIONAL_REQUIRED` `LOSS_REPORT` tasks with `{"type":"HAS_ACTUAL_LOSS"}`, skips only explicit no-loss formal origins, and keeps missing/unknown conditions or actual loss as blocking.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss+keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss+keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures, 0 errors.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests, 0 failures, 0 errors.
- GREEN: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseBusinessReadinessService.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseBusinessReadinessServiceTest.java` -> PASS, whitespace check clean; Git reported only CRLF normalization warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-026-conditional-loss-form-required\bug-regression-evidence.md` -> PASS.
- Experience consolidation: reused existing `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`, added this task evidence, and added `EDHR-STATIC-026`/`HAS_ACTUAL_LOSS` keywords to `docs/experience-index.md`.
- GREEN: `rg -n "EDHR-STATIC-026|HAS_ACTUAL_LOSS|损耗表无损耗不阻断|条件适用性检查" docs\experience-index.md docs\backend-development.md` -> PASS.
- GREEN: `git diff --check -- docs/backend-development.md docs/experience-index.md doc/tasks/20260914-edhr-static-026-conditional-loss-form-required` -> PASS, whitespace check clean; Git reported only CRLF normalization warnings.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-026-conditional-loss-form-required --mode preview` -> BLOCKED, current linked worktree branch could not be resolved because repository is detached HEAD; no files were deleted.
- User authorization: 用户于 2026-09-14 明确要求“提交并融合进int_main”，解除本任务原 `git commit/push` 禁止项，仅用于任务提交与 `int_main` 融合收尾。
- Branch setup: `git switch -c codex/20260914-edhr-static-026-conditional-loss-form-required` -> PASS，detached HEAD 已转为任务分支。
- Runtime slot: `powershell -ExecutionPolicy Bypass -File scripts/runtime/reserve-worktree-slot.ps1 -Name IntRuoyi -Path 'C:\Users\BJB110\.codex\worktrees\6330\IntRuoyi' -Branch 'codex/20260914-edhr-static-026-conditional-loss-form-required' -Profile int_main -WorktreeRoot 'C:\Users\BJB110\.codex\worktrees\6330' -AsJson` -> PASS，slot 21，frontend 8155，backend 48155。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss+keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss+keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures, 0 errors.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts/preflight/branch-runtime-port-guard.ps1` -> PASS, branch `codex/20260914-edhr-static-026-conditional-loss-form-required` / profile `int_main`, frontend 8155, backend 48155.
- Commit: `git commit -m "fix: skip no-loss conditional DHR loss form"` -> PASS, task branch implementation commit `7838ff738`.
- Fusion: `git cherry-pick -x 7838ff738` on `int_main` -> PASS, `int_main` commit `ded9c4a51`.
- GREEN: on `int_main`, `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseBusinessReadinessServiceTest#skipsNoLossConditionalLossFormWhenFormalCompletionOriginHasNoActualLoss+keepsConditionalLossFormRequiredWhenFormalCompletionOriginHasActualLoss+keepsUnconditionalLossFormRequiredEvenWhenFormalCompletionOriginHasNoActualLoss" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures, 0 errors.
- Cleanup preview/apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-026-conditional-loss-form-required --mode preview` -> PASS, keep task records, delete none; `--mode apply` -> PASS, deleted none.

## Current Work

- RED: completed.
- GREEN: completed.
- Suggested shared defect summary update: mark EDHR-STATIC-026 as targeted fixed with non-E2E regression coverage, pending integrated real-order E2E after related main-flow blockers are addressed.
- Blockers: none for EDHR-STATIC-026 implementation and `int_main` fusion. `int_main` still contains unrelated concurrent task changes outside this task scope.
