# 执行记录

## 2026-08-23 16:06:35 当前 int_main 标准复核（最新）

- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests clean compile -> PASS`；当前 `int_main` 的 24 模块 reactor 完成 clean compile，MES 编译 2858 个主源码文件，`BUILD SUCCESS`，结束时间 `2026-08-23T16:05:23+08:00`。
- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test -> PASS`；流程7定向测试 29 项（validator 17 + service contract 12），0 failures、0 errors、0 skipped，`BUILD SUCCESS`，结束时间 `2026-08-23T16:06:35+08:00`，testResources 正常复制。
- 当前 `int_main` 已包含 task-owned 提交 `0767b1fa5` 及后续验证/文档提交；未重复提交 DTO，未使用 `git add -A`，未混入流程6/8/10或其它 dirty/untracked。
- Tx-C 正式合同：入口 `POST /mes/pro/edhr-batch-execution/traceability/tx-c`；成功写入 Origin/TraceLink/Manifest/outbox 并发布 `FLOW7_TRACE_MAPPING_SUCCEEDED`，失败以 `TRACE_MAPPING_BLOCKED` 持久化可重试/最终失败事件；流程6消费成功事件并拥有 `BATCH_READY`，流程7不写流程6状态。流程8通过 source-precheck resolver 读取 `batchExecutionId`、`originLinkId`、`traceLinkHash`、`sourceSnapshotHash`。
- 来源变化证据：流程8 resolver 的正式预检先读取版本/hash，预检后替换来源再读取立即返回 `FLOW8_SOURCE_PRECHECK_STALE`；批次与 origin link 不一致返回 `FLOW8_TRACE_LINK_ORIGIN_MISMATCH`。Tx-C producer 对同一变化以稳定 blocker `TRACE_MAPPING_BLOCKED` 和原因 `SOURCE_CHANGED_AFTER_PRECHECK` 失败，并不发布成功事件或推进 `BATCH_READY`；事件键 `eventId + idempotencyKey` 保持重复消费幂等。
- `BDD: source-precheck freshness -> Given 已持久化 Origin/TraceLink/Manifest When 预检后来源版本或 hash 改变 Then resolver fail-fast 并保留关系不一致证据；Tx-C 以 TRACE_MAPPING_BLOCKED 失败且流程6不推进 BATCH_READY`。
- `REGRESSION: full cross-flow regression, real database migration/append-only trigger/Mapper/permissions/runtime, service startup, Flow6 consumer, Flow8 four-material gate, Flow10 final RELEASED, and write-enabled E2E -> NOT RUN`；上游正式 receipt/sourceEvidence 适配器及真实运行环境仍是 blocker。

## 2026-08-23 15:40:52 当前 int_main 标准复核（历史，已被 16:06:35 当前 int_main 复验取代）

- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests clean compile -> PASS`；当前 `int_main` 的 24 模块 reactor 完成 clean compile，MES 编译 2857 个主源码文件，`BUILD SUCCESS`，结束时间 `2026-08-23T14:55:52+08:00`。
- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test -> PASS`；流程7定向测试 29 项（validator 17 + service contract 12），0 failures、0 errors、0 skipped，`BUILD SUCCESS`，结束时间 `2026-08-23T14:57:33+08:00`，testResources 正常复制。
- 当前 `int_main` 已包含 task-owned 提交 `0767b1fa5` 及后续验证/文档提交；未重复提交 DTO，未使用 `git add -A`，未混入流程6/8/10或其它 dirty/untracked。
- Tx-C 正式合同：入口 `POST /mes/pro/edhr-batch-execution/traceability/tx-c`；成功写入 Origin/TraceLink/Manifest/outbox 并发布 `FLOW7_TRACE_MAPPING_SUCCEEDED`，失败以 `TRACE_MAPPING_BLOCKED` 持久化可重试/最终失败事件；流程6消费成功事件并拥有 `BATCH_READY`，流程7不写流程6状态。流程8通过 source-precheck resolver 读取 `batchExecutionId`、`originLinkId`、`traceLinkHash`、`sourceSnapshotHash`。
- `REGRESSION: full cross-flow regression, real database migration/append-only trigger/Mapper/permissions/runtime, service startup, Flow6 consumer, Flow8 four-material gate, Flow10 final RELEASED, and write-enabled E2E -> NOT RUN`；上游正式 receipt/sourceEvidence 适配器及真实运行环境仍是 blocker。

## 2026-08-23 14:34:54 Clean 后标准复验（历史，已被 15:40:52 当前 int_main 复验取代）

- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests clean compile -> PASS`；24 模块 reactor 执行 clean compile，MES 编译 2857 个主源码文件，`BUILD SUCCESS`，结束时间 `2026-08-23T14:33:27+08:00`。
- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test -> PASS`；testResources 正常复制，流程7定向测试 29 项（validator 17 + service contract 12），0 failures、0 errors、0 skipped，`BUILD SUCCESS`，结束时间 `2026-08-23T14:34:54+08:00`。
- 本次结果验证的是已提交的 Flow7 task-owned 切片（实现提交 `0767b1fa5`，证据提交 `e4df56ce3`），不改变流程6/8/10 状态 owner。
- `REGRESSION: full cross-flow regression, real database migration/append-only trigger/Mapper/permissions/runtime, service startup, Flow6 consumer, Flow8 four-material gate, Flow10 final RELEASED, and write-enabled E2E -> NOT RUN`；上游正式 receipt/owner/fixture 和真实运行环境仍是 blocker。

