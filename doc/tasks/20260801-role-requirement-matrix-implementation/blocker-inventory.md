# 岗位需求分解矩阵 Blocker Inventory

## Purpose and Scope

本文件汇总 `role-requirement-matrix` 当前真实 E2E 前置检查和规划静态合同暴露的正式 SOURCE / code-path blockers，并保留已验证关闭的 blocker 记录。2026-08-02 用户已明确调整 M0 门禁口径：M0 只负责识别并结构化冻结 SOURCE blocker，不要求在 M0 清零这些需要 M1-M5 正式实现的 blocker。当前 M1 activeOrderId authority 切片已验证关闭 RRM-BLK-001..007，M2 production coefficient snapshots 切片已验证关闭 RRM-BLK-026..028，M3 QA/PQC 切片已验证关闭 RRM-BLK-017..025；剩余 12 个 SOURCE blocker 是 M4-M5 的待清零 backlog；不得用 mock、默认值、fallback、API-only 或临时夹具伪造验收通过。

## Evidence Reviewed

- `doc/tasks/20260801-role-requirement-matrix-implementation/task-state.json`
- `doc/tasks/20260801-role-requirement-matrix-implementation/task.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/execution-log.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/verification-report.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/m0-gate-audit.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`
- `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`
- `doc/tasks/20260801-role-requirement-matrix-excel/development-plan.md`
- `doc/tasks/20260801-role-requirement-matrix-excel/test-plan.md`

## Blocker Summary

说明：`Downstream work can continue` 表示在 2026-08-02 用户批准的 M0 新口径下，当前或后续里程碑是否可继续推进不依赖该 blocker 的切片。当前主线程已完成 M1 activeOrderId source gate、M2 production coefficient snapshots gate 和 M3 QA/PQC source gate，并切换到 M4；M5-M6 仍受各自依赖门禁约束。

