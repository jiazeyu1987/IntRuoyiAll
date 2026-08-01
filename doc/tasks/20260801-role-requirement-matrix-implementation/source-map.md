# M0 Source Map - 岗位需求分解矩阵

## Result

M0 source map 结论：`BLOCKED`。规划包的 BDD/TDD/测试矩阵结构已经通过校验，但当前代码和运行前置仍不能支撑进入 M1。以下缺口不得用 fallback、mock、默认值、API-only 或静态合同替代。

## Confirmed Sources

| 领域 | 状态 | 证据 | M0 结论 |
|---|---|---|---|
| ERP 生产订单同步 | `CONFIRMED_PARTIAL` | `MesKingdeeProductionOrderSyncServiceImpl` 通过 `ErpKingdeeProductionOrderClient` 拉取生产订单，并在创建/更新 `MesProWorkOrderDO` 时写入 `quantity`、`businessStatus`、计划时间等 ERP 快照；`20260513_kingdee_multi_sync.sql` 定义 `mes_kingdee_production_order_sync_record(work_order_id)`。 | 可作为 ERP 生产订单候选来源，但还未满足 M1 要求的统一活跃订单身份和跨链路 activeOrderId。 |
| 活跃订单表 | `CONFIRMED_INADEQUATE` | `MesProcessPoolActiveOrderDO` 当前只有 `leaderUserId/workOrderId/activeStatus/joinedAt/removedAt`；缺少 `routeId`、`routeVersionId`、ERP 固定数量快照、`businessStatus`、乐观锁版本等字段；`20260731_mes_process_pool_team_leader_p1_runtime_config.sql` 唯一键为 `tenant_id + leader_user_id + work_order_id + deleted`。 | 当前是生产组长范围下的活跃订单，不是跨生产/PQC/批记录/放行统一 activeOrderId。 |
| PQC 活跃订单读取 | `BLOCKED` | `MesFrontlinePqcContextServiceImpl.listActiveOrders()` 仍通过 `processPoolMapper.selectActiveList()` 读取 `mes_pro_process_pool` 活跃行；`submitPqcInspection()` 还要求 `selectActiveByWorkOrderRouteProcess(...)` 命中最新生产事件。 | 不满足“PQC 只通过统一活跃订单服务读取”的 M1/M3 前置，且无生产事件时无法独立生成 PQC 任务。 |
| 正式逐工序批记录绑定 | `CONFIRMED_PARTIAL` | `MesProRouteFlowProcessBatchRecordDO` 对应 `mes_pro_route_flow_process_batch_record`，包含 `routeProcessId`、`batchRecordReportId`、`batchRecordDefinitionId`、`batchRecordVersionId`、`formSlotType`；但 `RouteFlowGraphDesigner.vue` 和 `MesProEdhrBatchExecutionServiceImpl` 仍存在缺失槽位时默认 `MAIN` 的路径。 | 正式绑定表存在，但 M0 仍阻塞缺失槽位 fail-fast 和 `formBindings`/`batchRecordFormNames` 互不替代证明。 |
| 工艺路线三类配置分离 | `CONFIRMED_PARTIAL` | `RouteFlowGraphDesigner.vue` 同时存在 `batchRecordFormNames`、`formBindings`、`工序开始` 展示/保存路径。 | 代码有分离锚点，但真实 E2E 尚未证明三条链路互不替代；缺失槽位默认 `MAIN` 仍是 blocker。 |
| 生产系数来源 | `CONFIRMED_PARTIAL` | `mes_pro_route_flow_process_config.production_quantity_factor`、路线页面 `productionQuantityFactor`、`MesProScheduleOrderProcessDO.productionQuantityFactor/plannedQuantity` 和正数校验存在。 | 来源和排产快照锚点存在；但统一 activeOrderId 缺生产系数/计划数量快照，自动排产仍在缺失系数时默认 `1`。 |
| WMS 调拨/库存/批次 | `CONFIRMED_PARTIAL` | `MesWmTransferDO/LineDO/DetailDO`、`MesWmMaterialStockDO`、`MesWmBatchDO` 表和服务存在；扫描未发现这些 WMS 模型持有统一 `activeOrderId`，部分模型仍只关联 `workOrderId` 或 `batchId`。 | 有 MES WMS 数据模型，但未确认 ERP 调拨申请、发货、补料、退料与活跃订单的正式关系源。 |
| MES QC 基础能力 | `CONFIRMED_PARTIAL` | `MesQcTemplateDO/ItemDO/IndicatorDO`、`MesQcIpqcDO`、`MesQcOqcDO`、`MesQcRqcDO`、`MesQcIndicatorResultDO` 及对应服务/测试存在。 | 只能说明现有 QC 基础表和流程存在；不能直接证明本需求要求的 QA 规程所有权、发布版本、PQC 任务身份和逐件明细模型已具备。 |
| PQC 记录模型 | `CONFIRMED_INADEQUATE` | `MesProProcessPoolPqcRecordDO` 存在，但当前扫描未发现 `inspectionType`、`businessDate`、`shiftCode`、`roundNo`、`regulationVersionId`、计划/实际检验数量、逐件明细、复核状态等字段。 | 不满足 M3 首检/巡检/末检、跨天/班次、规程快照、逐件提交和复核闭环。 |
| PQC 前端表单 | `CONFIRMED_INADEQUATE` | `FrontlineFixedTemplatePanel.vue` 仍固定 `length/appearance/seal/pressure`，默认 `inspectionType='PATROL'`，默认 `inspectionQuantity=30`，并由前端本地构造逐件值。 | 页面仍是固定示例项目/默认数量，不满足按发布规程动态渲染和 fail-fast 阻塞缺规程。 |
| eDHR 放行事务 | `CONFIRMED_PARTIAL` | `MesProEdhrReleaseServiceImpl`、`MesProEdhrReleaseTransactionDO`、`MesProEdhrReleaseCheckItemDO` 存在；`buildCheckItems()` 对 `INSPECTION_RESULT`、`DEVIATION_CLOSED`、`REWORK_CLOSED`、`SCRAP_RECORDED`、`INVENTORY_CONSISTENCY` 仍调用 `buildSourceNotIntegratedItem(...)`。 | 放行框架存在，但检验、偏差、返工、报废、库存五类关键来源仍是 blocker。 |
| 电子签名 | `CONFIRMED_PARTIAL` | 批记录签名表、eDHR 签名页面、报工/复核签名字段存在。 | 签名链路锚点存在；本任务六角色签名账号和签名凭据未确认。 |

