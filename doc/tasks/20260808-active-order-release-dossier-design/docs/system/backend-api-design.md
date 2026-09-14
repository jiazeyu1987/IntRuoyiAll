# 活跃订单放行资料后端设计 V2

## Purpose and Scope

本文定义后端如何实现生产组长手动申请放行资料。核心不是“看到双 100% 就填资料”，而是校验双 100% 背后的生产/PQC 历史数据，并把这些历史数据按正式映射写入 eDHR 批次执行中的批记录、过程检验单和损耗单。

## Evidence Reviewed

- PRD V2。
- 用户关于真实历史数据、QA 文件、批记录表单、人员签名和测试数据的澄清。
- 当前 MES/eDHR 已有活跃订单、生产报工、PQC、批记录、工作任务和放行基础链路。
- 项目要求缺正式来源时 fail fast，不做 fallback。

## Modules

- `MesTeamLeaderActiveOrderReleaseApplicationService`：第一版唯一申请编排入口，负责调用现有活跃订单、生产历史、PQC 汇集、eDHR 批次、批记录写入和放行待办能力。
- 现有能力复用：前端申请入口、申请接口、申请记录、eDHR 批次创建、`submitForApproval` 和 `MesTeamLeaderBatchRecordBackfillServiceImpl` 已有基础；第一版只补缺口和集成，不重复建设平行主流程。
- 包内内部方法/小组件：
  - `checkReadiness(...)`：校验双 100% 来源、历史表单、签名和完成性。
  - `collectProductionSources(...)`：读取生产历史、生产历史表单和生产组长确认。
  - `collectPqcSources(...)`：读取 PQC 历史、PQC 历史表单、PQC 组长复核和汇集明细。
  - `mapAndWriteDossier(...)`：按已确认承载写批记录、过程检验单和损耗单。
  - `submitForReleaseApproval(...)`：创建或复用生产负责人待办。
- `ActiveOrderReleaseApplicationMapper`：保存申请、幂等、状态、blocker 和摘要。
- 后续拆分条件：当单类映射超过可维护范围或多个入口复用同一映射时，再提取独立 source/mapping/writer service。

## API Contracts

### POST /mes/pro/process-pool/team-leader/active-order/release/apply

请求：

```json
{
  "activeOrderId": 10001,
  "idempotencyKey": "AO-REL-10001-20260808-001",
  "applyRemark": "双进度完成，申请负责人放行"
}
```

响应：

```json
{
  "applicationId": 50001,
  "activeOrderId": 10001,
  "status": "PENDING_RELEASE_APPROVAL",
  "batchExecutionId": 30001,
  "releaseTransactionId": 40001,
  "releaseApprovalWorkTaskId": 60001,
  "dossierSummary": {
    "batchRecordCount": 3,
    "processInspectionFormCount": 1,
    "lossReportFormCount": 1,
    "signatureEvidenceCount": 6,
    "sourceSnapshotHash": "sha256..."
  },
  "blockers": []
}
```

阻塞响应：

```json
{
  "applicationId": 50002,
  "activeOrderId": 10001,
  "status": "BLOCKED",
  "blockers": [
    {
      "blockerType": "PQC_QA_ITEM_MISSING",
      "objectType": "ROUTE_PROCESS",
      "objectId": "9001",
      "objectCode": "PROC-PRESSURE",
      "reason": "PQC 历史数据缺少 QA 文件要求的检验项目",
      "suggestion": "请补齐该工序 PQC 检验记录并由 PQC 组长复核",
      "fieldCode": "pressure_value"
    }
  ]
}
```

### GET /mes/pro/process-pool/team-leader/active-order/release/get

用于读取某活跃订单最近申请状态、blocker、资料摘要和待办关联。

### Active Order List Extension

活跃订单列表扩展：

- `releaseApplicationStatus`
- `releaseApplicationBlockerSummary`
- `releaseBatchExecutionId`
- `releaseApprovalWorkTaskId`
- `releaseApplicationUpdatedAt`

