# 建批权威凭证链专项验收清单

## 目的与范围

本清单用于流程6/9提交后的集成验证，覆盖 active-order 与 independent 两条建批入口、统一凭证解析、来源持久化和流程7消费边界。流程11只负责验收编排、BDD/TDD证据和失败归属，不修改流程6/9业务代码，不写数据库，不使用 mock、API-only 或默认成功替代真实建批路径。

## 已核对证据

- 流程6合同：active-order 只能消费流程4 BACKFILL_SUCCEEDED receipt；独立入口只能消费流程9服务端签发的 IndependentBatchPrerequisiteReceipt，流程6只接收 receiptId/正式验证结果。
- 流程9合同：服务端按租户重新读取 receipt，校验 canonical payload、payloadHash、receiptHash、签名、entryType、sourceSnapshotHash、有效期和撤销状态；客户端 nested receipt 不可信。
- 当前 int_main 基线：628fb8a99。本清单不宣称流程6/9尚未融合前的历史结果为当前建批通过。

## BDD 验收场景

### A1 活跃订单仅接受成功回填 receipt

Given 活跃订单双进度已达 100%，流程1正式领料绑定、流程2/3来源事实和流程4 BACKFILL_SUCCEEDED receiptId 均属于当前租户；When 任一活跃订单入口调用统一建批解析器；Then 创建或复用唯一 batchExecutionId，持久化 completionBackfillReceiptId、receipt hash 和 source snapshot hash，并允许流程7继续消费。

### A2 活跃订单缺失或失败 receipt 阻断

Given receiptId 缺失、未知、租户不符、状态非 BACKFILL_SUCCEEDED 或仅存在 CompletionBackfillFailureAttempt；When 调用建批；Then fail fast，返回稳定阻断码，不创建/复用批次，不写入来源关系。

### A3 独立入口仅接受服务端签发 receipt

Given MANUAL、SCHEDULED 或 PQC_INDEPENDENT 请求携带流程9服务端签发、未过期、未撤销的 receiptId 和正式 batchExecutionSourceRelation；When 调用统一建批解析器；Then 创建或复用唯一批次，持久化 sourceCredentialId、receipt hash 和 source snapshot hash，且不伪造 activeOrderId。

### A4 独立 receipt 生命周期阻断

Given receipt 已过期、已撤销、未知、签名/hash 被篡改或 entryType/sourceSnapshotHash 不匹配；When 调用建批；Then 由流程9验真阻断，流程6不创建/复用批次。

### A5 nested receipt 与业务字段篡改阻断

Given 请求体附带 forged nested receipt，或篡改 workOrderId、routeId/routeVersionId、batchCode、sourceSnapshotHash、正式来源 relation；When 调用建批；Then 服务端以 receiptId 重新读取权威记录，发现任一不一致即返回稳定错误，不信任请求体，不创建/复用批次。

### A6 跨租户阻断

Given receipt 属于租户 A，调用上下文为租户 B；When 任一入口建批；Then 返回租户不匹配错误，不泄露 receipt 内容，不创建/复用批次。

### A7 所有入口统一解析与幂等

Given active-order、MANUAL、SCHEDULED、PQC_INDEPENDENT 等合法入口分别提交同一权威 receipt 和幂等键；When 重复或并发建批；Then 全部进入同一解析器，返回同一 batchExecutionId，created/reused 语义稳定；不同 payload 使用同一幂等键必须冲突。

### A8 流程7消费来源凭证

Given 建批成功且来源凭证 ID/hash/snapshot 已持久化；When 流程7执行 Tx-C Origin/TraceLink/Manifest 映射；Then 能按 batchExecutionId 读取同一权威来源，不接受客户端替代来源，映射失败保持 TRACE_MAPPING_BLOCKED，不得提前 BATCH_READY。

## 失败分类与 owner

