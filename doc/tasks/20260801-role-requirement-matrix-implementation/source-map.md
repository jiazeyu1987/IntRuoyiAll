# M0 Source Map - 岗位需求分解矩阵

## Result

M0 source map 结论：`ACCEPTED_BY_REVISED_GATE`。规划包的 BDD/TDD/测试矩阵结构已经通过校验；M0 只负责识别并结构化冻结 SOURCE blocker，不要求清零属于 M1-M5 正式实现范围的 blocker。M1 activeOrderId authority source gate 已在 2026-08-02 验证关闭 RRM-BLK-001..007，M2 production coefficient snapshots source gate 已验证关闭 RRM-BLK-026..028，M3 QA/PQC source gate 已验证关闭 RRM-BLK-017..025；当前剩余缺口不得用 fallback、mock、默认值、API-only 或静态合同替代。

## Confirmed Sources

| 领域 | 状态 | 证据 | M0 结论 |
|---|---|---|---|
| ERP 生产订单同步 | `CONFIRMED_PARTIAL` | `MesKingdeeProductionOrderSyncServiceImpl` 通过 `ErpKingdeeProductionOrderClient` 拉取生产订单，并在创建/更新 `MesProWorkOrderDO` 时写入 `quantity`、`businessStatus`、计划时间等 ERP 快照；`20260513_kingdee_multi_sync.sql` 定义 `mes_kingdee_production_order_sync_record(work_order_id)`。 | 可作为 ERP 生产订单候选来源；M1 已冻结 ERP 固定数量快照，M2 已冻结逐工序生产系数和计划数量快照。 |
| 活跃订单表 | `CONFIRMED_M1_ACCEPTED` | `MesProcessPoolActiveOrderDO` 已包含 `routeId`、`routeVersionId`、`erpFixedQuantitySnapshot`、`businessStatus`、`@Version version`；`20260802_mes_process_pool_active_order_authority.sql` 删除旧 `leader_user_id` 唯一键并新增 `tenant_id + work_order_id + route_id + route_version_id + deleted` 唯一键。 | M1 activeOrderId authority schema/source gate 已通过；M2 使用独立逐工序快照表补齐生产系数和计划数量。 |
| 活跃订单逐工序目标快照 | `CONFIRMED_M2_ACCEPTED` | `mes_pro_process_pool_active_order_process_snapshot`、`MesProcessPoolActiveOrderProcessSnapshotDO`、`MesTeamLeaderOrderProcessTargetService` 保存并读取 `erpFixedQuantitySnapshot`、`productionQuantityFactorSnapshot`、`plannedQuantitySnapshot`；分配、手动确认和工序完成链路统一读取该目标数量。 | M2 production coefficient snapshots source gate 已通过；后续不得回退到 `MesProWorkOrderDO.quantity` 或默认系数作为工序目标量。 |
| PQC 活跃订单读取 | `CONFIRMED_M3_ACCEPTED` | `MesFrontlinePqcContextServiceImpl.listActiveOrders()` 和 `requireActiveOrder()` 已通过 `MesProcessPoolActiveOrderMapper` 读取统一 active order；`submitPqcInspection()` 已要求 `activeOrderId + pqcTaskId + regulationVersionId + inspectionType/businessDate/shiftCode/roundNo` 任务身份。 | M1 PQC 订单列表旧来源已关闭；M3 PQC 提交已切到统一 activeOrderId 和发布规程任务身份。 |
| 正式逐工序批记录绑定 | `CONFIRMED_PARTIAL` | `MesProRouteFlowProcessBatchRecordDO` 对应 `mes_pro_route_flow_process_batch_record`，包含 `routeProcessId`、`batchRecordReportId`、`batchRecordDefinitionId`、`batchRecordVersionId`、`formSlotType`；但 `RouteFlowGraphDesigner.vue` 和 `MesProEdhrBatchExecutionServiceImpl` 仍存在缺失槽位时默认 `MAIN` 的路径。 | 正式绑定表存在，但 M0 仍阻塞缺失槽位 fail-fast 和 `formBindings`/`batchRecordFormNames` 互不替代证明。 |
| 工艺路线三类配置分离 | `CONFIRMED_PARTIAL` | `RouteFlowGraphDesigner.vue` 同时存在 `batchRecordFormNames`、`formBindings`、`工序开始` 展示/保存路径。 | 代码有分离锚点，但真实 E2E 尚未证明三条链路互不替代；缺失槽位默认 `MAIN` 仍是 blocker。 |
| 生产系数来源 | `CONFIRMED_M2_ACCEPTED` | `mes_pro_route_flow_process_config.production_quantity_factor`、路线页面 `productionQuantityFactor`、`MesProScheduleOrderProcessDO.productionQuantityFactor/plannedQuantity`、`MesProcessPoolActiveOrderProcessSnapshotDO.productionQuantityFactorSnapshot/plannedQuantitySnapshot` 和正数校验存在；`MesProAutoScheduleServiceImpl` 缺失或非正系数时抛 `PRO_ROUTE_FLOW_CONFIG_PRODUCTION_QUANTITY_FACTOR_INVALID`。 | 来源、排产快照和 activeOrderId 逐工序目标快照已闭环；`real:check` 不再输出 RRM-BLK-026..028。 |
| WMS 调拨/库存/批次 | `CONFIRMED_PARTIAL` | `MesWmTransferDO/LineDO/DetailDO`、`MesWmMaterialStockDO`、`MesWmBatchDO` 表和服务存在；扫描未发现这些 WMS 模型持有统一 `activeOrderId`，部分模型仍只关联 `workOrderId` 或 `batchId`。 | 有 MES WMS 数据模型，但未确认 ERP 调拨申请、发货、补料、退料与活跃订单的正式关系源。 |
| MES QC 基础能力 | `CONFIRMED_M3_ACCEPTED` | 现有 `MesQcTemplateDO/ItemDO/IndicatorDO`、`MesQcIpqcDO`、`MesQcOqcDO`、`MesQcRqcDO`、`MesQcIndicatorResultDO` 保留为基础能力；M3 新增 `MesQaInspectionRegulationDO/VersionDO/ItemDO` 作为岗位矩阵 QA 规程正式来源。 | M3 不再用临时 QC 模板替代正式 QA 规程；发布规程、版本和明细已有独立模型。 |
| PQC 记录模型 | `CONFIRMED_M3_ACCEPTED` | M3 新增 `MesPqcInspectionTaskDO` 和 `MesPqcInspectionPieceDetailDO`，任务身份包含 activeOrder、route/version/process、regulationVersion、inspectionType、businessDate、shiftCode、roundNo、计划/实际数量和提交状态。 | 满足 M3 首检/巡检/末检、跨天/班次、规程快照、逐件提交和提交来源切换的 SOURCE gate。 |
| PQC 前端表单 | `CONFIRMED_M3_ACCEPTED` | `FrontlineFixedTemplatePanel.vue` 从 `selectedProcess.inspectionItems` 动态渲染检验项目，从 PQC task snapshot 读取 `inspectionType`、`roundNo` 和 `plannedInspectionQuantity`，提交 payload 携带 `activeOrderId/pqcTaskId/regulationVersionId` 等任务身份。 | 页面不再使用固定示例项目、默认 `PATROL`、默认检验数量 30 或默认损耗 1；M3 动态渲染 gate 已通过。 |
| eDHR 放行事务 | `CONFIRMED_PARTIAL` | `MesProEdhrReleaseServiceImpl`、`MesProEdhrReleaseTransactionDO`、`MesProEdhrReleaseCheckItemDO` 存在；`buildCheckItems()` 对 `INSPECTION_RESULT`、`DEVIATION_CLOSED`、`REWORK_CLOSED`、`SCRAP_RECORDED`、`INVENTORY_CONSISTENCY` 仍调用 `buildSourceNotIntegratedItem(...)`。 | 放行框架存在，但检验、偏差、返工、报废、库存五类关键来源仍是 blocker。 |
| 电子签名 | `CONFIRMED_LOCAL_TEST` | 批记录签名表、eDHR 签名页面、报工/复核签名字段存在；本机 M0 夹具已确认六角色账号和签名图片 ID `22..27`，文档不记录明文密码。 | 本机真实前置已满足；后续正式 E2E 仍必须使用授权账号并保持凭据不落文档。 |

