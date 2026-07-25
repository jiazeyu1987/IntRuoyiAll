# 20260724-batch-fda-audit-log-coverage

## Task Goal

按 FDA 追溯口径补齐批执行相关电子记录创建、修改、删除、责任归属、放行决策链和附件链路的不可篡改审计日志，并确保新增日志能在日志表格和批次追溯中体现。

## Milestones

- [x] M1: 定位现有批执行、附件、任务、放行、归档、操作日志与追溯展示链路。
- [x] M2: 先补 BDD 场景与失败验证，明确审计缺口。
- [x] M3: 实现后端审计事件/操作日志写入，覆盖本地状态样本创建、附件删除/预登记/保存、规则保存、候选签名完成、任务重派、放行预检。
- [x] M4: 确保审计日志能被现有日志表格与批次追溯查询到，补齐前端展示字段与批次维度查询。
- [x] M5: 运行针对性验证并记录 RED/GREEN 证据。
- [x] M6: 收尾清理、经验沉淀、任务状态完成。

## Expected Verification

- 后端针对性测试覆盖新增审计日志的 operationType、操作者、时间、对象、原因、前后状态/hash、关联批次/任务/附件/签名/幂等键/请求来源等字段。
- 追溯/日志查询接口返回新增 operationType，并包含批次维度筛选所需字段。
- 前端日志表格/批次追溯展示字段与后端返回数据一致。
- 无新增 fallback、降级、吞异常、默认成功或模拟成功路径。

## Current Status

completed

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
- GREEN: `mvn -pl yudao-module-mes -am -DskipTests compile` -> PASS，依赖模块同 reactor 构建后主代码编译通过。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrLocalStateSampleServiceTest#createLocalStateSample_writesExpectedStateCombination' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS。
- GREEN: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-write-sample.e2e.cjs` -> PASS，真实前端路径创建任务自有样本批次并在批次追溯操作审计中展示 `LOCAL_STATE_SAMPLE_CREATE`。
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
- 当前请求的写入型真实前端 E2E 已复跑成功；M6 cleanup、经验沉淀和任务状态更新已完成。


## E2E Verification Update - 2026-07-25

- BLOCKED: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-readonly.e2e.cjs` -> FAIL，真实前端只读 E2E 已登录 `芋道源码/admin` 并捕获前端真实授权请求头；扫描可见批次数 25、审计行数 10、权限范围阻塞批次数 24，未找到包含本任务新增 operationType 的可展示追溯样本。
- 证据：`doc\tasks\20260724-batch-fda-audit-log-coverage\test-results\operation-audit-trace-readonly\evidence.md`、`result.json`、`failure.png`。
- 状态：保持 `blocked`；本轮只读验证未创建、修改或删除业务数据，后续需要授权测试租户/测试账号/任务自有批次样本后才能完成追溯抽屉 E2E。

## Write E2E Regression Update - 2026-07-25 13:36 Asia/Shanghai

- 写入型真实 UI E2E 已在授权账号下从“临时状态样本 > 放行预检样本”创建任务自有批次样本。
- E2E RED：批次详情“追溯记录 > 操作审计”真实请求被 `BATCH_EXECUTION:900000000788` 对象级权限 scope 缺失拒绝。
- 根因修复：本地状态样本创建事务现为 batch task 创建并绑定 `BATCH_EXECUTION_TASK` / `AUDIT_VIEW` 权限 scope，并在 created-record audit payload 中记录 `permissionScopeId`。
- GREEN：静态契约 `edhr-fda-operation-audit-coverage-static.spec.cjs` PASS。
- BLOCKED：后端主代码编译仍被非本任务 route projection 编译错误阻塞，修复后的真实运行态 E2E 尚不能重跑确认。

## Final E2E Pass - 2026-07-25 15:25 Asia/Shanghai

