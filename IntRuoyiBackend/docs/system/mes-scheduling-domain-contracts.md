# MES 排产域契约

本文档把排产模块里最容易误踩的旧口径集中成稳定契约。后续修改排产算法、工序身份、日历产能、报工联动、重排应用或工作台统计前，必须先对照本文，并同步维护 `script/tests/test_mes_scheduling_domain_contracts.py`。

## 排产域词典

| 名词 | 业务定义 | 业务主身份 | 辅助身份 | 禁止行为 | 必须验证 |
| --- | --- | --- | --- | --- | --- |
| 生产工单 | MES 承接 ERP 或人工创建的生产需求源，是排产工单的来源对象。 | `workOrderId` | `erpWorkOrderCode`、`productId` | 禁止只凭产品号随机反推生产工单。 | 必须验证生产工单存在、未取消、未完成且租户一致。 |
| 排产工单 | 生产工单进入排产池后的业务对象，承载冻结、优先级、状态、路线版本和排产快照。 | `scheduleOrderId` | `workOrderId`、`routeVersionId` | 禁止直接 SQL 拼主表或跳过入池入口。 | 必须验证状态、冻结标识、路线版本和有效排产工序快照。 |
| 路线工序 | 工艺路线版本中的具体工序节点，是排产配置和拓扑隔离的核心。 | `routeVersionId + routeProcessId` | `processId`、`sort` | 禁止用 `processId` 跨路线合并路线工序。 | 必须验证路线版本、路线工序、前置关系和配置唯一。 |
| 基础工序 | 工序主数据，例如吹球囊成型、RX口检测等，可被多条路线复用。 | `processId` | `processCode`、`processName` | 禁止把基础工序当作路线内唯一身份。 | 必须验证与路线工序、工作站能力和报工工序一致。 |
| 当前路线工序 | 当前有效路线版本中的最新路线工序定义。 | `routeVersionId + routeProcessId` | `processId`、`sort` | 禁止用当前路线覆盖仍有剩余量的历史快照工序。 | 必须验证新建入池、配置维护和当前路线展示使用当前定义。 |
| 历史快照工序 | 排产工单创建时固化下来的路线工序快照，可能与当前路线漂移。 | `scheduleOrderId + routeProcessId` | `processId`、`sort`、`predecessorRouteProcessId` | 禁止因当前路线已变更就静默丢弃仍有剩余量的快照工序。 | 必须验证启用状态、剩余量、前置关系和资源快照。 |
| 排产工序快照 | 排产工单内每道工序的冻结证据，包含路线工序、基础工序、产能、班次、日历和资源。 | `scheduleOrderId + routeProcessId` | `scheduleOrderProcessId`、`processId` | 禁止用空快照或默认工序继续排程。 | 必须验证唯一 root、直接前置、无断点、无循环。 |
| 生产任务 | 自动排产或手工排产生成的实际执行任务。 | `taskId` | `workOrderId`、`processId`、`workstationId` | 禁止只按任务状态推断完整排产链。 | 必须验证任务扩展、工序快照、工作站和时间窗口。 |
| 任务扩展 | 生产任务与排产工单、排产工序快照、重排来源之间的连接层。 | `taskId` | `scheduleOrderId`、`scheduleOrderProcessId` | 禁止绕过任务扩展做报工归属。 | 必须验证 `mes_pro_task -> mes_pro_task_schedule_ext -> mes_pro_schedule_order_process` 链路。 |
| 报工记录 | 生产任务上的执行反馈，影响进度、剩余量和受保护任务。 | `feedbackId` | `taskId`、`workOrderId`、`processId` | 禁止只凭产品号、工序名或 Excel 行文本归属报工。 | 必须验证任务、排产工序快照、正式业务单和超量负向。 |
| 日历规则 | 租户级排程日历规则，决定休息日、模拟日期、特殊日期和夜班启用口径。 | `calendarRuleId` | `tenantId` | 禁止缺规则时用白班或默认日期继续应用。 | 必须验证 `calendarContextToken`、休息日、夜班和特殊日期模式。 |
| 产能计划 | 产线在日历日期与班次上的可用产能。 | `lineId + date + shift` | `calendarPlanId`、`capacityMinutes` | 禁止只看源码配置就认定运行态产能存在。 | 必须验证产线排班、容量覆盖和缺失 issue 级别。 |
| 资源池 | 排产算法计算中按产线、工序、工作站、设备、人员聚合出来的可用资源集合。 | `lineId + processId` 或 `routeProcessAvailabilityKey` | `workstationId`、`machineId`、`workerId` | 禁止把产线 + 工序粒度误当设备/人员唯一锁定。 | 必须验证需求是否需要扩展到设备级或人员级锁定。 |
| 受保护任务 | 已报工、锁定、手工调整或被保护策略覆盖的任务。 | `taskId` | `workOrderId + routeProcessId`、`processId` | 禁止重排时覆盖受保护任务时间和工作站上下文。 | 必须验证开始/结束时间、前置顺序、单工序唯一性和剩余量。 |