## 2026-08-23 14:24:55 提交后标准复验（历史，已被 14:57:33 当前 int_main 复验取代）

- `COMMIT: git commit -m "feat(mes): add batch traceability tx-c producer" -> PASS`；task-owned 提交 `0767b1fa5`，46 files changed，未暂存流程9或其它并行 dirty/untracked。
- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests compile -> PASS`；24 模块 reactor `BUILD SUCCESS`，结束时间 `2026-08-23T14:23:59+08:00`。
- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test -> PASS`；未跳过 testResources，流程7定向测试 29 项（validator 17 + service contract 12），0 failures、0 errors、0 skipped，`BUILD SUCCESS`，结束时间 `2026-08-23T14:24:55+08:00`。
- 本次修复包含批次执行 DO 的 `tenant_id` 映射，使 Tx-C producer 在读取 Flow6 provision audit 前执行当前租户边界校验；不改变流程6/8状态 owner。
- 全量回归、真实数据库 migration/trigger/Mapper/outbox、服务启动、流程6 consumer、流程8 gate、流程10 `RELEASED` 和写入型 E2E 仍为 `NOT RUN`/blocked。

## 2026-08-23 13:54:30 主工作区复验（历史，已被 14:24:55 提交后复验取代）

- `GREEN: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DfailIfNoTests=false" test -> PASS`；流程7定向测试 28 项（validator 17 + service contract 11），0 failures、0 errors、0 skipped，`BUILD SUCCESS`，结束时间 `2026-08-23T13:54:30+08:00`。
- 同一 Maven 3.9.16 reactor compile 已在 `2026-08-23T13:50:02+08:00` 通过；本次补验增加 Tx-C 批次租户边界契约，要求先验证 `batchExecutionId` 所属租户，再读取 Flow6 provision audit。
- 该结果只证明 Flow7 task-owned 编译和 focused contract/validator slice。真实 Mapper/数据库 append-only trigger/outbox、Flow4 receipt producer、Flow6 consumer、Flow8 四材料 gate、Flow10 `RELEASED`、完整回归、服务启动和写入型 E2E 仍为 `NOT RUN`/blocked。

## 2026-08-23 Historical Continuation Evidence (superseded by commit `0767b1fa5`)

- User-authorized continuation implemented and verified only the Flow7 task-owned slice; unrelated dirty worktree changes were not staged or modified.
- `GREEN: mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests compile -> PASS`; Maven 3.9.16 compiled the MES reactor in the main workspace with `BUILD SUCCESS`.
- `GREEN: mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DfailIfNoTests=false" test -> PASS`; 28 focused tests (17 validator + 11 service contract) completed with 0 failures/errors/skips and `BUILD SUCCESS`.
- Tx-C transaction boundary was corrected to use a success `TransactionTemplate` and a separate failure transaction. A source/hash/precheck failure rolls back any partial Origin/TraceLink/Manifest writes, then commits a `TRACE_MAPPING_BLOCKED` outbox failure event; a success commits the immutable graph and outbox event before after-commit publication.
- `BDD: Tx-C formal producer and event closure -> Given Flow6 has persisted a successful provision audit and formal source evidence When Flow7 POST /traceability/tx-c reads the sources and passes the precheck Then it re-reads before commit, persists Origin/TraceLink/Manifest/outbox atomically, and publishes FLOW7_TRACE_MAPPING_SUCCEEDED after commit; When evidence is missing, changed, or inconsistent Then no partial graph remains and a committed retryable/final TRACE_MAPPING_BLOCKED event is emitted.`
- `RED: Tx-C failure transaction boundary -> FAIL, previous catch-inside-transaction path could leave a rollback-only transaction or partial graph while trying to persist the failure event.`
- `GREEN: Tx-C failure transaction boundary -> PASS, TransactionTemplate rollback plus new failure transaction preserves only the immutable failure outbox record.`
- Historical `RED` records remain for linked-worktree flatten/compiler ACL failures; they do not describe the main-workspace task-owned source set.
- Covered contract evidence includes explicit `originId` for release decisions, persisted `RELEASE_DECISION` TraceLink lookup, unowned-origin rejection, canonical source identity mismatch blocking, and `hasActualLoss`/`NO_LOSS` mapping.
- `GREEN: validate_backend_api.py --evidence backend-api-evidence.md -> PASS` and `GREEN: validate_database_schema.py --evidence database-schema-evidence.md -> PASS`.
- `GREEN: SQL_STATIC_SCAN -> PASS (tables=3, append-only triggers=6, SIGNAL guards=7)` and `GREEN: DOC_STRUCTURE_SCAN -> PASS (8 task-local evidence/design files present)`.
- 历史提交阻断（已由提交 `0767b1fa5` 解除）：当时主工作区 `.git/index` 位于受限只读边界，`git add` 创建 `E:\IntRuoyi\.git\index.lock` 返回 `Permission denied`。未修改 ACL、未复制 Git 元数据、未使用旁路提交；后续在主工作区恢复受保护 Git 写权限后按明确路径完成选择性暂存和提交。
- `REGRESSION: full cross-flow regression, real DB migration/append-only trigger/Mapper/permissions/runtime, service startup, Flow8 four-material gate, Flow10 final RELEASED, and write-enabled E2E -> NOT RUN` because formal upstream receipts/owners/fixtures, runtime prerequisites, and a writable verification worktree are not available.
- Current task status is `partial / blocked`. Earlier M1-M4, cleanup, and `completed` entries below are historical design/closeout records and are superseded for current status; they must not be read as proof of production completion.

