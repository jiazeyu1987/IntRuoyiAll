# P0 生产执行主闭环 TDD 计划

## Purpose and Scope

本文档把 P0 主闭环 BDD 场景映射为严格 TDD 顺序。实现阶段必须先写失败测试并记录 RED，再做最小正式实现并记录 GREEN；本文档设计阶段不修改生产代码。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/scope-contract.md`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesProFrontlineFeedbackSubmitServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesFrontlinePqcContextServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesTeamLeaderReportConfirmationServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesTeamLeaderBatchRecordBackfillServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesTeamLeaderTraceServiceImpl.java`
- `IntRuoyiFronted/package.json`

## Command Conventions

- 当前任务默认工作区是 `D:\IntRuoyiWorktree\worktree_20260803_p0`；后续开发和验证必须优先在该 worktree 执行，不得误跑到 `E:\IntRuoyi` 主工作区。
- 后端 Maven 命令必须在 `D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiBackend` 作为工作目录执行，或显式使用该 worktree 下的 `IntRuoyiBackend/pom.xml`。
- 前端 pnpm 命令必须在 `D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiFronted` 作为工作目录执行；若使用 `pnpm --dir` 失败，按 `docs/e2e-rules.md` 改为显式工作目录复核。
- M0 初始缺口是 `IntRuoyiFronted/package.json` 缺少 `e2e:p0-production-execution-loop:static` / `real` 脚本；若后续分支再次缺失，必须重新记录 RED，再新增脚本和正式 spec。
- PowerShell 不得使用 `&&` 串联命令；每条 RED/GREEN 必须单独记录退出码。
- 任何命令若在错误目录、错误 branch、错误端口或错误运行态失败，只能记录为前置失败；修正前不得把它当业务 RED 或 GREEN。
- 本文档所有窄范围 Maven RED/GREEN 命令默认追加 `"-Dsurefire.failIfNoSpecifiedTests=false"`；否则 `-am` 反应堆中无匹配测试的兄弟模块可能把命令形状失败伪装成业务失败。
- 不得使用 `-DskipTests`、`-Dmaven.test.skip=true` 或跳过 MES 测试的命令作为 GREEN 证据；这些命令只能作为依赖编译诊断，不能证明 P0 行为通过。
- 新增或修改 `IntRuoyiBackend/sql/mysql/` 正式 SQL 时，必须从 worktree 根目录运行全量 release migration policy gate；若只诊断单个 SQL，必须同时纳入完整依赖闭包。单文件缺依赖闭包导致的失败记录为 `COMMAND-BLOCKED`，不得用本地 H2 schema PASS 替代。

## Maven Reactor Failure Classification

窄范围 Maven 命令必须先判定失败归因，再写入 `execution-log.md`：

- `RED`：目标 P0 测试类或测试方法已经被 Surefire 执行，且失败原因正是当前业务缺口。
- `GREEN`：目标 P0 测试类或测试方法已经被 Surefire 执行并 PASS，日志包含目标测试 summary 或等价证据。
- `COMMAND-BLOCKED`：命令在目标 MES 测试执行前失败，例如错误工作目录、缺依赖、反应堆兄弟模块 testCompile 失败、无匹配测试、Maven 参数未加引号或端口/运行态不匹配。
- `DIAGNOSTIC-ONLY`：命令跳过测试、只编译依赖、只做语法检查或只运行非目标模块；不得作为 RED/GREEN。
- 发生 `COMMAND-BLOCKED` 时，先记录阻塞命令、失败模块和影响，再选择仍会执行目标 MES 测试的最小合法命令形态；不得通过跳过目标测试换取 GREEN。

## Contract Freeze Before Coding

进入生产代码 GREEN 前必须先冻结以下请求/DTO/实体字段合同；若字段缺失，先写 RED，不得用实现过程中的隐式推断补齐：

