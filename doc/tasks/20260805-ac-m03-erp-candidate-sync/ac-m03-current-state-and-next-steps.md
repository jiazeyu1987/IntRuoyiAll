# AC-M03 当前状态与下一步

## 结论

AC-M03 当前已经从「仅有部分源码能力、缺少幂等冲突证明」推进到「后端订单同步正式源 ID 幂等和冲突路径已有定向测试证明」。但它仍不能标记为 `ACCEPTED`，因为角色需求矩阵里的 M6 真实 E2E 覆盖账本仍未补入 AC-M03 的真实动作、失败路径和只读核验证据。

## 已做到

- ERP 生产订单同步已经能拉取金蝶生产订单，创建或更新 MES 工单，并写入 `mes_kingdee_production_order_sync_record`。
- 本次修复前，系统会优先按 `billNo` 查工单；若同一 `fid + materialNumber` 后续携带新的 `billNo`，可能创建重复工单事实。
- 本次修复后，系统优先按同步记录的 `workOrderId` 定位既有 MES 工单，再校验新 `billNo` 是否被其它工单占用。
- 若同一 ERP 正式源 ID 对应的同步记录缺失 `workOrderId`、指向不存在工单，或新 `billNo` 与其它工单冲突，系统会 fail fast，不会静默改绑或生成重复事实。
- 调拨/批次追溯链路已有 `activeOrder + transfer + line + detail` 维度的 idempotency key，并通过服务测试和 schema 测试复验。

## 本次补强

- 增加订单同步用例：同一 `fid + materialNumber` 已有同步记录、`billNo` 变化时，必须更新原工单，`createdCount=0`。
- 增加冲突用例：同一来源记录的新 `billNo` 已属于其它工单时，必须抛出业务异常，不得更新工单或同步记录。
- 增加源字段校验：ERP `fid` 或物料编码为空时直接业务异常，避免生成 `null:xxx` 形式的伪正式 ID。
- 增加同批次源键去重：同一同步批次内重复的正式源键会计入 skipped，不再继续创建或更新事实。

## 还差什么

- 还没有把 AC-M03 写入 `role-requirement-matrix-real-flow.e2e.js` 的真实动作路径和 result ledger。
- 还没有用真实页面阶段证明「订单、调拨、发货、批次」全链路都按正式 ID 幂等同步。
- 还没有覆盖真实 E2E 下重复、乱序、冲突来源不生成重复事实的失败路径。
- 发货侧如果存在独立 ERP shipment 源表或接口，还需要继续确认正式来源 ID；不能用 WMS 调拨或批次表反推替代。

## 建议下一步

1. 为 AC-M03 增加 M6 action key，例如 `erpCandidateSyncIdempotent`，把订单同步和调拨/批次追溯证据接入矩阵真实流结果。
2. 查清发货侧正式 ERP 来源：若已有 shipment/dispatch 源 ID，则补对应静态或后端契约；若没有，记录正式 blocker，不能用调拨数据冒充发货。
3. 补真实 E2E 或 approved real-flow read-only verification：重复运行同步、乱序输入、冲突输入后，核验工单、同步记录、调拨追溯和批次事实没有重复。
4. 复跑角色需求矩阵 M6 覆盖脚本，确认 AC-M03 从 `UNCOVERED_BY_REAL_E2E` 至少推进到有明确 action/blocker 状态；只有全部动作和失败路径通过后才能标记 `ACCEPTED`。
