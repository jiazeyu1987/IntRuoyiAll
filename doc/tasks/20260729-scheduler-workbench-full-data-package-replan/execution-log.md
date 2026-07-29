# Execution Log

## 2026-07-29

- USER: 查看当前手动重排需要哪些数据，并要求这些数据可以通过截图中的导入导出按钮承载；用户随后明确允许将全部数据包扩展到手动重排业务数据。
- READONLY: 已读取前端、后端、PowerShell、任务收尾规则和相关技能。
- READONLY: 已定位截图按钮在 `IntRuoyiFronted/src/views/mes/pro/scheduler-workbench/index.vue`，接口在 `/mes/pro/scheduler-workbench/*-config/*`。
- READONLY: 已确认手动重排直接请求字段为 `scheduleOrderIds/startTime/runtimeCapacityBasis/preserveManualLockedTasks/reason`，后端计算会读取排产工单、生产工单、工序快照、路线、工位产线、日历产能、任务保护、报工、用料、物料和库存。
- BDD: 全部数据包承载手动重排数据 -> Given 源环境存在可手动重排的排产工单、生产工单、路线配置、产能日历、任务保护、用料和库存 / When 用户点击排产员工作台“导出全部数据包”并在目标环境“导入全部数据包” / Then 数据包必须包含并回放手动重排所需业务数据，缺少必要字段或引用时 fail-fast。
- BDD: 路线配置包保持边界 -> Given 用户只点击“导出排产工艺路线” / When 导入路线配置包 / Then 只导入路线排产用途、排产配置和资源引用，不导入生产工单、排产工单、任务、库存或报工业务数据。