- GREEN: `mvn -pl yudao-module-mes -am -DskipTests compile` -> PASS，依赖模块同 reactor 构建后主代码编译通过。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrLocalStateSampleServiceTest#createLocalStateSample_writesExpectedStateCombination' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，6 tests / 0 failures / 0 errors。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS。
- GREEN: Clean runtime jar build from `D:/IntRuoyiWorktree/20260724-batch-fda-audit-runtime` -> PASS，jar SHA256 `1DC505A97E6BD91F94F0D975A6F404E7469DAE92F1960833DD9DCE05B241DC35`。
- GREEN: Backend runtime health `http://127.0.0.1:48081/actuator/health` -> `UP`，PID `50968`，jar `D:/IntRuoyiWorktree/20260724-batch-fda-audit-runtime/IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`。
- GREEN: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-write-sample.e2e.cjs` -> PASS。
- E2E Sample: batchExecutionId=`900000000799`, batchExecutionCode=`EDHR-UI-SAMPLE-PRECHECK-20260725152451737`, operationAuditId=`18421`, operationType=`LOCAL_STATE_SAMPLE_CREATE`, auditHash=`d711fb2e57ab6f2b62af95731b15f94bf57a84280724b03ac6a2834be73ef205`.
- UI Trace Request: `/mes/pro/edhr-operation-audit/page?pageNo=1&pageSize=10&batchExecutionId=900000000799`; asserted `batchExecutionId` present and `objectType/objectId` absent.
- Evidence: `test-results\operation-audit-trace-write-sample\evidence.md`, `result.json`, `operation-audit-trace-write-sample.png`.
## E2E Re-run Pass - 2026-07-25 15:37 Asia/Shanghai

- GREEN: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-write-sample.e2e.cjs` -> PASS。
- E2E Sample: batchExecutionId=`900000000802`, batchExecutionCode=`EDHR-UI-SAMPLE-PRECHECK-20260725153739914`, operationAuditId=`18442`, operationType=`LOCAL_STATE_SAMPLE_CREATE`, auditHash=`abc32c392ed603186d44c89621a6960b029d0e7e993786d36e9f1cf3ac0160e3`.
- UI Trace Request: `/mes/pro/edhr-operation-audit/page?pageNo=1&pageSize=10&batchExecutionId=900000000802`; asserted `batchExecutionId` present and `objectType/objectId` absent.
- Evidence: `test-results\operation-audit-trace-write-sample\evidence.md`, `result.json`, `operation-audit-trace-write-sample.png`.
## Closeout - 2026-07-25 16:05 Asia/Shanghai

- Cleanup Preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-batch-fda-audit-log-coverage --mode preview` -> `status: ready`，blocked=`<none>`，warnings=`<none>`。
- Cleanup Apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-batch-fda-audit-log-coverage --mode apply` -> `status: applied`，保留 `task.md`、`execution-log.md`、`verification-report.md`，清理任务临时 evidence、Playwright 脚本和 runtime logs。
- Concurrent Dirty Baseline: `d719203b` / `工作区: 保存 FDA 收尾前并发脏区基线`，保存非本任务并发 artifact，未混入 FDA 任务实现。
- Final Status: completed；本任务正式验证摘要已保留在 `verification-report.md`，未保留明文密码、token 或临时运行日志。
## Runtime Worktree Cleanup - 2026-07-25 16:12 Asia/Shanghai

- Old Task Backend PID: `50968` no longer exists; no process was stopped during closeout.
- Current 48081 Listener: PID `29320` belongs to the main workspace backend jar under `E:\IntRuoyi\IntRuoyiBackend`; not stopped because it is not the task runtime process.
- Worktree Cleanup: `D:\IntRuoyiWorktree\20260724-batch-fda-audit-runtime` was clean detached HEAD and removed after `git worktree remove` hit Windows `Filename too long`; deletion used a verified `D:\IntRuoyiWorktree` child path with long-path prefix, then `git worktree prune`.
