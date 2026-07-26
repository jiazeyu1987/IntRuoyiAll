# Execution Log

## User Intent

用户指出 V14 版本中仍存在此前声称已修复的截图/表格位置识别问题，要求不能针对某个表单做特例，要用适合所有表格的全局方式解决，并用相关表单截图验证。

## Milestone Evidence

BDD: V14 物料矩阵列归属不串列 -> Given 批记录 Word 中存在密集物料/自检表格且物料名称列包含压力表勾选项, When 使用新识别前后端导入并生成 V14, Then 只有物料名称列应出现压力表勾选项，批号、生产数量、自检合格数量等业务数量列不得重复承载该文本。

BDD: 说明区边界保持独立 -> Given 表格底部存在短标题加长说明的生产自检/合格标准/检验方法行, When 解析表格行结构, Then 说明区不得被上一条操作明细或物料矩阵吞并。

## Command And Verification Log

- 初始化任务：记录 V14 回归修复目标、全局约束和预期验证。
- GREEN: experience-preflight -> PASS，`docs/experience-index.md` 命中 `docs/backend-development.md#eDHR 批记录 Word 表格解析门禁`；本任务必须使用真实 DOC + 合成回归，且不得按表单名/工序名/文件名写特例。

## Blockers

- 暂无。