| 动作 | 必须冻结的正式字段 | 对应验证 |
| --- | --- | --- |
| 生产提交 | `processPoolSubmissionIdempotencyKey`、`actualEmployeeId`、`deviceAccountId`、`deviceId`、`workstationId`、`signatureId`、`signatureEmployeeUserId`、`signatureSnapshotJson`、`recordbookEntryId`、`recordbookEventId` | P0-T01、P0-T02 |
| PQC 提交 | `pqcSubmissionIdempotencyKey`、`pqcTaskId`、`qaRegulationVersionId`、`actualEmployeeId`、`deviceAccountId`、`deviceId`、`workstationId`、`inspectionQuantity`、`qualifiedQuantity`、`allocatableQuantity`、`consumedQualityQuantity`、`signatureId`、`signatureEmployeeUserId`、`signatureSnapshotJson`、正式生产提交绑定 ID 或绑定关系 ID；rawPayload 只能作为审计快照 | P0-T03、P0-T04、P0-T09A |
| 生产/PQC 复核 | `reviewIdempotencyKey`、`reviewTargetProcessPoolEventId`、`reviewerUserId`、`reviewRole`、`reviewRequiredFlag`、`reviewResult`、`reviewSignatureId`、`reviewSignatureUserId`、`reviewSignatureSnapshotJson` | P0-T05、P0-T06 |
| FIFO 确认 | `confirmationIdempotencyKey`、`sourceReviewId`、`sourceProcessPoolEventId`、`activeOrderId`、`targetWorkOrderId`、`allocatedQuantity`、`confirmedQuantity`、`confirmSignatureId` | P0-T07 |
| 批记录回填 | `batchRecordExecutionId`、`batchRecordReportId`、`batchRecordDefinitionId`、`batchRecordVersionId`、`fieldAuditBatchId`、`fieldAuditItemId`、`sourceProcessPoolEventId` 或 `sourceAllocationId`、`backfillIdempotencyKey` | P0-T08、P0-T10 |
| 统一 trace | `processPoolEventId`、`complete`、`sections`、`sourceIds`、`blockers`、`candidateEvents`、`lastUpdatedAt` | P0-T09、P0-T10 |

字段名可以按当前 VO/DTO 命名规范调整，但必须能一一映射到上表语义；任何“后端可根据名称/时间/当前用户猜出来”的字段都不视为已冻结。

## Schema Freeze Before Coding

新增正式字段或关系必须同时冻结以下 schema 证据；缺任一层时，相关业务 GREEN 无效：

| 层级 | 必须证明 | 对应阻塞 |
| --- | --- | --- |
| 迁移脚本 | 新字段、唯一约束、普通索引、字段注释和幂等键唯一范围已写入 `IntRuoyiBackend/sql/mysql/` 正式 SQL，且通过 release migration policy gate。 | `SCHEMA_MIGRATION_MISSING`、`MIGRATION_POLICY_GATE_FAILED` |
| 历史数据收紧 | `NOT NULL` 或唯一约束收紧前，未删除历史行缺正式来源 ID 时必须 fail fast，并保留正式 backfill 解除条件。 | `HISTORICAL_LINK_BACKFILL_REQUIRED` |
| 测试 schema | `yudao-module-mes/src/test/resources/sql/create_tables.sql` 与迁移字段、索引和唯一约束保持一致。 | `TEST_SCHEMA_DRIFT` |
| Java 持久化 | DO 字段、Mapper 查询、BO/DTO/VO 映射和 service 写入路径全部包含正式字段。 | `PERSISTENCE_MAPPING_MISSING` |
| 读模型聚合 | trace 查询按 `tenant_id + processPoolEventId` 或正式来源链聚合，一对多明细不得放大分页或拼接其它事件。 | `TRACE_AGGREGATION_DRIFT` |
| 租户权限 | 查询和写入均校验租户、操作者、复核角色、生产工单、路线工序和 MES 工序同源。 | `TENANT_SCOPE_MISMATCH` |
| 幂等约束 | 生产提交、PQC 提交、复核、确认和批记录回填有业务级唯一范围，重复/并发请求可复验。 | `IDEMPOTENCY_CONSTRAINT_MISSING` |

## TDD Sequence

