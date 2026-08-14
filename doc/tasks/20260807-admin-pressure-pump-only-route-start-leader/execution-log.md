# 执行日志

## 用户意图

- 给 `admin` 只配置 `球囊扩张压力泵`、`按压式球囊扩充压力泵` 两条工艺路线的生产组长，其他工艺路线不配置。
- 结合既有正式术语契约，当前任务仅调整“工序开始生产组长”快照，不触碰批记录表单或表单槽位。

## BDD

- BDD: admin 仅负责两条目标压力泵路线 -> Given 本机 tenant `1` 的 admin 与当前 active 工艺路线存在；When 收敛工序开始生产组长配置；Then admin 的 active 路线集合严格等于 `922119` 和 `980091`。
- BDD: 其它生产组长配置保持 -> Given 非目标 active 路线可能配置 admin 与其它生产组长；When 移除 admin；Then 仅删除 admin 对应候选，保留同一快照中的其它用户或角色候选及非生产组长字段。
- BDD: 非目标数据不变 -> Given 存在其它租户、非 active version、`formBindings` 和批记录表单；When 执行本次数据收敛；Then 这些数据不发生变化。
- BDD: 配置缺少正式唯一来源时失败 -> Given 目标路线或 admin 在 tenant `1` 中不存在、不唯一或目标路线无唯一 current active version；When 执行写入；Then 事务失败且不修改任何数据。

## 命令意图与证据

- 已读取 `docs/task-closeout-rules.md`、`docs/database-rules.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md` 和 `docs/experience-index.md`。
- 已读取既有同链路任务 `doc/tasks/20260806-admin-pressure-pump-route-start-leader/`，确认正式数据源、目标 tenant、admin 用户 ID、两条路线 ID 和当前 active version 动态复验要求。
- Schema：本机 Docker MySQL `8.0.39`，数据库 `ruoyi-vue-pro`；`mes_pro_route_version.route_snapshot_json` 为 `mediumtext`，正式字段位于 `$.configSnapshots.routeStartProductionLeaders`。
- 只读现状：tenant `1`、user `1/admin/瑛泰管理员` 唯一且启用；当前 6 条 active 路线中，`622 / 980091 / 按压式球囊扩充压力泵` 已直接配置 admin，`627 / 922119 / 球囊扩张压力泵` 配置为空，其余 4 条 active 路线配置为空。
- 并发检查：当前 Playwright 会话属于 ERP、智能排产和默认浏览器会话，未发现命令行指向 `route-start-production-leaders` 或本任务范围；本任务不终止、不复用这些会话。
- 授权边界：当前后端对拥有 `mes:pro-process-pool-team-leader:maintain` 权限的 admin 放行全部 active 路线，因此 `process-config/list` 是维护入口范围，不是本任务“工序开始生产组长”配置范围的证明；本任务逐路读取正式路线配置接口。
- 未访问远端环境，未执行 Git 操作，未修改角色权限、表单槽位或批记录表单。
- RED: `red-admin-route-scope.sql` -> FAIL，符合预期：`directTargetRoutes=1, effectiveNonTargetRoutes=0, expected=2/0`；唯一现有命中为 `622 / 980091 / USERS / admin`。
- BACKUP: `mysqldump --replace --where="id=627 AND tenant_id=1"` -> PASS；文件 `db-backup/route-version-627-before.sql`，`82860` bytes，SHA-256 `555F44E051E7196A613DCF74701BC50D587404CD19B5E0895BCA87CCBD26FC04`，精确 `REPLACE` 行数 `1`。
- GREEN: `apply-admin-route-scope.sql` -> PASS；锁定版本 `622, 627`，仅更新 `627` 一行，事务结果 `updated_rows=1, direct_target_routes=2, effective_non_target_routes=0`。
- GREEN: `verify-admin-route-scope.sql` -> PASS；当前 active 直接命中为 `627 / 922119 / 球囊扩张压力泵` 与 `622 / 980091 / 按压式球囊扩充压力泵`，其它 4 条 active 路线 `routeStartProductionLeaders=NULL`。
- 保持性断言：version `627` 移除目标字段后的 JSON hash 写前写后一致；version `622` 完整 JSON hash 写前写后一致；事务未写其它 route version。
- GREEN: 官方 `scripts/preflight/login-preflight.mjs` -> PASS，身份 `芋道源码/admin`，目标 `/mes/pro/route`，本机前端 HTTP `200`、后端 health `UP`。
- Playwright 首轮页面 RED：旧路线列表“路线编码”输入定位超时；当前列表已切换新版筛选控件，未发生 MES 写请求。验证脚本改用正式 `/mes/pro/route/edit/:id?tab=flow` 深链。
- Playwright 次轮页面 RED：未最大化流程图时右侧详情遮挡工序开始节点；随后按既有真实 E2E 路径先最大化流程图，并修正最大化状态定位范围。
- GREEN: `node doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/e2e/verify-admin-route-scope.cjs` -> PASS。正式配置 GET 接口逐路返回：目标 `922119`、`980091` 各 `1` 条 admin；非目标 `900025`、`900026`、`980075`、`980094` 各 `0` 条。
- GREEN: 真实页面打开两条目标路线和非目标 `900025` 的“工序开始 -> 生产组长”面板；目标各显示 `瑛泰管理员（admin）`，非目标显示“暂无生产组长配置”。
- GREEN: Playwright 记录 `mesWriteRequests=[]`、`targetNetworkFailures=[]`、`pageErrors=[]`；三张 `1440x900` 截图人工复核通过，无遮挡或错误状态。
- EXPERIENCE: 通过 `project-experience-consolidation` 合并到 `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦` 和 `docs/experience-index.md`，记录维护权限与实际职责范围分离、逐路正式配置验证及已有草稿发布连续性门禁。
- GREEN: `validate_database_schema.py --evidence .../database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`；技能 evidence 的关键结论已归档到本日志和 `verification-report.md`，允许 cleanup 删除临时 evidence 文件。
- REGRESSION: 收尾前重新运行 `verify-admin-route-scope.sql` -> PASS；当前 active 仍严格命中 `627/922119` 与 `622/980091`，其它四条 active 路线配置为空；备份 SHA-256 复核不变。
- CLEANUP PREVIEW: `task_closeout.py --mode preview` -> PASS；keep 为三份核心文档、`db-backup/`、`db-repair/`，delete 为临时 database evidence、任务专用 Playwright 脚本和任务截图目录，blocked/warnings 均为空。
- CLEANUP APPLY: `task_closeout.py --mode apply` -> PASS；仅删除预览列出的三个任务自有临时路径，未操作其它任务文件、进程或数据。
- STATUS: 任务标记 `completed`；按项目 Git Policy 未执行 stage、commit、merge 或 push。

## 里程碑状态

- M1：已完成。
- M2：已完成。
- M3：已完成。
- M4：已完成。
- M5：已完成。

## 阻塞项

- 当前无。
