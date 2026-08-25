# 验证报告

## 验证范围

本报告验证流程10专项实现、融合、启动 Bean 修复和当前主线程复核；不宣称全链路生产就绪。未运行 SQL 迁移或写入型 E2E。

## 结构结果

- PASS：任务目录存在。
- PASS：task.md、development-plan.md、test-plan.md、execution-log.md、verification-report.md 五个必需文件存在。
- PASS：文档覆盖目标态、当前代码事实、根因、修改边界、接口/数据/状态设计、BDD、RED/GREEN/REGRESSION、blocker、迁移/回滚和流程 7/8/9/11 契约。
- PASS：task.md 已记录任务目标、里程碑、预期验证、设计约束、经验门禁和当前状态。

## 代码符合性结论

PASS（专项范围）：流程10实现符合统一最终放行终态和追溯出口设计；全链路仍 No-Go。

证据：

1. 多入口保留各自权限，但终态写入统一进入 finalizeRelease；submitForApproval 仅准备审批。
2. 唯一 release decision 使用版本/CAS 和事务级唯一约束；订单/工单仅通过 owner 受控端口收敛，领料单不被直接改写。
3. 记录来源关系、快照/hash、材料 manifest、独立来源凭证和条件化 active-order 关系。
4. 流程8四材料 gate、流程4/6正式回执接口和审批 owner 校验已纳入代码合同测试。

## 目标设计结论

development-plan.md 规定流程 6 负责三类回填成功后的批次执行创建/复用，流程 10 只消费已存在 batchExecutionId；所有放行入口统一调用 finalizeRelease。active-order 来源校验流程 1 pickListBindingId/sourceSnapshotHash、流程 4 BACKFILL_SUCCEEDED receipt、双 100% 和三类回填；MANUAL/SCHEDULED/PQC_INDEPENDENT 来源校验 IndependentBatchPrerequisiteReceipt、正式 source relation、来源快照/hash 和自身前置，不伪造 activeOrderId、pickListId 或 completionBackfillReceipt。流程 8 提供四份材料硬门禁，流程 7 提供映射和 trace graph，流程 10 只写唯一 release decision/release transaction 终态；订单、工单后续状态通过各自 owner 的受控命令或事件收敛，领料单不被直接改写。

## 未解决 blocker

流程4/6/8权威持久化凭证适配器、审批中心权威上下文接入、生产迁移/历史回填、跨服务 outbox 投递和全链路真实 E2E 尚未完成；历史未关联批次必须先迁移审查，不能自动放行。流程11任务文档已交付，不是当前协作 blocker。

## 主线程验证证据

 - 当前 `int_main` HEAD：`a6574c3631dfa3c5f8381596fcef5c91acd98db0`；`9b18ee093`、`1b59dd8d2`、`0002767c0` 均为其祖先。
 - 流程10 focused suite 命令：`mvn -pl yudao-module-mes "-Dtest=MesReleaseAuthoritativeContextConfigurationTest,MesReleaseFinalizationValidatorTest,MesProEdhrReleaseServiceImplTest,MesProductionReleaseManagerApprovalServiceTest" test`；退出码 0，47 tests, 0 failures, 0 errors。
 - yudao-server package 命令：`mvn -pl yudao-server -am -DskipTests package`；退出码 0，BUILD SUCCESS。
- commit diff-check：PASS；branch runtime guard：PASS（int_main，frontend 8081/backend 48081）。

## 最终判定

文档交付：PASS。

生产代码符合性：PASS（流程10专项范围）；全链路 No-Go，权威适配器、迁移/历史回填、outbox 和真实 E2E 未完成。

任务限制遵守：PASS；未执行数据库迁移或写入型 E2E，未绕过权限/凭证门禁；仅为启动烟雾验证启动本地 server。

## 启动 Bean 修复验证证据

- 9b18ee093 已进入 int_main，新增 MesReleaseAuthoritativeContextConfiguration 显式 @Bean，移除实现类上的扫描条件注解。
 - MesReleaseAuthoritativeContextConfigurationTest：2/2 PASS；端口类型 Bean 恰好一个，且未接入权威适配器时返回结构化 blocker。