## 身份优先级

1. 配置保存、工作台在制聚合、路线工序设置、跨路线分行：业务主身份是 `routeVersionId + routeProcessId`，`processId` 只能作为辅助展示或能力匹配。
2. 排产工单内的工序计算：业务主身份是 `scheduleOrderId + routeProcessId`；当前路线发生漂移时，仍有剩余量的历史快照工序优先进入重排。
3. 报工归属和进度同步：业务主身份必须沿 `taskId -> scheduleOrderProcessId -> scheduleOrderId -> workOrderId` 证明；`workOrderId + routeProcessId` 可作为排产链一致性校验。
4. 生产任务资源匹配：工作站能力可使用 `processId`，但任务写入、重排保护和拓扑判断必须回到排产工序快照。
5. 日历产能判断：产线、日期、班次是产能主身份；路线工序的日历规则和 `calendarContextToken` 决定预览能否应用。

## 身份口径判定矩阵

| 场景 | 业务主身份 | 辅助身份 | 禁止行为 | 必须验证 |
| --- | --- | --- | --- | --- |
| 新建排产工单 | `workOrderId + routeVersionId` | `productId`、`routeId` | 禁止绕过 `create-from-work-order` 直接写排产主表。 | 生产工单、路线产品、路线工序、前置拓扑、配置快照。 |
| 路线排产配置保存 | `routeVersionId + routeProcessId` | `processId` | 禁止按 `itemId` 或 `processId` 生成有效配置。 | 通用配置唯一、产品级旧配置不参与有效识别。 |
| 工作台在制统计 | `routeVersionId + routeProcessId` | `processId`、`productId` | 禁止同基础工序跨路线合并。 | 跨路线分行、同路线跨产品合并、班次产能不按订单累加。 |
| 自动排产预览 | `scheduleOrderId + routeProcessId` | `workOrderId`、`processId` | 禁止当前路线覆盖仍有剩余量的历史快照工序。 | 快照拓扑、前置结束时间、资源池、日历窗口、阻断 issue。 |
| 应用重排 | `scheduleOrderId + routeProcessId` | `calendarContextToken`、`requestId` | 禁止沿用旧预览结果或跳过应用前重算。 | preflight、preview、token、阻断 issue、FS link 重新计算。 |
| 报工导入归属 | `taskId + scheduleOrderProcessId` | `workOrderId + routeProcessId`、`processId` | 禁止只凭产品号、工序名或 Excel 行文本归属。 | 任务链、租户、报工人、审批人、超量负向和重复指纹。 |
| 进度同步 | `scheduleOrderId + routeProcessId` | `taskId`、`feedbackId` | 禁止把完成/取消/不可归属工单继续统计为有效排产。 | 任务状态、报工状态、排产工序进度、正式业务单。 |
| 日历产能检查 | `lineId + date + shift` | `calendarRuleId`、`routeProcessId` | 禁止缺夜班或缺容量时使用白班、默认日期或空窗口继续排。 | 日历规则、产线排班、容量覆盖、issue severity。 |
| 受保护任务重排 | `taskId` | `workOrderId + routeProcessId` | 禁止覆盖受保护任务已有时间、工作站和反馈事实。 | 单工序唯一、时间完整、前置顺序、剩余量计算。 |

