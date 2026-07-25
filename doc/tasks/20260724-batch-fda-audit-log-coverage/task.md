# 20260724-batch-fda-audit-log-coverage

## Task Goal

按 FDA 追溯口径补齐批执行相关电子记录创建、修改、删除、责任归属、放行决策链和附件链路的不可篡改审计日志，并确保新增日志能在日志表格和批次追溯中体现。

## Milestones

- [x] M1: 定位现有批执行、附件、任务、放行、归档、操作日志与追溯展示链路。
- [x] M2: 先补 BDD 场景与失败验证，明确审计缺口。
- [x] M3: 实现后端审计事件/操作日志写入，覆盖本地状态样本创建、附件删除/预登记/保存、规则保存、候选签名完成、任务重派、放行预检。
- [x] M4: 确保审计日志能被现有日志表格与批次追溯查询到，补齐前端展示字段与批次维度查询。
- [ ] M5: 运行针对性验证并记录 RED/GREEN 证据。
- [ ] M6: 收尾清理、经验沉淀、任务状态完成。

## Expected Verification

- 后端针对性测试覆盖新增审计日志的 operationType、操作者、时间、对象、原因、前后状态/hash、关联批次/任务/附件/签名/幂等键/请求来源等字段。
- 追溯/日志查询接口返回新增 operationType，并包含批次维度筛选所需字段。
- 前端日志表格/批次追溯展示字段与后端返回数据一致。
- 无新增 fallback、降级、吞异常、默认成功或模拟成功路径。

## Current Status

blocked

## Completed Work

- 复用现有 `mes_pro_edhr_operation_audit_event` 与 `mes_pro_edhr_release_transaction_event`，未新增数据库迁移。
- 新增/补强操作审计：`LOCAL_STATE_SAMPLE_CREATE`、`ATTACHMENT_PREPARE_UPLOAD`、`ATTACHMENT_PENDING_DELETE`、`ATTACHMENT_SAVE_PENDING`、`WORK_TASK_RULE_SAVE`、`CANDIDATE_SIGNATURE_COMPLETE`、`FILL_TASK_REASSIGN`。
- 放行预检新增 `PRECHECK` release transaction event 与 operation audit event。
- 特殊节点附件删除和保存待提交附件要求显式原因；附件审计记录 fileId、文件名、大小、sha256、storage path/config、前序 hash 与链 head hash。
- 候选签名完成记录签名位、完成任务、取消候选任务和 `associatedSignatureId`；非签名类新增日志显式记录 `associatedSignatureId=NOT_APPLICABLE`。
- 批次追溯的操作审计列表支持仅按 `batchExecutionId` 查询，避免被对象类型/对象 ID 过滤掉附件、任务、放行事务等日志。
- 前端新增 operation/release 标签、`PRECHECK` 类型、附件删除原因弹窗、保存待提交附件原因传参。

## Verification Summary

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，原因分别为本地状态样本创建缺 operationType、样本创建 metadata 缺 `permissionDecision/resultStatus`、后端审计控制器不支持仅 `batchExecutionId` 的批次追溯查询。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS。
- GREEN: `git -C E:\IntRuoyi diff --check` -> PASS，仅 CRLF warning。
- BLOCKED: `mvn -pl yudao-module-mes -DskipTests compile` -> FAIL，外部无关阻塞：`MesProRouteFlowConfigServiceImpl` 缺失 `resolveRecordbookEnabled(Boolean, String)`。
- BLOCKED: `pnpm -C IntRuoyiFronted ts:check` -> FAIL，外部无关阻塞：`src/views/dcc/controlled-file/browser/index.vue` 存在既有 id 类型错误。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，统一写入现有不可篡改操作审计/放行事件链路，并补齐批次追溯查询门禁。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md` 命中的 PowerShell/验证/收尾相关门禁。
- 已应用 `docs\powershell-memory.md`、`docs\powershell-preflight-lessons.md`、`docs\powershell-encoding.md`、`docs\task-closeout-rules.md`：PowerShell 未使用 `&&`，中文文件读取使用 `Get-Content -Encoding utf8`，中文写入使用 `apply_patch`，命令退出码和阻塞原因已记录。

## Blockers

- 后端全模块编译被非本任务文件 `IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\route\MesProRouteFlowConfigServiceImpl.java` 中缺失方法 `resolveRecordbookEnabled(Boolean, String)` 阻塞；按并行改动隔离规则未修复。
- 前端类型检查被非本任务 DCC 页面既有 `string | number` 与 `number/string` 类型不匹配阻塞；按并行改动隔离规则未修复。
- 因必需编译/类型检查阻塞，任务不能标记为 `completed`，也不提交。


## E2E Verification Update - 2026-07-25

- BLOCKED: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-readonly.e2e.cjs` -> FAIL，真实前端只读 E2E 已登录 `芋道源码/admin` 并捕获前端真实授权请求头；扫描可见批次数 25、审计行数 10、权限范围阻塞批次数 24，未找到包含本任务新增 operationType 的可展示追溯样本。
- 证据：`doc\tasks\20260724-batch-fda-audit-log-coverage\test-results\operation-audit-trace-readonly\evidence.md`、`result.json`、`failure.png`。
- 状态：保持 `blocked`；本轮只读验证未创建、修改或删除业务数据，后续需要授权测试租户/测试账号/任务自有批次样本后才能完成追溯抽屉 E2E。
