# Backend API Evidence

## Scope

T2 共享后端合同：生产放行申请生命周期加锁/CAS、统一状态与审计命令、ASCII 幂等键、结构化 blocker 异常响应，以及工作待办查询投影字段。

## Contract

- 申请持久状态只允许 `PQC_RELEASE_PENDING`、`PQC_RELEASE_REJECTED`、`REPORT_UPLOAD_PENDING`、`MANAGER_RELEASE_PENDING`、`RELEASED`。
- 状态变更按 `id + expectedVersion + expectedStatus` CAS，成功版本加一。
- 命中旧状态不推断，返回 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED`。
- `MesReleaseFlowBlockerException` 通过专用 Advice 返回非零 `CommonResult` 并保留 `data.blockers`。
- 工作待办响应的 `nodeType/nodeName/version` 是查询投影，不是工作待办持久化列。

## Validation

- 幂等键必填、长度 1 至 128、只允许可见 ASCII。
- 未定义状态转换、状态/版本竞争和旧数据均 fail fast。
- 所有目标响应 Long ID 显式序列化为十进制字符串。
- 当前 T2 不新增 Controller，也不改变现有权限入口；后续阶段仍须执行角色和待办候选权限校验。

## Required Dependencies

依赖 MIG-RF-1；不依赖外部服务、秘密或测试夹具。审计 recorder 是唯一共享端口，阶段服务必须在调用方事务中提供正式实现，禁止 no-op。

## BDD Scenarios

- BDD: 并发状态转换只成功一次 -> Given 申请状态和版本匹配 / When 两个处理者竞争转换 / Then 只有一个 CAS 成功，版本加一并记录审计命令。
- BDD: 结构化 blocker 不丢失 -> Given 正式来源缺失 / When 后端抛出生产放行 blocker / Then 响应 code 非零且 `data.blockers` 保持稳定结构。
- BDD: 不安全长 ID 保真 -> Given ID 大于 JavaScript 安全整数 / When 序列化工作待办或 blocker / Then JSON 中仍是完整十进制字符串。
- BDD: 幂等键拒绝非 ASCII -> Given 幂等键含中文 / When 校验命令 / Then 明确失败且不执行写入。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译只缺计划内 lifecycle/CAS/blocker/idempotency/projection 合同。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest,MesReleaseFlowCoreContractTest,MesReleaseFlowSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；15 tests，0 failures，0 errors，覆盖状态/CAS、旧状态阻塞、结构化 blocker、Long ID 字符串序列化、ASCII 幂等键、共享 schema 合同和角色候选解析。

## Verification

`MesReleaseFlowCoreContractTest`、`MesReleaseFlowSchemaContractTest`、角色 resolver 测试和角色 SQL 静态合同均已通过；`python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` 为 6 passed。

## Observability

审计命令冻结 requestId、idempotencyKey、tenantId、applicationId、关联对象、from/to 状态、actor、时间、来源快照、结果和 blockerType；不得记录密码、上传 token 或签署秘密。

## Blockers And Downstream Skills

T2 无功能阻塞。T3 至 T9 必须提供加入调用方事务的正式审计 recorder，并继续使用本共享合同；现有 eDHR 通用审计服务使用 `REQUIRES_NEW`，不能直接作为该 recorder。前端阶段需同步稳定 blockerType 和字符串 ID。

## T7 SP-3 Report Upload Contract

- 四份正式报告节点为 `INCOMING_INSPECTION_REPORT`、`STERILIZATION_REPORT`、`FINISHED_PRODUCT_INSPECTION_REPORT`、`FINISHED_PRODUCT_INSPECTION_RECORD`；共享待办查询按冻结候选、节点类型和批次过滤，并返回节点名称及申请当前版本。
- 附件准备和报告完成都要求申请、报告节点与工作待办一致，当前用户属于冻结候选，`expectedVersion` 匹配，且幂等键为可见 ASCII。灭菌报告额外要求正式灭菌批号。
- 报告附件同键同载荷可重放；同键异载荷、同文件异键、完成后覆盖及旧 special-node 写入口都明确阻塞，不提供 skip、默认成功或静默降级。
- 前三份完成只增加版本并保持 `REPORT_UPLOAD_PENDING`。第四份完成必须同时冻结四报告快照、初始化管理者阶段并以 CAS 推进 `MANAGER_RELEASE_PENDING`；正式 initializer 缺失或失败时整个调用失败。
- Controller 复用现有 special-node 路径，通过 `expectedVersion + idempotencyKey` 进入生产放行专用服务；响应中的 Java `Long` ID 保持十进制字符串。

### T7 BDD And TDD Evidence

- BDD: 冻结候选只能处理自己的四报告节点，附件不可覆盖，申请版本逐份推进；前三份不创建最终阶段，第四份与管理者放行事务和待办原子交接。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseReportServiceTest" test` -> FAIL；仅缺计划内 report service、node port、manager-stage initializer、command/evidence 合同。
- GREEN: T7 目标服务、Controller JSON、候选查询、持久化 CAS、阶段初始化和 special-node 防绕过组合 -> PASS；23 tests，0 failures，0 errors。
- GREEN: SP-2 PQC、共享核心合同、报告任务初始化和相邻候选查询回归 -> PASS；17 tests，0 failures，0 errors。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules。
- observability：完成回执记录申请、批次节点、工作待办、版本、幂等键、附件 ID/哈希、报告快照哈希和阶段交接结果；不记录签署密码或上传秘密。
- blocker：实现与本地验证无功能阻塞。Git 提交仍受共享 branch runtime guard 的无关 worktree 非法 slot 20 登记阻塞；暂存区为空，未绕过、未 push。