## 核心身份

- `routeProcessId 是路线工序身份`：用于表达某条工艺路线版本中的具体工序节点，是排产配置、路线拓扑、在制统计隔离和跨路线分行的首要身份。
- `processId 是基础工序身份`：用于表达工序主数据，例如吹球囊成型、RX口检测等。它可以在多条路线中复用，禁止用 processId 跨路线合并排产配置、在制行或路线工序设置。
- `routeVersionId + routeProcessId` 是当前有效排产配置的最小业务键。产品 `itemId` 不参与有效配置识别；产品级旧配置只能作为历史记录或迁移输入。
- 任务、报工、资源池在部分链路仍需要 `processId` 匹配基础工序和工作站能力，但任何涉及路线隔离、配置保存、在制聚合或拓扑判断的逻辑必须优先保留 `routeProcessId`。

## 快照与当前路线

- `排产工单快照` 是排产工单创建时固化的路线工序、前置关系、产能、班次、资源和配置证据。它用于保护已入池或已生产工单不被后续路线主数据漂移直接破坏。
- `当前路线工序` 是工艺路线当前版本中的最新工序定义。新建排产工单、配置维护和工作台当前路线展示应优先使用当前路线工序。
- 手动重排不能只按当前工艺路线定义生成任务。排产工单快照中仍启用且有剩余量的工序，即使当前路线工序已被删除、替换或 processId 漂移，也必须纳入重排计算并生成活动任务。
- 当快照与当前路线发生漂移时，必须显式判断“当前路线定义优先”还是“历史快照优先”，并用测试说明原因；禁止用默认工序、空快照或静默跳过掩盖配置问题。
- 快照拓扑必须包含根工序集合、完整直接前置集合、无断点、无循环；合法多前置汇合不得被单值 `predecessorRouteProcessId` 校验拒绝。缺少完整前置快照时应 fail fast，而不是按排序前一工序或默认空集合继续排程。

## 受保护任务

- `受保护任务` 指已报工、锁定、手工调整或其他被重排保护策略覆盖的生产任务。
- 受保护任务必须保留已有开始/结束时间和工作站上下文；同一工单工序存在多个受保护任务时必须阻断。
- 已报工任务受 `FEEDBACK` 保护不等于“按剩余量重排”已经正确。涉及剩余量算法时，必须分别验证受保护任务、未完成工序、生成任务和排产工单进度。
- 受保护任务早于前置工序结束属于风险警告或阻断口径变更点，不能被当成普通可忽略提示。

## 算法变更门禁

- `算法变更门禁`：任何调整排序、约束、容量分配、任务切段、依赖生成、应用落库或重排说明的改动，必须先确认是否影响 `preview`、`replanPreview`、`apply` 和 `replanApply` 四条路径。
- 应用重排必须重新计算，不能沿用旧预览结果。应用前必须重新执行 preflight、preview、日历上下文 token 校验和阻断 issue 校验。
- `calendarContextToken` 是应用前置门禁。日历规则、休息日、模拟日期或特殊日期模式变更后，旧预览不得继续应用。
- 前后工序依赖必须通过 `predecessorRouteProcessId` 和 FS link 表达；后工序最早开始时间不得早于直接前置工序结束时间。
- 资源占用如果仍按产线 + 工序聚合，必须明确该粒度不能代表设备级或人员级唯一锁定；若需求要求设备/人员不重叠，必须扩展资源锁定模型并补回归。

## 工序身份变更门禁

