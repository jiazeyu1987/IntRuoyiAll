# Execution Log

## User Intent

按 FDA 追溯口径补齐批执行相关电子记录创建、修改、删除、责任归属、放行决策链和附件链路审计日志，并要求新增日志在日志表格和批次追溯中体现。

## BDD Scenarios

- BDD: 本地状态样本创建审计 -> Given 系统创建批次、任务、放行事务、检查项、待办和归档记录 When 本地状态样本创建完成 Then 记录可按批次追溯的审计事件，包含操作者、时间、对象、动作、原因、权限、结果和幂等键。
- BDD: 特殊节点待提交附件删除审计 -> Given 特殊节点存在 PENDING 附件 When 操作者删除附件 Then 记录附件删除日志，包含删除人、时间、fileId、文件名、大小、sha256、存储路径、所属批次/任务和删除原因。
- BDD: 工作任务规则保存审计 -> Given 归档员、关闭人、放行审批负责人规则会影响责任或签名归属 When 操作者保存规则 Then 记录规则前后值、操作者、权限判定、原因和结果状态。
- BDD: 候选签名任务完成审计 -> Given 同一签名位有多个候选任务 When 某候选签名任务完成 Then 记录完成人、签名位、被取消候选任务、原因、时间和关联签名 ID。
- BDD: 填写任务重新派发审计 -> Given 填写任务已有责任人 When 任务重新派发给新责任人 Then 记录原责任人、新责任人、重派原因、授权同步结果、通知结果和运行态授权变化。
- BDD: 附件上传预登记审计 -> Given 操作者预登记特殊节点附件上传 When PENDING 附件记录创建 Then 记录 ATTACHMENT_PREPARE_UPLOAD，包含 fileId、hash、存储配置、链路前序 hash 和请求来源。
- BDD: 保存待提交特殊节点附件审计 -> Given PENDING 附件转为正式 ADD 附件 When 操作者保存待提交附件 Then 记录 ATTACHMENT_SAVE_PENDING，包含转入账前后 hash、附件清单和任务 payload hash。
- BDD: 放行预检审计 -> Given 放行事务执行预检并写入检查项 When 预检完成或阻断 Then 记录 PRECHECK 事务事件，包含检查项数量、失败项、阻断项、操作者和状态变化。

## Command Log

- 初始化任务记录：创建 `doc/tasks/20260724-batch-fda-audit-log-coverage/task.md` 与 `execution-log.md`。

## RED/GREEN Evidence

- RED: pending
- GREEN: pending

## Blockers

- `docs\experience-index.md` 缺失；已按用户明确任务要求登记风险后继续。
