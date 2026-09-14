# Verification Report

## Status

blocked - implementation, required non-E2E verification, local task commit, and `int_main` integration are complete; final `completed` closeout is blocked until remote push is explicitly authorized and unrelated dirty artifacts in the `int_main` worktree are resolved.

## Bug

EDHR-STATIC-024: dynamic PROCESS_INSPECTION backfill returned a valid FormCenter instance, but PQC release dossier readiness only accepted traditional batch-record execution IDs, so dynamic-only inspection evidence was misreported as missing.

## Expected

Dynamic process inspection evidence must be accepted when the writer returns an EFFECTIVE FormCenter instance plus field audit snapshot from the same formal PQC task/signature/project/result chain. Traditional execution IDs and dynamic FormCenter instance IDs must remain separate evidence types.

## Reproduction

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at MES testCompile because `MesPqcReleaseDossierWriteResult` lacked `getProcessInspectionFormCenterInstanceIds()`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL after initial typed-field fix because dynamic FormCenter instance without field audit evidence was accepted.

## Root Cause

`MesPqcReleaseDossierPortImpl.requireFormalWrite` required `inspectionWrite.getBatchRecordExecutionIds()` to be non-empty, even though the process-inspection writer already returns dynamic `formCenterInstanceIds`. The release decision result and controller response also had no separate surface for dynamic process-inspection FormCenter evidence.

## Verification

## Commands

- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseControllerJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `rg -n "processInspectionFormCenterInstanceIds|setProcessInspectionFormCenterInstanceIds|empty\\(inspectionWrite\\.getBatchRecordExecutionIds\\(\\)\\)|empty\\(inspectionWrite\\.getFormCenterInstanceIds\\(\\)|getFieldAuditIds\\(\\)|getFieldAuditHeadHashes\\(\\)|formal process-inspection mapping" ...`
- `rg -n "PQC_CONFIRMED_AGGREGATE_REQUIRED|TASK_STATUS_CONFIRMED|PQC_SIGNATURE_REQUIRED|validateSignatureEvidence|PQC_DCC_PROJECT_IDENTITY_REQUIRED|validateDccProjectIdentity|PQC_QA_REGULATION_REQUIRED|EFFECTIVE" IntRuoyiBackend\\yudao-module-mes\\src\\main\\java\\cn\\iocoder\\yudao\\module\\mes\\service\\pro\\processpool\\team\\MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java`
- `git diff --check -- <task-owned tracked source/test files>`
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\\preflight\\branch-runtime-port-guard.ps1`
- `python C:\\Users\\BJB110\\.codex\\skills\\bug-regression-fix-loop\\scripts\\validate_bug_regression.py --evidence doc\\tasks\\20260914-edhr-static-024-dynamic-inspection-release-evidence\\verification-report.md`
- `python C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --task-id 20260914-edhr-static-024-dynamic-inspection-release-evidence --mode preview`
- `git commit -m "fix: accept dynamic process inspection evidence"`
- `git -C E:\\IntRuoyi cherry-pick a170e0033`
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseControllerJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` from `E:\\IntRuoyi\\IntRuoyiBackend`
- `python C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --workspace E:\\IntRuoyi --task-id 20260914-edhr-static-024-dynamic-inspection-release-evidence --mode preview`

## Results

- GREEN: PASS, 25 tests run, 0 failures, 0 errors, 0 skipped.
- Static contract: PASS, release dossier and response mappings expose `processInspectionFormCenterInstanceIds`; dossier write accepts process evidence only when traditional execution IDs or dynamic FormCenter instance IDs exist, and it requires field audit IDs/head hashes.
- Static contract: PASS, process-inspection writer still requires CONFIRMED task/aggregate, PQC signatures, DCC project/QA provenance, and EFFECTIVE dynamic writer output.
- Whitespace check: PASS for task-owned tracked source/test files.
- Branch runtime guard: PASS for `codex/20260914-edhr-static-024-dynamic-inspection-release-evidence/int_main`, slot 28, frontend 8162, backend 48162.
- Post-integration branch runtime guard: PASS for `int_main/int_main`, frontend 8081, backend 48081.
- Post-integration `int_main` GREEN: PASS, 25 tests run, 0 failures, 0 errors, 0 skipped.
- Bug regression evidence validator: PASS.
- Cleanup preview: BLOCKED before branch registration because the linked worktree was detached; cleanup apply was not run.
- Cleanup preview in `E:\IntRuoyi`: PASS, keep only task core records and delete/blocked/warnings are empty.
- Local task branch commit: `a170e0033 fix: accept dynamic process inspection evidence`.
- `int_main` integration commit: `5f996bd01 fix: accept dynamic process inspection evidence`.
- No E2E, database writes, service lifecycle operations, remote operations, or push were performed.

## Blockers

- Remote push was not explicitly authorized in the current turn; `docs/task-closeout-rules.md` therefore prevents marking the task `completed`.
- Cleanup apply was not run because the final task status remains `blocked`.
- The source worktree contains many unrelated pre-existing dirty paths outside EDHR-STATIC-024 scope; they were not reverted or included in this task.
- The `int_main` worktree contains an unrelated dirty resource artifact rename/delete outside this task; it was not staged or modified.
- Runtime E2E/database proof is intentionally out of scope per user instruction; coverage is static/unit contract only.

## Suggested Shared Defect Table Update

- `EDHR-STATIC-024 | FIXED_STATIC_VERIFIED | 动态过程检验 FormCenter instance 已作为独立类型化证据接入 PQC 放行汇总；传统 execution 与动态 instance 不混用；字段审计缺失会阻断；定向 Maven 25 tests PASS；已本地融合进 int_main；未执行 E2E/DB/push。`
