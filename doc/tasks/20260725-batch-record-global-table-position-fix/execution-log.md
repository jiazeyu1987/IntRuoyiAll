# Execution Log

用户要求：解决图1 `光固Ⅰ` 和图2 `清洁` 截图位置识别错误时，不做某个表单的特例；用适合所有表格的全局方式解决，并用这两个表单验证。

## BDD

- BDD: packed material continuation lines stay in one material cell -> Given Word 表格中的 packed 物料矩阵包含多行物料名称续行（例如括号说明）, When 批记录导入展开该 packed 矩阵, Then 续行应合并回前一个物料名称，后续物料不应整体错位。
- BDD: detail operation area stops before self-inspection block -> Given 工序生产操作明细表后紧跟 `生产自检` 等说明区块, When 解析和校验操作明细区域, Then 明细区域不得把说明区块误算为操作明细截图范围。

## Command And Evidence Log

- PRECHECK: `git status --short --branch` -> PASS，当前分支 `int_main`，工作区已有大量其他任务改动；本任务只修改批记录解析相关文件和 `doc/tasks/20260725-batch-record-global-table-position-fix/`。
- PRECHECK: `docs/task-closeout-rules.md`, `docs/backend-development.md`, `docs/powershell-encoding.md`, `bug-regression-fix-loop` skill and `bug-contract.md` read -> PASS。
- PRECHECK: `docs/experience-index.md` read -> PASS；适用门禁已摘入 `task.md`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`延长管（尼龙编织管）` 被拆为 `延长管` + 续行，且 `生产自检` 说明行 expected `LONG_DESCRIPTION` but was `FIELD`。
- IMPLEMENTATION: 使用 `MesProBatchRecordPackedMaterialMatrixTextSupport` 统一 packed 物料矩阵非空行解析和括号续行合并；在共享行类型规则中按“说明区短标题 + 长说明文本 + 少量值型单元格”识别说明行，不按表单名、工序名或压力泵模板名分支。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests, 0 failures。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines+calibrate_actualPressurePumpLightCureOne_shouldKeepParentheticalMaterialWithPreviousItem+calibrate_actualPressurePumpCleanDetailBand_shouldStopBeforeSelfInspectionSection,MesProBatchRecordSharedRowTypeRulesTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，13 tests, 0 failures；真实源 DOC `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 覆盖 `光固Ⅰ工序生产记录` 与 `清洁工序生产记录`。
- REGRESSION BLOCKER: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordJingxiTableStructureVerificationTest#routeBPackedMaterialMatrix_shouldRenderAsGridInsteadOfCollapsedWideCell+routeBPackedMaterialMatrixSideHeader_shouldStartAtMaterialMatrixRow" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before exercising parsing logic，因为该测试硬编码旧本地 fixture `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`，当前机器不存在；本任务指定源 DOC 存在并已通过目标验证。

## Current Status

ready_for_closeout