## T5 SP-2 Contract

- `POST /mes/pro/production-release/pqc/approve` requires `applicationId`, `pqcReleaseWorkTaskId`, `expectedVersion`, visible-ASCII `idempotencyKey`, and optional opinion; it requires both the frozen PQC candidate snapshot and the current enabled `MES_PQC_RELEASE_OWNER` candidate set.
- `POST /mes/pro/production-release/pqc/reject` requires the same CAS identity and a 1..500 character rejection reason; it has no batch, dossier, or report-stage side effect.
- `GET /mes/pro/production-release/get?applicationId={id}` reads the authoritative persisted receipt. Decision receipt fields preserve all Java `Long` identifiers as decimal strings at the controller boundary.
- Approve writes `REPORT_UPLOAD_PENDING`, one `PQC_RELEASE:{applicationId}` batch context, three formal evidence sets, and exactly four frozen report upload tasks in one caller transaction. The batch entry refuses an existing same-context mismatch and blocks legacy same-work-order/batch/route executions with `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`.
- The dossier adapter requires formal `batchRecordReportId` bindings for `BATCH_RECORD`, `PROCESS_INSPECTION`, and `LOSS_REPORT`; `formBindings` cannot substitute any of these sources. The loss writer must return a formal evidence id even for zero loss.
- The report stage initializer requires exactly `INCOMING_INSPECTION_REPORT`, `STERILIZATION_REPORT`, `FINISHED_PRODUCT_INSPECTION_REPORT`, and `FINISHED_PRODUCT_INSPECTION_RECORD`; every task must have one batch task, one active `FILL` work task and a non-empty enabled frozen owner snapshot.

### T5 BDD And TDD Evidence

- BDD: PQC approve creates the unique batch and four report tasks only after the pending PQC decision is authorized and version-matched.
- BDD: PQC reject is terminal and does not create downstream objects.
- BDD: role plus frozen-candidate authorization, formal-source-only mappings, legacy batch migration blocking, same-key replay/conflict, and report-stage failure-before-CAS are all covered by targeted tests.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseBatchExecutionServiceTest#pqcApproveCreatesBatchExecutionOnlyAfterPqcRelease" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；only the planned SP-2 contracts were missing at the initial test compile.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesPqcReleaseBatchExecutionServiceTest" test` -> PASS；9 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseBatchExecutionPortTest,MesProductionReleaseReportStageInitializerTest,MesProductionReleaseControllerJsonTest" test` -> PASS；16 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseBatchExecutionPortTest,MesProductionReleaseReportStageInitializerTest,MesProductionReleaseControllerJsonTest,MesTeamLeaderActiveOrderReleaseAuditRecorderTest" test` -> PASS；19 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseApplySp1Test,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesReleaseFlowCoreContractTest,MesProEdhrBatchExecutionServiceTest#openOrCreate_mustGenerateSpecialNoTemplateNodesAroundRouteForms+detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes" test` -> PASS；46 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules。
- GREEN: prerequisite integration regression `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest,MesReleaseFlowCoreContractTest,MesReleaseFlowSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；15 tests，0 failures，0 errors，0 skipped。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` -> PASS；6 passed。
- commit evidence：`3048b84e8 feat: add PQC production release backend flow`；精确包含 35 个 T5 后端/测试文件；前置提交 `28923b171` 已在当前分支祖先链中。

