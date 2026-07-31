# Bug Regression Evidence

## Bug Summary

压力泵批记录 Word 导入后，`光固Ⅰ工序生产记录` 的 packed 物料矩阵把 `延长管` 与括号说明拆成两个物料，导致 `40atm压力表`、`旋转接头`、`光固胶` 等位置错位；`清洁工序生产记录` 的操作明细截图范围把后续 `生产自检` 说明块误包含进来。

## Expected Behavior

- packed 物料矩阵应按视觉物料行展开，括号续行属于前一个物料名称，不得创建额外物料项。
- 操作明细区域应在重复物料明细结束处停止，遇到 `生产自检`、合格标准等说明块时不得继续归入操作明细。
- 修复必须是全局规则，不得以表单名、工序名或压力泵模板名写特例。

## Reproduction

- Source DOC: `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`
- Target samples: `光固Ⅰ工序生产记录`, `清洁工序生产记录`

## Root Cause

- packed 物料矩阵解析直接对宽单元格执行 `text.lines()`，跳过 `/` 后把每个非空行都当成独立物料；当 Word 视觉物料名称包含独立括号说明行时，续行被误当成新物料，导致后续物料整体错位。
- 共享行类型规则只覆盖单格长说明或有限的多格长说明形态；`生产自检` 这类短说明区标题旁跟长合格标准/检验方法文本时，被误归类为 `FIELD`，使后续说明块可能参与操作明细区域边界和分页计算。

## Regression Test

- `MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines`：合成 packed 矩阵断言括号续行应合并为前一物料，后续物料不应错位。
- `MesProBatchRecordReportLayoutCalibratorTest#calibrate_actualPressurePumpLightCureOne_shouldKeepParentheticalMaterialWithPreviousItem`：使用真实 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 验证 `光固Ⅰ工序生产记录` 中 `延长管（尼龙编织管）` 与后续 `40atm压力表`、`旋转接头`、`光固胶` 对齐。
- `MesProBatchRecordReportLayoutCalibratorTest#calibrate_actualPressurePumpCleanDetailBand_shouldStopBeforeSelfInspectionSection`：使用同一真实 DOC 验证 `清洁工序生产记录` 的操作明细区域在 `生产自检` 前停止。
- `MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows`：断言短说明区标题 + 长检验说明行归类为 `LONG_DESCRIPTION`。

## RED:

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL, 2 failures。
- Evidence: `calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines` expected `延长管（尼龙编织管）` but was `延长管`；`classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows` expected `LONG_DESCRIPTION` but was `FIELD`。

## GREEN:

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS, 2 tests, 0 failures。
- Real DOC verification: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines+calibrate_actualPressurePumpLightCureOne_shouldKeepParentheticalMaterialWithPreviousItem+calibrate_actualPressurePumpCleanDetailBand_shouldStopBeforeSelfInspectionSection,MesProBatchRecordSharedRowTypeRulesTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 13 tests, 0 failures。

## Verification

- Target RED/GREEN and real DOC verification are recorded above.
- The global parser change was validated against both requested forms without form-name, process-name, or file-name branches.

## Risk And Regression Scope

- Scope: 批记录 Word route B 解析、packed 物料矩阵展开、操作明细区域边界校验。
- Risk: packed 矩阵 token 化变更可能影响其他带括号续行的物料矩阵；需使用现有 batchrecordreport 测试回归。
- Mitigation: 续行识别限定为括号/方括号类独立说明行，并排除 header、勾选项和 `/` 分隔符；说明行识别基于行形态，不读取表单名、工序名或文件名。

## Blockers

- 当前工作区已有其他任务未提交改动，且分支已 ahead；实现提交/推送前需要按项目 Git 规则处理或获得明确边界，避免混入非本任务改动。
- 额外结构验证 `MesProBatchRecordJingxiTableStructureVerificationTest#routeBPackedMaterialMatrix...` 被旧本地 fixture 缺失阻断：`C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` 不存在。该测试未进入解析逻辑；用户指定源 DOC 已通过本任务目标验证。
