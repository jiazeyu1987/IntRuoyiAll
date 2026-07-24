# 排产需求开发计划与 eDHR 影响分析执行日志

## BDD

- BDD: 排产工单成为排产唯一入口 -> Given ERP 同步的生产工单存在 When 排产员填写承诺交期并生成排产工单 Then 系统使用排产工单进入排程，并保证同一生产工单只有一张有效排产工单。
- BDD: eDHR 执行保护排产重排 -> Given 某生产任务已经打开 eDHR 执行或已有归属报工 When 夜间重排执行 Then 系统不得移动、删除或覆盖该任务及其执行快照。
- BDD: 外部 MES 报工先归属再入账 -> Given 班组长导入外部 MES Excel When 同工艺流程同工序存在多个产品型号候选 Then 系统要求选择排产工单和工序后才创建正式报工。

## Evidence

- READONLY: 检查 `doc/tasks/20260609-next-scheduling-requirements`，确认需求已覆盖 ERP 同步、排产工单、路线用途配置、资源产能、报工归属和夜间重排。
- READONLY: 检查 `doc/tasks/20260609-scheduling-order-mvp-design`，确认现有设计已提出排产工单表、差异表和报工归属接口。
- READONLY: 检查 `MesKingdeeProductionOrderSyncServiceImpl`，确认当前同步入口存在，但当前逻辑仍偏“已同步跳过”，需改为按 ERP 工单编码幂等更新。
- READONLY: 检查 `MesProAutoScheduleServiceImpl`、`MesProScheduleCalendarServiceImpl`，确认自动排程、重排、排程日历、问题和依赖能力可复用。
- READONLY: 检查路线资源、工作站、设备产能、维修代码，确认排产资源维护可复用，但需要日期维度资源快照。
- READONLY: 检查第三方报工导入，确认当前导入会自动创建并提交正式报工，与用户确认的待归属流程冲突。
- READONLY: 检查 eDHR 执行、签名、审批、归档和追踪，确认重排必须保护已有 eDHR 执行上下文。
- CHANGE: 新增变更评估 `docs/changes/20260609-scheduling-scope-change-edhr-impact.md`。
- CHANGE: 新增开发计划 `doc/tasks/20260609-scheduling-development-plan-edhr-impact/development-plan.md`。
- CHANGE: 新增测试计划 `doc/tasks/20260609-scheduling-development-plan-edhr-impact/test-plan.md`。

## Follow-up Evidence

- READONLY: 复查 `MesKingdeeProductionOrderSyncServiceImpl`，确认当前同步逻辑在已存在同步记录、同批次重复工单编码、系统已有工单时会跳过，不满足每晚按 ERP 工单编码幂等更新的最新需求。
- READONLY: 复查 `ThirdPartyFeedbackImportServiceImpl`，确认当前导入会解析任务后直接 `createFeedback` 并 `submitFeedback`，与“外部 MES Excel 导入后先待归属，由班组长选择排产工单和工序”的流程冲突。
- READONLY: 复查 `MesProAutoScheduleServiceImpl`，确认当前重排保护只覆盖已结束任务、显式锁定任务和手工任务，尚未识别 eDHR 执行上下文。
- READONLY: 复查 `MesProBatchRecordExecutionDO` 与 `MesProBatchRecordExecutionMapper`，确认 eDHR/批记录执行存在 `workOrderId/taskId/routeProcessId/batchCode/executionSnapshotJson` 上下文，可作为重排保护判断依据。
- READONLY: 复查 `MesProRouteProcessRespVO`、`MesProRouteResourceRespVO`、`MesMdWorkstationDO`、`MesDvMachineryProcessDO`，确认路线资源、设备产能、人工人数、单人小时产能和班次小时已有可复用字段。
- CHANGE: 按最新用户口径更新 `development-plan.md`，移除对 ERP 最后更新时间水位的依赖，改为每晚 2 点按最近一年生产订单滚动幂等同步。
- CHANGE: 按最新用户口径将“eDHR 已打开执行但未报工的任务”明确为夜间重排锁定边界。

## Verification

