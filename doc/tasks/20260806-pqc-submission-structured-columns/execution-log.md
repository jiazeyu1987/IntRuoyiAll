# Execution Log

## User Intent

- 用户要求：PQC 管理下的列表也要像提交页面一样展示实际提交参数，不能再统一塞到“提交内容”列；参数超出上下限允许提交，但异常数值要标红；损耗数量必须等于各损耗原因数量之和。

## BDD Scenarios

- BDD: 提交列表删除红框列 -> Given 组长打开报工管理或 PQC 管理提交列表 / When 列表渲染提交记录 / Then 主表不再显示“生产工单”“PQC”“提交内容”三列，操作和复核列继续保留。
- BDD: 报工/PQC 主列表展示结构化参数 -> Given 员工提交完成数量、损耗数量、损耗原因、设备和参数 / When 组长查看主列表 / Then 列表以完成/检验数量、损耗数量、损耗明细、设备、参数明细等结构化列展示，不能只展示汇总文本。
- BDD: PQC 超限值红色提示且不阻止提交 -> Given PQC 样本值来自冻结项目明细且超出标准上下限 / When 组长查看 PQC 管理列表 / Then 超限样本值在参数明细列标红显示，且该展示逻辑不改变提交接口或提交校验。

## Milestone Updates

- in_progress: 已创建任务目录，读取前端功能交付、前端开发、PowerShell/编码、任务收尾规则和 MES PQC 项目级检验快照门禁。

## TDD Evidence

- RED: `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> FAIL, expected because the old submission main table still renders the red-box `label="生产工单"` column and unified `提交内容` column.
- GREEN: pending
- REGRESSION: pending

## Blockers

- pending