| Step | 闭环段 | RED Commands | Expected Failures | 最小 GREEN 目标 | GREEN Commands | Refactor Checks |
| --- | --- | --- | --- | --- | --- | --- |
| P0-T00 | 实现前置门禁 | `workdir=IntRuoyiFronted; python -X utf8 -c "import json,pathlib; s=json.loads(pathlib.Path('package.json').read_text(encoding='utf-8'))['scripts']; assert 'e2e:p0-production-execution-loop:static' in s and 'e2e:p0-production-execution-loop:real' in s"` | 缺 P0 专用脚本或 spec 时，不能启动真实 E2E。 | 新增正式脚本、static spec、real E2E spec，并在实现任务记录脚本入口 PASS。 | 同 RED 命令 PASS。 | 不新增空脚本、假脚本或 API-only wrapper 冒充真实 E2E。 |
| P0-T00A | schema 合同门禁 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolSchemaTest,MesP0ProductionExecutionSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 新增正式字段缺迁移、测试 schema、DO/Mapper、索引、唯一约束或租户同源字段。 | schema 合同测试能证明生产提交、PQC 绑定、复核、FIFO、批记录字段审计和幂等键均有正式持久化支撑。 | 同 RED 命令 PASS。 | 不把 VO/DTO 字段或 rawPayload 当正式持久化完成。 |
| P0-T00B | 迁移发布策略门禁 | `workdir=D:\IntRuoyiWorktree\worktree_20260803_p0; python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql` | SQL 缺 release metadata、依赖、风险等级、环境范围，或 `NOT NULL` 收紧没有历史断链 fail-fast 保护；单文件诊断缺依赖闭包只能记为 `COMMAND-BLOCKED`。 | 正式 SQL 通过全量 policy gate；历史缺结构化来源 ID 时迁移明确失败并给出 backfill blocker，不写默认值、不解析 rawPayload。 | 同 RED 命令 PASS。 | policy gate PASS 不能替代目标 JUnit、测试 schema 和真实运行态迁移核验。 |
| P0-T00C | 真实运行态迁移核验 | `workdir=D:\IntRuoyiWorktree\worktree_20260803_p0; python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py` | 缺 `P0_RUNTIME_DB_HOST`、`P0_RUNTIME_DB_PORT`、`P0_RUNTIME_DB_NAME`、`P0_RUNTIME_DB_USER`、`P0_RUNTIME_DB_PASSWORD`，真实 MySQL 缺 P0 required columns/indexes，或历史断链检查返回 blocker。 | 验证器以只读连接确认当前运行态 required columns、indexes 和 historical checks 均 PASS；缺字段时必须返回 schema blocker 并跳过 historical SQL。 | 同 RED 命令 PASS；如果缺 env 或 schema blocked，记录 `BLOCKED` 而不是 GREEN。 | 该命令是真实 E2E 浏览器写入前置；不得用 release policy gate、H2 schema、测试容器或人工说明替代。 |
| P0-T01 | 主提交幂等 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 一线主提交请求缺少主提交级幂等键、重复请求产生重复事件，或部分失败重试留下半成功。 | 生产提交按提交幂等键或正式唯一约束保证只形成一条有效主事件；重复请求返回同一结果或明确重复拒绝。 | 同 RED 命令 PASS。 | 不把记录本幂等字段当成整个闭环幂等；不得吞掉重复请求；不得只靠前端按钮禁用。 |
| P0-T02 | 生产提交闭环合同 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionSubmitClosedLoopContractTest#shouldCreateFeedbackRecordbookAndProcessPoolEventInOneTransaction" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 不能同时证明报工、记录本和工序池事件同事务写入并互相关联。 | 返回 `feedbackId`、`recordbookEntryId`、`recordbookEventId`、`processPoolEventId`，且任一失败整体回滚。 | 同 RED 命令 PASS。 | 不用前端串联接口模拟事务。 |
| P0-T03 | PQC 入工序池事件 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 将旧断言 `never()` 改成期望创建 PQC 工序池事件后，当前实现应失败在未调用事件服务或缺设备/工作站/签名/幂等字段。 | PQC 提交创建或绑定 `PQC_INSPECTION` 工序池事件，并关联 PQC 任务、规程、逐件明细、实际员工、设备账号、设备、工作站、签名和幂等键。 | 同 RED 命令 PASS。 | PQC 事件不得旁路保存成孤立质量数据；不得保留旧 `never()` 断言。 |
| P0-T04 | PQC 质量门禁 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | FIFO 可消费失败、待检、质量状态无法确认、合格数量不足、只存在 rawPayload 绑定或未解析到生产提交根事件的数量。 | FIFO 确认写库前必须重新确认 PQC 结构化绑定目标生产提交根事件、质量状态可分配、合格数量覆盖本次确认数量且未被重复消耗；失败、待检、缺状态、数量不足或 rawPayload-only 绑定均 fail-fast，并且不写确认、分配、完成或批记录终态。 | 同 RED 命令 PASS。 | 不用默认合格、`inspectionResult=SUCCESS` 单字段、rawPayload 解析、历史 trace 或前端提示替代后端事务门禁。 |
| P0-T05 | 复核电子签名 schema | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | `mes_pro_process_pool_submission_review` 缺复核签名 ID、签名员工和签名快照。 | 复核记录模型和迁移包含正式签名字段。 | 同 RED 命令 PASS。 | 复核签名不能只写备注或登录用户。 |
| P0-T06 | 复核签名服务门禁 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 无签名仍可复核或确认分配；强制复核角色缺失时仍可 FIFO。 | `reviewSubmission` 和 `confirmSubmission` 必须校验复核签名，签名员工等于复核人或正式授权复核人，并在 FIFO 前校验所有强制复核角色。 | 同 RED 命令 PASS。 | 复核只写复核事实，不修改原提交。 |
| P0-T07 | FIFO 活跃订单闭环 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ActiveOrderFifoClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 分配可指向非活跃订单、总数与确认数量不一致，或并发确认产生重复分配。 | FIFO 和手工分配都只允许活跃生产工单，且总数、剩余数量、当前工序一致；写入事务内重新校验质量和强制复核。 | 同 RED 命令 PASS。 | 不回退到排产、创建时间或非活跃订单；不信任页面预检状态。 |
| P0-T08 | 工序完成批记录回填 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0BatchRecordBackfillClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 完成后无法证明回填来自正式逐工序绑定和字段映射，或重复确认产生重复字段审计。 | 完成触发正式批记录执行和字段审计，缺绑定或映射时阻塞；字段审计记录来源值、旧值、新值、单元格和幂等键。 | 同 RED 命令 PASS。 | 禁止使用 `formBindings`、默认 `MAIN`、空批记录或无来源值审计替代。 |
| P0-T09 | 统一闭环 trace 后端 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 现有 trace 分散，无法按事件返回提交、质量、复核、分配、完成、批记录字段审计。 | 新增或扩展 trace 服务，按 `processPoolEventId` 返回六分组和基础 blocker；该 GREEN 只代表 M3 initial，不代表 P0 完成。 | 同 RED 命令 PASS。 | trace 只读，不执行分配、复核、回填或修改；不得因六分组存在就返回 `complete=true`。 |
| P0-T09A | trace 质量绑定与候选歧义 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceQualityBindingTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | trace 只按同工单、同工序、时间接近、rawPayload 解析或第一条 PQC 记录拼接质量结果，或 `candidateEvents` 永远为空。 | 只有正式结构化绑定目标 `processPoolEventId` 或提交数量片段的 PQC 可完成；多候选返回 `candidateEvents` 且 `complete=false`。 | 同 RED 命令 PASS。 | 不用 nearest/first/order+process/rawPayload-only 拼接质量事实。 |
| P0-T09B | trace 复核聚合与强制角色 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceReviewGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | trace 只取第一条复核，或强制复核角色缺签名、缺来源事件时仍完成。 | 生产组长和配置要求的 PQC 组长复核均返回配置状态、实际状态、签名和来源事件；缺任一强制项时 `review.status=BLOCKED`。 | 同 RED 命令 PASS。 | 不用前端角色标签、备注或登录人推断复核完成。 |
| P0-T10 | trace 批记录来源与缺投影阻塞 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest,MesP0ProductionExecutionTraceBatchRecordSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 批记录字段审计、`sourceAllocationId` 或正式逐工序绑定缺失时仍返回追溯成功。 | 缺关键投影返回明确阻塞状态；批记录分组必须返回正式执行、报表/定义/版本、字段审计 batch/item、来源事件或来源分配 ID。 | 同 RED 命令 PASS。 | 不用空列表、摘要文案、`formBindings` 或默认 `MAIN` 冒充批记录追溯完成。 |
| P0-T11 | 前端静态合同 | `workdir=IntRuoyiFronted; pnpm e2e:p0-production-execution-loop:static` | 缺脚本、缺真实入口、缺签名字段、缺 trace UI 或错误展示。 | 页面和 API wrapper 暴露生产提交、PQC 提交、复核签名、FIFO 确认和 trace 入口。 | 同 RED 命令 PASS。 | 前端不得本地拼接越权数据。 |
| P0-T12 | 真实 E2E | `workdir=IntRuoyiFronted; pnpm e2e:p0-production-execution-loop:real` | 缺测试租户、账号、签名、活跃订单、PQC 任务、批记录绑定或 trace 入口时 BLOCKED；前置齐备但页面步骤未完成时 FAIL/RED。 | Playwright 走真实页面完成完整闭环并写入证据。 | 同 RED 命令 PASS；若仍为 BLOCKED 或 FAIL/RED，任务不得标记完成。 | API 只用于只读核验和清理证据。 |
| P0-T13 | 闭环收口证据包 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionClosureAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | trace `complete=true` 但九个审计问题任一缺业务值、正式 sourceIds、同源证据或只读复验入口。 | 后端能从 `processPoolEventId` 生成或返回闭环证据结构；九个审计问题全部由正式来源 ID 支撑，缺任一项则 `complete=false`。 | 同 RED 命令 PASS，并由真实 E2E 保存本次 run 的脱敏证据摘要。 | 不用截图、前端拼接 JSON、历史 ID、静态合同或人工说明补齐证据包。 |