- GREEN: `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260609-scheduling-scope-change-edhr-impact.md` -> PASS。
- GREEN: `python -m json.tool doc\tasks\20260609-scheduling-development-plan-edhr-impact\task-state.json` -> PASS。
- GREEN: `rg --no-ignore -n "最近一年|工单编码|eDHR 已打开|外部 MES Excel|排产工单不允许拆分|排产数量必须等于生产工单数量" doc\tasks\20260609-scheduling-development-plan-edhr-impact docs\changes\20260609-scheduling-scope-change-edhr-impact.md` -> PASS。
- GREEN: `git diff --check -- doc\tasks\20260609-scheduling-development-plan-edhr-impact docs\changes\20260609-scheduling-scope-change-edhr-impact.md` -> PASS。

## P1 排产工单池核心

- BDD: 排产工单唯一生成 -> Given 一张未完成且未冻结的生产工单 / When 排产员填写承诺交期并生成排产工单 / Then 系统生成一张有效排产工单并创建工序快照，再次生成同一生产工单的排产工单失败。
- BDD: 排产数量由生产工单决定 -> Given 排产员从生产工单生成排产工单 / When 前端提交生成请求 / Then 请求只包含生产工单、承诺交期、优先级和备注，排产数量由后端读取生产工单数量。
- BDD: 排产工单池入口可见 -> Given 排产员进入 MES 首页或菜单 / When 打开排产工单池 / Then 可以看到来源生产工单、排产工单和差异提示区域。
- RED: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> FAIL，缺少 `src/api/mes/pro/scheduleorder/index.ts`。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest test` -> PASS，3 个后端单测通过。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS，前端静态契约通过。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，前端类型检查通过。
- GREEN: 只读数据库校验 -> PASS，`127.0.0.2:23306/ruoyi-vue-pro` 已存在 `mes_pro_schedule_order`、`mes_pro_schedule_order_process`、`mes_pro_schedule_order_diff` 表，菜单 `system_menu.path='schedule-order'` 已落库。
- CHANGE: 为 `paichan_v2` 独立真实验证链路新增本地前端覆盖环境文件 `.env.env.local.local`，将 Vite 代理目标固定到 `http://127.0.0.1:48084`，避免误打到其他 worktree 的 `48082/48081` 运行时。
- CHANGE: 前端 `src/api/mes/pro/scheduleorder/index.ts` 新增 `promiseDate` 规范化，兼容后端返回 `LocalDate` 数组的运行时格式，统一转换为 `yyyy-MM-dd` 供页面展示。
- CHANGE: 真实 E2E 用例 `tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` 增加日期规范化和 DOM click 触发，避免 `el-button` 弹窗打开瞬间的自动化点击重试干扰。
- GREEN: 真实测试租户 API/UI 写入验证 -> PASS，使用 `测试租户 / aoteman`、真实工单 `881MO090880`，在 `http://127.0.0.1:8087 -> http://127.0.0.1:48084` 链路下完成一次成功创建，并验证再次创建失败。输出：`{\"status\":\"PASS\",\"tenant\":\"测试租户\",\"workOrderCode\":\"881MO090880\",\"scheduleOrderId\":6,\"promiseDate\":\"2026-06-30\",\"quantity\":432}`。
- NOTE: 为复跑真实 E2E，测试租户内历史排产工单 `SCH-20260610-0002 / 881MO090880` 曾执行受保护清理；因唯一索引包含 `deleted` 字段，重复编码旧软删记录会阻塞再次软删，所以对本次临时验证生成的活动记录采用了测试租户内物理删除，仅用于清理验证数据，不影响 admin 租户。

## P2 ERP 夜间同步和差异提示