## 2026-08-23 Tx-C Producer Delivery (authoritative)

- Producer entry: `POST /mes/pro/edhr-batch-execution/traceability/tx-c`, with only `batchExecutionId`, event/idempotency keys and optional witness hashes in the request. Origin, TraceLink, Manifest and source payloads are read from formal persistence, never accepted from the client.
- Formal read set: the latest successful Flow6 `OPEN` audit for the batch, Flow1 binding header/items in the current tenant, and the audit metadata's Flow4 completion receipt, Flow2/3 production/PQC facts, Flow5 `REQUIRED`/`NO_LOSS` fact, Flow6 provision receipt/status and canonical source evidence. Missing or inconsistent evidence fails with `TRACE_MAPPING_BLOCKED`.
- Tx-C success writes `mes_pro_edhr_batch_execution_origin`, `mes_pro_edhr_batch_execution_trace_link`, `mes_pro_edhr_batch_execution_trace_manifest`, and `mes_pro_edhr_batch_trace_outbox_event`; the event carries `batchExecutionId`, `originLinkId`, `traceLinkHash`, `sourceSnapshotHash`, manifest version and source IDs. Flow6 remains the `BATCH_READY` state owner and consumes only `FLOW7_TRACE_MAPPING_SUCCEEDED`.
- Tx-C failure writes no partial mapping and emits `FLOW7_TRACE_MAPPING_FAILED_RETRYABLE` or `FLOW7_TRACE_MAPPING_FAILED_FINAL` with stable `TRACE_MAPPING_BLOCKED`. A second read after precheck detects `SOURCE_CHANGED_AFTER_PRECHECK`; duplicate event/idempotency requests return the existing immutable outbox result.
- `REGRESSION: full cross-flow/real DB/outbox/permissions/runtime/E2E -> NOT RUN`; 本历史节点的 28 项结果已被当前 `int_main` 的 29 项定向测试取代，且两者都只属于 task-owned validator/service-contract 证据。上游流程4 receipt 持久化及流程6 sourceEvidence/provision receipt 适配器仍是 blocker。

## 2026-08-21 任务建立

- 用户意图：仅审计、澄清和设计流程修复 7；不改生产代码/数据库，不启动服务，不运行写入型 E2E。
- 已读取：AGENTS.md、task-closeout-rules、experience-index、生产角色运行文档、backend-development 的正式来源章节，以及 frontend-development 和 e2e-rules。
- 使用 system-design-docs 设计数据/API/权限；使用 bdd-tdd-acceptance-planner 设计 BDD、RED/GREEN 与验收计划。均未生成生产代码。

## M1：现状审计（completed）

- 批次实体和响应仅有工单、批号、路线、状态/hash；没有订单、领料单分录、生产/PQC/损耗来源和放行申请的一等关系。
- 批次复用/查询按工单、批号、路线，不能建立订单完成交易级幂等。
- 放行申请有订单到批次的应用级关系，但没有批次正向完整来源图。
- 资料规划拒绝 formBindings 冒充传统批记录来源，但不消费领料单分录；存在零损耗报告规则冲突。

## M2：设计完成（completed）

- 定义主来源、逐来源 link、manifest hash 链、四项独立资料要求、状态所有者、事务/幂等、读取 API、权限、历史迁移及跨线程合同，详见 development-plan.md。
- 将缺来源、hash 不一致、领料不唯一/分录无效、文件缺失和历史关系缺失定义为 fail-fast blocker。

## M3：测试计划完成（completed）

BDD: 完成/建批/映射事务边界 -> Given 流程2/3正式事实和双100%成立 When 流程4点击完成 Then Tx-A 仅提交三类适用回填与 completionBackfillReceipt；When 流程6消费 receipt Then Tx-B 创建/复用 batchExecutionId；When 流程7收到 batch provision receipt Then 后继 Tx-C 建立批次来源图。

BDD: 无损耗不创建损耗单 -> Given 流程5提交 NO_LOSS 事实 When 流程4 Tx-A 完成回填 Then 只记录无损耗确认，不生成零损耗资料。