| ID | Milestone / AC / TC | Blocked source or code path | Failing command | Expected reason | Impact | Required formal solution | Downstream work can continue |
|---|---|---|---|---|---|---|---|
| RRM-BLK-001 | M1 / AC-M04 / TC-M04 | `MesProcessPoolActiveOrderDO.routeId` | `pnpm e2e:role-requirement-matrix:real:check` | 统一 activeOrderId 缺 `routeId` 字段 | 不能把生产、PQC、批记录、调拨和放行收敛到同一正式路线身份 | 已实现：active order DO / migration / add service 写入正式 routeId | 已验证关闭 |
| RRM-BLK-002 | M1 / AC-M04 / TC-M04 | `MesProcessPoolActiveOrderDO.routeVersionId` | `pnpm e2e:role-requirement-matrix:real:check` | 统一 activeOrderId 缺 `routeVersionId` 字段 | 不能冻结路线发布版本，PQC 和批记录可能读取漂移配置 | 已实现：active order DO / migration / add service 写入 routeVersionId | 已验证关闭 |
| RRM-BLK-003 | M1 / AC-M03, AC-M04 / TC-M03, TC-M04 | `MesProcessPoolActiveOrderDO.erpFixedQuantitySnapshot` | `pnpm e2e:role-requirement-matrix:real:check` | 统一 activeOrderId 缺 ERP 固定数量快照 | M2 不能证明 ERP 订单数量不被报工分配改写 | 已实现：加入活跃订单时冻结 `MesProWorkOrderDO.quantity`，缺失即 fail-fast | 已验证关闭 |
| RRM-BLK-004 | M1 / AC-M04 / TC-M04 | `MesProcessPoolActiveOrderDO.businessStatus` | `pnpm e2e:role-requirement-matrix:real:check` | 统一 activeOrderId 缺业务状态字段 | 无法表达 ACTIVE / REMOVED / TERMINATED / COMPLETED 的跨角色状态机 | 已实现：active order DO / migration / add-remove service 写入业务状态 | 已验证关闭 |
| RRM-BLK-005 | M1 / AC-M04 / TC-M04 | `MesProcessPoolActiveOrderDO.version` | `pnpm e2e:role-requirement-matrix:real:check` | 统一 activeOrderId 缺版本字段 | 并发加入、移出、分配、放行不能靠乐观锁防重复终态 | 已实现：active order DO 增加 `@Version` 字段并在 migration 中定义版本列 | 已验证关闭 |
| RRM-BLK-006 | M1 / AC-M04 / TC-M04 | `uk_mes_pp_active_order` | `pnpm e2e:role-requirement-matrix:real:check` | 活跃订单唯一键仍绑定 `leader_user_id` | 同一订单在不同角色/组长下会形成不同活跃身份 | 已实现：M1 authority migration 删除旧 key 并新增租户 + 订单 + 路线 + 路线版本 + 删除标记唯一键 | 已验证关闭 |
| RRM-BLK-007 | M1, M3 / AC-M04, AC-D24 / TC-M04, TC-D24 | `MesFrontlinePqcContextServiceImpl` -> `processPoolMapper.selectActiveList` | `pnpm e2e:role-requirement-matrix:real:check` | PQC 仍通过 `mes_pro_process_pool` 活跃行读取订单 | PQC 订单列表不等于统一 activeOrderId；不能证明跨角色同一订单身份 | 已实现：PQC 订单列表和活跃订单校验改读 `MesProcessPoolActiveOrderMapper` | 已验证关闭 |
| RRM-BLK-008 | M4 / AC-M22, AC-M23 / TC-M22, TC-M23 | `CHECK_INSPECTION_RESULT` -> `buildSourceNotIntegratedItem` | `pnpm e2e:role-requirement-matrix:real:check` | 放行检验结果来源未接入 | eDHR 放行预检不能追溯真实过程检验 | 建立过程检验聚合来源适配器，缺来源 fail-fast | 是 |
| RRM-BLK-009 | M4 / AC-D36, AC-M22 / TC-D36, TC-M22 | `CHECK_DEVIATION_CLOSED` -> `buildSourceNotIntegratedItem` | `pnpm e2e:role-requirement-matrix:real:check` | 偏差关闭来源未接入 | 有偏差时放行完整性无法判断 | 建立偏差/质量异常关闭来源适配器 | 是 |
| RRM-BLK-010 | M4 / AC-M22, AC-M23 / TC-M22, TC-M23 | `CHECK_REWORK_CLOSED` -> `buildSourceNotIntegratedItem` | `pnpm e2e:role-requirement-matrix:real:check` | 返工关闭来源未接入 | 返工未闭环时可能无法阻塞放行 | 建立返工状态来源适配器 | 是 |
| RRM-BLK-011 | M4 / AC-M22, AC-M23 / TC-M22, TC-M23 | `CHECK_SCRAP_RECORDED` -> `buildSourceNotIntegratedItem` | `pnpm e2e:role-requirement-matrix:real:check` | 报废记录来源未接入 | 报废未记录时放行完整性无法判断 | 建立报废记录来源适配器 | 是 |
| RRM-BLK-012 | M4 / AC-M22, AC-M23 / TC-M22, TC-M23 | `CHECK_INVENTORY_CONSISTENCY` -> `buildSourceNotIntegratedItem` | `pnpm e2e:role-requirement-matrix:real:check` | 库存一致性来源未接入 | 物料批次/库存不一致时无法阻塞放行 | 建立库存一致性来源适配器并关联 activeOrderId | 是 |
| RRM-BLK-013 | M4 / AC-M02, AC-M07 / TC-M02, TC-M07 | `activeOrderTransferRelation` | `pnpm e2e:role-requirement-matrix:real:check` | 缺 activeOrderId 与调拨头/行正式关系 | 多调拨、多物料、多批次无法追溯到统一订单身份 | 新增 active order transfer trace schema / service / idempotency key | 是 |
| RRM-BLK-014 | M4 / AC-M05 / TC-M05 | `activeOrderShipmentSource` | `pnpm e2e:role-requirement-matrix:real:check` | 缺 activeOrderId 与发货事实正式关系源 | 开工检查不能证明已发货物料覆盖 | 冻结 ERP/MES 发货正式来源并建立 activeOrderId 关联 | 是 |
| RRM-BLK-015 | M4 / AC-M05, AC-M06 / TC-M05, TC-M06 | `activeOrderReplenishmentReturnSource` | `pnpm e2e:role-requirement-matrix:real:check` | 缺 activeOrderId 与补料/退料正式关系源 | 净物料覆盖计算不完整 | 建立补料/退料方向、数量、批次来源和关系模型 | 是 |
| RRM-BLK-016 | M4 / AC-M06, AC-M08 / TC-M06, TC-M08 | `activeOrderBatchTraceSource` | `pnpm e2e:role-requirement-matrix:real:check` | 缺 activeOrderId 与物料批次/库存追溯正式关系源 | 开工检查和放行库存一致性无法闭环 | 建立 activeOrderId 与物料批次/库存追溯读模型 | 是 |
| RRM-BLK-017 | M3 / AC-M09, AC-D15 / TC-M09, TC-D15 | `qaRegulationOwnership` | `pnpm e2e:role-requirement-matrix:real:check` | QA 规程唯一所有权和正式表/API 未冻结 | 临时 QC 模板不能作为正式 QA 规程来源 | 已实现：新增 QA 规程、版本和明细正式 schema / DO / Mapper，并由 PQC 上下文读取发布规程 | 已验证关闭 |
| RRM-BLK-018 | M3 / AC-D23 / TC-D23 | `qaRegulationVersionModel` | `pnpm e2e:role-requirement-matrix:real:check` | QA 规程发布版本模型未确认 | 不能证明发布不可变和历史任务保留版本 | 已实现：发布规程版本模型包含状态、版本号、发布快照和历史版本身份 | 已验证关闭 |
| RRM-BLK-019 | M3 / AC-M12~M15, AC-D29 / TC-M12~M15, TC-D29 | `pqcTaskModel` | `pnpm e2e:role-requirement-matrix:real:check` | PQC 任务身份缺检验类型、日期、班次、轮次、规程版本 | 首检/巡检/末检任务不可唯一生成或跨天隔离 | 已实现：新增 PQC task schema / DO / Mapper，前后端提交携带任务身份和规程版本 | 已验证关闭 |
| RRM-BLK-020 | M3 / AC-D27, AC-D33 / TC-D27, TC-D33 | `pqcPieceDetailModel` | `pnpm e2e:role-requirement-matrix:real:check` | PQC 逐件明细模型未确认 | 计划数量对应的逐件结果不可还原 | 已实现：新增 PQC piece detail schema / DO / Mapper，提交时写入逐件结果 | 已验证关闭 |
| RRM-BLK-021 | M3 / AC-D24, AC-D29 / TC-D24, TC-D29 | `selectActiveByWorkOrderRouteProcess` | `pnpm e2e:role-requirement-matrix:real:check` | PQC 提交仍依赖最新生产事件 | 无生产事件时不能独立提交 PQC | 已实现：PQC 提交改为基于 activeOrderId + pqcTaskId + regulationVersionId + 任务身份 | 已验证关闭 |
| RRM-BLK-022 | M3 / AC-D17 / TC-D17 | `hardcodedPqcInspectionItems` / `FrontlineFixedTemplatePanel.vue` hardcoded PQC items | `pnpm e2e:role-requirement-matrix:real:check` | 前端仍硬编码 `length/appearance/seal/pressure` | PQC 页面不能按发布规程动态渲染 | 已实现：前端从 `selectedProcess.inspectionItems` 动态渲染检验项目，不再硬编码四项 | 已验证关闭 |
| RRM-BLK-023 | M3 / AC-M12~M15 / TC-M12~M15 | `defaultPqcInspectionType` / `FrontlineFixedTemplatePanel.vue` default `PATROL` | `pnpm e2e:role-requirement-matrix:real:check` | 前端默认巡检类型 `PATROL` | 首检/上午/下午/末检身份会被错误归类 | 已实现：检验类型从 PQC task snapshot 读取，禁止前端切换为不一致类型 | 已验证关闭 |
| RRM-BLK-024 | M3 / AC-D18, AC-D19, AC-D27 / TC-D18, TC-D19, TC-D27 | `defaultPqcInspectionQuantity` / `FrontlineFixedTemplatePanel.vue` default quantity `30` | `pnpm e2e:role-requirement-matrix:real:check` | 前端默认检验数量 30 | 首检固定数量和巡检比例任务无法验收 | 已实现：计划检验数量由后端 PQC task snapshot 下发，前端不再默认 30 | 已验证关闭 |
| RRM-BLK-025 | M3 / AC-D28 / TC-D28 | `defaultPqcScrapQuantity` / `FrontlineFixedTemplatePanel.vue` default scrap `1` | `pnpm e2e:role-requirement-matrix:real:check` | 前端默认损耗数量 1 | 不合格/损耗事实被默认值污染 | 已实现：损耗数量默认空值，提交前按实际检验输入计算和校验 | 已验证关闭 |
| RRM-BLK-026 | M2 / AC-M17, AC-M18 / TC-M17, TC-M18 | `activeOrderProductionQuantityFactorSnapshot` | `pnpm e2e:role-requirement-matrix:real:check` | activeOrderId 缺生产系数快照 | 分配和进度不能按路线工序正式系数复核 | 已实现：创建 active order 时写入逐工序 `productionQuantityFactorSnapshot`，分配/完成统一读取目标数量服务 | 已验证关闭 |
| RRM-BLK-027 | M2 / AC-M18 / TC-M18 | `activeOrderPlannedQuantitySnapshot` | `pnpm e2e:role-requirement-matrix:real:check` | activeOrderId 缺计划数量快照 | 目标量无法按 ERP 固定数量 × 系数冻结 | 已实现：逐工序快照保存 `plannedQuantitySnapshot = ERP固定数量 × 生产系数`，分配/完成用该快照 | 已验证关闭 |
| RRM-BLK-028 | M2 / AC-M18 / TC-M18 | `defaultProductionQuantityFactorInAutoSchedule` / `DEFAULT_PRODUCTION_QUANTITY_FACTOR` auto schedule path | `pnpm e2e:role-requirement-matrix:real:check` | 自动排产缺系数时默认使用生产系数 1 | 缺正式系数也可继续生产，违反 fail-fast | 已实现：移除默认系数路径，缺失或非正数抛 `PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID` | 已验证关闭 |
| RRM-BLK-029 | M0, M5 / AC-D14 / TC-D14 | `normalizeRecordBindingSlotTypeDefaultMain` | `pnpm e2e:role-requirement-matrix:real:check` | 工艺路线前端缺槽位默认 `MAIN` | `formBindings` / 旧字段可能被误归为正式批记录表单 | 缺正式批记录槽位时 fail-fast，逐工序绑定只读正式表 | 是 |
| RRM-BLK-030 | M0, M5 / AC-D14 / TC-D14 | `batchRecordFormNamesFormBindingsSeparation` | `pnpm e2e:role-requirement-matrix:real:check` | `batchRecordFormNames` 与 `formBindings` 分离未由真实 E2E 证明 | 三类工艺路线配置仍可能互相替代 | 增加真实路径验证和静态合同，分别证明工序开始、批记录表单、表单槽位来源 | 是 |
| RRM-BLK-031 | M0, M5 / AC-D14 / TC-D14 | `edhrRuntimeDefaultMainSlot` | `pnpm e2e:role-requirement-matrix:real:check` | eDHR 运行态缺 `formSlotType` 时默认 `MAIN` | 正式批记录绑定缺失可能被表单槽位或旧字段掩盖 | eDHR 运行态读取正式逐工序绑定；缺失时 fail-fast，不默认 `MAIN` | 是 |
| RRM-BLK-032 | M0 / runtime precondition | `backendHealth` / `http://127.0.0.1:48081/actuator/health` | `pnpm e2e:role-requirement-matrix:real:check` | 历史运行态失败：后端 health 不可达；最新复核已恢复 | 历史上真实 Playwright E2E 无法证明后端 API、登录、权限和业务流；当前不再阻塞 M0 前置 | 已复核 `48081` health 为 `UP`，并重跑 `real:check` 确认无 RUNTIME blocker | 是，已验证关闭；M0 已按新口径 accepted |

