# 岗位需求分解矩阵真实 E2E 前置证据

- Task ID: `20260801-role-requirement-matrix-implementation`
- Generated At: `2026-08-01T17:55:56.999Z`
- Status: `BLOCKED`
- Frontend: `http://127.0.0.1:8081`
- Backend: `http://127.0.0.1:48081`
- Tenant: `芋道源码`
- Data Prefix: `RRM-20260801-`

## Result

- BLOCKED: 31 prerequisite blockers remain.
- SOURCE:MesProcessPoolActiveOrderDO.routeId -> 统一 activeOrderId 仍缺 routeId 字段。
- SOURCE:MesProcessPoolActiveOrderDO.routeVersionId -> 统一 activeOrderId 仍缺 routeVersionId 字段。
- SOURCE:MesProcessPoolActiveOrderDO.erpFixedQuantitySnapshot -> 统一 activeOrderId 仍缺 erpFixedQuantitySnapshot 字段。
- SOURCE:MesProcessPoolActiveOrderDO.businessStatus -> 统一 activeOrderId 仍缺 businessStatus 字段。
- SOURCE:MesProcessPoolActiveOrderDO.version -> 统一 activeOrderId 仍缺 version 字段。
- SOURCE:uk_mes_pp_active_order -> 活跃订单唯一键仍绑定 leader_user_id，不能作为跨角色统一订单身份。
- SOURCE:processPoolMapper.selectActiveList -> PQC 仍通过 mes_pro_process_pool 活跃行读取订单，未切换统一 activeOrderId。
- SOURCE:CHECK_INSPECTION_RESULT -> CHECK_INSPECTION_RESULT 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_DEVIATION_CLOSED -> CHECK_DEVIATION_CLOSED 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_REWORK_CLOSED -> CHECK_REWORK_CLOSED 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_SCRAP_RECORDED -> CHECK_SCRAP_RECORDED 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_INVENTORY_CONSISTENCY -> CHECK_INVENTORY_CONSISTENCY 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:activeOrderTransferRelation -> 缺少 activeOrderId 与调拨头/行的正式关系表或迁移。
- SOURCE:activeOrderShipmentSource -> 缺少 activeOrderId 与发货/交付事实的正式关系源。
- SOURCE:activeOrderReplenishmentReturnSource -> 缺少 activeOrderId 与补料/退料事实的正式关系源。
- SOURCE:activeOrderBatchTraceSource -> 缺少 activeOrderId 与物料批次/库存追溯的正式关系源。
- SOURCE:qaRegulationOwnership -> QA 规程唯一所有权和正式表/API 未冻结。
- SOURCE:qaRegulationVersionModel -> QA 规程发布版本模型未确认，不能证明发布后不可原地修改。
- SOURCE:pqcTaskModel -> PQC 任务身份模型未具备检验类型、日期、班次、轮次和规程版本。
- SOURCE:pqcPieceDetailModel -> PQC 逐件明细正式模型未确认，不能证明逐件可还原。
- SOURCE:selectActiveByWorkOrderRouteProcess -> PQC 提交仍依赖最新 mes_pro_process_pool 生产事件，未按统一 activeOrderId 和发布规程任务独立提交。
- SOURCE:hardcodedPqcInspectionItems -> PQC 前端仍硬编码 length/appearance/seal/pressure 检验项目，未按发布规程动态渲染。
- SOURCE:defaultPqcInspectionType -> PQC 前端仍默认 PATROL，未从规程任务身份读取检验类型。
- SOURCE:defaultPqcInspectionQuantity -> PQC 前端仍默认检验数量 30，未从规程任务计划数量读取。
- SOURCE:defaultPqcScrapQuantity -> PQC 前端仍默认损耗数量 1，未由实际检验结果或规程规则驱动。
- SOURCE:activeOrderProductionQuantityFactorSnapshot -> 统一 activeOrderId 模型未保存生产系数快照，后续分配/完成/PQC 无法以同一订单身份复核系数。
- SOURCE:activeOrderPlannedQuantitySnapshot -> 统一 activeOrderId 模型未保存按生产系数计算后的计划数量快照。
- SOURCE:defaultProductionQuantityFactorInAutoSchedule -> 自动排产仍在生产系数缺失时默认使用 DEFAULT_PRODUCTION_QUANTITY_FACTOR，M2 前必须改为正式配置缺失即失败。
- SOURCE:normalizeRecordBindingSlotTypeDefaultMain -> 工艺路线前端 normalizeRecordBindingSlotType 对缺失槽位默认 MAIN，存在把 formBindings/旧字段误归为批记录表单的风险。
- SOURCE:batchRecordFormNamesFormBindingsSeparation -> 批记录表单字段和 formBindings 同屏存在，M0 尚未用真实 E2E 证明二者不会互相替代。
- SOURCE:edhrRuntimeDefaultMainSlot -> eDHR 运行态仍在缺失 formSlotType 时默认 MAIN，正式批记录绑定缺失时应 fail-fast。