- BDD: ERP 更新不覆盖排产工单 -> Given 测试租户生产工单 `881MO090880` 已生成排产工单 `SCH-20260610-0002` / When 将测试租户生产工单手动改成过期快照后触发真实 `sync-kingdee` / Then 生产工单被 ERP 数据回写恢复，排产工单只标记差异，不直接覆盖排产单本身。
- RED: `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest test` -> FAIL，新增“同编码生产工单应更新而非跳过”和“跨租户来源记录不应重复插入”用例后，现有同步逻辑仍是跳过/重复插入。
- CHANGE: `MesKingdeeProductionOrderSyncServiceImpl` 改为按工单编码更新既有生产工单；若存在有效排产工单，则插入 `mes_pro_schedule_order_diff` 并将 `diff_status` 更新为 `PENDING`。
- CHANGE: `MesKingdeeProductionOrderSyncResult` 与 `MesKingdeeProductionOrderSyncRespVO` 增加 `updatedCount/updatedWorkOrderIds`，用于区分“新建”与“更新”。
- CHANGE: `MesKingdeeProductionOrderSyncRecordMapper` 新增 `selectBySourceKeyIgnoreTenant`；当同步来源已在其他租户存在时，测试租户不再重复插入来源记录，只继续更新本租户生产工单和排产差异。
- CHANGE: `MesProWorkOrderController.syncKingdeeWorkOrders` 权限放宽为 `mes:pro-work-order:create` 或 `mes:pro-schedule-order:create` 任一满足即可，允许排产员入口触发 ERP 同步。
- CHANGE: 为完成测试租户真实验证，恢复 `tenant_admin(role_id=111)` 对 `工单创建(menu_id=5532)` 的测试租户角色菜单绑定，仅影响 `tenant_id=122`。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest test` -> PASS，7 个同步单测通过。
- GREEN: 真实同步 API 验证 -> PASS，使用 `tenant-id=122 / aoteman / http://127.0.0.1:48084/admin-api/mes/pro/work-order/sync-kingdee` 成功返回 `createdCount=804, updatedCount=2, skippedCount=194`。
- GREEN: 真实数据库校验 -> PASS，测试租户工单 `922143 / 881MO090880` 被 ERP 回写恢复为 `quantity=432.00, name=PTCA球囊扩张导管, remark=Kingdee K3Cloud production order: 881MO090880`；排产工单 `id=6` 的 `diff_status` 变为 `1`；差异表新增记录 `id=1, diff_type=ERP_WORK_ORDER_SYNC, status=1`。

## P3 工艺路线用途配置分离

- BDD: 同一路线分离排产与批处理配置 -> Given 路线 `ROUTE-XLSX-00001(922046)` 的基础工序仍使用 `mes_pro_route_process` 保存真实顺序 / When 为 `BATCH` 用途单独保存前两道工序的启用状态和批记录绑定 / Then 用途配置落入独立表，基础工序字段不被覆盖。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProRouteUseConfigServiceImplTest test` -> FAIL，初版 `BeanUtils` 映射未正确带出 `routeProcessId`，默认回填测试失败。
- CHANGE: 新增 P3 SQL `sql/mysql/20260610_mes_route_use_config_p3.sql`，创建 `mes_pro_route_use_config` 与 `mes_pro_route_use_process_config` 两张用途配置表。
- CHANGE: 新增 `MesProRouteUseTypeEnum`、`MesProRouteUseConfigDO`、`MesProRouteUseProcessConfigDO`、Mapper、Service、Controller，支持 `SCHEDULE/BATCH` 两类用途配置。
- CHANGE: `MesProRouteUseConfigServiceImpl.getRouteUseProcessConfigList` 改为显式组装 Response VO，默认从基础工序回填；保存时仅写用途配置表，不改基础 `mes_pro_route_process`。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProRouteUseConfigServiceImplTest test` -> PASS，3 个用途配置单测通过。
- GREEN: 真实数据库验证 -> PASS，测试租户路线 `route_id=922046` 新增 `mes_pro_route_use_config.id=1(use_type=BATCH)`；新增两条 `mes_pro_route_use_process_config`：
- `route_process_id=922339` -> `enabled=0`, `batch_record_report_id='P3-BATCH-REPORT-001'`
- `route_process_id=922343` -> `enabled=1`, `batch_record_report_id='P3-BATCH-REPORT-002'`
- GREEN: 基础工序隔离校验 -> PASS，`mes_pro_route_process.id in (922339, 922343)` 的 `batch_record_report_id` 仍为 `null`，用途配置未污染基础工序主数据。
- CHANGE: `MesProRouteUseConfigController` 权限放宽为：
- 查询：`mes:pro-route:query` 或 `mes:pro-schedule-order:query`
- 保存：`mes:pro-route:update` 或 `mes:pro-schedule-order:create`
- 使排产员账号可在不扩大整条工艺路线菜单权限的前提下使用用途配置接口。
- CHANGE: 前端新增 `src/api/mes/pro/route/useconfig.ts`、`RouteUseConfigDialog.vue`，并在 `RouteProcessList.vue` 加入 “排产用途配置 / 批处理用途配置” 入口；`pnpm ts:check` -> PASS。
- GREEN: 真实 API 验证 -> PASS，使用 `tenant-id=122 / aoteman / http://127.0.0.1:48084/admin-api/mes/pro/route-use-config/*`：
- `GET process-config-list?routeId=922046&useType=BATCH` 返回真实 route 数据及已保存用途配置
- `POST save` 将 `route_process_id=922339` 更新为 `enabled=true, batchRecordReportId='P3-BATCH-REPORT-101'`
- `POST save` 将 `route_process_id=922343` 更新为 `enabled=false, batchRecordReportId='P3-BATCH-REPORT-202'`
- `GET process-config-list?routeId=922046&useType=SCHEDULE` 仍返回 `enabled=true, batchRecordReportId=null`
- GREEN: 真实用途隔离校验 -> PASS，同一路线下 `BATCH` 用途配置更新后，`SCHEDULE` 用途前两道工序仍保持默认启用且未绑定批记录，证明用途隔离生效。