## Current Real Check Key Map

说明：本表按 `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` 当前 12 个 `SOURCE` blocker 的精确 `key` 映射到结构化 blocker ID。RRM-BLK-001..007、RRM-BLK-017..025 和 RRM-BLK-026..028 已从当前 `result.json` 移除并在 `Status Register` 标为 `RESOLVED_VERIFIED`。该映射用于防止后续清单改名、合并描述或中文说明导致 `real:check` 输出无法追踪。

| result.json key | Category | Blocker ID | Current status |
|---|---|---|---|
| `CHECK_INSPECTION_RESULT` | SOURCE | RRM-BLK-008 | OPEN_BLOCKED |
| `CHECK_DEVIATION_CLOSED` | SOURCE | RRM-BLK-009 | OPEN_BLOCKED |
| `CHECK_REWORK_CLOSED` | SOURCE | RRM-BLK-010 | OPEN_BLOCKED |
| `CHECK_SCRAP_RECORDED` | SOURCE | RRM-BLK-011 | OPEN_BLOCKED |
| `CHECK_INVENTORY_CONSISTENCY` | SOURCE | RRM-BLK-012 | OPEN_BLOCKED |
| `activeOrderTransferRelation` | SOURCE | RRM-BLK-013 | OPEN_BLOCKED |
| `activeOrderShipmentSource` | SOURCE | RRM-BLK-014 | OPEN_BLOCKED |
| `activeOrderReplenishmentReturnSource` | SOURCE | RRM-BLK-015 | OPEN_BLOCKED |
| `activeOrderBatchTraceSource` | SOURCE | RRM-BLK-016 | OPEN_BLOCKED |
| `normalizeRecordBindingSlotTypeDefaultMain` | SOURCE | RRM-BLK-029 | OPEN_BLOCKED |
| `batchRecordFormNamesFormBindingsSeparation` | SOURCE | RRM-BLK-030 | OPEN_BLOCKED |
| `edhrRuntimeDefaultMainSlot` | SOURCE | RRM-BLK-031 | OPEN_BLOCKED |