## RED Commands

实现阶段必须记录以下类型的 RED 证据：

```text
RED: workdir=IntRuoyiFronted; python -X utf8 -c "<script existence check>" -> FAIL, P0 前端脚本尚未登记
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolSchemaTest,MesP0ProductionExecutionSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, P0 正式字段缺迁移、测试 schema、DO/Mapper、索引或唯一约束
RED: workdir=D:\IntRuoyiWorktree\worktree_20260803_p0; python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql -> FAIL, SQL 缺发布迁移元数据、依赖或历史断链 fail-fast 保护
RED/BLOCKED: workdir=D:\IntRuoyiWorktree\worktree_20260803_p0; python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py -> FAIL/BLOCKED, 真实运行态缺 DB env、P0 字段/索引或历史断链检查
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, PQC 正式提交尚未创建工序池 PQC 事件
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 班组长复核尚未要求电子签名
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 尚无按 processPoolEventId 聚合的生产执行闭环 trace
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionClosureAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, trace 无法逐项输出九个审计问题的正式来源证据
RED: workdir=IntRuoyiFronted; pnpm e2e:p0-production-execution-loop:static -> FAIL, P0 trace 页面或签名字段尚未接入
```

## M1 PQC RED Shape

P0-G01 / P0-T03 是后续实现的第一条业务 RED，必须满足以下形态后才允许 GREEN：