- 流程10定向合同 suite：47/47 PASS。
- mvn -pl yudao-server -am -DskipTests package：BUILD SUCCESS。
- 实际启动 yudao-server-exec.jar：48081 LISTEN；GET http://127.0.0.1:48081/actuator/health 返回 status=UP。
- 运行时 nested yudao-module-mes JAR 中配置类和 blocker 类 SHA-256 与当前构建产物一致；启动日志无缺失 Bean 或应用启动失败。
- 流程4/6/8适配器未接入时仍保留 AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED 结构化阻断，不伪造放行成功。

## 本轮打包与启动复核（2026-08-25）

- `mvn -pl yudao-server -am '-Dmaven.test.skip=true' '-Dcheckstyle.skip=true' package`：退出码 0，`BUILD SUCCESS`，生成 `IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`。该命令跳过所有测试源编译，不能替代测试证据。
- 实际启动该新包（local profile，显式传入已有本地 DCC artifact-directory、MySQL/Redis 参数）：日志 `E:/IntRuoyi/output/runtime/int_main/logs/flow10-explicit-20260825-125343.log` 出现 `Started YudaoServerApplication`，未出现 `APPLICATION FAILED`。
- 启动期间 `48081` LISTEN；`GET /actuator/health` 返回 HTTP 200 和 `{"status":"UP"}`。本轮启动进程已在核验后停止。
- 已有 `MesReleaseAuthoritativeContextConfigurationTest` 的 2/2 PASS 继续作为唯一 Bean 配置 smoke 证据；本轮新增权威上下文组合测试未能运行，原因是其它未跟踪 MES/BPM 测试源的既有语法/编译错误，详见 execution-log，未修改这些文件。
- 结论：流程10实现可编译、可打包、可启动；缺少流程8持久化材料 receipt 适配器时仍结构化阻断。任务状态保持 `ready_for_closeout`，不能标记全链路完成。

## 当前复核（2026-08-23）

- `48081`：LISTEN，PID 37224。
- `GET http://127.0.0.1:48081/actuator/health`：退出码 0，返回 `{"status":"UP"}`。
- 最新运行日志：`E:/IntRuoyi/output/runtime/int_main/bean-fix-20260823-1551/logs/yudao-server.log`；包含 `Started YudaoServerApplication` 和“项目启动成功”，未匹配 `APPLICATION FAILED TO START`、缺失 `MesReleaseAuthoritativeContextPort` 或 executor 构造注入失败签名。
- 任务状态保持 `ready_for_closeout`；仅全链路权威适配器、迁移/历史回填、outbox 和真实全链路 E2E 仍为 No-Go blocker。

## 复核修订项

- PASS：流程 6 创建/复用批次执行，流程 10 只消费 batchExecutionId；所有放行入口统一 finalizeRelease。
- PASS：前置按 origin/entryType 条件化，独立来源统一使用 IndependentBatchPrerequisiteReceipt。
- PASS：状态 owner、流程 1/4/5/6/7/8/9/11 契约和 owner 受控联动边界已写明。
- PASS：四份材料固定为来料检报告、灭菌报告、成品检报告、成品检记录；历史三材料仅为迁移阻断，旧开关仅为实现 blocker。
- PASS：RED、GREEN、REGRESSION 均诚实标记；实际定向验证单独列出，未把计划结果冒充 GREEN PASS。

## 终态分区复核（2026-08-23）

- 当前 int_main 验证基线：7770f36fb6ed64f4e306320410d131f184cf2789。
- MesProEdhrTraceTerminalPartitionContractTest：修复前 2 failures；修复后 2/2 PASS。修复内容只扩大 completedTraceOnly 的终态分区到 RELEASED、ARCHIVED、REJECTED，并继续排除 VOIDED。
- 流程10 focused suite：47/47 PASS；包含终态分区的扩展 suite：49/49 PASS。
- mvn -pl yudao-server -am -DskipTests package：BUILD SUCCESS。
- 48081：既有 runtime-control PID 4176 LISTEN；GET /actuator/health 返回 {"status":"UP"}；启动日志最新成功段包含 Started YudaoServerApplication 和“项目启动成功”，未见最新启动段的 APPLICATION FAILED TO START 或 MesReleaseAuthoritativeContextPort Bean 缺失。
- 流程10仍是唯一 release transaction RELEASED owner；流程7继续拥有 Origin/TraceLink 来源映射；流程4/6/8适配器未接入时结构化 blocker 仍 fail-fast，无默认成功。
- 本轮不停止 PID 4176：它是既有长期 runtime-control 服务，非本轮启动进程；无残留的本轮前台服务需要清理。
- 独立启动日志 E:/IntRuoyi/output/runtime/int_main/bean-fix-20260823-1551/logs/yudao-server.log：只匹配 Started YudaoServerApplication 和“项目启动成功”，未匹配 APPLICATION FAILED、BeanCreationException 或 MesReleaseAuthoritativeContextPort 缺失。