## Blocking Sources

| 缺口 | 影响 | 必须补齐后才能进入 |
|---|---|---|
| ERP 调拨申请/调拨单/发货/补料/退料/物料批次与 activeOrderId 的正式关系源未确认 | M4 无法实现多调拨追溯和开工检查。 | M4 前置，M0 记录 blocker |
| 放行检验、偏差、返工、报废、库存来源仍为 `buildSourceNotIntegratedItem` | M4/M6 不能把放行 blocker 改写成 PASS。 | M4/M6 |
| activeOrderId 到调拨、发货、补料/退料、批次/库存追溯的正式关系源未确认 | M4 无法证明多调拨净额、开工检查和放行物料来源。 | M4 前置，M0 预检 blocker |
| 批记录绑定缺失槽位仍可默认 `MAIN`，且 `batchRecordFormNames`/`formBindings` 分离缺真实证明 | D14/M5 无法证明正式批记录表单不会被表单槽位或旧字段替代。 | M5/D14 前置，M0 预检 blocker |

## No-Fallback Decision

- 不把 `mes_pro_process_pool` 的 PQC 活跃行视为统一 activeOrderId；PQC 列表必须继续使用 `MesProcessPoolActiveOrderMapper`。
- 不把 MES WMS 基础表推断为 ERP 调拨申请/发货/补料/退料的正式来源。
- 不把现有 QC 模板/IPQC 页面推断为本需求的 QA 规程版本体系；M3 已新增独立 QA 规程版本模型，后续不得回退。
- 不把 `MesProProcessPoolPqcRecordDO` 的存在推断为 PQC 任务/规程快照/逐件明细已经满足；M3 已新增独立 PQC 任务和逐件明细模型。
- 不把 `FrontlineFixedTemplatePanel.vue` 的硬编码 PQC 项目、`PATROL` 和数量 `30` 当作可验收默认配置；M3 已改为规程任务快照驱动。
- 不把生产系数缺失时的默认 `1` 当作正式系数来源；M2 已移除自动排产默认系数路径，后续不得重新引入。
- 不把缺失 `formSlotType` 时默认 `MAIN` 当作正式逐工序批记录绑定。
- 不把 eDHR 放行占位 blocker 转成默认通过。
- 不把已存在的静态合同、API wrapper 或旧真实 E2E 当作 `role-requirement-matrix` 真实主链路 E2E。
