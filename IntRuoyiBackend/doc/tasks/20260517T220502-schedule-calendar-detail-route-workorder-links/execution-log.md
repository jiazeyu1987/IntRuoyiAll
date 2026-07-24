# Execution Log

BDD: 单日详情任务项返回工艺流程跳转字段 -> Given 某日期存在排产任务且任务已绑定工艺路线 When 后端返回 day-detail 响应 Then 每个任务项都包含 `routeId` 和可展示的 `routeName`。
BDD: 缺料与原有工单字段保持不变 -> Given 单日详情仍需支持缺料汇总与工单跳转 When 本次补充路线字段 Then 原有 `workOrderId/workOrderCode` 与缺料汇总字段不回退、不丢失。

- 2026-05-17 22:05 Asia/Shanghai: 已检查后端最近任务 `doc/tasks/20260517-workorder-status-column-and-kingdee-confirmed/task.md`，状态为 completed for code delivery，不阻塞本任务。
- 2026-05-17 22:05 Asia/Shanghai: 已确认暂停中的 `doc/tasks/20260517-rough-wash-visual-fidelity-phase6/task.md` 已记录阻塞与影响，本任务不接手其未完成工作。
- 2026-05-17 22:05 Asia/Shanghai: 已定位 `MesProScheduleCalendarServiceImpl#getDayDetail` 当前只回填 `processName`，未返回 `routeId/routeName`，无法支持前端按工艺流程点击跳转。
- 2026-05-18 00:31 Asia/Shanghai: 真实验证通过，day-detail 已返回 `routeId` 并支持跳转；当前 live 数据里的 `route_id=900020` 对应路线在运行库中是逻辑删除状态，因此 route name 不能从现有数据直接回显。
