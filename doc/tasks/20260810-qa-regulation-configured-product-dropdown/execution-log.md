# Execution Log

## User Intent

- 用户要求 QA 规程配置产品下拉中，已经配置好的产品显示为绿色，并排布在最前。

## BDD Scenarios

- BDD: 已配置产品置顶高亮 -> Given 产品下拉同时包含已配置和未配置 QA 规程的产品 / When 用户展开 QA 规程配置产品下拉 / Then 已配置产品排在未配置产品之前且使用绿色视觉状态。
- BDD: 未配置产品保留可选 -> Given 产品下拉存在未配置 QA 规程的产品 / When 用户展开下拉 / Then 未配置产品仍显示在列表中但排在已配置产品之后，选择行为不变。

## Evidence

- 2026-08-10：已读取 frontend-feature-delivery 技能、docs/task-closeout-rules.md、docs/frontend-development.md、docs/powershell-encoding.md 和技能 references/frontend-contract.md。
- 2026-08-10：已创建任务目录并读取 docs/experience-index.md；命中 QA 规程配置状态相关经验，待读取匹配门禁。
- 2026-08-10：已读取 docs/backend-development.md 中“QA 规程配置状态必须来自产品级规程记录”门禁；本任务必须复用正式 project-statuses 状态，不得前端猜测配置状态。