BDD: 四份资料未齐不得放行 -> Given 缺任一文件 When 管理者代表放行 Then 返回 blocker 且不写放行决定。

RED: 未运行（设计任务禁止修改代码和运行测试） -> 未来测试应先因缺少来源模型、领料关系和四项资料合同失败。

GREEN: 未运行（设计任务禁止修改代码和运行测试） -> 实现后按 test-plan.md 执行。

REGRESSION: 未运行（仅完成文档结构验证） -> 流程2/3/4/5/6/7/8/9/10/11职责、Tx-A/Tx-B/Tx-C边界和生产/E2E回归留待实施任务。

## M4：文档验证（completed）

- 已创建并复核 task.md、development-plan.md、test-plan.md、execution-log.md、verification-report.md。
- 未修改生产代码、数据库、环境、业务数据或运行服务。

## Blockers

1. 正式领料单头/分录实体和唯一审核解析契约尚未确认；实现前无法建立合法领料追溯。
2. 零损耗报告合同与本目标冲突，必须先统一。
3. 四份文件的类型、版本、hash、对象权限和管理者代表授权尚未由关联线程提供冻结合同。

## 2026-08-22 补充复核修订（已按二次复核纠正）

- 复核发现原设计错误：将尚未发生的 releaseApplicationId 作为活跃订单建批不可替代前置，混淆了“完成/回填建批”和“后续资料齐套/放行”两个业务节点。
- 已修正 ACTIVE_ORDER_COMPLETION 的正式主来源为流程 4 completionBackfillReceipt + 流程 6 batch provision receipt；receipt 内冻结 activeOrderId + completionTransactionId、流程 1 pickListBindingId/sourceSnapshotHash 和三类适用回填结果；releaseApplicationId 改为流程 10 后续实际放行时才追加的 RELEASE_DECISION 关系。
- 已新增 PQC_INDEPENDENT、MANUAL、SCHEDULED 入口矩阵，分别定义自己的 sourceCredential、幂等键、状态所有者和追溯边界，不再强制 activeOrderId 或 releaseApplicationId。
- 已按复核纠正流程 2/3/4/5/6/7/8/9/10/11 owner：2/3 形成生产/PQC事实，4 唯一拥有双100%完成和 Tx-A 回填 receipt，5 拥有损耗决定，6 拥有 Tx-B batch provision，7 仅在 batchExecutionId 返回后建立映射，8 负责四份材料 gate，9 负责入口凭证/幂等前置，10 唯一写最终放行，11 负责 BDD/TDD、回归、迁移和总门禁。

BDD: 活跃订单完成/建批/映射分离 -> Given 双100%、流程1绑定、流程2/3正式事实和流程5损耗决定齐全 When 流程4点击完成 Then Tx-A 提交 completionBackfillReceipt；When 流程6成功 Then Tx-B 返回 batchExecutionId；When 流程7消费成功结果 Then 后继事务建立来源图，建批时不要求 releaseApplicationId。

BDD: 后续放行追加关系 -> Given 已存在完成/回填来源完整的批次 When 后续实际产生 releaseApplicationId 并完成管理者代表放行 Then 追加 RELEASE_DECISION 关系，不能改变原 batch origin key。

BDD: 建批失败 receipt 保留与映射缺失阻断 -> Given Tx-A receipt 已成功 When 流程6失败 Then receipt 保留且可重试，流程7不得写 batch 映射；When batchExecutionId 已有但映射缺失 Then 追溯和放行阻断。

BDD: 独立入口 NOT_APPLICABLE -> Given 流程9已验证独立/手工/排产凭证 When 流程6返回 batchExecutionId 且流程7建图 Then 活跃订单/领料/完成关系显式为 NOT_APPLICABLE。

RED: 未运行（设计任务禁止修改代码和运行测试） -> 未来测试应先证明流程4/6/7事务边界、缺 batchExecutionId 阻断和旧 owner 错位失败；不是生产测试通过。

GREEN: 未运行（设计任务禁止修改代码和运行测试） -> 实现后按 test-plan.md 验证 Tx-A receipt、Tx-B batch provision、Tx-C 映射幂等、四份材料 gate、后置放行和其它入口独立幂等。

REGRESSION: 未运行（仅完成文档核验） -> 流程 2/3/4/5/6/7/8/9/10/11 owner、事务边界和总门禁回归留待实施任务。

## 2026-08-22 修订收尾准备

- 已完成二次复核修订：流程4提交 completionBackfillReceipt，流程6随后创建/复用 batchExecutionId，流程7再建立批次映射；releaseApplicationId 仅在流程10后续实际放行时追加。
- 已区分 ACTIVE_ORDER_COMPLETION、PQC_INDEPENDENT、MANUAL、SCHEDULED 四类入口，各自保留 sourceCredential、幂等键、状态所有者和追溯边界。
- 已复核流程 2/3/4/5/6/7/8/9/10/11 职责，并将 Tx-A/Tx-B/Tx-C、BDD、TDD、迁移 blocker、时序与 verification-report 同步修订。
- 当前状态切换为 ready_for_closeout；下一步仅执行 task-closeout-cleanup preview/apply，不修改生产代码、数据库、服务或业务数据。

