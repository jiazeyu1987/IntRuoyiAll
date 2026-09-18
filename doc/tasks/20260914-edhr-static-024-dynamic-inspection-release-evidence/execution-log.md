# Execution Log

## BDD

- BDD: Dynamic inspection evidence accepted -> Given PROCESS_INSPECTION 使用正式动态表单且动态 writer 返回 EFFECTIVE 实例、字段审计、正式签名、任务身份、项目身份和就绪结论, When PQC 组长批准生产放行并汇总正式资料证据, Then 放行汇总读取动态 FormCenter 实例证据并判定过程检验证据已就绪，不要求传统 batchRecordExecutionId 非空。
- BDD: Mixed inspection evidence preserved -> Given 同一放行申请同时存在传统过程检验执行记录和动态 FormCenter 实例, When 放行汇总生成回执, Then 回执分别保留 batchRecordExecutionIds 与 formCenterInstanceIds，不混用不同证据类型。
- BDD: Missing formal dynamic inspection evidence blocks -> Given 动态过程检验缺少实例 ID、任务身份、项目身份、签名或就绪结论任一正式证据, When PQC 组长批准生产放行, Then 放行汇总以明确 blocker 阻断，不返回默认成功。

## Rule Reading

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read bug-regression-fix-loop skill and bug evidence contract.
- Read task-closeout-cleanup skill and closeout rules.
- Read project-experience-consolidation skill.
- Read eDHR readiness, dynamic form, formal source, PQC production release, and release summary gates from `docs/backend-development.md`, `docs/database-rules.md`, and `docs/bugs/20260913-edhr-additional-logic-audit.md`.

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesPqcReleaseDossierWriteResult` lacked `getProcessInspectionFormCenterInstanceIds()`, proving the release dossier receipt had no typed dynamic FormCenter evidence surface.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: dynamic FormCenter instance without field audit evidence was accepted.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseControllerJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests run, 0 failures, 0 errors, 0 skipped.

## Static Contract Evidence

- `MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl` still blocks missing task/ready conclusion with `PQC_CONFIRMED_AGGREGATE_REQUIRED` and `TASK_STATUS_CONFIRMED`.
- `MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl` still blocks missing formal signatures with `PQC_SIGNATURE_REQUIRED`.
- `MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl` still blocks missing project identity with `PQC_DCC_PROJECT_IDENTITY_REQUIRED` and QA provenance/version issues with `PQC_QA_REGULATION_REQUIRED`.
- Dynamic writer output is still required to be `EFFECTIVE` and include FormCenter instance plus field audit evidence before the port receives it.

## Implementation Notes

- `MesPqcReleaseDossierPortImpl` now accepts process-inspection evidence when either traditional `batchRecordExecutionIds` or typed `formCenterInstanceIds` exists.
- `MesPqcReleaseDossierPortImpl` now requires process-inspection field audit IDs and head hashes before accepting either evidence carrier.
- `MesPqcReleaseDossierWriteResult`, `MesPqcProductionReleaseDecisionResult`, and the controller response now expose `processInspectionFormCenterInstanceIds` separately from traditional `processInspectionEvidenceIds`.
- `MesPqcProductionReleaseServiceImpl` now validates process-inspection evidence with the same typed `either traditional execution or dynamic FormCenter instance` rule and raises `PROCESS_INSPECTION_SOURCE_REQUIRED` when neither exists.
- The legacy `processInspectionEvidenceIds` list remains traditional execution IDs only; FormCenter instance IDs are not copied into it.

## Current Notes

- Original scope forbade E2E, database writes, service lifecycle operations, remote server operations, and git commit/push.
- 2026-09-14 user explicitly requested local Git commit and fusion into `int_main`; remote push was not explicitly authorized.
- E2E and database writes were not run.
- Existing unrelated dirty worktree changes are present outside EDHR-STATIC-024 scope and were not modified or reverted.
- Project experience consolidation checked existing memory targets; `docs/backend-development.md` was the matching long-term destination and now records the evidence-carrier continuity rule.
- Bug regression evidence validator passed for `verification-report.md`.
- Cleanup preview reported `blocked: Current worktree branch could not be resolved` and delete list `<none>`; cleanup apply was not run.
- Current branch `codex/20260914-edhr-static-024-dynamic-inspection-release-evidence` reserved `int_main` slot 28 (`8162/48162`) so branch runtime guard could run before commit.
- Branch runtime guard PASS for `codex/20260914-edhr-static-024-dynamic-inspection-release-evidence/int_main`.
- Re-run GREEN before commit: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseControllerJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests run, 0 failures, 0 errors, 0 skipped.
- Implementation commit on task branch: `a170e0033 fix: accept dynamic process inspection evidence`.
- Integrated into `int_main` by cherry-picking task commit: `5f996bd01 fix: accept dynamic process inspection evidence`.
- Post-integration `int_main` branch runtime guard: PASS for `int_main/int_main`, frontend 8081, backend 48081.
- Post-integration `int_main` GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseControllerJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 25 tests run, 0 failures, 0 errors, 0 skipped.
- Cleanup preview in `E:\IntRuoyi`: PASS, keep only `task.md`, `execution-log.md`, and `verification-report.md`; delete/blocked/warnings are empty.
- `int_main` worktree has unrelated dirty resource artifact rename/delete outside this task; it was not staged or modified.
- Final `completed` status remains blocked until remote push is explicitly authorized and the unrelated `int_main` dirty artifact state is resolved.

## Changed Paths

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierWriteResult.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseDecisionResult.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesPqcProductionReleaseDecisionRespVO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesProductionReleaseController.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseBatchExecutionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesProductionReleaseControllerJsonTest.java`
- `docs/backend-development.md`
- `doc/tasks/20260914-edhr-static-024-dynamic-inspection-release-evidence/task.md`
- `doc/tasks/20260914-edhr-static-024-dynamic-inspection-release-evidence/execution-log.md`
- `doc/tasks/20260914-edhr-static-024-dynamic-inspection-release-evidence/verification-report.md`
