# Frontend Feature Evidence

## Feature

确保新增 FDA 审计日志在 eDHR 操作日志表格和批次追溯中可见。

## Acceptance

- 批次追溯内嵌操作审计表格可仅按 `batchExecutionId` 查询，不再强制 objectType/objectId，避免过滤掉附件、任务、放行事务日志。
- 前端展示新增 operationType 标签：本地状态样本创建、附件上传预登记、待提交附件删除、待提交附件保存、工作任务规则保存、候选签名完成、填写任务重新派发、预检。
- release event 类型和标签包含 `PRECHECK`。
- 删除待提交附件时弹出原因输入框；保存待提交附件时向后端传递显式原因。

## BDD

- BDD: 批次追溯显示多对象审计日志 -> Given 批次下存在附件、任务、放行事务等不同 objectType 日志 When 打开批次追溯操作审计表格 Then 请求只带 batchExecutionId 并展示所有相关日志。
- BDD: 附件删除原因 -> Given 特殊节点存在待提交附件 When 用户删除附件 Then 前端必须输入删除原因并随请求发送。
- BDD: 放行前保存附件原因 -> Given 批次存在待提交附件 When 用户继续放行并确认保存 Then 请求包含保存原因。

## RED/GREEN

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，前端/后端批次追溯合同缺少批次维度无对象过滤支持。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS。

## Verification

- PASS: 静态契约确认 `OperationAuditListPane.vue` 在隐藏对象过滤器且存在 batchExecutionId 时不发送 objectType/objectId。
- PASS: 静态契约确认 `release.ts` 和 `releaseCheckPresentation.ts` 暴露 `PRECHECK`。
- PASS: 静态契约确认 `BatchExecutionDetailPage.vue` 使用 `ElMessageBox.prompt` 收集附件删除原因，保存待提交附件发送 `reason: '放行前保存待提交特殊节点附件'`。
- BLOCKED: `pnpm -C IntRuoyiFronted ts:check` 被非本任务 `src/views/dcc/controlled-file/browser/index.vue` 既有类型错误阻塞。

## Blockers

- 前端全量类型检查阻塞来自 DCC 文件既有 id 类型不匹配，未在本任务中修复。
