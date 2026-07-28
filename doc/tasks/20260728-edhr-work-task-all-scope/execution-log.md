# Execution Log

## User Intent

用户反馈创建 eDHR 批次执行时页面报错：`eDHR 工作任务责任范围快照无效：scopeKey=ALL`。

## BDD

- BDD: 普通整表填写人规则生成责任范围快照 -> Given 批次工序任务绑定正式批记录表单且填写人规则为 `scopeKey=ALL`、未显式保存单元格范围 When 创建初始填写工作任务 Then 系统应从正式批记录报表生成整表可填写范围快照并创建任务。
- BDD: 责任范围缺少正式来源仍失败 -> Given 任务无法解析批记录报表布局或动态表单范围 When 创建工作任务 Then 系统应 fail fast 并暴露责任范围快照无效。

## Milestone Updates

- in_progress: 已定位报错来自 `MesProEdhrWorkTaskServiceImpl#parseRequiredFillableScope` 对 `scopeKey=ALL` 的空 `fillableScopeJson` 校验。

## Evidence

- GREEN: experience-preflight -> PASS, read `docs/experience-index.md`, applied `docs/backend-development.md#edhr-详情回填门禁` and `docs/backend-development.md#edhr-批次任务配置来源门禁`.