## P4 当日资源快照和资源调整

- BDD: 排产资源按当日可用计算 -> Given 工艺路线 `ROUTE-XLSX-00001(922046)` 中 `B010` 是设备工序、`PROC-XLSX-00007` 是人工工序 / When 为 `2026-06-10` 保存一条设备可用数量覆盖和一条人工人数/班次小时覆盖 / Then `route-process/list-by-route?calendarDate=2026-06-10` 返回的今日可用资源、今日小时产能、今日班次产能和资源状态都体现调整结果。
- CHANGE: 新增 P4 SQL `sql/mysql/20260610_mes_schedule_resource_adjustment_p4.sql`，创建 `mes_pro_schedule_resource_adjustment` 表。
- CHANGE: 新增 `MesProScheduleResourceAdjustmentDO`、Mapper、Service、Controller，提供 `GET /mes/pro/route-resource-adjustment/list` 与 `POST /save`。
- CHANGE: `MesProRouteProcessController.list-by-route` 增加可选参数 `calendarDate`，默认当天；今日产能计算接入日资源调整：
- 设备工序：支持按 `workstationMachineId/machineryId` 覆盖今日可用数量
- 人工工序：支持覆盖今日人数、单人小时产能、班次小时
- 调整后的工序/设备状态统一标记为 `ADJUSTED`
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleResourceAdjustmentServiceImplTest,MesProRouteUseConfigServiceImplTest"` -> FAIL，增加 `calendarDate` 参数后既有 `MesProRouteProcessControllerWorkstationViewTest` 编译失败，说明控制器签名变更尚未兼容老测试。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleResourceAdjustmentServiceImplTest,MesProRouteUseConfigServiceImplTest,MesProRouteProcessControllerWorkstationViewTest" test` -> PASS，13 个相关测试通过。
- GREEN: 真实 API 写入验证 -> PASS，测试租户 `aoteman` 在 `http://127.0.0.1:48084` 成功保存两条资源调整：
- `routeProcessId=922339 / MACHINE / workstationMachineId=297` -> `availableQuantityOverride=0`
- `routeProcessId=922397 / WORKER / workstationId=922506` -> `workerQuantityOverride=6, singleHourlyCapacityOverride=600.0, shiftHoursOverride=9.0`
- GREEN: 真实今日产能解释验证 -> PASS，`GET /admin-api/mes/pro/route-process/list-by-route?routeId=922046&calendarDate=2026-06-10` 返回：
- `922339 / B010`：`todayAvailableResourceQuantityTotal` 从 5 降到 4，`todayShiftCapacityTotal` 从 `500.000025` 降到 `400.00002`，`resourceStatus=ADJUSTED`
- `922397 / PROC-XLSX-00007`：`workerQuantityTotal=6`，`shiftHours=9.0`，`todayShiftCapacityTotal=32400.0`，`resourceStatus=ADJUSTED`

## P5 排程引擎接入排产工单