| 分类 | 识别信号 | owner | 是否阻断建批验收 | 动作 |
|---|---|---|---|---|
| F6-ACTIVE | active receipt 解析、状态、完成回填字段或 Tx-B 持久化失败 | 流程6/流程4 | 是 | 复现原测试类，退回流程6/4 |
| F9-INDEPENDENT | receipt 签发、验真、有效期、撤销、签名/hash、跨租户失败 | 流程9 | 是 | 复现 receipt service/contract 测试，退回流程9 |
| F7-TRACE | 来源凭证已持久化但 Tx-C 映射、hash/snapshot 或 outbox 消费失败 | 流程7 | 是 | 返回 TRACE_MAPPING_BLOCKED，不得改流程6状态 |
| F4-SOURCE | 完成 receipt 缺失、失败尝试被误消费、领料/损耗来源快照不一致 | 流程4/5 | 是 | 记录 BACKFILL_ATOMIC_ROLLBACK 或来源快照错误 |
| F8/10-DOWNSTREAM | 建批成功但材料 gate 或最终 RELEASED 失败 | 流程8/10 | 否（建批阶段）/是（放行阶段） | 不回滚权威 receipt，不绕过硬门禁 |
| PAR/ENV | Bean、fixture、schema、依赖、端口或资源错误，未进入目标业务断言 | 并行 owner/环境 owner | 证据阻断 | 标记环境 blocker，不写业务 RED |

## 严格 TDD 顺序

### RED

1. mvn.cmd -o -pl yudao-module-mes -Dtest=MesBatchExecutionEntryContractTest,MesProductionReleaseBatchExecutionPortTest,MesIndependentBatchPrerequisiteReceiptServiceTest,MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest -Dsurefire.failIfNoSpecifiedTests=false test -> 预期在流程6/9提交前暴露缺失解析接线、receipt 持久化或来源字段断言。
2. 真实集成路径（待干净 worktree、测试租户、服务和正式数据就绪）执行 active-order 与 independent 建批 UI/服务路径；禁止用请求体 nested receipt 代替服务端签发记录。

### GREEN

流程6/9提交后，在主线程托管的干净 integration worktree 执行：

    $env:MAVEN_OPTS='-Xms256m -Xmx1536m -XX:MaxMetaspaceSize=384m -XX:CICompilerCount=2'
    & 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -o -pl yudao-module-mes -am -DskipTests compile
    & 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -o -pl yudao-module-mes -Dtest=MesBatchExecutionEntryContractTest,MesProductionReleaseBatchExecutionPortTest,MesIndependentBatchPrerequisiteReceiptServiceTest,MesIndependentBatchPrerequisiteReceiptSqlContractTest,MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test

GREEN 仅在目标测试真实进入 Surefire 且 Failures=0, Errors=0 时成立；若只通过 receipt service 单测而未证明建批持久化和流程7消费，不得标记专项通过。

### REGRESSION

- 逐入口重复/并发建批：确认唯一 batchExecutionId、created/reused 和来源 ID/hash/snapshot 不变。
- 逐项负向：缺 receipt、失败 receipt、过期、撤销、篡改 nested payload、工单/路线/批号/hash、跨租户均阻断。
- 追溯：建批后流程7能读取持久化来源并在映射失败时保持 TRACE_MAPPING_BLOCKED。
- 运行 git diff --check、branch-runtime guard 和目标模块 compile；不把环境 PASS、SQL 文件存在或 mock 验证写成真实建批 GREEN。

## 验收数据与前置

- 测试租户 A/B、真实用户角色和租户上下文。
- 一条有效 BACKFILL_SUCCEEDED active-order receipt，至少一条失败尝试但无 receipt 的记录。
- 三种独立 entryType 各一条服务端签发 receipt，另备过期、撤销、篡改 hash、错误 tenant fixture。
- 正式工单、路线/版本、批号、batchExecutionSourceRelation 和可验证 source snapshot；不得按工单号/批号猜测关系。
- 可查询的 batchExecution 来源持久化及流程7 Tx-C 映射记录。

## Blocker 与 Go/No-Go

- 流程6/9提交未进入当前干净基线前：NOT RUN。
- 缺真实测试租户、正式 receipt、来源关系、服务/数据库或写入权限：BLOCKED，不得 fallback。
- 仅 receipt service 4/4、SQL 合同 1/1 或入口单测通过：不是完整建批 GREEN。
- 流程4/6/7/8/9/10任一权威来源、持久化、映射或门禁证据缺失：全链路 No-Go。

## 证据记录模板

记录基线 HEAD、worktree、命令、退出码、Surefire 总数、F/E/S、测试数据 ID（脱敏）、首个根因、owner、是否阻断、修复提交和复跑结果。禁止记录凭证正文、签名密钥或其他 secret。
