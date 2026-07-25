# 20260725 Batch Record Global Table Position Fix

## Task Goal

用全局表格解析规则修复批记录 Word 导入后的表格区域与物料矩阵位置识别问题，不针对单个表单名称、工序名称或压力泵模板写特例；使用“光固Ⅰ工序生产记录”和“清洁工序生产记录”作为回归验证样本。

## Milestones

- [x] 建立任务记录并确认适用规则。
- [x] 复现两个截图对应的结构偏差并补 RED 回归测试。
- [x] 实施全局解析修复，避免表单级特例和降级分支。
- [x] 运行目标 Maven 测试与相关回归验证。
- [x] 更新验证报告和收尾状态。

## Expected Verification

- RED：新增/更新测试在修复前能暴露 `延长管（尼龙编织管）` 被拆成两个物料、清洁明细区域包含 `生产自检` 的问题。
- GREEN：目标测试通过，确认 packed 物料矩阵的括号续行合并为同一物料，且清洁操作明细验证不包含后续自检块。
- REGRESSION：运行受影响的 `batchrecordreport` 定向 Maven 测试，确认现有批记录表格解析能力未退化。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是改进全局 packed 物料矩阵 token 化和明细区域边界校验。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: eDHR / 批记录 / Word 模板 / Jimu 表格 JSON / 单元格规则识别。
- Preflight check: 修改后端批记录解析前读取 `docs/backend-development.md`，执行 BDD + RED/GREEN，并用 `mvn -pl yudao-module-mes -am -Dtest=... test` 覆盖目标模块。
- Blocker: 不得通过表单名、工序名、压力泵模板名硬编码特例绕过解析问题；缺少源 DOC 或 Maven 依赖时必须 fail fast。
- Verification: 任务日志记录 RED/GREEN 命令和两个目标表单的结构断言结果。
- Forbidden action: 禁止引入 fallback、静默吞错、改测试迎合错误输出、仅靠截图人工判断完成。
- Evidence: 本任务 `doc/tasks/20260725-batch-record-global-table-position-fix/`。

## Verification Summary

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines,MesProBatchRecordSharedRowTypeRulesTest#classifyRow_returnsLongDescriptionForLabeledSelfInspectionNarrativeRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，复现 packed 括号续行被拆开和 `生产自检` 说明行被归为 `FIELD`。
- GREEN: 同一目标命令 -> PASS，2 tests, 0 failures。
- REAL DOC: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldMergePackedMaterialMatrixParentheticalContinuationLines+calibrate_actualPressurePumpLightCureOne_shouldKeepParentheticalMaterialWithPreviousItem+calibrate_actualPressurePumpCleanDetailBand_shouldStopBeforeSelfInspectionSection,MesProBatchRecordSharedRowTypeRulesTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，13 tests, 0 failures；使用 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 验证 `光固Ⅰ工序生产记录` 和 `清洁工序生产记录`。
- REGRESSION NOTE: `MesProBatchRecordJingxiTableStructureVerificationTest#routeBPackedMaterialMatrix...` 未进入解析逻辑，因测试类硬编码的旧本地 fixture `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` 不存在而失败；本次用户指定源 DOC 已通过目标回归。

## Closeout Notes

- 实现提交/推送尚未执行：当前 `int_main` 已存在 ahead 状态和其他任务未提交改动，需要按项目 Git 规则先处理既有状态，避免把非本任务改动混入。