## Error Model

- `ACTIVE_ORDER_NOT_FOUND`：活跃订单不存在。
- `ACTIVE_ORDER_LEADER_MISMATCH`：不是当前生产组长负责。
- `ACTIVE_ORDER_PROGRESS_SOURCE_INCOMPLETE`：双 100% 无法追溯到真实历史数据。
- `PRODUCTION_HISTORY_FORM_INCOMPLETE`：生产历史表单缺必填。
- `PRODUCTION_DEVICE_PARAMETER_INVALID`：设备或设备参数不符合批记录表单要求。
- `PQC_QA_CONSTRAINT_MISMATCH`：PQC 数据不符合产品 QA 文件约束。
- `PQC_AGGREGATE_DETAIL_REQUIRED`：缺 PQC 汇集明细。
- `BATCH_RECORD_BINDING_REQUIRED`：缺正式批记录绑定。
- `DOSSIER_FIELD_MAPPING_REQUIRED`：缺字段映射。
- `LOSS_REPORT_SOURCE_REQUIRED`：缺损耗来源或损耗映射。
- `SIGNATURE_SOURCE_REQUIRED`：缺填写/审核签名来源。
- `RELEASE_OWNER_REQUIRED`：缺生产负责人。
- `RELEASE_APPLICATION_CONFLICT`：幂等或并发冲突。

## Transactions and Idempotency

- 请求幂等：`tenantId + activeOrderId + idempotencyKey`。
- 业务幂等：`tenantId + activeOrderId + workOrderId + routeVersionId + sourceSnapshotHash`。
- 事务包含申请记录、批次执行、正式表单写入、字段审计、放行事务、负责人待办和操作审计。
- 任一正式资料写入失败，整体回滚或进入明确 blocker，不留下半生成待办。
- 已存在同来源快照申请时返回既有对象。

## First Version Simplification

- 第一版不重建完整进度计算体系；只核验当前双 100% 背后的正式来源记录、历史表单、汇集明细和签名证据是否存在且匹配当前活跃订单。
- 第一版不为过程检验单/损耗单做猜测性适配；正式承载未确认时返回 blocker。
- 第一版优先复用现有字段审计和操作审计；不默认新增字段映射快照表。
- 第一版默认复用 `RELEASE_APPROVE` 负责人规则；除非确认现有规则不能表达生产负责人，否则不新增第二套配置。
- 第一版接口沿用当前 `idempotencyKey`、`dossierSummary.sourceSnapshotHash`、`blockerType/reason/suggestion` 字段，不新增 `clientRequestId` 和必填 `generatedDocuments[]`。

## Source Verification Flow

1. 校验当前用户是活跃订单生产组长。
2. 读取活跃订单、生产工单、产品、路线版本和工序快照。
3. 从生产历史与表单核验生产完成。
4. 从 PQC 历史与表单核验检验完成。
5. 校验 QA 文件和批记录表单约束。
6. 生成来源快照 hash。
7. 校验字段映射和签名来源。
8. 创建/复用批次执行。
9. 调用生产批记录、过程检验单和损耗单正式 writer 写入资料。
10. 完成性检查通过后执行 release precheck。
11. 通过 `submitForApproval` 创建或复用生产负责人待办。

## Open Questions

- OQ-01 过程检验单和损耗单在当前 eDHR 里的正式任务类型和 report category 名称；未确认前首版只能验收 blocker 或已确认部分。
- OQ-02 生产组长申请是否需要单独电子签名。
- OQ-03 生产负责人来源是否完全复用 `RELEASE_APPROVE`。

## Design Blockers

- DB-01 没有正式损耗单字段映射时，不能验收损耗单成功生成。
- DB-02 没有能制造历史生产/PQC 数据的测试 fixture 时，不能验收成功路径。
- DB-03 放行待审批状态不能与已放行状态混用。