- `工序身份变更门禁`：任何新增字段、查询、统计、导出、前端行 key 或筛选条件，只要涉及工序，都必须标注使用的是 `routeProcessId` 还是 `processId`。
- 排产员工作台、路线工序配置、在制统计、工序设置保存必须按 `routeVersionId + routeProcessId` 隔离。
- 同一个 `processId` 出现在多条路线中时，页面必须分行展示，后端必须分组隔离，保存设置只能影响目标路线工序。
- 报工归属、导入归属、生产订单回填必须沿 `mes_pro_task -> mes_pro_task_schedule_ext -> mes_pro_schedule_order_process -> mes_pro_schedule_order -> mes_pro_work_order` 证明链路，不能只凭产品号或基础工序反推。

## 日历产能变更门禁

- `日历产能变更门禁`：任何涉及开排日期、夜班、班时、日历规则、产能模式或产线排班的改动，必须同时验证预览、应用、日历视图和工作台在制统计。
- 开排日期是最早开始时间约束，不是保存后立即重排现有任务，也不是强制精确开工时间。
- 夜班启用必须有对应日历规则和可用班次；缺少夜班日历不能用白班、默认日期或空窗口继续排。
- 产线日历缺失、路线工序日历缺失、班时冲突、容量覆盖不足都必须形成明确 issue；是否 BLOCKING 或 WARNING 必须由业务规则显式决定。

## 排程日历用料映射告警门禁

- Trigger: 排程日历月份视图、单日详情、生产用料清单、`mes_kingdee_production_material_list`、`childMaterialId`、`生产用料清单子项未映射本地物料`、`MATERIAL_DEMAND`。
- Preflight check: 先区分“整张生产用料清单缺失”“用料清单子项缺应发数量”“子项 ERP 编码存在但本地物料 ID 未映射”三类情况；未映射子项只能在排程日历读模型中作为 `WARNING` 告警暴露，库存需求只统计已经解析到正式本地物料 ID 的子项。
- Blocker: 整张生产用料清单缺失、已映射子项缺应发数量、试图伪造或跨租户填充 `childMaterialId`、或把未映射子项计入库存汇总时必须停止。
- Verification: 后端回归必须覆盖月份视图和单日详情在未映射子项存在时主流程继续，单日详情 `scheduleIssueSummary` 返回 `MATERIAL_DEMAND / WARNING`，且 `materialDemandSummary` 不包含无本地物料 ID 的子项；同时保留整张清单缺失和缺应发数量的阻断测试。
- Forbidden action: 禁止吞掉告警、返回默认库存充足、手工补本地物料 ID、跨租户引用物料、把未映射 ERP 编码当作本地物料参与缺料计算，或为了日历展示放宽生产用料清单同步的数据治理。
- Evidence: `doc/tasks/20260830-schedule-calendar-material-mapping-warning/verification-report.md`。

## 工作台产能覆盖门禁

- Trigger: 排产员工作台编辑“班次产能”、保存 `process-wip-settings`、统一班次小时、手动重排刷新路线配置快照或修改 `refreshScheduleOrderProcessesFromRouteConfig`。
- Preflight check: 先区分工艺路线基准产能与工作台当前排产覆盖值；工作台展示/编辑班次产能，持久化到排产工序快照的小时产量覆盖值，重排刷新最新路线配置时必须识别并保留 `capacitySource=MANUAL_OVERRIDE` 的工作台覆盖快照。
- Blocker: 工作台保存产能会更新工艺路线配置或触发审批，班次小时变化后仍沿用旧班次总产能，重排刷新把工作台覆盖值改回 `ROUTE_PROCESS`、`MACHINE`、`WORKER` 或路线/设备产能，必须停止并补回归。
- Verification: 后端回归同时覆盖“保存班次产能为小时覆盖值”“统一班次小时后按小时覆盖值重算班次产能”“手动重排刷新路线配置仍保留工作台覆盖值”；前端合同覆盖编辑入口只调用工作台在制设置接口。
- Forbidden action: 禁止用前端临时缓存冒充已保存，禁止把工作台覆盖写回工艺路线版本、工作站主数据或路线审批链，禁止重排时用最新资源快照静默覆盖已保存的工作台当前产能。
- Evidence: `doc/tasks/20260818-scheduler-workbench-capacity-override/verification-report.md`。