## Status Register

说明：本表与上方 `Blocker Summary` 通过 `ID` 组成同一 blocker 记录，用于满足每条 blocker 的当前状态和创建/更新时间要求。

| ID | Current status | Created/updated date |
|---|---|---|
| RRM-BLK-001 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-002 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-003 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-004 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-005 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-006 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-007 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-008 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-009 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-010 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-011 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-012 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-013 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-014 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-015 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-016 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-017 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-018 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-019 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-020 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-021 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-022 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-023 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-024 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-025 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-026 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-027 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-028 | RESOLVED_VERIFIED | 2026-08-02 |
| RRM-BLK-029 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-030 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-031 | OPEN_BLOCKED | 2026-08-02 |
| RRM-BLK-032 | RESOLVED_VERIFIED | 2026-08-02 |

## Owners and Acquisition Steps

- M1 activeOrderId blockers：已验证关闭；后续不得重新引入 leader-scoped active order 唯一键、PQC `processPoolMapper.selectActiveList` 来源或缺路线/版本/ERP 数量快照的 active order。
- M2 production coefficient blockers：已验证关闭；后续不得重新引入缺系数默认 `1`、按 `MesProWorkOrderDO.quantity` 直接当工序目标量、或绕过逐工序目标数量快照的分配/完成路径。
- M3 QA / PQC blockers：已验证关闭；后续不得重新引入临时 QC 模板替代正式 QA 规程、PQC 提交依赖最新生产事件、前端硬编码检验项或默认 PQC 数量。
- M4 transfer / release blockers：当前里程碑；调拨、库存、异常、过程检验和 eDHR 放行服务负责真实来源适配器。
- M5 route configuration separation blockers：工艺路线前端、eDHR 运行态和真实 E2E 共同证明三类配置互不替代。
- M0 runtime blocker：本地 `int_main` 后端运行态当前已恢复 `127.0.0.1:48081` health；后续若 health 再次失败，必须重新打开 runtime blocker。

