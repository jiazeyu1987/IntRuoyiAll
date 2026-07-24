# Backend API Evidence

## Scope

批执行 FDA 口径操作审计写入与追溯查询覆盖。

## Contract

- 审计写入沿用 `MesProEdhrOperationAuditService`，落库至 `mes_pro_edhr_operation_audit_event`，保留 previous/current audit hash 链。
- 新增 operationType：`LOCAL_STATE_SAMPLE_CREATE`、`ATTACHMENT_PREPARE_UPLOAD`、`ATTACHMENT_PENDING_DELETE`、`ATTACHMENT_SAVE_PENDING`、`WORK_TASK_RULE_SAVE`、`CANDIDATE_SIGNATURE_COMPLETE`、`FILL_TASK_REASSIGN`、`PRECHECK`。
- 放行预检同时写 `mes_pro_edhr_release_transaction_event` 的 `PRECHECK` 事件。
- 附件删除/保存请求增加必填 `reason`，缺失原因 fail fast。
- 批次追溯查询允许仅传 `batchExecutionId`，后端仍通过批次权限能力校验，不再要求 objectType/objectId。

## Validation

- 附件受监管审计使用真实登录用户，登录缺失时抛出 `UNAUTHORIZED`，不使用 `actor=0`。
- 新增日志 metadata 包含 `requestSource`、`idempotencyKey`、`associatedSignatureId`、`permissionDecision`、`resultStatus`、`reason`。
- 操作审计命令包含对象、批次/执行/任务上下文、权限判定、结果状态、before/after summary hash。

## BDD

- BDD: 本地状态样本创建审计 -> Given 本地状态样本创建批次与任务 When 创建完成 Then 写入批次维度可追溯的 `LOCAL_STATE_SAMPLE_CREATE`。
- BDD: 附件电子记录审计 -> Given 特殊节点附件预登记、删除、入账 When 状态变化 Then 写入附件标识、hash、原因和链 head hash。
- BDD: 责任/签名归属审计 -> Given 规则保存、候选签名完成、填写任务重派 When 责任归属变化 Then 写入前后值、签名绑定、取消候选和授权同步结果。
- BDD: 放行预检审计 -> Given 放行预检写检查项和事务状态 When 预检完成 Then 写 operation audit 与 release transaction event。
- BDD: 批次追溯查询 -> Given 操作审计表格嵌入批次追溯 When 仅按 batchExecutionId 查询 Then 返回该批次所有相关 objectType 日志。

## RED/GREEN

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，缺本地状态样本审计、强制 metadata 字段和批次追溯门禁覆盖。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS。

## Verification

- PASS: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs`。
- PASS: `git -C E:\IntRuoyi diff --check`，仅 CRLF warning。
- BLOCKED: `mvn -pl yudao-module-mes -DskipTests compile` 被 `MesProRouteFlowConfigServiceImpl.resolveRecordbookEnabled(Boolean, String)` 缺失阻塞。

## Blockers

- 后端编译阻塞来自非本任务并行改动文件 `MesProRouteFlowConfigServiceImpl.java`，未在本任务中修复。
