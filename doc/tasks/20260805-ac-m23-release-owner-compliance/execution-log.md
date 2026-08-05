# Execution Log

## User Intent

用户要求对 AC-M23「放行负责人审核并放行生产订单」剩余不符合项进行修复。

## BDD Scenarios

- BDD: 放行负责人签名放行并审计 -> Given 批次放行预检已通过且当前用户是 `RELEASE_APPROVE` 负责人 When 用户输入电子签名密码提交放行 Then 批次进入已放行、签名证据落库、事务事件和操作审计均记录成功。
- BDD: 放行负责人退回并审计 -> Given 批次放行预检已通过且当前用户是 `RELEASE_APPROVE` 负责人 When 用户填写退回原因提交退回 Then 放行事务进入退回状态、批次保持未放行、事务事件和操作审计均记录成功。
- BDD: 越权放行或退回被拒绝 -> Given 当前用户不是 `RELEASE_APPROVE` 负责人或审批任务候选人 When 调用放行或退回 Then 服务拒绝且不写签名、终态事务或成功审计。
- BDD: 缺签名或伪造签名证据被拒绝 -> Given 审批中心放行批准请求缺少当前用户真实签名记录 When 调用审批放行 Then 服务拒绝且不写终态事务。
- BDD: 预检未通过或重复终态被拒绝 -> Given 放行预检未通过或事务已终态 When 用户提交放行/审批/退回 Then 服务 fail fast 且不写第二个终态签名。

## Commands And Evidence

- Skill: bug-regression-fix-loop, backend-api-delivery, frontend-feature-delivery loaded.
- Gate: docs/task-closeout-rules.md, docs/backend-development.md, docs/frontend-development.md, docs/e2e-rules.md, docs/powershell-encoding.md read.
- Experience gate: docs/experience-index.md read; applicable eDHR release owner, frontend static isolation, E2E entry gates copied into task.md.

## Current Status

in_progress