## 工作台最近一次排产口径门禁

- Trigger: 排产员工作台工序列表、`process-wip-statistics`、`process-wip-settings`、班次小时刷新或用户反馈“最近只排了 N 个订单但工序显示更多订单在做”。
- Preflight check: 先定位最新成功 `AUTO_APPLY` / `REPLAN_APPLY` 操作日志，并只读取该日志 `afterSnapshotJson.scheduleOrderIds` 作为工作台当前排产范围；工序统计、设置保存和班次产能刷新都必须先收口到这批排产工单，再按 `routeVersionId + routeProcessId` 聚合。
- Blocker: 最新成功排产日志缺少工单范围快照、快照不是数组、包含非数字工单 ID、目标逻辑把同批操作日志行的 `scheduleOrderId` 并入快照范围，或目标逻辑仍从所有 PREPARE/SCHEDULED/IN_PROGRESS 历史工单直接聚合时，必须停止并补正式读模型，不得继续展示默认全量在制。
- Verification: 后端回归至少覆盖同工序历史订单不计入工作台在制单数、最新快照范围不被同批操作日志行扩展、保存工作台工序设置不更新历史订单、统一班次小时刷新不更新历史订单；本机运行态复验必须确认 48081 已加载新 Jar。
- Forbidden action: 禁止只在前端隐藏历史行、按产品号或基础 `processId` 去重、用所有未完成订单冒充最近排产、用同批 operation log 的多行 `scheduleOrderId` 补齐或扩大最新快照范围，或让可见列表是最近排产但保存/刷新仍批量更新历史工单。
- Evidence: `doc/tasks/20260828-scheduler-workbench-latest-run-wip/verification-report.md`；`doc/tasks/20260829-scheduler-workbench-latest-run-current-orders/verification-report.md`。

## 报工联动变更门禁

- `报工联动变更门禁`：任何影响报工、导入报工、进度同步、历史报工、进度数量或剩余量的改动，必须同时核对排产任务、排产工序快照、排产工单进度和正式业务单。
- 导入归属类场景必须覆盖正向和超量负向；正向核导入记录已归属、正式业务单、进度数量增减，负向核记录仍待归属且页面错误可见。
- 进度同步不能把已完成、已取消或不可归属工单当有效排产继续统计。
- 报工联动发现缺任务、缺排产链、缺活动任务、缺报工人、缺审批人或重复指纹时，应输出行级原因，不能返回泛化成功或“报工单号：无”。

## 手动重排数据包门禁

- Trigger: 排产员工作台“导出全部数据包/导入全部数据包”、手动重排复现、跨环境排产数据迁移、`manualReplanDataPackage`、`scheduler-manual-replan-data`。
- Preflight check: 区分路线配置包和全部数据包；路线配置包只承载路线排产用途、排产配置和资源引用，全部数据包若承诺可复现手动重排，必须同时承载排产工单、生产工单、排产工序快照、路线拓扑、日历规则、计划/实际产能、现有任务、任务扩展、报工、用料、物料、库存和工作台策略设置；若承诺跨租户恢复，导入必须把租户型数据行重写到目标租户上下文。
- Blocker: 只导出岗位/角色/路线配置却宣称可手动重排，或导入缺少排产工序快照、任务扩展、日历产能、用料/库存、策略设置任一正式字段，或 `TenantBaseDO` 数据保留源租户 `tenantId` 时，必须 fail-fast；不得用空列表、默认路线、默认产能、默认策略或运行时重新同步冒充数据包完整。
- Verification: 后端契约必须证明全量包包含手动重排数据包和策略设置、缺包时导入失败、导入结果返回主数据/排产工单数据/运行态数据/策略设置计数、跨租户导入重写 `tenantId`；前端按钮提示必须展示这些计数。
- Forbidden action: 禁止把“导出排产工艺路线”扩大成业务数据导出；禁止在手动重排接口里补 mock 或默认数据；禁止导入时吞掉缺引用、保留源租户 `tenantId` 或用随机新 ID 破坏排产工单快照身份。
- Evidence: `doc/tasks/20260729-scheduler-workbench-full-data-package-replan/verification-report.md`；`doc/tasks/20260730-scheduler-workbench-full-package-tenant-policy/verification-report.md`。