## 2026-08-22 修订收尾完成

- cleanup preview：PASS；五份正式任务文档全部保留，删除项为空，blocked 为空，warnings 为空。
- cleanup apply：PASS；当前为主工作树，未涉及 worktree 合并或删除，删除项为空。
- 最终状态：completed。代码符合性结论及未解决 blocker 仍以 verification-report.md 为准；后续需另行实施并通过测试。

## 收尾准备（ready_for_closeout）

- 已按收尾基线将任务状态切换为 ready_for_closeout。
- 已读取 task-closeout-cleanup 规则，准备先 preview 再 apply；默认保留 task.md、execution-log.md、verification-report.md 和 development-plan.md、test-plan.md 这五份任务文档。
- 已按 project-experience-consolidation 检查现有经验归宿；本次经验已由现有 backend-development、e2e-rules 和 experience-index 的长期规则覆盖，不新建或改写长期经验文档。

## 收尾完成（completed）

- cleanup preview：PASS；保留五份正式任务文档，删除项为空，blocked 为空，warnings 为空。
- cleanup apply：PASS；任务位于主工作树，未涉及 worktree 合并或删除，删除项为空。
- 最终验证：文档交付完成；代码不符合目标态及四项未解决 blocker 仍按 verification-report.md 保留，待后续实现任务关闭。

## 2026-08-22 二次复核修订收尾准备

- 已删除/改正流程编号和状态 owner 错位：流程2/3生产与PQC事实，流程4完成及Tx-A回填，流程5损耗决定，流程6批次provision，流程7后继建图，流程8材料gate，流程9入口合同，流程10最终放行，流程11总门禁。
- 已将时序和事务边界统一为正式来源事实 -> 流程4 Tx-A receipt -> 流程6 Tx-B batchExecutionId -> 流程7后继 Tx-C 映射 -> 流程8材料 -> 流程10放行；明确 receipt 保留、映射缺失阻断和 NOT_APPLICABLE 关系。
- RED/GREEN/REGRESSION 仍为 NOT RUN/计划语义；没有将文档结构验证冒充生产测试通过。
- 当前状态切换为 ready_for_closeout，下一步仅执行 task-closeout-cleanup preview/apply。

## 2026-08-22 二次复核修订收尾完成

- cleanup preview：PASS；五份正式任务文档全部保留，删除项为空，blocked 为空，warnings 为空。
- cleanup apply：PASS；当前为主工作树，未涉及 worktree 合并或删除，删除项为空。
- 最终状态：completed。代码符合性仍为不符合目标态，RED/GREEN/REGRESSION 均未运行；未解决 blocker 以 verification-report.md 为准。

## 2026-08-22 流程9入口类型最终对齐

- BDD: canonical 独立入口 -> Given 流程9提供 `IndependentBatchPrerequisiteReceipt` When 流程7消费已由流程6 provision 的批次 Then `entryType=PQC_INDEPENDENT`、`MANUAL`、`SCHEDULED` 分别使用自己的正式 sourceCredential、幂等键和来源边界；不要求 activeOrderId 或 releaseApplicationId，并为不适用的订单/领料/完成关系写 `NOT_APPLICABLE`。
- RED: 未运行（本主任务保持只审计/设计边界，不修改生产代码或运行测试）；实施时必须先证明旧 `INDEPENDENT_WORK_ORDER` 不能作为流程9 canonical 合同。
- GREEN: 未运行；文档只冻结接口合同，不把文档结构 PASS 计为生产代码通过。
- REGRESSION: NOT RUN；流程1/4/5/6正式 receipt、流程8四材料 gate、流程10最终放行、数据库迁移、真实 Mapper、权限对象和写入型 E2E 仍待后续实施线程提供证据。

## 2026-08-23 P1 规则与现状审计证据（development-plan-delivery executor）

### 范围和方法

- 本次只执行 P1 只读审计：`task.md`、`backend-api-evidence.md`、流程 1/2/3/4/5/6/8/9/10/11 任务文档，以及当前 Flow7 后端 Java/SQL/API 代码。未修改 `task-state.json`，未修改生产代码、SQL、数据库、服务或业务数据。
- `Get-Content`/`rg` 静态核验通过：任务目标、当前事实、跨流程 owner 和 blocker 均已逐项落到可定位文件/行号；未以文档 GREEN 冒充运行态闭环。

### 正式来源、ID、receipt 与 schema 事实