## T9 SP-4 Manager Release Contract

- 第四份报告完成后的 manager-stage provider 只解析当前租户 `MES_MANAGEMENT_REPRESENTATIVE` 角色，冻结启用成员到 `RELEASE_APPROVE` 待办；不读取 route rule，不比较用户名或固定用户 ID。
- provider 复核申请 ID、批次、`REPORT_UPLOAD_PENDING`、申请版本和四报告快照；同一批次已有放行事务时明确阻塞。成功时在调用方事务内创建 `PENDING_APPROVAL` 放行事务和角色待办，由 T7 随后 CAS 绑定并推进 `MANAGER_RELEASE_PENDING`。
- 最终批准必须携带 `releaseTransactionId + workTaskId + expectedVersion + visible-ASCII idempotencyKey + signoffEvidenceHash`。处理人必须同时属于当前启用的管理者代表角色和冻结待办候选，并提交已通过密码验证、与工作待办绑定的电子签名证据。
- 批准前重新从四个已批准批次节点解析附件 ID、SHA-256、附件版本和灭菌批号，按与 SP-3 相同算法重算报告快照；任一报告变化、缺失或不一致返回 `REPORT_SNAPSHOT_CHANGED`，不执行写入。
- 放行事务按 `id + expectedVersion + PENDING_APPROVAL` CAS，申请按 `id + currentVersion + MANAGER_RELEASE_PENDING + reportSnapshotHash + releaseTransactionId + workTaskId` CAS；待办完成、事件和 SP-4 审计加入同一事务，任一步失败整体回滚。
- 同键同载荷在 `RELEASED` 状态返回原回执；同键异载荷返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`；旧版本返回 `STATE_VERSION_CONFLICT`；目标申请的 reject/withdraw 返回 `UNSUPPORTED_RELEASE_ACTION`。
- `completedTraceOnly=true` 时服务端强制 `releaseStatus=RELEASED`，批次查询也必须存在 `release_status = 'RELEASED'` 的放行事务；归档或驳回批次不再作为单条件替代。

### T9 BDD And TDD Evidence

- BDD: 管理者角色和冻结候选双重授权 -> Given 当前用户既是启用管理者代表又在冻结待办候选中 / When 提交最终批准 / Then 才能进入快照复核和原子放行；任一条件不满足均无写入。
- BDD: 四报告快照必须保持不变 -> Given SP-3 冻结的四报告快照 / When 管理者批准前重新读取正式报告证据 / Then 只有四节点、附件哈希、版本和灭菌批号完全一致才可放行。
- BDD: 最终放行原子且可重放 -> Given `MANAGER_RELEASE_PENDING` 申请与 `PENDING_APPROVAL` 事务 / When 使用当前版本和同一幂等载荷批准 / Then 事务、申请、待办、事件和审计一次写入 `RELEASED`；同载荷重放返回原回执，异载荷或旧版本失败。
- BDD: 可追溯只显示已放行 -> Given 调用方传入 `completedTraceOnly=true` 并尝试改变状态参数 / When 查询可追溯列表 / Then 后端仍强制已放行事务双条件，非 `RELEASED` 不入列。
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProductionReleaseManagerStageInitializerTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionReleaseTraceContractTest" "-DforkCount=0" test` -> FAIL；测试编译仅缺 manager-stage provider、manager approval service 和共享报告快照合同，符合 T9 预期。
- GREEN: 同一 T9 定向命令 -> PASS；9 tests，0 failures，0 errors，覆盖角色/冻结候选、快照变化、旧版本、同键重放/冲突、禁用 reject/withdraw 和 trace 双条件。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProductionReleaseManagerStageInitializerTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionReleaseTraceContractTest,MesProEdhrReleaseServiceImplTest" "-DforkCount=0" test` -> PASS；35 tests，0 failures，0 errors。
- GREEN: release、SP-3 report、共享 core 和角色 resolver 组合回归 -> PASS；54 tests，0 failures，0 errors。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules。
- blocker：实现和本地验证无功能阻塞；Git 提交仍受共享 branch runtime guard 的无关 worktree 非法 slot 20 登记阻塞，暂存区保持为空，未绕过、未 push。