## 默认值与历史兼容

- 默认值必须明确是业务默认还是历史兼容。业务默认必须有产品/流程认可、文档说明和测试；历史兼容只能存在于明确标注的迁移或历史快照读取边界。
- 当前存在的 `10.5h` 班时、默认排产配置 warning、默认资源快照 warning 等口径，后续改动前必须先确认是否仍是正式规则。
- 禁止新增静默 fallback、默认成功、空 catch、mock 成功或吞异常来掩盖缺配置、缺路线、缺日历、缺产能、缺快照。
- 只读展示可以容忍历史记录缺失，但必须避免把缺失显示成看似正常的业务结论；写入、预览、应用、重排、报工归属必须 fail fast。

### 默认值分类清单

| 分类 | 当前口径 | 允许边界 | 禁止扩散 |
| --- | --- | --- | --- |
| 业务默认 | 容量模式空值按计划产能；保留手动锁定任务空值按 `true`；`10.5h` 班时继续沿用既有排产工单资源快照口径。 | 只能通过 `ScheduleDefaultCompatibilityPolicy.businessDefaultCapacityMode`、`businessDefaultPreserveManualLockedTasks` 或已记录业务规则的命名方法进入计算。 | 不得把缺日历、缺产能、缺路线、缺正式配置解释成业务默认。 |
| 历史兼容读取 | 旧排产快照的计划数量可回读生产工单数量；默认排产配置 warning、默认资源快照 warning 保持 warning；`TenantUtils.executeIgnore` 仅用于历史路线/已删除路线只读读取。 | 只能通过 `ScheduleDefaultCompatibilityPolicy.historicalSnapshotScheduleQuantity`、`warnDefaultRouteScheduleConfig`、`warnDefaultResourceSnapshot`、`historicalReadRouteMapIgnoreDeleted` 这类带 `historical` / `warnDefault` 命名的边界出现。 | 不得把历史快照缺字段变成新写入成功，不得把默认资源快照 warning 提升或降低为其他 severity。 |
| 必须失败 | 缺路线、缺路线工序、缺排产策略配置、缺日历规则、缺夜班日历、缺容量、缺人员数量、缺活动任务、应用前 token 缺失或漂移。 | 必须返回明确 blocking issue 或抛出业务异常；缺正式排产配置对应 `failFastMissingRouteScheduleConfig`，缺日历/产能对应 `failFastMissingCalendarOrCapacity`。 | 不得新增默认成功、静默 fallback、mock 成功、空 `catch ignored`、自动补默认路线或默认产能。 |

- `catch ignored` 当前不允许作为排产域默认值边界；如未来确需保留，只能在历史只读展示中命名说明触发条件、影响和移除策略。
- 默认排产配置 warning 与默认资源快照 warning 是历史兼容提醒，不代表新业务配置完整；写入、预览、应用、重排和报工归属仍需执行正式前置校验。

## 修改前检查清单

- 改排产算法：检查排序、前置依赖、资源锁定粒度、预览和应用是否同口径。
- 改工序身份：检查 `routeProcessId` / `processId` 是否被混用，跨路线同基础工序是否被误合并。
- 改日历产能：检查日历 token、夜班、休息日、容量覆盖和应用前重算。
- 改报工联动：检查任务扩展表、排产工序快照、进度回写、超量负向和正式业务单。
- 改前端展示：检查页面行 key、筛选字段、导出列、详情统计和错误提示是否仍能暴露后端真实问题。