- 旧测试 `MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource` 必须从 `verify(processPoolEventService, never())` 改成捕获 `MesProcessPoolCreatePqcInspectionReqDTO`。
- RED 断言必须覆盖 `workOrderId`、`routeId`、`routeProcessId`、`processId`、`actualEmployeeId`、`deviceAccountId`、`deviceId`、`workstationId`、`templateType`、`feedbackSourceType/sourceId`、`recordbookSourceType/sourceId`、`inspectionResult`、`rawPayload`、`clientSubmitTime`、`signatureId`、`signatureUserId` 和 `signatureSnapshot`。
- 如果引入 `pqcSubmissionIdempotencyKey`，RED 必须覆盖请求 VO、命令对象、前端 payload 和后端服务，不能只在 DTO 中增加字段。
- 前端静态合同必须检查 `FrontlinePqcInspectionSubmitReqVO` 和 PQC payload builder 携带设备账号、设备、工作站、签名和 PQC 幂等键；只修后端、不修前端不能进入 P0-T03 GREEN。
- 当前测试若仍因为旧 `never()` 断言通过，必须记录为“测试尚未进入 RED”，不得记为业务 GREEN。

## Expected Failures

- 当前 PQC 提交测试应先失败在“未创建或绑定工序池事件”。
- 如果 `MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource` 仍因 `verify(..., never())` 通过，说明测试尚未进入 P0 RED 状态，必须先修改测试期望。
- 当前复核签名测试应先失败在 schema、VO 或服务层缺复核签名字段。
- 当前统一 trace 测试应先失败在缺少单一聚合服务或缺少 PQC/复核/批记录字段审计投影。
- trace 初版测试通过后，质量绑定测试仍应先失败在缺正式结构化生产提交绑定、只从 rawPayload 解析绑定或多候选未返回 `candidateEvents`。
- FIFO 质量门禁测试应先失败在 `confirmSubmission` 或等价确认写链路未在写入前重新校验 PQC 结构化绑定、合格数量、已消耗数量和生产提交根事件一致性。
- trace 复核聚合测试应先失败在只取第一条复核、缺强制角色配置状态或缺复核签名来源事件。
- trace 批记录来源测试应先失败在缺 `sourceAllocationId`、字段审计 item、字段路径、单元格位置、来源值或正式逐工序批记录绑定。
- 闭环收口证据测试应先失败在九个审计问题任一项缺正式 sourceIds、同源校验或只读复验入口。
- 当前主提交幂等测试应先失败在缺少主提交级幂等键或重复请求防护。
- 当前前端 P0 脚本检查应先失败在 package scripts 缺失；这是 M0 前置 RED，不是业务链路 PASS。
- 缺少正式批记录绑定、字段映射、电子签名、质量可分配状态时，应失败为明确 blocker。

