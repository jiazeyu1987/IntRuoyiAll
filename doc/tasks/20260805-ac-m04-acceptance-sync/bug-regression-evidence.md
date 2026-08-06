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
