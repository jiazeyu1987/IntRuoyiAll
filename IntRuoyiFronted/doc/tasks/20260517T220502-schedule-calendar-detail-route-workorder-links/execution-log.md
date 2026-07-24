# Execution Log

BDD: 日历右侧详情只显示工艺流程与工单编号 -> Given 用户打开生产排程日历并选中存在排产任务的日期 When 右侧单日详情渲染 Then 页面不再显示车间与产线层级，而是按工艺流程展示对应任务卡片。
BDD: 工单编号支持从日历详情直接跳转 -> Given 右侧详情存在已关联工单的任务 When 用户点击工单编号 Then 页面跳转到对应工单列表并自动打开该工单详情。
BDD: 工艺流程支持从日历详情直接跳转 -> Given 右侧详情存在已关联工艺流程的任务 When 用户点击工艺流程 Then 页面跳转到对应工艺流程列表并自动打开该路线详情。

- 2026-05-17 22:05 Asia/Shanghai: 已检查前端最近任务 `doc/tasks/20260517-workorder-list-hide-type-unit-add-finish-time/task.md`，状态为 completed for code delivery，不阻塞本任务。
- 2026-05-17 22:05 Asia/Shanghai: 已定位前端页面 `src/views/mes/pro/task/calendar/index.vue`，当前右侧详情仍为车间 -> 产线 -> 任务卡片结构。
- 2026-05-17 22:05 Asia/Shanghai: 已确认当前 day-detail 接口类型缺少 `routeId/routeName`，无法直接满足工艺流程点击跳转，需要同步补后端字段。
- 2026-05-18 00:31 Asia/Shanghai: 真实 Playwright 已验证工单点击跳转可用；当前 live 数据里 `route_id=900020` 对应路线被逻辑删除，因此右侧工艺流程标题暂显示为 `工艺流程#900020`，不是实际路线名称。