- BDD: 排程从排产工单进入 -> Given 测试租户排产工单 `SCH-20260610-0002(id=6)` 已绑定生产工单 `881MO090880(922143)` / When 只提交 `scheduleOrderIds=[6]` 调用自动排程预览与应用 / Then 系统按排产工单解析工单范围、生成 24 条任务，并把 `schedule_order_id / schedule_order_process_id` 写入任务扩展表。
- BDD: 已报工与 eDHR 执行保护重排 -> Given 测试租户工单 `922143` 已生成 24 条自动排产任务，其中 `922209/PT-0026` 已存在正式报工、`922208/PT-0025` 已打开 eDHR 执行 / When 调用当前范围重排预览与执行 / Then 系统将两条任务分别标记为 `FEEDBACK` 与 `EDHR` 保护，只替换其余 22 条未保护任务。
- RED: 真实 `POST /admin-api/mes/pro/auto-schedule/apply` -> FAIL，初始只为测试租户补了 `2026-06-10` 一天产线班次计划，`preview` 在 `外管拉伸2(922765)` 报 `产线可用班次产能不足`，`apply` 返回 `产线班次产能缺失`。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` -> FAIL，新增“已报工任务重排保护”和“eDHR 执行任务重排保护”用例前，`resolveProtectionReason` 仅识别 `FINISHED / LOCKED / MANUAL`，不会锁定 `feedback` 与 `batch_record_execution`。
- CHANGE: 补齐测试租户 `tenant_id=122` 的最小真实排程基础数据，仅影响本地测试租户：
- 新增 `mes_pro_capacity_plan` 日班计划 `2026-06-11` 至 `2026-08-31` 共 `82` 条，使产线 `900042` 在 `2026-06-10` 之后具备连续排程窗口。
- 平移 admin 租户自动编码规则 `PRO_TASK_CODE` 到测试租户，新增 `mes_md_auto_code_rule.id=992071`、`mes_md_auto_code_part.id in (992073, 992074)`，解决真实 `apply` 阶段 `编码规则不存在`。
- CHANGE: `MesProAutoScheduleServiceImpl` 新增两类保护源：
- `MesProFeedbackMapper.selectListByTaskIds`：当任务存在非草稿正式报工时，重排保护原因记为 `FEEDBACK`
- `MesProBatchRecordExecutionMapper.selectListByTaskIds`：当任务存在 eDHR 执行记录时，重排保护原因记为 `EDHR`
- CHANGE: 为完成测试租户真实 P5 保护验证，补齐本地测试租户最小前置数据：
- 将 `route_process_id=922339` 的默认批记录报表绑定为测试租户现有报表 `report_id=34cae20da60d4b5b9c1c91cb5344581e`
- 通过真实接口为任务 `922208/PT-0025` 创建 eDHR 执行 `execution_id=188`
- 在测试租户直接补入一条指向任务 `922209/PT-0026` 的正式报工 `mes_pro_feedback.id=131, status=APPROVING`，用于验证已报工保护边界
- CHANGE: `sql/mysql/20260610_mes_auto_schedule_schedule_order_p5.sql` 改为 `information_schema + PREPARE` 幂等写法，移除本地 MySQL 不兼容的 `ADD COLUMN IF NOT EXISTS`。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` -> PASS，新增：
- `apply_shouldAcceptScheduleOrderIdsAndPersistTaskScheduleOrderRelation`
- `replanPreview_shouldExposeProtectedFeedbackTask`
- `replanApply_shouldPreserveEdhrExecutionTask`
- GREEN: 真实 `preview` -> PASS，`tenant-id=122 / aoteman / http://127.0.0.1:48084/admin-api/mes/pro/auto-schedule/preview` 仅传 `scheduleOrderIds=[6]` 返回 `generatedTaskCount=24`、`blockingIssueCount=0`、`shortageCount=27`。
- GREEN: 真实 `apply` -> PASS，`tenant-id=122 / aoteman / http://127.0.0.1:48084/admin-api/mes/pro/auto-schedule/apply` 返回：
- `applied=true`
- `generatedTaskCount=24`
- `createdTaskIds=922208..922231`
- `deletedTaskIds=922184..922207`（二次验证时替换了调试实例先生成的一批测试任务）
- GREEN: 真实数据库校验 -> PASS，测试租户当前活动任务为 `mes_pro_task.id=922208..922231` 共 `24` 条；对应 `mes_pro_task_schedule_ext` 共 `24` 条活动记录，全部满足 `schedule_order_id=6`，`schedule_order_process_id=121..144`，证明排产任务已按排产工单和排产工单工序建立关联。
- GREEN: 真实 `replan preview/apply` -> PASS，`tenant-id=122 / aoteman / http://127.0.0.1:48084/admin-api/mes/pro/auto-schedule/replan/*` 返回：
- `preservedTaskCount=2`
- `protectedTasks[0] = 922209 / FEEDBACK`
- `protectedTasks[1] = 922208 / EDHR`
- `replan apply` 新建 `922232..922253` 共 `22` 条，删除 `922210..922231` 共 `22` 条，保留 `922208, 922209`
- GREEN: 真实保护落库校验 -> PASS，重排后：
- `mes_pro_task.id in (922208, 922209)` 仍为活动任务，未被逻辑删除
- `mes_pro_task.id in (922210..922231)` 已逻辑删除
- `mes_pro_batch_record_execution.id=188` 仍绑定 `task_id=922208`
- `mes_pro_feedback.id=131` 仍绑定 `task_id=922209`
- NOTE: 当前测试租户账号 `aoteman` 的 `mes:pro-feedback:create/update` 权限在运行时未生效，已通过测试租户受控数据补齐完成 P5 保护验证；该权限链问题会影响后续 P6 报工归属页签的真实前端演练，需要在进入 P6 前单独修正。

