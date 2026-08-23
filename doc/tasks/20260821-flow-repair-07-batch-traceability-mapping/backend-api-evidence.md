# 流程7后端 API 实施证据

## Endpoint and Scope

目标接口为流程6成功后调用的 `captureBatchExecutionTrace` 内部命令，以及批次追溯详情、列表和 manifest 读取接口。流程7只写 BatchExecutionOrigin、TraceLink、Manifest，不拥有完成、回填、建批、四材料或最终 RELEASED。

## Contract and Data

活跃订单入口必须消费 `activeOrderId + completionTransactionId + completionBackfillReceiptId/hash + pickListBindingId + sourceSnapshotHash + batchExecutionId`；独立、手工、排产入口必须消费各自正式凭证。所有来源按正式 ID、版本和快照 hash 消费，缺失或冲突返回稳定 blocker，不按工单号、批号、名称或当前投影猜测。

## Auth and Failure Behavior

查询权限按批次、订单和审计 scope 控制；写入仅允许流程7内部端口/事件消费。缺 `batchExecutionId` 返回 `BATCH_PROVISION_REQUIRED`，来源冲突返回 `TRACE_SOURCE_CONFLICT`，关系缺失保持 `TRACE_MAPPING_BLOCKED`，不得返回默认成功。

## Required Inputs and Blockers

- 流程1正式领料绑定及头/分录快照、稳定 `sourceEntryId` 尚未落地。
- 流程4不可变 completionBackfillReceipt 尚未落地。
- 流程6当前仍使用 `applicationId` 的旧放行建批端口，未提供 active-order completion 建批结果。
- 流程5正式 NO_LOSS/LOSS receipt 尚未冻结。
- 流程8/10四材料 manifest、权限和最终放行消费合同尚未落码。
- 当前任务目录已补齐 task-local `prd.md`；若运行器要求 `docs/tasks/<task-id>` 的固定路径，需另行确认路径映射，但不影响本切片文档合同。

## BDD and TDD Evidence

`BDD: batch provision is prerequisite -> Given Tx-A receipt exists When flow 6 has not returned batchExecutionId Then flow 7 returns BATCH_PROVISION_REQUIRED and writes no mapping.`

`BDD: active order origin does not require release application -> Given flow 4 receipt and flow 6 batchExecutionId exist When flow 7 captures trace Then origin uses activeOrderId/completionBackfillReceipt/pickListBindingId and releaseApplicationId is optional.`

`BDD: missing formal material source blocks trace -> Given a required source ID or hash is absent When capture is requested Then TRACE_MAPPING_BLOCKED is returned and no manifest is sealed.`

`RED: static source contract scan -> FAIL, batch DO/service/controller/SQL contain no complete active-order, pick-list, receipt, origin/manifest contract.`

Historical GREEN: PASS for the isolated Flow7 slice; the validator run executes 13 tests with 0 failures and 0 errors, and the validator plus service-contract run executes 15 tests with 0 failures and 0 errors. This record is superseded by the latest 18-test run and does not close missing upstream contracts or schema ownership.

REGRESSION: NOT RUN, no service was started, no database migration was executed, and no cross-flow or write E2E was run.

## Observability

Every capture attempt must log entry type, idempotency key, source bundle hash, blocker code and batch ID without storing secrets or signature plaintext. This cannot be implemented until the upstream receipt and schema identities are frozen.

## Validation

服务、权限、来源 hash 和幂等键必须由后端权威校验；未提供正式 receipt 或 schema 时不得继续。

## Verification

本轮完成静态 RED 审计并运行流程7 validator 定向 Maven 测试；未启动服务、未执行数据库迁移或写入型 E2E。

## 2026-08-22 Slice Evidence

已落地 MesProEdhrBatchTraceabilityService、批次控制器的 capture/detail/release-decision 路由、严格入口校验和 append-only manifest 追加。该实现只接受已存在的 batchExecutionId 和流程6 provision receipt；活跃订单入口以 completion/backfill/pick-list 正式 ID/hash 为核心，releaseApplicationId 仅可在后置 release-decision 关系中出现。

当前增量门禁：无损耗 Origin 必须使用 `NO_LOSS_CONFIRMED` 正式关系；有损耗必须同时提供 `LOSS_FACT(HAS_LOSS)` 与 `LOSS_REPORT_RECEIPT`。详情读取校验 manifest canonical hash 链和逐 Origin 关系完整性，失败返回 `TRACE_MAPPING_BLOCKED`。