## GREEN Commands

P0 第一版实现完成后至少运行：

```powershell
cd D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiBackend
cd D:\IntRuoyiWorktree\worktree_20260803_p0
python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql
cd D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesP0ProductionSubmitClosedLoopContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest,MesP0TeamLeaderReviewSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0ActiveOrderFifoClosedLoopTest,MesP0BatchRecordBackfillClosedLoopTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceServiceTest,MesP0ProductionExecutionTraceQualityBindingTest,MesP0ProductionExecutionTraceReviewGateTest,MesP0ProductionExecutionTraceFailureTest,MesP0ProductionExecutionTraceBatchRecordSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionClosureAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cd D:\IntRuoyiWorktree\worktree_20260803_p0\IntRuoyiFronted
pnpm ts:check
pnpm e2e:p0-production-execution-loop:static
pnpm e2e:p0-production-execution-loop:real
```

## Refactor Checks

- 不新增 fallback、默认成功、默认员工、默认设备、默认质量合格、默认签名或 mock 成功。
- 生产提交、PQC 提交、复核、分配和批记录回填都必须有正式结构化 ID 关联。
- 复核、审核副本、原始 revision、批记录回填是不同写链路，不得互相覆盖。
- 统一 trace 只能读取和聚合，不得带写副作用。
- trace `complete=true` 必须由后端源 ID 和 blocker 计算得出；前端静态合同不得替代后端完成判定测试。
- P0 闭环证据包必须由后端 trace 或只读核验事实生成；前端可以展示或保存脱敏摘要，但不得本地拼接缺失来源 ID。
- 批记录表单来源只能是工序设置中的正式逐工序批记录表单绑定。
- 前端只展示后端授权结果，不拿全量数据后本地隐藏。
- 新增字段必须通过迁移、测试 schema、DO/Mapper、服务写入、查询聚合和唯一/索引约束同步证明。
- 新增正式 SQL 必须通过 release migration policy gate；历史断链行必须阻塞并给出正式 backfill 解除条件，不得靠默认值或 rawPayload 伪造结构化绑定。
- 真实 E2E 浏览器写入前必须先通过 `verify_p0_runtime_migration.py` 只读核验真实 MySQL；缺 `P0_RUNTIME_DB_*`、schema blocked 或历史断链时只能 `BLOCKED`。
- 一对多读模型必须先聚合后分页，不能因 PQC 明细、复核历史或字段审计明细导致 trace 列表重复。