1. 流程1正式领料来源已存在：`MesTeamLeaderActiveOrderServiceImpl.persistPickListBinding` 保存 `activeOrderId`、`workOrderId`、`pickListId`、ERP source FID/BillNo、`sourceSnapshotHash`、`bindingVersion`、幂等键以及逐行 `sourceEntryId/itemSnapshotHash`（`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:1431-1479`）；`IntRuoyiBackend/sql/mysql/20260822_mes_active_order_pick_list_binding.sql:1-57` 提供绑定头/明细、稳定分录和唯一约束。建批/追溯不得从业务号码或当前 ERP 投影猜测。
2. 流程2正式生产事实及复核事件的 ID、版本、payload hash、签名快照、`activeOrderId/workOrderId/pickListBindingId/routeVersionId` 在 `doc/tasks/20260821-flow-repair-02-production-submit-review-boundary/development-plan.md:5-23` 冻结；流程2只产生提交/复核/驳回/分配事实，不拥有完成、回填、建批、材料或放行。
3. 流程3 `CONFIRMED` 仅代表 PQC 来源事实确认；任务/逐件明细/aggregate detail 及内容哈希是正式来源骨架，但唯一下游有效 aggregate version 到流程4正式过程检验单的生产消费尚未证明（`doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/development-plan.md:9-28,177-197`）。流程3不拥有完成、建批或最终放行。
4. 流程4已有 `MesCompletionBackfillReceipt` DTO（`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesCompletionBackfillReceipt.java:9-51`）和核心 `CompletionBackfillReceipt` 形状（`.../productionrelease/core/CompletionBackfillReceipt.java:15-64`），但当前完成服务只在 `MesTeamLeaderOrderProcessCompletionService.completeAndBackfill` 写 completion/backfill execution、事件 ID、aggregate hash、幂等键（`.../MesTeamLeaderOrderProcessCompletionService.java:231-265`），未发现正式 receipt producer、不可变 receipt 持久化表或提交后可查询的 receipt owner。流程4仍应是双100%完成 Tx-A、三类回填和 receipt 唯一 owner。
5. 流程5已有正式损耗事实读取/校验和条件写入：正损耗输出 `REQUIRED/hasActualLoss=true/lossQuantity>0`，明确无损耗输出 `NO_LOSS/false/0/NOT_REQUIRED`，逻辑及来源校验见 `.../MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java:219-240,343-366,406-424,467-491`。但尚未发现与 Flow7 `LOSS_FACT`、`LOSS_REPORT_RECEIPT`、`NO_LOSS_CONFIRMED` link type 对应的不可变 completion receipt/持久化来源；Flow7 因而只能在合同层阻断不完整损耗关系。
6. 流程6/9前置合同代码已存在：`MesBatchExecutionEntryContractService` active 分支要求 `CompletionBackfillReceipt`、`BACKFILL_SUCCEEDED`、完成/领料/损耗字段（`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesBatchExecutionEntryContractService.java:29-158`），independent 分支要求 `IndependentBatchPrerequisiteReceipt` 且不得有 activeOrder（`...:161-237`）。但实际批次服务接口仍只有通用 `openOrCreate`、`openOrCreateFromProductionRelease`、`openOrCreateFromScheduleCompletion`，没有独立的 `openOrCreateFromActiveOrderCompletion`/`BatchProvisioningRecord` owner（`.../MesProEdhrBatchExecutionService.java:24-43`）。旧 PQC 生产放行适配器仍以 `PQC_RELEASE:<applicationId>` 查批并把 `applicationId` 作为命令字段（`.../productionrelease/pqc/MesProductionReleaseBatchExecutionPortImpl.java:27,50-103`）；`MesProEdhrBatchExecutionServiceImpl.openOrCreateFromProductionRelease` 仍在 `...:906-918` 拒绝缺少 `applicationId`。这与“activeOrderId + completion/backfill receipt + Flow1 binding 为建批主键，releaseApplicationId 仅放行后追加”不一致，是 P1 实施 blocker。
7. Flow7 当前 task-owned 追溯 schema 已存在：Origin/TraceLink/Manifest DO 保存 batch、active order、work order、completion transaction、completionBackfillReceipt、pick-list binding、source snapshot/hash、batch provision receipt、canonical source identity 和 hash-chain manifest（`IntRuoyiBackend/.../MesProEdhrBatchExecutionOriginDO.java:26-51`、`.../MesProEdhrBatchExecutionTraceLinkDO.java:26-43`、`.../MesProEdhrBatchExecutionTraceManifestDO.java:17-39`）；SQL 表和 append-only trigger 见 `IntRuoyiBackend/sql/mysql/20260822_mes_edhr_batch_traceability.sql:1-166`。`MesProEdhrBatchExecutionController` 提供详情、manifest、source-precheck、列表、capture 和后置 release-decision API，权限为 query/trace-capture（`.../MesProEdhrBatchExecutionController.java:232-288`）。`MesProEdhrBatchTraceabilityServiceImpl` 的 capture 只接受已有 batchExecutionId 和已提供的正式来源；releaseApplicationId 由后置 `RELEASE_DECISION` TraceLink 反查，不是 Origin 建批字段（`.../MesProEdhrBatchTraceabilityServiceImpl.java:46-124,127-198`）。本段审计时的 25 项 focused tests 已被最新当前 `int_main` 29 项结果 supersede；仍无真实 Mapper/DB、权限对象、服务启动或写入 E2E 证据。
8. 流程8四份材料的固定节点常量已存在：`INCOMING_INSPECTION_REPORT`、`STERILIZATION_REPORT`、`FINISHED_PRODUCT_INSPECTION_REPORT`、`FINISHED_PRODUCT_INSPECTION_RECORD`（`.../MesProEdhrBatchExecutionServiceImpl.java:237-246`、`.../productionrelease/pqc/MesProductionReleaseReportStageInitializerImpl.java:44-47`）；任务文档要求服务端固定 `MATERIALS_PENDING/READY/RECHECK_REQUIRED`，但所有放行入口共用硬 gate、旧前端可选开关隔离和运行态证据仍缺（`doc/tasks/20260821-flow-repair-08-four-material-upload-gate/task.md:1-67`）。流程8不创建批次、不写 RELEASED。
9. 流程9 owner 是多入口 `entryType` 分流、正式 receipt/source credential、幂等和冲突阻断；active 入口只能消费流程4 receipt，MANUAL/SCHEDULED/PQC_INDEPENDENT 只能消费独立凭证（`doc/tasks/20260821-flow-repair-09-multi-entry-precondition-contract/task.md:1-62`）。当前 validator 只证明命令合同，旧生产放行/排产/PQC 入口统一适配、状态持久化和真实权限路径未完成。
10. 流程10唯一拥有最终 `RELEASED`、release decision/manifest、管理者代表签名和 CAS；其开发方案要求只消费已有 batchExecutionId 和 Flow8 `MATERIALS_READY`（`doc/tasks/20260821-flow-repair-10-final-release-state-and-trace/development-plan.md:1-45`）。专项 focused suite 已有通过记录，但 Flow4/6权威 receipt 适配器、审批中心上下文、outbox、历史迁移和全入口真实运行仍是 No-Go。
11. 流程11只拥有 BDD/TDD、回归、迁移/回滚和总门禁，不拥有业务状态（`doc/tasks/20260821-flow-repair-11-bdd-tdd-regression-and-migration/task.md:1-80`）。其 runner/合同测试已有历史通过记录，但真实生产历史 dry-run、数据库迁移、跨流程回归、Playwright 真实多角色路径尚未执行。