历史定向测试记录为 validator 13 tests + service contract 2 tests，共 15 tests；该记录已被后续结果 supersede。GREEN 仅覆盖流程7独立 slice，REGRESSION 仍为 NOT RUN。跨线程真实集成、权限对象范围和流程10 gate 仍是 blocker。追溯详情和只读 `/traceability/manifest` 路由现在同时返回最新 manifest 与完整 manifestHistory；manifest 封存载荷覆盖 Origin/TraceLink 的完整关系字段，且流程6 batch provision receipt 以独立 BATCH_PROVISION_RECEIPT 来源关系参与校验。

## Blockers

上游流程1/4/5/6/8/9/10 尚未落地正式凭证与 schema，因此无法进入完整跨流程 GREEN；当前仅有流程7 validator slice GREEN。本任务 task-local `prd.md` 已补齐。
来源身份校验已固定为正式 link/object/line/event ID 组合，sourceVersion 不参与唯一性；caller-supplied sourceIdentityKey 不能定义身份，若非空必须与后端 canonical identity 完全一致，否则返回 TRACE_SOURCE_CONFLICT；同一正式来源不同快照 hash 会 fail-fast。

最新 GREEN：validator 13 tests + service contract 5 tests，共 18 tests、0 failures、0 errors、BUILD SUCCESS；service contract 覆盖显式 originId 归属、按 RELEASE_DECISION TraceLink 的 releaseApplicationId 列表筛选和映射缺失门禁。REGRESSION 仍为 NOT RUN。

当前门禁补强：详情服务按 Origin 逐一确认对应 TraceLink 和入口必需关系集合；非空但孤立/缺类型的关系图返回 `TRACE_MAPPING_BLOCKED`，不改变 releaseApplicationId 仅存在于后置 RELEASE_DECISION TraceLink 的契约。

## 2026-08-22 后置放行关系修订

后置 release-decision 命令要求显式 originId，并校验该 Origin 属于当前 batchExecutionId；多 Origin 不取首条记录猜测。traceability/list 的 releaseApplicationId 条件通过 RELEASE_DECISION TraceLink 的 sourceObjectId 查询 batchExecutionId，再与 Origin 条件交集，不依赖 Origin 上不存在的放行申请列。该修订及映射缺失门禁已由 service contract 5 tests 验证；真实 Mapper、数据库和权限回归仍未运行。

历史定向 GREEN：validator 16 tests + service contract 7 tests，共 23 tests；该结果已被后续 25-test 结果 supersede，覆盖 `PQC_INDEPENDENT` canonical 独立入口、损耗条件、manifest hash 链、逐 Origin 关系门禁及后置放行归属。REGRESSION 仍为 NOT RUN。

## 2026-08-22 最新来源身份完整性验证

- BDD: caller-supplied sourceIdentityKey 只能作为一致性证据，不能覆盖 canonical formal identity；不一致时 fail-fast。
- RED: 新增 mismatch 场景首次运行暴露 validator 未检查调用方 sourceIdentityKey；新增篡改 TraceLink identity/snapshot 场景首次运行暴露详情门禁只检查关系类型。
- GREEN: 绝对路径 Maven 合并定向命令通过，validator 17 + service contract 8，共 25 tests，0 failures、0 errors、BUILD SUCCESS。
- REGRESSION: NOT RUN；真实 Mapper、数据库迁移/触发器、对象级权限和跨流程流程8/10 gate 未执行。

## 2026-08-23 Tx-C Producer and Main Workspace Verification (authoritative)

- Tx-C producer entry is `POST /mes/pro/edhr-batch-execution/traceability/tx-c`; its request is a witness/idempotency envelope only. The producer reads the successful Flow6 `OPEN` audit, Flow1 binding header/items and formal Flow4/2/3/5 source evidence from persistence under the current tenant. It does not accept client-supplied Origin/TraceLink/Manifest/source payloads.
- Success writes Origin/TraceLink/Manifest and `mes_pro_edhr_batch_trace_outbox_event` in one transaction, then publishes `FLOW7_TRACE_MAPPING_SUCCEEDED` after commit. Failure rolls back graph writes and commits a separate retryable/final outbox event with `TRACE_MAPPING_BLOCKED`; precheck mutation is `SOURCE_CHANGED_AFTER_PRECHECK`. Flow6 remains the `BATCH_READY` owner.
- `GREEN: mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests compile -> PASS` and focused test command -> PASS; 29 focused tests (validator 17 + service contract 12) completed with 0 failures/errors/skips and `BUILD SUCCESS`. The latest test invocation used `-Dmaven.testResources.skip=true` due to an old fixture lock; full resource-copy verification remains `NOT RUN`.
- This proves the task-owned implementation slice only. Service startup, real Mapper/database/permission runtime, upstream formal receipt adapters, Flow8 material gate, Flow10 final release and write-enabled E2E remain `NOT RUN`.
- Evidence validators: `validate_backend_api.py -> PASS`; task-local documentation structure and SQL static scans also pass. These are static checks and do not replace runtime or cross-flow verification.