## Future RED Plan Register

当前门禁：本表把 SOURCE blocker 转换成后续可执行的 RED 计划和依赖关系。M0 已按新口径 accepted，M1 activeOrderId 切片、M2 production coefficient snapshots 切片和 M3 QA/PQC 切片已验证关闭；`Allowed in current milestone` 现在只允许 M4 调拨/放行来源切片启动，其它计划仍需等待依赖里程碑。

| RED plan | Covered blocker IDs | Earliest allowed milestone | Required precondition before running RED | Future RED command | Expected RED reason | Allowed in current milestone |
|---|---|---|---|---|---|---|
| Unified activeOrderId authority | RRM-BLK-001..007 | M1 | M0 source gate accepted；确认 activeOrderId schema 设计和迁移范围 | `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 已 GREEN；`real:check` 不再输出 RRM-BLK-001..007 | 已完成 |
| Production quantity factor snapshots | RRM-BLK-026..028 | M2 | M1 activeOrderId GREEN 且路线工序系数正式来源冻结 | `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 已 GREEN；`real:check` 不再输出 RRM-BLK-026..028 | 已完成 |
| QA regulation version and PQC task model | RRM-BLK-017..021 | M3 | M1/M2 GREEN；QA 规程所有权由正式设计冻结 | `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 已 GREEN；`real:check` 不再输出 RRM-BLK-017..021 | 已完成 |
| PQC dynamic frontend rendering | RRM-BLK-022..025 | M3 | 后端 PQC task/regulation snapshot API 契约明确；不得使用前端默认值 | `pnpm e2e:role-matrix-pqc-dynamic-form:static` | 已 GREEN；`real:check` 不再输出 RRM-BLK-022..025 | 已完成 |
| Transfer, shipment, batch trace and release sources | RRM-BLK-008..016 | M4 | M1/M2/M3 GREEN；ERP/MES 调拨、发货、补退料和批次关系源冻结 | `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderStartCheckServiceTest,MesQualityAbnormalServiceTest,MesPqcProcessInspectionAggregationTest,MesOrderReleaseCompletenessServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 调拨/发货/补退料/批次缺 activeOrderId 正式关系；eDHR 放行仍返回 `buildSourceNotIntegratedItem` 占位来源 | 是，当前 M4 起点 |
| Route batch-record and formBindings separation | RRM-BLK-029..031 | M5 | M5 进入前需保留 blocker；后续解除需真实页面证明三类配置互不替代 | `pnpm e2e:role-requirement-matrix:real:check` plus future focused static/E2E contract | 工艺路线前端和 eDHR 运行态仍可能把缺失槽位默认 `MAIN`，`batchRecordFormNames` 与 `formBindings` 分离缺真实 E2E 证明 | 否，只能等 M5 |