### P1 根因与剩余 blocker

- 根因 A：Flow4 的 receipt 只有 DTO/输入字段，没有与完成 Tx-A 同事务提交后可查询的不可变事实 owner；Flow6 只能接受请求中携带的 receipt，无法证明 receipt 来自完成事务。
- 根因 B：Flow6 的合同校验已向正确方向收敛，但 `PQC_RELEASE:<applicationId>` 旧入口仍把未来放行申请当作批次复用键，且服务没有独立 completion receipt -> BatchProvisioningRecord -> batchExecutionId 端口。
- 根因 C：Flow5 的 `NO_LOSS/REQUIRED` 结果未形成 Flow7 要求的一等 source link/receipt，正损耗与无损耗不能在真实映射中闭环核验。
- 根因 D：Flow3 aggregate、Flow4 正式过程检验单和 Flow7 batch process-inspection record 之间缺少已验证的唯一 version/hash 消费关系。
- 根因 E：Flow8 四节点与附件基础存在，但统一材料 hard gate、旧可选开关隔离和所有 Flow10 入口复用未由运行态证明；Flow10 需要上游权威 receipt、outbox 和审批上下文。
- 根因 F：Flow7 自身 API/validator/schema focused slice 已有证据，真实 Mapper/数据库 append-only 触发器、权限对象、跨线程正式 receipt 和迁移数据均未验证。

### P1 RED/GREEN/REGRESSION 与独立测试结论

- `BDD: P1 规则与现状审计 -> Given task.md/backend-api-evidence.md/Flow7 后端及流程2/3/4/5/6/8/9/10/11文档 When 只读核验正式来源、owner、ID/receipt/schema 和调用链 Then 输出可定位事实与 blocker，不修改生产状态。`
- `RED: production/runtime/DB/E2E -> NOT RUN, 因为 P1 仅允许只读审计且上游正式 receipt、Mapper/权限/运行环境和可写验证工作树未具备。`
- `GREEN: Get-Content/rg 静态证据核验 -> PASS, 已记录上述文件/行号；既有 Flow7 25 项 focused validator/service contract tests 的 PASS 仅证明 task-owned slice，不证明跨流程闭环。`
- `REGRESSION: full cross-flow, migration, permissions, startup and write-enabled E2E -> NOT RUN, blocker 清单保持 fail-fast，不使用默认值、旧申请号或 fallback 补链。`
- P1 可供独立测试：**是（仅 P1 审计证据）**；独立测试者可按本节路径/行号复核。代码符合性/生产放行结论：**否**，需先关闭 Flow4 receipt persistence、Flow6 active-order provision owner/旧 applicationId 路径、Flow5 loss links、Flow8 shared gate、Flow10 upstream adapters/outbox 及 Flow11 全链路迁移/E2E blocker。

## 2026-08-23 当前 int_main 收尾复核

