# Execution Log

## User Intent

按 FDA 追溯口径补齐批执行相关电子记录创建、修改、删除、责任归属、放行决策链和附件链路审计日志，并要求新增日志在日志表格和批次追溯中体现。

## Experience Gate

- 读取 `docs\experience-index.md` 后应用 PowerShell/编码/收尾门禁。
- 读取 `docs\powershell-memory.md`、`docs\powershell-preflight-lessons.md`、`docs\powershell-encoding.md`、`docs\task-closeout-rules.md`。
- 命令执行未使用 `&&`；中文文档读取使用 UTF-8；中文写入使用 `apply_patch`。

## BDD Scenarios

- BDD: 本地状态样本创建审计 -> Given 系统创建批次、任务、放行事务、检查项、待办和归档记录 When 本地状态样本创建完成 Then 记录可按批次追溯的审计事件，包含操作者、时间、对象、动作、原因、权限、结果、关联签名占位和幂等键。
- BDD: 特殊节点待提交附件删除审计 -> Given 特殊节点存在 PENDING 附件 When 操作者删除附件 Then 记录附件删除日志，包含删除人、时间、fileId、文件名、大小、sha256、存储路径、所属批次/任务和删除原因。
- BDD: 工作任务规则保存审计 -> Given 归档员、关闭人、放行审批负责人规则会影响责任或签名归属 When 操作者保存规则 Then 记录规则前后值、操作者、权限判定、原因和结果状态。
- BDD: 候选签名任务完成审计 -> Given 同一签名位有多个候选任务 When 某候选签名任务完成 Then 记录完成人、签名位、被取消候选任务、原因、时间和关联签名 ID。
- BDD: 填写任务重新派发审计 -> Given 填写任务已有责任人 When 任务重新派发给新责任人 Then 记录原责任人、新责任人、重派原因、授权同步结果、通知结果和运行态授权变化。
- BDD: 附件上传预登记审计 -> Given 操作者预登记特殊节点附件上传 When PENDING 附件记录创建 Then 记录 ATTACHMENT_PREPARE_UPLOAD，包含 fileId、hash、存储配置、链路前序 hash、附件链 head hash 和请求来源。
- BDD: 保存待提交特殊节点附件审计 -> Given PENDING 附件转为正式 ADD 附件 When 操作者保存待提交附件 Then 记录 ATTACHMENT_SAVE_PENDING，包含转入账前后 hash、附件清单和任务 payload hash。
- BDD: 放行预检审计 -> Given 放行事务执行预检并写入检查项 When 预检完成或阻断 Then 记录 PRECHECK 事务事件，包含检查项数量、失败项、阻断项、操作者和状态变化。
- BDD: 批次追溯展示新增审计日志 -> Given 批次追溯嵌入操作审计表格 When 仅按 batchExecutionId 查询 Then 后端按批次权限放行查询，列表展示该批次下不同 objectType 的新增日志。

## Command Log

- 初始化任务记录：识别并沿用 `doc/tasks/20260724-batch-fda-audit-log-coverage/`。
- 验证现有契约：运行 `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs`。
- 后端编译验证：运行 `mvn -pl yudao-module-mes -DskipTests compile`。
- 前端类型验证：运行 `pnpm -C IntRuoyiFronted ts:check`。
- 格式验证：运行 `git -C E:\IntRuoyi diff --check`。
- 技能证据验证：计划运行后端、数据库、前端 evidence validator。

## RED/GREEN Evidence

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，本地状态样本创建缺少 `LOCAL_STATE_SAMPLE_CREATE` 静态覆盖。
- GREEN: 实现本地状态样本、附件、工作任务、放行预检审计写入后，同一静态契约 -> PASS。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，新增强制字段契约发现本地状态样本 metadata 未显式包含 `permissionDecision/resultStatus`。
- GREEN: 补齐本地状态样本 metadata 后，同一静态契约 -> PASS。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，新增批次追溯契约发现操作审计控制器仍要求 objectType/objectId。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS，控制器支持仅 `batchExecutionId` 进行批次追溯查询并仍走批次权限能力校验。
- GREEN: `git -C E:\IntRuoyi diff --check` -> PASS，仅 CRLF warning。

## Verification

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS: `PASS: eDHR FDA operation audit coverage static contract`。
- `git -C E:\IntRuoyi diff --check` -> PASS，仅报告 CRLF warning。
- `mvn -pl yudao-module-mes -DskipTests compile` -> BLOCKED，`MesProRouteFlowConfigServiceImpl.java:[603,45]` 与 `[707,45]` 找不到 `resolveRecordbookEnabled(Boolean, String)`。
- `pnpm -C IntRuoyiFronted ts:check` -> BLOCKED，`src/views/dcc/controlled-file/browser/index.vue` 第 1431、1432、1472-1480、1548-1569、1697、1717 行存在既有类型不匹配。

## Blockers

- 后端全量编译和前端类型检查被非本任务并行改动阻塞；本轮按任务边界未修复无关文件。
- 因必需验证阻塞，当前状态保持 `blocked`，不提交、不标记 completed。


## E2E Verification - 2026-07-25

- BDD: 批次追溯操作审计真实只读 E2E -> Given 账号通过真实前端登录并定位到含新增 FDA 审计 operationType 的批次 When 打开批次详情并点击“追溯记录 > 操作审计” Then 前端仅按 batchExecutionId 查询操作审计，表格展示新增审计动作标签且不发送 objectType/objectId。
- Command: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-readonly.e2e.cjs`。
- BLOCKED: 命令通过 Playwright 完成真实前端登录和授权上下文捕获，但最近可见 25 个批次未找到含本任务新增 operationType 的可验证追溯样本；24 个批次返回 `eDHR 对象级权限范围不存在或未启用：BATCH_EXECUTION:<id>`，已扫描审计行数 10。
- Evidence: `doc\tasks\20260724-batch-fda-audit-log-coverage\test-results\operation-audit-trace-readonly\evidence.md`、`result.json`、`failure.png`。
- Guardrail: 本轮未使用 mock、API-only 替代页面路径、直接 SQL 或写入型造数；缺少测试租户/测试账号/任务自有样本前保持 `blocked`。
