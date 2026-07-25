# Verification Report

## Scope

- Global fix for batch record Word table parsing, without table-name, process-name, or pressure-pump-template special cases.
- Validation samples: `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`, `光固Ⅰ工序生产记录`, `清洁工序生产记录`.

## Code Changes Verified

- `MesProBatchRecordReportLayoutCalibrator` now uses shared packed-material matrix text parsing so parenthetical continuation lines stay attached to the previous material item.
- `MesProBatchRecordSharedRowTypeRules` now treats rows with a short narrative section label plus long inspection/instruction text as `LONG_DESCRIPTION`, preventing narrative sections from being folded into detail rows.

## RED Evidence

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL, 2 failures.
- Failure evidence: packed continuation expected `延长管（尼龙编织管）` but got `延长管`; self-inspection narrative expected `LONG_DESCRIPTION` but got `FIELD`.

## GREEN Evidence

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, 2 tests, 0 failures.
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines+calibrate_actualPressurePumpLightCureOne_shouldKeepParentheticalMaterialWithPreviousItem+calibrate_actualPressurePumpCleanDetailBand_shouldStopBeforeSelfInspectionSection,MesProBatchRecordSharedRowTypeRulesTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, 13 tests, 0 failures.

## Screenshot Evidence

- Command: temporary artifact-generation JUnit executed with `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordPressurePumpVisualArtifactTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test, 0 failures; the temporary test source was deleted after generating artifacts.
- `doc/tasks/20260725-batch-record-global-table-position-fix/artifacts/verification-screenshots/light-cure-one-verification.png`: shows `延长管（尼龙编织管）` in one material cell and later `40atm压力表 / 旋转接头 / 光固胶` still aligned.
- `doc/tasks/20260725-batch-record-global-table-position-fix/artifacts/verification-screenshots/clean-detail-boundary-verification.png`: shows operation detail rows ending before the highlighted `生产自检` narrative section.

## Non-Blocking Regression Note

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest#routeBPackedMaterialMatrix_shouldRenderAsGridInsteadOfCollapsedWideCell+routeBPackedMaterialMatrixSideHeader_shouldStartAtMaterialMatrixRow" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: BLOCKED before exercising parsing logic because the test hardcodes missing local fixture `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`.

## Closeout Status

- Implementation and target verification are complete.
- Commit/push closeout is pending because the workspace already contains unrelated dirty changes and the branch is ahead of `origin/int_main`; task-owned changes should not be mixed with unrelated work.
