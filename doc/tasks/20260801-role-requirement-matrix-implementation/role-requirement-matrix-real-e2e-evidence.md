# 岗位需求分解矩阵真实 E2E 前置证据

- Task ID: `20260801-role-requirement-matrix-implementation`
- Generated At: `2026-08-02T04:12:50.031Z`
- Status: `BLOCKED`
- Frontend: `http://127.0.0.1:8081`
- Backend: `http://127.0.0.1:48081`
- Tenant: `芋道源码`
- Data Prefix: `RRM-20260801-`

## Result

- BLOCKED: 12 prerequisite blockers remain.
- M3 gate note: RRM-BLK-017..025 no longer appear; remaining SOURCE blockers belong to M4/M5.
- SOURCE:CHECK_INSPECTION_RESULT -> CHECK_INSPECTION_RESULT 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_DEVIATION_CLOSED -> CHECK_DEVIATION_CLOSED 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_REWORK_CLOSED -> CHECK_REWORK_CLOSED 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_SCRAP_RECORDED -> CHECK_SCRAP_RECORDED 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:CHECK_INVENTORY_CONSISTENCY -> CHECK_INVENTORY_CONSISTENCY 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。
- SOURCE:activeOrderTransferRelation -> 缺少 activeOrderId 与调拨头/行的正式关系表或迁移。
- SOURCE:activeOrderShipmentSource -> 缺少 activeOrderId 与发货/交付事实的正式关系源。
- SOURCE:activeOrderReplenishmentReturnSource -> 缺少 activeOrderId 与补料/退料事实的正式关系源。
- SOURCE:activeOrderBatchTraceSource -> 缺少 activeOrderId 与物料批次/库存追溯的正式关系源。
- SOURCE:normalizeRecordBindingSlotTypeDefaultMain -> 工艺路线前端 normalizeRecordBindingSlotType 对缺失槽位默认 MAIN，存在把 formBindings/旧字段误归为批记录表单的风险。
- SOURCE:batchRecordFormNamesFormBindingsSeparation -> 批记录表单字段和 formBindings 同屏存在，M0 尚未用真实 E2E 证明二者不会互相替代。
- SOURCE:edhrRuntimeDefaultMainSlot -> eDHR 运行态仍在缺失 formSlotType 时默认 MAIN，正式批记录绑定缺失时应 fail-fast。