## Implementation Slice Locks

- P0-G01 / P0-T03 实现前，PQC 请求、命令对象和前端 payload 必须显式携带或正式解析 `deviceAccountId`、`deviceId`、`workstationId`、`pqcSubmissionIdempotencyKey`、`signatureId`、`signatureEmployeeUserId`、`signatureSnapshotJson`。
- P0-G02 / P0-T05-T06 实现前，schema、VO、DTO、服务层和前端复核表单必须使用同一组复核签名字段；禁止只在前端补字段或只在 DTO 补字段。
- P0-G03 / P0-T09-T10 实现前，trace DTO 必须冻结顶层 `complete`、`sections`、`blockers`、`candidateEvents` 和每个分组的 `sourceIds/status/blockers/lastUpdatedAt`；质量绑定不得只来自 rawPayload。
- P0-G03 / P0-T09-T13 实现前，`processPoolEventId` 在完整闭环中必须固定为 `PRODUCTION_SUBMIT` 生产提交根事件；PQC 事件只能作为 `quality.sourceIds.pqcEventId`，按 PQC 事件查询时必须解析到唯一生产提交根事件或返回候选/阻塞。
- P0-G03 初版 trace GREEN 只允许解除“没有统一入口/DTO”的缺口；质量唯一绑定、多候选选择、强制复核聚合、`sourceAllocationId` 和批记录字段审计未 GREEN 前，M3 只能标为 initial，P0 不得 completed。
- P0-G04 / P0-T01 实现前，生产提交、PQC 提交和组长确认必须分别有业务幂等键或唯一提交凭证，并在测试中覆盖重复点击、并发请求和部分失败回滚。
- P0-G05 / P0-T04-T07 实现前，后端必须冻结可分配质量状态白名单、PQC 合格数量计算口径、已消耗数量查询和确认数量勾稽；未冻结时仅明确合格且数量覆盖的片段可进入 FIFO，其它状态或数量不足全部 fail-fast。
- P0-T12 真实 E2E 实现前，real 脚本必须先验证 `P0_RUNTIME_DB_*`，调用 `verify_p0_runtime_migration.py`，并把 `runtimeMigration` PASS/BLOCKED/FAIL 写入 E2E evidence；缺 env 时不得启动浏览器。

## Evidence Log Template

```text
BDD: <场景名> -> Given/When/Then 摘要
RED: <命令> -> FAIL, <预期失败原因>
GREEN: <命令> -> PASS
COMMAND-BLOCKED: <命令> -> BLOCKED, <目标测试执行前失败的模块/原因和修正后的合法命令>
E2E: <命令> -> PASS/BLOCKED/FAIL, frontend=<url>, backend=<url>, tenant=<label>, dataPrefix=<prefix>
BLOCKER: <缺失正式前置> -> <影响和解除条件>
```

## Test Blockers

- 缺少真实电子签名能力或签名测试账号时，生产提交、PQC 提交和复核链路阻塞。
- PQC 任务、QA 规程快照或逐件明细模型不可用时，PQC 入池链路阻塞。
- 复核签名 schema 未完成时，班组长复核链路阻塞。
- 统一 trace 无法读取批记录字段审计投影时，P0 追溯链路阻塞。
- 前端无真实入口、菜单权限、路由、按钮或脚本时，真实 E2E 阻塞。
- 正式字段缺迁移脚本、测试 schema、DO/Mapper 映射、索引或唯一约束时，对应后端 slice 阻塞。
- 正式 SQL 缺 release migration policy gate PASS、历史缺 ID 行没有 fail-fast blocker、或运行态未应用迁移时，对应 schema slice 和真实 E2E 阻塞。
- trace 查询缺租户、权限、工单、路线工序或 MES 工序同源校验时，追溯链路阻塞。
- 无法为九个 P0 审计问题生成正式来源证据包时，P0 收口验收阻塞。