## Blocking Sources

| 缺口 | 影响 | 必须补齐后才能进入 |
|---|---|---|
| 统一 activeOrderId 的正式服务和 schema 未具备 | M1 不能证明生产、PQC、批记录、放行读取同一订单身份。 | M1 |
| PQC 提交仍依赖 `mes_pro_process_pool` 最新生产事件 | M3 无法证明 PQC 可按统一活跃订单和发布规程独立生成任务。 | M3 |
| ERP 调拨申请/调拨单/发货/补料/退料/物料批次与 activeOrderId 的正式关系源未确认 | M4 无法实现多调拨追溯和开工检查。 | M4 前置，M0 记录 blocker |
| QA 规程所有权未冻结为 MES 或 QMS 唯一写入口 | M3 不能设计唯一规程版本、发布、任务生成和历史快照。 | M3 前置，M0 记录 blocker |
| PQC 任务/规程版本/逐件明细正式模型未具备 | M3 无法实现首检/巡检/末检、逐件明细、自我确认阻塞。 | M3 |
| PQC 前端仍硬编码项目、巡检类型和默认检验数量 | M3 页面 RED 当前是业务缺口，不能用静态脚本或默认值替代规程驱动渲染。 | M3 |
| 放行检验、偏差、返工、报废、库存来源仍为 `buildSourceNotIntegratedItem` | M4/M6 不能把放行 blocker 改写成 PASS。 | M4/M6 |
| 六角色真实账号、权限、电子签名和任务专用测试数据未确认 | 真实 Playwright E2E 不能开始。 | M0 |
| activeOrderId 到调拨、发货、补料/退料、批次/库存追溯的正式关系源未确认 | M4 无法证明多调拨净额、开工检查和放行物料来源。 | M4 前置，M0 预检 blocker |
| QA 规程归属、规程版本、PQC 任务身份和逐件明细模型未确认 | M3 无法证明规程版本、任务生成、逐件明细和历史快照。 | M3 前置，M0 预检 blocker |
| activeOrderId 缺生产系数和计划数量快照，自动排产仍有默认系数路径 | M2 无法证明 200/50/10 等分配数量按正式系数冻结，不能靠默认 `1` 继续。 | M2 前置，M0 预检 blocker |
| 批记录绑定缺失槽位仍可默认 `MAIN`，且 `batchRecordFormNames`/`formBindings` 分离缺真实证明 | D14/M2 无法证明正式批记录表单不会被表单槽位或旧字段替代。 | M2/D14 前置，M0 预检 blocker |

## No-Fallback Decision

- 不把 `mes_pro_process_pool` 的 PQC 活跃行视为统一 activeOrderId。
- 不把 MES WMS 基础表推断为 ERP 调拨申请/发货/补料/退料的正式来源。
- 不把现有 QC 模板/IPQC 页面推断为本需求的 QA 规程版本体系。
- 不把 `MesProProcessPoolPqcRecordDO` 的存在推断为 PQC 任务/规程快照/逐件明细已经满足。
- 不把 `FrontlineFixedTemplatePanel.vue` 的硬编码 PQC 项目、`PATROL` 和数量 `30` 当作可验收默认配置。
- 不把生产系数缺失时的默认 `1` 当作正式系数来源。
- 不把缺失 `formSlotType` 时默认 `MAIN` 当作正式逐工序批记录绑定。
- 不把 eDHR 放行占位 blocker 转成默认通过。
- 不把已存在的静态合同、API wrapper 或旧真实 E2E 当作 `role-requirement-matrix` 真实主链路 E2E。