## 权威上下文输入面复核（2026-08-24）

- 基线：int_main 628fb8a990952fce7ef9128d958b728c5aa9f6c5。
- 发现：最终化命令和批准 DTO 曾暴露客户端嵌套 independentPrerequisiteReceipt、materialGateReceipt 字段，虽然当前 unavailable port 不读取它们，但接口合同未明确禁止未来适配器误用。
- 修复范围：仅在 MesReleaseFinalizationCommand 和 MesProEdhrReleaseApproveReqVO 的嵌套凭证字段增加 @JsonIgnore；客户端仍可提交 receipt ID/hash，权威 payload 只能由 MesReleaseAuthoritativeContextPort 返回。
- BDD/TDD：新增 MesReleaseFinalizationRequestContractTest，断言四个 HTTP 嵌套凭证字段均标记 @JsonIgnore。
- 验证：单类 1/1 PASS；配置、请求面和 validator 回归 11/11 PASS；两条 Maven 命令均 BUILD SUCCESS。
- 无跳过参数复核：MesReleaseFinalizationRequestContractTest 1/1 PASS，BUILD SUCCESS。
- 结论：需要最小接口层代码修改，已完成；不修改 finalizeRelease 主逻辑，不创建流程6未提供的凭证解析器。
- 未解决 blocker：流程4/6/7/8持久化 owner 适配器仍未接入；在适配器接入前，流程10必须返回结构化 AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED，不得放行。

## 本轮代码复核（2026-08-25）

- `MesReleaseAuthoritativeContextPortImpl` 已作为流程10正式 `@Service` 实现，批准入口仍统一进入 `finalizeRelease`；它不创建批次，也不把客户端嵌套 receipt 当作权威事实。
- 流程6：先读取已有 `batchExecutionId`，仅接受 `READY_TO_CLOSE/CLOSED` 批次状态；缺失或状态不符返回结构化 blocker。
- 流程7：服务端读取 Origin/TraceLink 预检，要求 batch、origin link、trace hash、source snapshot hash 和 `CAPTURED/READY` 关系状态一致。
- 流程8：新增 `MesReleaseMaterialGateReceiptPort` 作为持久化 `MATERIALS_READY` receipt 的 owner 端口；端口缺失、重复或返回不完整 receipt 均 fail-fast。没有从附件结果拼接 receipt 的旁路逻辑。
- 活跃订单与独立来源已条件化：活跃订单保留流程4成功回执、领料绑定、双100%和三类回填门禁；独立来源使用现有服务验证 `IndependentBatchPrerequisiteReceipt` 并要求 Flow7 source credential hash 一致，不要求 activeOrderId/pickListId。
- 验证命令：`mvn -pl yudao-module-mes -am '-DskipTests' '-Dcheckstyle.skip=true' compile` -> `BUILD SUCCESS`。
- 验证阻断：新增测试尚未运行。带 `-am` 的定向 test 被 BPM 现有 `FormTemplateFillRuleAutoDetectServiceTest.java:96` 编译错误阻断；不带 `-am` 的 MES test 被现有 `MesProEdhrBatchTraceFormalSourceResolverTest.java:65-66` 语法错误阻断。两项均为非流程10文件，本轮未修改。
- 当前状态：`ready_for_closeout`。流程8持久化 receipt 实现、流程4/6正式适配器联调、迁移/历史回填、outbox 和真实全链路 E2E 仍为 No-Go。
