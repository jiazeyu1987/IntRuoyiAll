# Verification Report

## Scope

批执行 FDA 审计日志覆盖：本地状态样本创建、特殊节点附件预登记/删除/保存、工作任务规则保存、候选签名完成、填写任务重新派发、放行预检，以及日志表格/批次追溯展示链路。

## Implementation Evidence

- 操作审计写入点：
  - `MesProEdhrLocalStateSampleServiceImpl` -> `LOCAL_STATE_SAMPLE_CREATE`
  - `MesProEdhrBatchExecutionServiceImpl` -> `ATTACHMENT_PREPARE_UPLOAD`、`ATTACHMENT_PENDING_DELETE`、`ATTACHMENT_SAVE_PENDING`
  - `MesProEdhrWorkTaskServiceImpl` -> `WORK_TASK_RULE_SAVE`、`CANDIDATE_SIGNATURE_COMPLETE`、`FILL_TASK_REASSIGN`
  - `MesProEdhrReleaseServiceImpl` -> `PRECHECK` operation audit 与 release transaction event
- 批次追溯：
  - `MesProEdhrOperationAuditController` 允许仅按 `batchExecutionId` 授权查询。
  - `MesProEdhrOperationAuditEventMapper` 已支持按 `batchExecutionId` 过滤。
  - `OperationAuditListPane.vue` 在批次追溯嵌入场景不再发送 objectType/objectId。
- 前端展示：
  - `releaseCheckPresentation.ts` 新增对象/操作/release event 标签。
  - `release.ts` 新增 `PRECHECK` release event 类型。
  - `BatchExecutionDetailPage.vue` 附件删除/保存请求包含原因。

## Command Results

- PASS: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs`
  - 输出：`PASS: eDHR FDA operation audit coverage static contract`
- PASS: `git -C E:\IntRuoyi diff --check`
  - 输出：仅 CRLF warning，无 whitespace error。
- BLOCKED: `mvn -pl yudao-module-mes -DskipTests compile`
  - 阻塞：`MesProRouteFlowConfigServiceImpl.java:[603,45]` 与 `[707,45]` 找不到 `resolveRecordbookEnabled(Boolean, String)`。
- BLOCKED: `pnpm -C IntRuoyiFronted ts:check`
  - 阻塞：`src/views/dcc/controlled-file/browser/index.vue` 第 1431、1432、1472-1480、1548-1569、1697、1717 行存在既有 id 类型不匹配。

## Compliance Field Check

- 新增日志均包含或可由审计表字段承载：`batchExecutionId`、`executionId`、`workTaskId`、`objectType/objectId`、`operationType`、操作者、操作时间、权限判定、结果状态、before/after hash、metadataJson。
- 新增 metadata 补齐：`requestSource`、`idempotencyKey`、`associatedSignatureId`、`reason`。
- 附件 metadata 补齐：`fileId`、文件名、大小、`sha256`、`storageConfigId`、`storagePath`、前序附件 hash、附件链 head hash、删除/入账原因。
- 候选签名完成记录真实签名绑定 `executionId:signatureCellKey`；非签名动作记录 `associatedSignatureId=NOT_APPLICABLE`。

## Final Status

ready_for_closeout

## Remaining Blockers

- 本轮请求的写入型真实 E2E 已 PASS；原后端编译阻塞已通过 `-am` 同 reactor 构建验证解除。
- 仍有非本任务工作区脏改动与全仓 `diff --check` 阻塞：`doc/tasks/20260724-batch-execution-published-route-runtime-update/real-e2e-evidence.md` 存在 EOF 空行问题，按任务隔离规则未修改。
- 当前任务仍待收尾：任务自有 runtime worktree `D:/IntRuoyiWorktree/20260724-batch-fda-audit-runtime` 与后端 PID `50968` 需要在 closeout 阶段处理。


## E2E Verification - 2026-07-25

- BLOCKED: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-readonly.e2e.cjs`。
- Result: 真实前端只读 E2E 已登录本机前端并捕获前端真实授权请求头；扫描可见批次数 25、审计行数 10、权限范围阻塞批次数 24，未找到包含本任务新增 operationType 的可展示追溯样本。
- Evidence: `doc\tasks\20260724-batch-fda-audit-log-coverage\test-results\operation-audit-trace-readonly\evidence.md`、`result.json`、`failure.png`。
- Remaining blocker: 需要提供或创建经授权的测试租户/测试账号/任务自有批次样本，使该批次包含本任务新增 operationType 的操作审计记录并具备 `BATCH_EXECUTION:<id>` 对象级 VIEW 权限，才能完成真实前端追溯抽屉 E2E；本轮只读验证未造数。

## Write E2E Regression Verification - 2026-07-25 13:36 Asia/Shanghai

- Status: BLOCKED after source fix; real E2E reproduced the missing permission scope defect, but full runtime re-verification cannot proceed until unrelated backend compile blockers are resolved and backend runtime is rebuilt/restarted.
- RED E2E: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-write-sample.e2e.cjs` -> FAIL. Real frontend path created an owned local PRECHECK sample, then batch trace operation audit failed with `BATCH_EXECUTION:900000000788` permission scope missing.
- Fix: `MesProEdhrLocalStateSampleServiceImpl` now saves a `BATCH_EXECUTION_TASK` permission scope with `AUDIT_VIEW=ALLOW` for the creating user, writes the returned scope ID to `MesProEdhrBatchExecutionTaskDO.permissionScopeId`, and includes the scope ID in created-record audit payload.
- Regression Test: `MesProEdhrLocalStateSampleServiceTest#createLocalStateSample_writesExpectedStateCombination` now asserts the created batch task has the returned permission scope ID. Target JUnit execution is blocked before tests run by unrelated test compilation errors.
- GREEN Static: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS.
- BLOCKED Compile: `mvn -pl yudao-module-mes -DskipTests compile` -> FAIL in unrelated `MesProRouteVersionPublishProjectionServiceImpl.java:[842,17]` because `BusinessApprovalPolicyDOBuilder.formPolicyType(String)` is unavailable.
- No fallback: The E2E was not replaced with SQL/API-only verification, mock data, or direct permission repair.

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