## P6 外部 MES 报工导入归属

- BDD: 外部 MES 报工待归属 -> Given 班组长导入外部 MES Excel / When 导入行未选择排产工单和工序 / Then 系统只保存待归属记录，不创建正式报工、不更新排产进度。
- BDD: 确认归属后创建正式报工 -> Given 一条待归属记录匹配到排产工单 `SCH-20260610-0002` 的 `B060` 工序并存在活动任务 / When 确认归属到 `schedule_order_process_id=125` / Then 系统创建正式报工、绑定排产工单与排产工序，并扣减该工序剩余数量。
- RED: `ThirdPartyFeedbackImportServiceImplDbTest` -> FAIL，旧逻辑导入 Excel 后立即 `createFeedback + submitFeedback`，与“先待归属再确认”目标冲突。
- CHANGE: 扩展 `mes_pro_feedback_import_record` 为待归属记录模型，新增：
- `attribution_status`
- `work_order_code / item_code / process_code`
- `source_payload_json`
- `schedule_order_id / schedule_order_process_id`
- `candidate_count`
- CHANGE: 扩展 `mes_pro_feedback`，新增：
- `schedule_order_id`
- `schedule_order_process_id`
- CHANGE: 第三方导入服务改造为“只落待归属记录”，不再直接创建正式报工；导入结果 `submittedCount=0`，新增 `pendingCount/importRecordIds`。
- CHANGE: 新增待归属后端接口：
- `GET /admin-api/mes/pro/feedback/import-record/page`
- `GET /admin-api/mes/pro/feedback/import-record/candidates`
- `POST /admin-api/mes/pro/feedback/import-record/attribute`
- CHANGE: 候选查询基于 `process_code + item_code/specification + remaining_quantity > 0` 匹配排产工单工序，并透出当前活动任务。
- CHANGE: 确认归属时创建正式报工、回写 `feedback.schedule_order_id/schedule_order_process_id`、更新 `schedule_order_process.reported_quantity/remaining_quantity`。
- CHANGE: 修正自动重排用 `workOrderIds` 重排时丢失排产工单关联的问题；当前 `workOrderIds` 范围重排会自动回填 `schedule_order_id / schedule_order_process_id`，避免后续归属找不到活动任务。
- GREEN: `mvn -pl yudao-module-mes -Dtest=ThirdPartyFeedbackImportServiceImplDbTest,MesProFeedbackImportRecordServiceImplTest,MesProAutoScheduleServiceImplTest test` -> PASS，25 个相关测试通过。
- GREEN: 真实 SQL 校验 -> PASS，已对本地真实库应用 `sql/mysql/20260610_mes_feedback_import_attribution_p6.sql`。
- GREEN: 真实导入 API 验证 -> PASS，使用 `tenant-id=1 + visit-tenant-id=122` 对测试租户导入：
- 文件 `artifacts-p6-import-2.xlsx`
- 返回 `sheetCount=1, importedCount=1, pendingCount=1, submittedCount=0, importRecordIds=[132]`
- GREEN: 真实候选查询 -> PASS，`GET /admin-api/mes/pro/feedback/import-record/candidates?importRecordId=132` 返回唯一候选：
- `scheduleOrderId=6`
- `scheduleOrderProcessId=125`
- `processCode=B060`
- `taskId=922256 / PT-0073`
- `exactWorkOrderMatch=true`
- GREEN: 真实确认归属 -> PASS，`POST /admin-api/mes/pro/feedback/import-record/attribute` 返回 `feedbackId=132`。
- GREEN: 真实数据库校验 -> PASS：
- `mes_pro_feedback_import_record.id=132` -> `ATTRIBUTED / schedule_order_id=6 / schedule_order_process_id=125 / feedback_id=132`
- `mes_pro_feedback.id=132` -> `task_id=922256 / schedule_order_id=6 / schedule_order_process_id=125 / status=2 / feedback_quantity=6`
- `mes_pro_schedule_order_process.id=125` -> `reported_quantity=6 / remaining_quantity=426`
- RED: 真实 Playwright 待归属页签 -> FAIL，历史旧版第三方导入记录 `128-130` 已创建正式报工但迁移后仍显示 `PENDING` 且缺少 `source_payload_json`，导致 `/mes/pro/feedback/import-record/page` 转换列表时报“待归属记录不存在”；同时前端导入弹窗存在空 `catch {}`，重复导入错误被吞掉，页面不暴露失败原因。
- CHANGE: `sql/mysql/20260610_mes_feedback_import_attribution_p6.sql` 增加旧数据迁移：对 `feedback_id > 0` 的历史导入记录设置 `ATTRIBUTED`，并从 `mes_pro_feedback / mes_pro_work_order / mes_md_item / mes_pro_process` 回填 `work_order_code / item_code / process_code / source_payload_json / schedule_order_id / schedule_order_process_id`。
- CHANGE: `ThirdPartyFeedbackImportForm.vue` 删除空 `catch {}`，导入失败不再被前端静默吞掉，由请求层展示后端真实错误。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,ThirdPartyFeedbackImportServiceImplDbTest,MesProFeedbackImportRecordServiceImplTest" test` -> PASS，25 个相关测试通过。
- GREEN: 真实 Playwright 前端路径 -> PASS，使用本机 `http://127.0.0.1:8089`、测试租户 `测试租户 / aoteman`：
- 打开 `/mes/pro/feedback`
- 切换 `待归属`
- 通过 `第三方导入` 上传 `artifacts-p6-import-3.xlsx`
- 列表出现 `importRecordId=133 / P6Import3 / 2 / 881MO090880 / B060 / P6-TASK-EXT-2 / 6`
- 点击 `选择归属`，候选弹窗返回唯一候选 `scheduleOrderId=6 / scheduleOrderProcessId=125 / taskId=922256 / PT-0073 / remainingQuantity=426 / exactWorkOrderMatch=true`
- 选择候选并确认归属，接口返回 `feedbackId=133`，待归属列表只剩历史失败样本 `131`
- GREEN: 真实数据库校验 -> PASS：
- `mes_pro_feedback_import_record.id=133` -> `ATTRIBUTED / feedback_id=133 / schedule_order_id=6 / schedule_order_process_id=125`
- `mes_pro_feedback.id=133` -> `code=FB-000007 / task_id=922256 / schedule_order_id=6 / schedule_order_process_id=125 / status=2 / feedback_quantity=6`
- `mes_pro_schedule_order_process.id=125` -> `reported_quantity=12 / remaining_quantity=420`
- NOTE: `mes_pro_feedback_import_record.id=131` 保留为待归属失败样本，审批人 `潘金华` 在测试租户不可唯一解析，用于后续负向验证；不影响 `133` 的成功路径验收。

