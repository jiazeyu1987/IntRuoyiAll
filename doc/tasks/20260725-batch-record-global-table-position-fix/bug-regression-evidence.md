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

待 RED 回归与代码定位后补充。

## Regression Test

待新增/更新。

## RED

待执行。

## GREEN

待执行。

## Risk And Regression Scope

- Scope: 批记录 Word route B 解析、packed 物料矩阵展开、操作明细区域边界校验。
- Risk: packed 矩阵 token 化变更可能影响其他带括号续行的物料矩阵；需使用现有 batchrecordreport 测试回归。

## Blockers

- 当前工作区已有大量其他任务未提交改动；实现提交/推送前需要按项目 Git 规则处理或获得明确边界。
