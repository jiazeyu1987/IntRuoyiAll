# Bug Regression Evidence

## Bug

The DCC controlled print page rejected the current controlled file with `Current controlled file cannot be printed as a controlled copy` under the direct controlled print strategy.

## Expected

A file that exists, is `ACTIVE`, is the master `currentActiveControlledFileId`, and has category `PRINT` permission should be eligible for direct controlled print even when generated artifact IDs are empty.

## Reproduction

`mvn -pl yudao-module-dcc "-Dtest=DccControlledFilePrintServiceImplTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` failed before the fix because `validatePrintableFile` rejected a current `ACTIVE` file with empty `publishedFileId` and `stampedFileId`.

## Root Cause

Backend print creation and action projection incorrectly required `publishedFileId` or `stampedFileId` to be present. Those generated artifact IDs are not the controlled print eligibility source of truth; the source of truth is current `ACTIVE` master pointer plus category `PRINT` permission.

## Regression Test

- `DccControlledFilePrintServiceImplTest#createPrintRecord_allowsCurrentActiveControlledFileWithoutGeneratedArtifact`
- `DccControlledFilePrintServiceImplTest#createPrintRecord_rejectsHistoricalActiveFileEvenWithPrintPermission`
- `DccControlledPrintContractTest#backendPrintEligibilityDoesNotRequireGeneratedArtifactIds`
- `dcc-controlled-print-static.spec.js` controlled print record load gate assertion

## RED:

`mvn -pl yudao-module-dcc "-Dtest=DccControlledFilePrintServiceImplTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL, current `ACTIVE` direct controlled print rejected because generated artifact IDs were empty.

## GREEN:

`mvn "-Dtest=DccControlledFilePrintServiceImplTest,DccControlledPrintContractTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` from `IntRuoyiBackend/yudao-module-dcc` -> PASS, 7 tests, 0 failures/errors/skipped.

## Verification

- `pnpm exec node tests/e2e/dcc-controlled-print-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- <task-owned paths>` -> PASS with CRLF normalization warnings only.
- Cleanup preview/apply -> PASS with no delete candidates or blockers.

## Risk And Regression Scope

The fix narrows eligibility to the formal DCC print gates: current `ACTIVE` master pointer and category `PRINT` permission. Historical/non-current active versions still fail fast, and the frontend no longer loads controlled print records when action projection does not allow printing.

## Blockers

Implementation is verified, but repository completion is blocked because `int_main` is behind `origin/int_main` and the shared workspace has many unrelated dirty tracked/untracked changes. No baseline commit or push was attempted without an explicit staging/baseline strategy.
