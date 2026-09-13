# Bug Regression Evidence

- Task ID: `one-button-app-release-20260911`
- Scope: local application worktree verification unblocker only.

## Bug Summary

`C07` (`mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test`) failed during P3 continuation even after the release workflow targeted tests passed. The failures were:

- `RuntimeControlHighRiskActionContractTest.defaultOwnerMatrixShouldAllowHighRiskRollbackToDispatchSelectedCandidate`: rollback fixture still used the retired `release-manifest.json` name and lacked the current `manifest.json.packageId` contract.
- `RuntimeOpsResponsibilityServiceImplTest.configuredRequiredOwnerShouldAllowProductionGateToReachDispatch`: production promotion fixture lacked `testOperationId` and `testOperationEvidencePath`, which are now mandatory for tested-package promotion.
- `CodegenEngineVue2Test` and `CodegenEngineVue3Test`: 12 snapshot mismatches caused by stale expected resources; the production generator was not changed.

## Expected Behavior

The full infra module regression must pass without weakening release gates. Runtime-control tests must use the current app-release package contract: one authoritative `manifest.json`, `packageId`, package digest/source role evidence, and explicit tested-operation evidence before promotion.

## Reproduction

Run `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` from the application worktree. The command originally reached Surefire, then failed in runtime-control fixture validation and stale codegen snapshot assertions.

## RED

RED: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` -> FAIL, 546 tests, 12 failures, 2 errors initially.

RED: after fixing runtime-control fixtures, the same command -> FAIL, 546 tests, 12 failures, 0 errors; remaining failures were only codegen stale snapshots.

## Root Cause

Older test fixtures still described pre-app-release package metadata (`release-manifest.json`, missing `packageId`, missing tested-operation evidence). Separately, codegen expected resources had drifted from the existing generator output, blocking the full infra gate even though the release workflow code was green.

## Fix

- Updated runtime-control rollback and promotion fixtures to use `manifest.json`, `packageId`, source roots, source roles, package digest, and tested-operation evidence.
- Refreshed only codegen test expected resources using the repository's existing `-Dcodegen.regenerate=true` path; no codegen production logic was changed.

## GREEN

GREEN: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=RuntimeControlHighRiskActionContractTest,RuntimeOpsResponsibilityServiceImplTest" test` -> PASS, 14 tests.

GREEN: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=CodegenEngineVue2Test,CodegenEngineVue3Test" test` -> PASS, 12 tests.

GREEN: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` -> PASS, 546 tests, 0 failures, 0 errors, 10 skipped.

## Verification

The final full-module C07 regression passed after the fixture and snapshot alignment. `git diff --check` is run before commit so the generated snapshot normalization does not carry whitespace errors.

## Risk and Regression Scope

This change affects test fixtures/resources only. It does not perform server writes, database writes, NAS upload, Docker build, MinIO operation, or production action. The next P3 release build must still use a new release tag from a clean committed source.

## Blockers

- None for this local verification unblocker.
