# Task: 粗洗工序合并标题区视觉修正

## Goal

继续收紧 `粗洗工序生产记录` 的报表形态约束，让生成结果更接近用户提供的原 DOC 截图。

本轮聚焦一个明确视觉差异：粗洗模板的标题与 `关键/特殊工序` 勾选说明合并为同一个表头区后，生成器会因为该单元格包含换行和较长文本，将它误判为叙述型内容并左对齐。正确形态应保持真实报表表头的水平居中、垂直居中。

## Scope

- 仅修改 `batchrecordreport` 生成 JSON 样式时的形态规则。
- 仅新增对应单元测试，锁定“显式居中的多行工序标题不得被长文本左对齐规则覆盖”。
- 不修改识别入口、接口契约、业务数据源、Route B 模板选择逻辑或非粗洗模板行为。
- 不引入 fallback、兼容分支或静默降级。

## Previous Task Check

- Previous task: `doc/tasks/20260517-rough-wash-visual-fidelity-phase2/task.md`
- Status before this task: completed
- Impact: phase2 已完成粗洗表格的标题/勾选行合并、灰底层级、红色 `/pcs` 强调和视觉截图验证；本轮只修正合并标题区的最终对齐约束。

## Milestones

- [x] M1: 创建本轮任务记录并确认上一轮任务已完成。
- [x] M2: 写入 RED 测试，复现多行工序标题显式居中被错误改为左对齐。
- [x] M3: 最小修改形态规则，使显式 `center/right` 优先于叙述型长文本左对齐规则。
- [x] M4: 回归验证默认左对齐单元格、识别空白格和布局校准不被破坏。
- [x] M5: 重新打包、重启后端、真实 Route B 重生粗洗报表并截图。
- [x] M6: 执行 task-closeout-cleanup 预览并完成提交准备。

## Expected Verification

- Focused RED/GREEN:
  `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldRespectExplicitCenterAlignForMultilineProcessHeader -Dsurefire.failIfNoSpecifiedTests=false test`
- Related regression:
  `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Package:
  `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- Runtime probe:
  `GET http://127.0.0.1:48081/v3/api-docs`
- Real route regeneration:
  `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- Screenshot artifact:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase3/artifacts/rough-wash-title-centered-20260517-1138.png`

## Final Verification Result

- RED: focused test failed as expected before the fix because the generated header style returned `align=left` instead of `align=center`.
- GREEN: focused test passed after the shape rule change.
- GREEN: related regression passed, total 23 tests, 0 failures.
- GREEN: `yudao-server` package passed with tests skipped for packaging only.
- GREEN: runtime probe returned HTTP 200.
- GREEN: real Route B regeneration imported 15 templates, updated 15 templates, rough wash report ID `1b1185fc32694fe1b24d2e83fdffddf5`.
- GREEN: screenshot captured and archived at the phase3 artifact path.
- Cleanup preview: completed; no cleanup apply was required because the screenshot is retained as task evidence.

## Cleanup Keep

- `doc/tasks/20260517-rough-wash-visual-fidelity-phase3/artifacts/rough-wash-title-centered-20260517-1138.png`

## Current Status

Completed. The merged rough-wash title/checklist header now preserves explicit center alignment while existing default-left and automatically centered cells continue to follow the established report shape rules.