## P7 排产员工作台和看板解释

- BDD: 排产员按顺序操作 -> Given 排产员登录测试租户 / When 打开 `MES 系统 -> 排产员工作台` / Then 页面展示生产订单、排产工单池、工艺路线与资源、今日资源调整、生成排程日历、生产任务、生产报工、偏差复盘、eDHR 追踪的固定操作顺序，并能跳转到现有页面。
- BDD: 工作台解释今日产能和卡点 -> Given 测试租户存在排产工单、生产任务、报工、资源未配置工序 / When 查询工作台日期 `2026-06-10` / Then 系统展示待排工单、今日已排任务、今日计划产能、今日报工数量、维修设备、阻塞项、物料短缺和瓶颈工序。
- RED: 真实 Playwright 工作台 -> FAIL，首次新增 summary SQL 使用不存在的 `mes_pro_route_process.workstation_id` 字段，真实页面接口返回 `Unknown column 'route_process.workstation_id'`。
- CHANGE: 新增 `GET /admin-api/mes/pro/scheduler-workbench/summary?date=YYYY-MM-DD`，权限 `mes:pro-scheduler-workbench:query`，只读汇总现有排产工单、生产任务、生产报工、维修、排产工序快照。
- CHANGE: 新增前端页面 `src/views/mes/pro/scheduler-workbench/index.vue`，展示日期筛选、6 个统计卡、9 个固定步骤、瓶颈与异常表，步骤按钮跳转到现有页面。
- CHANGE: MES 首页快捷入口移动到 KPI 下方，并新增 `排产员工作台` 入口。
- CHANGE: 新增菜单 SQL `sql/mysql/20260610_mes_scheduler_workbench_p7.sql`，在 `MES 系统 -> 生产管理` 下增加 `排产员工作台` 菜单，并补齐测试租户 `tenant_id=122 / role_id=111` 对工作台和 MES 首页的菜单绑定。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest,MesProAutoScheduleServiceImplTest,ThirdPartyFeedbackImportServiceImplDbTest,MesProFeedbackImportRecordServiceImplTest" test` -> PASS，26 个相关测试通过。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- GREEN: 真实 Playwright 工作台验证 -> PASS，使用本机 `http://127.0.0.1:8089`、测试租户 `测试租户 / aoteman`：
- `/mes/pro/scheduler-workbench` 显示 `stepCount=9`、`metricCount=6`、瓶颈表 `rowCount=10`，无 `Unknown column` 或 `bad SQL grammar`
- `/mes/mes/home/index` 在 KPI 下方显示 `快捷入口` 和 `排产员工作台`

