# PQC Production Source Context Regression

## Bug Summary

PQC 页面已经取得本轮真实 `productionSubmitEventId`，但提交载荷仍要求前端自行提供
`deviceAccountId`、`deviceId` 和 `workstationId`。当前 PQC 工序响应没有完整设备上下文，
导致模板校验成功后在浏览器内同步抛出“缺少PQC正式提交上下文”，不会发送
`/mes/pro/feedback/frontline/device-account/pqc/submit`。

## Expected Behavior

`productionSubmitEventId` 是本轮生产执行链路的正式根。PQC 后端必须读取该
`PRODUCTION_SUBMIT` 事件冻结的设备账号、设备和工作站，并验证订单、路线、路线工序和
工序与 PQC 任务一致；前端不得猜测或伪造这些字段。

## Reproduction

- Real path: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real`
- Observed: PQC 模板验证接口业务码为 `0`，提交按钮可用，但未出现 PQC submit 请求，
  `productionSubmitEventId=127` 对应的 PQC 记录未创建。

## Root Cause

- `FrontlineFixedTemplatePanel.vue` 从 PQC 当前登录人、页面设备卡和路线工序工作站拼装设备上下文。
- `MesFrontlinePqcContextServiceImpl` 接收并直接使用客户端设备上下文，没有按
  `productionSubmitEventId` 读取和校验正式生产事件。
- 当前 PQC 工序候选的 `deviceId` 为空，路线工序工作站也不能替代生产提交事件快照。

## Regression Tests

- Backend: exact `productionSubmitEventId` supplies device account/device/workstation and rejects mismatched event identity.
- Frontend static contract: PQC submit payload only requires the production event root and does not infer source device context.

## RED

Pending.

## GREEN

Pending.

## Risk And Regression Scope

Scope is limited to PQC formal submission context resolution. Production submission, PQC task identity,
QA regulation item snapshots, idempotency, signatures, and process-pool persistence remain under their
existing fail-fast rules.

## Blockers And Follow-up

Pending targeted verification and real RRM E2E.

## PQC Leader Review Dialog Regression

### Bug Summary

PQC 组长真实复核弹窗要求正式 `reviewSignatureId`、
`reviewSignatureEmployeeUserId` 和 `reviewSignatureSnapshotJson`。旧 E2E 只填写复核说明，
导致浏览器表单校验阻止正式复核请求，弹窗持续打开并遮挡后续筛选重置按钮。

### Expected Behavior

页面批准和退回复核都必须填写正式签名上下文，捕获正式复核接口响应；业务成功后等待弹窗
关闭，失败时记录结构化 blocker 并主动关闭弹窗。

### Reproduction

- Real path: `run-rrm-real-e2e-local.ps1 -Mode Real`
- Observed: `result.json` 为 `status=FAILED`，复核弹窗 `textarea` 拦截后续“重置”按钮点击。

### Root Cause

E2E 与当前复核表单契约不同步，只填写 `reviewRemark`，没有填写三个必需签名字段。

### Regression Test

- `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs`

### RED

静态合同在第 755 行失败；退回补正断言错误要求弹窗先关闭、后发送复核请求。

### GREEN

静态合同已锁定真实顺序：填写签名、发送复核请求、等待弹窗关闭、进入补正修订；语法检查、
聚焦静态合同和本机安全包装静态合同均通过。

### Risk And Regression Scope

范围仅限真实 E2E 的 PQC 组长复核交互和证据归因，不改变生产复核接口、签名校验或业务状态机。

### Blockers And Follow-up

需运行安全包装 full real E2E，确认真实批准、退回、汇集和后续清理链路。

## AC-M21 Aggregate Runtime Closure Regression

### Bug Summary

PQC 组长批准复核进入后端过程检验汇集事务后，`MesPqcProcessInspectionAggregateDetailMapper.insert`
写入 `actual_inspection_quantity` 报缺列，导致页面真实批准链路返回 `系统异常`。

### Expected Behavior

既有运行库中的 `mes_pqc_process_inspection_aggregate_detail` 必须具备 AC-M21 汇集明细所需的
`active_order_id`、`route_version_id`、`actual_inspection_quantity`、唯一键和查询索引；批准复核时
后端应写入结构化汇集明细，不得因旧表形态失败。

### Reproduction

- Real path: `run-rrm-real-e2e-local.ps1 -Mode Real`
- Observed: `pqcLeaderReviewApprovedAndAggregated` 阻塞，后端日志显示汇集明细 insert 缺
  `actual_inspection_quantity`。

### Root Cause

AC-M20 已提前创建 `mes_pqc_process_inspection_aggregate_detail`，后续 AC-M21 使用
`CREATE TABLE IF NOT EXISTS`，因此不会修改既有表；本机库保留旧表结构，缺少 mapper 当前写入所需列。

### Regression Test

- `MesQaPqcSchemaTest#pqcProcessInspectionAggregateRuntimeClosureMustRepairExistingTable`
- `MesProcessPoolSchemaTest` 相邻 schema 契约

### RED:

`MesQaPqcSchemaTest` 先失败，原因是运行态闭合迁移文件不存在，无法证明既有表会被补齐。

### GREEN:

`mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
PASS；本机闭合迁移 apply PASS，post-verify 证明缺失列、NOT NULL 和索引均已修复。

### Verification

`ac-m21-runtime-closure-policy-gate.json` 为 `status=passed`；本机备份 SHA256 为
`8D9DD18114ED4BD603EA94CB504D76B3954660E6645C12FF8BE86F37342BF674`；回滚脚本为
`db-repair/acm21-aggregate-runtime-closure-rollback.sql`。

### Blockers

代码和本机 schema blocker 已解除；仍需重跑 full real E2E，证明真实 PQC 批准复核后汇集明细和只读汇集证据闭环。