## Current Execution Permission After M0 Gate Redefinition

| Work item | Current permission | Reason |
|---|---|---|
| M0 evidence, blocker inventory, gate audit, test-report and verification-report updates | 允许 | 需要记录用户批准的 M0 gate 变更和准出证据 |
| M0 static preflight and real preflight check | 允许 | `real:check` 在 M0 可为 EXPECTED_BLOCKED，前提是仅剩已结构化 SOURCE blocker 且 ENV/RUNTIME 为 0 |
| Local fixture documentation for authorized tenant/accounts/signatures/pressure-pump route/derived QA template | 允许 | 只作为 M0 预检夹具证据，不解除正式 SOURCE blocker |
| M1 activeOrderId schema/service implementation | 已完成 | RRM-BLK-001..007 已通过 GREEN/REGRESSION/real:check 验证关闭 |
| M2 production coefficient / planned quantity implementation | 已完成 | RRM-BLK-026..028 已通过 GREEN/REGRESSION/real:check 验证关闭 |
| M3 QA/PQC production/frontend/backend implementation | 已完成 | RRM-BLK-017..025 已通过 GREEN/REGRESSION/real:check 验证关闭 |
| M4 transfer/release production/frontend/backend implementation | 允许 | 当前里程碑为 M4，必须先 BDD 和 RED |
| M5-M6 production/frontend/backend implementation | 禁止 | 后续里程碑仍 blocked by upstream，不得越级 |
| Future RED commands listed above | 仅 M4 命令允许启动 | M5 命令仍只作为后续 TDD 起点 |

## Verification Methods

当前门禁：以下 M2-M5 命令作为 blocker 归属后的验证入口或 RED/GREEN 计划。M0/M1/M2/M3 已 accepted；当前主线程只允许启动 M4 命令，M5 仍需等待依赖里程碑。

- 最小 SOURCE gate：`pnpm e2e:role-requirement-matrix:real:check`
- 最小 RUNTIME gate：`Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 或 `pnpm e2e:role-requirement-matrix:real:check`
- M0 静态入口合同：`pnpm e2e:role-requirement-matrix:preflight:static`
- M1 completed schema/service gate：`mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- M2 completed schema/service gate：`mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- M3 completed static contract gate：`pnpm e2e:role-matrix-qa-regulation:static`、`pnpm e2e:role-matrix-pqc-dynamic-form:static`
- M4 current static contract gate：`pnpm e2e:role-matrix-transfer-start-check:static`
- M5 future static contract gate：`pnpm e2e:role-matrix-daily-close-scope:static`

## Risks

- 临时 QA 模板 `6 / RRM-20260801-QA-REG-PP-V21` 只能作为本机测试夹具，不解除正式 QA 规程版本模型 blocker。
- M4 当前仍缺 activeOrderId 到调拨、发货、补退料、批次/库存和 eDHR 放行检查的正式来源；不得用已有 WMS 基础表、API-only、静态合同或截图伪造 GREEN。
- 任何默认 `MAIN`、默认人员、默认系数或来源未接入成功都会使对应 AC 不能验收。
- 后端 health 不可达时不能用静态合同、API-only 局部检查或历史截图替代真实 E2E 前置；本轮已通过 health 和 `real:check` 关闭该 runtime blocker。

## Open Questions

- activeOrderId 与 ERP 发货、补料、退料、库存批次的正式同步边界需要在 M4 前冻结。
- 日结和扩展责任范围涉及的产线、设备、订单范围模型需要在 M5 前确认表结构和后端授权口径。
