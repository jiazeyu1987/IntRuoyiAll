# 损耗报告单填写格识别修复

## 任务目标
修复 Route E 损耗报告单识别后顶部填写格丢失的问题，确保图片回识别不会吞掉源 Word 的结构化空白填写单元格。

## 里程碑
- M1：复现 Route E 顶部填写格丢失并补 RED 回归测试。
- M2：实现结构化字段行的空白格恢复。
- M3：执行 Route E 与 JsonBuilder 回归测试并完成候选评审。

## 预期验证
- `MesProBatchRecordRouteERecognizerTest` 覆盖损耗单元数据行丢失空白格的 RED/GREEN。
- `MesProBatchRecordReportJsonBuilderTest#build_shouldNotCreateFillFormControlsForTrailingPaddingColumns` 继续通过，证明恢复后的空白格仍由现有 JsonBuilder 生成 fillForm。

## 经验门禁
- Route E 修复只能恢复源 Word 中真实存在的结构化空白格，不得凭图片猜测额外字段。
- 不引入 fallback、静默降级或按文件名写特例。
- 所有生产代码改动必须附带 RED/GREEN 证据。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。
- 是否存在临时补丁或绕过：否。

## 当前状态
completed