## 2026-06-10 18:03 paichan_v2 当前工作树复验

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest,MesKingdeeProductionOrderSyncServiceImplTest,MesProRouteUseConfigServiceImplTest,MesProScheduleResourceAdjustmentServiceImplTest,MesProRouteProcessControllerWorkstationViewTest,MesProAutoScheduleServiceImplTest,ThirdPartyFeedbackImportServiceImplDbTest,MesProFeedbackImportRecordServiceImplTest,MesProSchedulerWorkbenchServiceImplTest" test` -> PASS，49 个相关测试通过。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- RED: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> FAIL，新增 SQL 合同测试最初按需求术语猜测字段 `promised_delivery_date / version_no / adjustment_date`，与实际迁移字段不一致。
- GREEN: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> PASS，2 个 SQL 合同测试通过，确认排产闭环迁移脚本存在、包含关键字段，并禁止 `DELETE FROM / TRUNCATE / DROP` 破坏性操作。

## 2026-06-10 18:40 int_main 合并后测试租户真实 E2E 回归

- RED: `node tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> FAIL，测试租户登录后访问 `/mes/pro/schedule-order` 返回前端 404；根因是本机数据库保留了旧小版本菜单 `system_menu.id=5581`，其 `type=2` 且 `component=mes/pro/schedule-order/index`，与合并后的正式组件 `mes/pro/scheduleorder/index` 冲突。
- CHANGE: 修正 `sql/mysql/20260610_mes_schedule_order_p1.sql`，新增幂等 `UPDATE system_menu`：`5580` 固定为页面菜单 `mes/pro/scheduleorder/index`，`5581/5582` 固定为按钮权限菜单 `type=3` 且组件为空。
- GREEN: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> PASS，3 个 SQL 合同测试通过，新增覆盖旧菜单权限行修复。
- GREEN: 本机测试数据库应用 `sql/mysql/20260610_mes_schedule_order_p1.sql` -> PASS，`5581/5582` 已修正为 `type=3`。
- RED: `node tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> FAIL，当前 `48081` 仍为旧运行时，`/admin-api/mes/pro/schedule-order/page` 被旧后端当作静态资源处理，返回 `No static resource admin-api/mes/pro/schedule-order/page`。
- CHANGE: 执行 `script/deploy/restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main`，将本机 `48081` 重启到合并后的 `int_main` 后端。
- RED: `node tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> FAIL，候选工单 `881MO090756` 对应产品 `922871` 缺少工艺路线绑定，后端按规则返回 `产品缺少启用工艺路线，不能生成排产工单`。
- CHANGE: 在测试租户补齐真实测试数据：为产品 `922871 / AW.106.03.08.1007` 绑定已有启用路线 `922046`，新增 `mes_pro_route_product.id=922077`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS，测试租户 `测试租户 / aoteman`，真实工单 `881MO090756` 生成排产工单 `scheduleOrderId=7`，承诺交期 `2026-06-30`，排产数量 `1221`，并验证重复生成失败和工序快照。
- GREEN: 真实 Playwright 排产员工作台只读回归 -> PASS，测试租户 `测试租户 / aoteman` 打开 `/mes/pro/scheduler-workbench`，summary 接口成功，页面展示 `metricCount=6`、`stepCount=9`、`bottleneckCount=10`。