- `RED: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests compile -> PASS`，24 模块 reactor，`BUILD SUCCESS`，完成于 `2026-08-23T17:30:45+08:00`。
- `GREEN: ... -Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest ... test -> PASS`，29/29（validator 17 + service contract 12），0 failures/errors/skips，`BUILD SUCCESS`，完成于 `2026-08-23T17:32:14+08:00`。
- 来源变化证据保持有效：正式 resolver 首次预检后来源变更返回 `FLOW8_SOURCE_PRECHECK_STALE`；batch/origin 不一致返回 `FLOW8_TRACE_LINK_ORIGIN_MISMATCH`；Tx-C 失败持久化 `TRACE_MAPPING_BLOCKED` 与 `SOURCE_CHANGED_AFTER_PRECHECK`，不得推进 `BATCH_READY`。
- Cleanup preview/apply 只允许删除 `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/tmp-flow7-verify`，保留五份正式文档及 PRD、证据、状态和测试报告；未执行全链路回归、真实 DB/Mapper、服务启动或写入型 E2E。
- Cleanup apply 首次尝试因临时副本已被部分清理而遇到 `FileNotFoundError`；随后仅对同一已核验路径执行空目录镜像清理并删除该 `tmp-flow7-verify`，未触碰任务证据文件或其它工作树路径；再次执行 cleanup apply 退出码 0，`tmp-flow7-verify` 不存在。

## 2026-08-23 第二轮主线回归修复与复核

BDD: 追溯终端分区 -> Given 查询 `completedTraceOnly=true` When 批次列表执行正式状态/放行关系过滤 Then 仅返回已归档、已拒绝或存在 RELEASED 交易的批次，并保留租户与作废状态边界。

BDD: DCC 路由正式身份 -> Given 启用的 DCC 项目代码及正式产品绑定 When Word 导入预检解析受治理路由 Then 只沿项目代码 -> 物料 -> routeProduct ID 关系解析，不按路由名称猜测。

RED: `mvn.cmd --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProEdhrTraceTerminalPartitionContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test` -> FAIL，2 tests/2 failures；缺少 completedTraceOnly 状态分支和终端放行分区合同。

RED: `mvn.cmd --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteIdentityContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test` -> FAIL，2 tests/1 failure；Word 导入未通过正式 DCC 项目代码和 routeProduct ID 解析。

GREEN: 同两条定向命令复跑 -> PASS；追溯终端类 2/2，路由身份类 2/2，0 failures/errors/skips。修复仅涉及 `MesProEdhrBatchExecutionMapper` 的正式状态分区和 `MesProBatchRecordReportServiceImpl` 的 DCC 项目代码 -> 物料 -> 路由产品关系。

REGRESSION: `mvn.cmd --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dtest=MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false test` -> PASS，流程7 29/29（validator 17 + service contract 12），0 failures/errors/skips。

REGRESSION: `mvn.cmd --% -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS，24 模块 reactor，`BUILD SUCCESS`，`2026-08-23T19:02:38+08:00`。`git diff --check` -> PASS（仅换行风格 warning，无 diff error）。

来源变化与流程边界保持不变：流程7正式 resolver/持久化读取返回 `batchExecutionId`、`originLinkId`、`traceLinkHash`、`sourceSnapshotHash`；预检后来源变化阻断为 `FLOW8_SOURCE_PRECHECK_STALE`，batch/origin 不一致为 `FLOW8_TRACE_LINK_ORIGIN_MISMATCH`，Tx-C 映射缺失、版本/hash 变化为 `TRACE_MAPPING_BLOCKED`，不得推进流程6 `BATCH_READY`。流程10 的最终 `RELEASED` 仍不属于流程7。

本轮未运行真实数据库迁移、服务启动、权限对象验证或写入型 E2E；主工作树仍包含其它任务 dirty/untracked，提交必须仅选择流程7两处代码和本任务日志。

## 2026-08-23 7770f36fb current int_main re-verification

RED: no new RED on the committed baseline; the two regression classes previously failed on the pre-7770f36fb source and now reproduce GREEN on current `int_main`.

GREEN: `MesProEdhrTraceTerminalPartitionContractTest` -> PASS, 2/2, finished `2026-08-23T19:46:29+08:00`; `MesProBatchRecordRouteIdentityContractTest` -> PASS, 2/2, finished `2026-08-23T19:47:44+08:00`.

REGRESSION: `MesProEdhrBatchTraceabilityValidatorTest,MesProEdhrBatchTraceabilityServiceContractTest` -> PASS, 29/29 (17 + 12), finished `2026-08-23T19:48:32+08:00`; MES `-DskipTests compile` -> PASS, 24-module reactor, finished `2026-08-23T19:50:26+08:00`; all commands exited 0.

These are fresh current-`int_main` results for HEAD `7770f36fb6ed64f4e306320410d131f184cf2789`, not inherited worktree reports. No Flow7 source was changed during this verification. Full cross-flow runtime, migration, permissions, Flow8/10 integration, and write-enabled E2E remain NOT RUN.
