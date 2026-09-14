# Execution Log

## Baseline

- 2026-08-09：用户要求主 Agent 启动 A1-A6、逐项 review、返修直至满足开发/测试文档，并完成集成测试。
- 2026-08-09：复用已批准 V4、M0、AC-01 至 AC-15、BDD/TDD/E2E，不重新规划业务来源。
- 2026-08-09：建立监督任务工件和依赖图；最多 4 个活跃 Agent，先 A2 RED，再 A3-A5，后 A2 集成/A1/A6。
- 2026-08-09：工作区为 `int_main` 且有大量非本任务改动；禁止 Agent 修改、清理、提交或回退不在 write scope 的文件。

## BDD/TDD

- BDD: 真实历史形成双 100 后申请 -> Given/When/Then 见 task.md。
- BDD: 正式来源缺失时阻塞 -> Given/When/Then 见 task.md。
- BDD: 重复申请幂等 -> Given/When/Then 见 task.md。
- BDD: 负责人处理 -> Given/When/Then 见 task.md。

## Agent Passes

等待 A2-RED。

### A2-RED

- BDD: 三类正式资料完成后才创建放行待办 -> Given 活跃订单具有正式生产完成、PQC CONFIRMED 汇集和三类传统报表绑定；When 生产组长申请放行；Then BatchRecordWriter、ProcessInspectionWriter、LossReportWriter 产出均完整且签名证据大于 0，完成性检查之后才允许 precheck 和 submitForApproval。
- BDD: 任一 writer 无法产出时无副作用阻塞 -> Given 损耗单缺少正式传统报表绑定；When 生产组长申请放行；Then 返回 LOSS_REPORT_SOURCE_REQUIRED，且不创建 batch execution、release transaction 或 RELEASE_APPROVE 待办。
- BDD: PQC 只有 SUBMITTED 不构成检验 100% -> Given 生产完成但 PQC 任务只有 SUBMITTED；When 生产组长申请放行；Then 在创建 batch execution、release transaction 和待办前拒绝申请。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest' test` -> FAIL；Tests run: 3, Failures: 3, Errors: 0, Skipped: 0。测试已正常编译并进入业务断言，不是路径、依赖、fixture 或编译错误。
- RED 失败断言：完整三类传统报表绑定场景预期 `PENDING_RELEASE_APPROVAL`、`signatureEvidenceCount > 0` 和唯一待办 `9001`，当前返回 `BLOCKED`、签名证据为 0、待办为空，且未调用 precheck/submitForApproval，证明没有三 writer 正式生成链路。
- RED 失败断言：缺损耗单正式输入场景预期 `BLOCKED + LOSS_REPORT_SOURCE_REQUIRED` 且 batch/precheck/submit 均不调用，当前返回 `PENDING_RELEASE_APPROVAL` 并实际调用三者、创建待办 `9001`，证明 writer blocker 未在待办前阻断。
- RED 失败断言：PQC 只有 `SUBMITTED` 场景预期在创建任何放行对象前抛出进度错误，当前没有抛出，证明后端仍把 `SUBMITTED` 错计为检验 100%。
- A2-RED 变更路径：`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseOrchestrationRedTest.java`、本日志。
- A2-RED 覆盖：AC-05（CONFIRMED-only 双 100）、AC-11（三资料完成后唯一待办）、AC-13（任一 writer blocker 无 batch/事务/待办副作用）、AC-14（成功路径 submitForApproval 仅一次且固定返回唯一待办）、AC-15（成功回执必须含正式签名证据，阻塞必须可定位）。
- A2-RED 状态：完成并停止；未进入 GREEN，未修改生产代码、前端、数据库或主 Agent 状态文件。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest" test` -> FAIL, 3 tests/3 failures/0 errors；失败点精确为缺三 writer/签名证据、缺损耗 writer 仍创建正式对象、SUBMITTED-only PQC 未拒绝。
- 主 Agent review：APPROVED。测试通过真实 service + mocked formal dependencies 证明当前副作用顺序错误；不是编译错误、fixture 错误或泛化断言。

## Orchestration Blocker

- 2026-08-09：A2-RED 完成后，主 Agent 尝试启动 A3-A5 并行波次。
- 连续三个 goal 续跑回合中，后续 collaboration 调度调用均被错误路由到不存在或已关闭的 `exec wait` cell，无法执行 `spawn_agent`、`followup_task` 或可靠恢复 Agent。
- 已验证 `functions.exec` 的动态工具集合不暴露 `spawn_agent`/`collaboration__spawn_agent`，不能通过安全替代路径调度。
- 影响：A3-A6 不能按用户要求由独立子 Agent 开发和接受主 Agent review；A2-INTEGRATE、A1、A6、独立测试和集成测试均不能开始。
- 处理：机器状态设为 `blocked`，保留 A2 RED 和监督工件；不由主 Agent冒充六个子 Agent，不使用新用户线程或外部 CLI 进程替代正式子 Agent。
- 2026-08-09 resumed audit #1：用户要求再次尝试；连续重试 Agent 调度后仍被路由到不存在的 `exec wait` cell（`try`、`try2`），A3-A6 仍无法创建。本次按 fresh blocked audit 只记录失败，不新增替代实现路径。

## Orchestration Correction

- 用户指出并经重新执行确认：此前失败是主 Agent 将 `spawn_agent`、`followup_task`、`list_agents` 错误发送到 `functions.wait`；这些调用未到达 collaboration 调度器。
- 更正：不是 A3-A6、并发额度、项目代码或任务配置故障；此前 `blocking_prereqs` 已清空，监督状态恢复为 `executing`。
- GREEN: 直接调用 collaboration `spawn_agent` -> PASS，已创建 `/root/a3_batch_record_writer`。
- GREEN: 直接调用 collaboration `spawn_agent` -> PASS，已创建 `/root/a4_process_inspection_writer` 和 `/root/a5_loss_report_writer`。
- 当前 A3、A4、A5 已按依赖图并行执行；A6 仍等待 A1 和 A2-INTEGRATE。

### A3 Batch Record Writer

- BDD: 当前批次任务生成正式批记录 -> Given 活跃订单工序已有 COMPLETED 回填、逐工序唯一正式 `useType=BATCH + recordCategory=BATCH_RECORD` 绑定、生产提交和生产组长 APPROVED 复核均有真实签名；When A3 在当前 `batchExecutionId/batchExecutionTaskId` 写入；Then 生成或复用只属于该批次任务的批记录 execution，返回字段审计批次、来源对象/值 hash 及填写和审核签名证据。
- BDD: 正式来源或当前任务不完整时无副作用阻塞 -> Given 缺正式绑定、映射、生产签名、组长确认签名，或 eDHR 任务不属于当前批次/报表；When A3 先执行 plan 或 write；Then 返回定位 blocker 或 fail fast，且不打开 execution、不保存字段审计。
- BDD: 同一来源重复写入幂等 -> Given 当前批次任务已按同一 completion aggregate hash 写入；When A3 重复执行；Then 复用同一 execution 和字段审计幂等键，不把历史 completion backfill execution 冒充本次输出。
- RED 计划命令：`mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest" test`；预期因 A3 writer/plan/result 及当前 batch/task 适配尚不存在而 FAIL，且失败不得来自测试 fixture 或错误模块路径。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；目标模块进入 `testCompile` 后因 `MesTeamLeaderActiveOrderReleaseBatchRecordWriter` 与 plan command 尚不存在而失败，符合 A3 未实现的预期原因。首次未带 `surefire.failIfNoSpecifiedTests=false` 的命令在上游 `yudao-common` 因无同名测试提前失败，不作为 RED 业务证据，已按 Maven reactor 门禁修正后复验。

### A4 Process Inspection Writer

- BDD: 已确认 PQC 汇集生成正式过程检验单 -> Given 活跃订单的 PQC task 为 `CONFIRMED`，event/record/aggregate/APPROVED 复核签名互相一致，且产品工序存在当前 `PUBLISHED` QA 版本、项目设备、传统 `PROCESS_INSPECTION` 绑定和 `PQC_AGGREGATE_DETAIL` 映射；When A4 在当前 `batchExecutionId/batchExecutionTaskId` 写入；Then 创建或复用 `recordCategory=INTERNAL_RECORD + validationProfile=INTERNAL_TRACE` 的传统 execution，逐项写入字段审计，并返回来源值 hash 与 PQC 填写/审核签名证据。
- BDD: 非确认、缺 QA/设备/映射/签名时无副作用阻塞 -> Given task 仅 `SUBMITTED`，或 aggregate 与发布 QA 项目/方法/标准/上下限/设备/判定不一致，或缺正式签名、传统绑定、启用映射；When A4 执行无副作用 plan；Then 返回可定位 blocker，且不打开 execution、不保存字段审计。
- BDD: 当前批次任务身份错误时 fail fast -> Given eDHR 批次任务不属于当前批次、工序、正式 report/version 或 `formSlotType` 不是 `PROCESS_INSPECTION`；When A4 进入 write；Then 在任何 execution/audit 写入前直接失败，不切换到 `formBindings`、FormCenter 或默认 `MAIN`。
- BDD: 同一正式来源重复写入幂等 -> Given 同一来源快照、映射和当前批次任务已经写入；When A4 重复 write；Then 使用稳定幂等键复用相同 execution/字段审计结果，来源 hash 不包含 raw payload、易变状态、当前用户或当前时间。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；目标模块进入 `testCompile` 后仅因 A4 的 reader/writer/plan/result 尚不存在而出现 9 个缺符号错误，符合生产实现未建立的预期原因；未出现错误 fixture、现有产品代码或错误 Maven 模块导致的失败。
- A4 实现：新增正式 PQC reader、无副作用 plan、当前批次传统报表 writer 和结果证据模型；仅接受 `CONFIRMED` PQC task、结构化 aggregate、已签名 event/record、`APPROVED` 签名复核及匹配产品/路线版本/工序的当前 `PUBLISHED` QA version/items/equipment；目标固定为 `batchRecordReportId + PROCESS_INSPECTION + INTERNAL_RECORD + INTERNAL_TRACE + QUALITY`，映射 `sourceType=PQC_AGGREGATE_DETAIL`，不读取 `formBindings`、FormCenter 或默认 `MAIN`。
- A4 GREEN 前修正：一次复验在全模块 `testCompile` 阶段同时暴露 A4 fixture 对继承租户字段误用 builder，以及当时 A2/A5 并行测试引用的生产类尚未落盘；A4 仅修正自身 fixture，等待共享工作区对应类完成，未修改 A2/A5 文件或共享契约。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；Reactor `BUILD SUCCESS`，A4 Tests run: 8, Failures: 0, Errors: 0, Skipped: 0。覆盖成功写入、`SUBMITTED` 阻断、QA 设备不一致、非法数值不可伪装失败判定、缺映射、缺复核签名、错误当前批次任务 fail fast、重放幂等与 raw payload 不入 hash。
- A4 reader 补充覆盖：新增 2 个 reader 聚焦测试，验证 aggregate 的唯一 event/record/review 和当前发布 QA graph 被原样读取，以及多 event ID 时不任意选择签名来源。新增测试后正常生命周期复验先后被并行任务中的 `yudao-module-system` 大量缺类、`MesTeamLeaderActiveOrderErpPlannedStartTest` 未同步新增构造参数阻断，均发生在 A4 测试执行前，编译输出无 A4 错误。
- GREEN: 先用 Maven `dependency:build-classpath` 取得模块测试 classpath，再以 `javac -encoding UTF-8` 仅编译 A4 reader/writer 两个测试，最后执行 `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" surefire:test` -> PASS；Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。此定向命令不把上述并行任务的全模块 `testCompile` 失败记录为通过，A2 集成后仍需恢复标准生命周期复验。
- A4 静态证据：A4 生产文件不存在 `rawPayload`、`LocalDateTime.now`、`SecurityFrameworkUtils`、`formBindings`、FormCenter 或默认 `MAIN` 引用；task/review/lifecycle/aggregation status 只用于前置校验，不进入 A4 `hash*` 方法。
- A4 集成依赖：现有共享 `MesProBatchRecordExecutionFieldAuditService.saveSystemCellLinkChanges` 在未传密码签名时会调用 `recordFieldChangeDraftSave`，其审计批次 actor/time 来自当前登录会话和数据库当前时间，接口不能导入既有 PQC signature ID/user/time。A4 已将正式 PQC 填写/审核人员与时间写入目标格、返回原始签名证据，并在 audit reason 记录两个原始签名 ID；若 A2 完成性要求“字段审计批次自身 actor/signature 行”也必须等于 PQC 原始签名，则需由共享审计契约另行提供正式 source-evidence import 端口，A4 未越权修改该共享服务。
- A4 复审 BDD: 非数值正式实测值不得在 plan 丢失 -> Given 发布 QA 项目和 CONFIRMED aggregate 的 `resultType=CHOICE/BOOLEAN/STRING` 且 `measuredValue` 为正式字符串；When 执行 side-effect-free plan；Then mapped value 保留原始正式字符串，不延迟到 write 才失败；`NUMBER` 仍在 plan 解析为 `BigDecimal`，非法数值形成确定 blocker。
- RED: 定向编译最新 A4 WriterTest 后执行 `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" surefire:test` -> FAIL；Tests run: 9, Failures: 1, Errors: 0；`choiceAggregateKeepsItsFormalMeasuredValueDuringSideEffectFreePlan` 预期 `合格`，实际为 `null`，精确证明 `numericMeasuredValue` 丢失 CHOICE 正式来源值。
- A4 复审修复：将 `numericMeasuredValue` 改为按正式 `resultType` 映射的 `mappedMeasuredValue`；`NUMBER` 在 plan 转为 `BigDecimal`，`CHOICE/BOOLEAN/STRING` 原样保留正式字符串，未知类型 fail fast；既有非法 NUMBER 测试继续以 `PQC_QA_ITEM_MISMATCH` 确定 blocker 阻断，未放宽签名、QA version、传统绑定或映射门禁。
- GREEN: 仅定向编译 A4 变更类后执行 `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" surefire:test` -> PASS；Tests run: 9, Failures: 0, Errors: 0, Skipped: 0。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" surefire:test` -> PASS；Tests run: 11, Failures: 0, Errors: 0, Skipped: 0。遵守主 Agent 指令，未与其他 Agent 并发运行全量 MES Maven 生命周期。

### A5 Loss Report Writer And Completeness

- 主 Agent 共享契约决策：A5 RED 需要 M0 已冻结的 blocker 可选定位字段，特别授权 A5 只在 `MesTeamLeaderActiveOrderReleaseBlocker` 增加 `routeProcessId/processId/fieldCode/cellKey`；不得改其他共享契约，A2-INTEGRATE 后续负责统一消费。

- BDD: 正损耗正式来源生成当前批次损耗单 -> Given 当前活跃订单的生产提交事件已签名，事件关联正式 `MesProFeedbackDO`、当前活跃订单分配和生产组长 `APPROVED` 签名复核，结构化 `lossDetails[]` 与反馈损耗总量、分类数量和原因快照精确对账，且存在传统 `LOSS_REPORT` 绑定、`PRODUCTION_LOSS` 映射和当前批次任务；When A5 先 plan 再 write；Then 创建或复用只属于当前 `batchExecutionId/batchExecutionTaskId` 的 `INTERNAL_RECORD/INTERNAL_TRACE` execution，保存逐字段审计并返回 source object/value hash 与填写/审核签名证据。
- BDD: 损耗来源、映射或签名不完整时无副作用阻塞 -> Given 总量与明细不一致、原因缺失、生产/审核签名缺失、传统绑定或启用映射缺失；When A5 执行 side-effect-free plan；Then 返回定位到工序/字段/单元格的 `LOSS_SOURCE_REQUIRED`、`PRODUCTION_SIGNATURE_REQUIRED`、`LOSS_REPORT_BINDING_REQUIRED` 或 `LOSS_REPORT_MAPPING_REQUIRED` blocker，且不打开 execution、不保存字段审计。
- BDD: 未证明无损耗确认字段时阻止空损耗单 -> Given 当前活跃订单正式反馈损耗合计为零，且没有已证明的正式无损耗确认字段及启用映射；When A5 计划损耗单；Then 返回 `ZERO_LOSS_CONFIRMATION_UNSUPPORTED`，不创建空损耗单或使用默认 `MAIN`/动态 `formBindings` 替代。
- BDD: 三资料完成性只校验不创建待办 -> Given 批记录、过程检验单、损耗单 evidence 均应属于同一 batch、具备 report/version/snapshot、必填字段审计、来源 hash、填写/审核签名、PQC/损耗一致性与有效 `RELEASE_APPROVE` 候选；When A5 执行 completeness check；Then 只有全部证据完整才返回 complete，任一缺失返回具体 blocker，且该组件没有 precheck、`submitForApproval` 或 work-task 创建依赖。
- BDD: 同一损耗快照重放稳定 -> Given 同一正式 feedback/event/review/binding/mapping/source hash；When A5 重复 write；Then 使用稳定幂等键复用同一 execution 和字段审计，不以当前用户、当前时间、原始 payload 或历史 execution 生成新结果。
- A5 共享契约依赖处理：按主 Agent 明确授权，仅在 `MesTeamLeaderActiveOrderReleaseBlocker` 增加 M0 已冻结的四个可选定位字段 `routeProcessId/processId/fieldCode/cellKey`，未改变原字段语义或增加其它共享行为；已通知 A3/A4，A4 明确不修改该共享类并兼容四字段。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 首次在 A5 生产类尚不存在时包含预期的 A5 reader/writer/completeness 缺符号；当时 A3-A5 并行 Maven 共用 `target` 还产生了非 A5 缺符号输出，因此该次只保留 TDD 时序证据，不作为隔离验证结论。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 稳定编译后 Tests run: 10, Failures: 1；失败原因是结构化 `lossDetails.quantity` 从 JSON 读取后丢失正式反馈的三位数量精度（期望 `2.500`，实际 `2.5`），其余 9 项通过。
- A5 最小实现：新增正式损耗 reader、side-effect-free plan、当前批次传统 `LOSS_REPORT` writer、通用 document/signature evidence 和三类资料完成性 checker。reader 仅解析已签名 `PRODUCTION_SUBMIT` 的精确 `lossDetails` 字段并与 `MesProFeedbackDO`、当前活跃订单 allocation、生产 `APPROVED` review 闭环；writer 只接受 `PRODUCTION_LOSS` 映射和 `PRODUCTION` owner 的 `LOSS_REPORT/INTERNAL_RECORD/INTERNAL_TRACE` 传统任务，正损耗总量/分类/明细精确对账，零损耗返回 `ZERO_LOSS_CONFIRMATION_UNSUPPORTED`。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" test` -> PASS；BUILD SUCCESS，Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。
- A5 静态证据：writer/completeness 不引用 `rawPayload`、当前用户、当前时间、`formBindings`、默认 `MAIN`、历史 execution、precheck 或 `submitForApproval`；reader 唯一的 raw payload 读取仅用于精确提取 `lossDetails`，测试证明 `lossQuantity=999` 和旧别名 `lossReasonDetails` 均不能作为正式来源。重放幂等键只由当前 source snapshot、当前 batch task、工序和正式 evidence hash 组成。
- A5 已知集成约束：共享 `saveSystemCellLinkChanges` 仍以当前会话/数据库时间形成审计批次 actor/time，无法把既有生产填写/审核签名导入审计签名行；A5 返回并哈希原始生产签名证据、将填写/审核人员与时间写入正式目标格，并在 audit reason 记录原始签名 ID。若 A2 要求审计批次自身签名行等于生产原始签名，需扩展共享审计导入端口，A5 未越权修改该共享服务。
- BDD: 路线 template 25 损耗单写入当前 FormCenter 实例 -> Given 工序存在唯一 `LOSS_REPORT` template 25 路线绑定、`FORM_TEMPLATE_VERSION + FORMTPL:<versionId>` 的 `PRODUCTION_LOSS` 映射和当前 eDHR batch 已关联的唯一 `ROUTE_FORM/LOSS_REPORT` 动态任务；When A5 write 执行；Then 仅写该任务已关联的 FormCenter instance 并提交 EFFECTIVE，返回 instance、submit snapshot/head hash、source/signature evidence，不打开传统 execution。
- RED: P4 复审聚焦命令在动态 FormCenter 成功路径修正前 -> FAIL；`MesTeamLeaderActiveOrderReleaseLossReportWriterTest` 9 tests / 0 failures / 2 errors，两个错误均由 `requireCurrentBatchTask` 抛出“当前 eDHR 批次缺少唯一 LOSS_REPORT 正式目标任务”，证明 writer 成功路径未使用当前 batch task 已关联的唯一动态 LOSS_REPORT 目标任务。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；BUILD SUCCESS，Tests run: 17, Failures: 0, Errors: 0, Skipped: 0。覆盖动态 template 25 当前任务匹配、FormCenter EFFECTIVE 写入、传统损耗 writer、正式损耗来源 reader 和三资料完成性。
- BDD: 动态损耗单 task 必须匹配路线 record 快照 -> Given template 25 路线绑定的 `recordCategorySnapshotHash` 与当前 eDHR `ROUTE_FORM/LOSS_REPORT` task 的 `routeBindingSnapshotHash` 不一致；When A5 动态端口 write；Then 在读取/保存 FormCenter 草稿前以 source-required 失败，不调用 `saveDraft` 或 `submitInstance`，避免把过期绑定实例当作正式放行资料。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest" test` -> FAIL；新增 `recordCategorySnapshotMismatchFailsBeforeDraftOrSubmit` 后 Tests run: 3, Failures: 1，失败原因为 record hash 错配时旧实现仍调用 `FormCenterRuntimeService.saveDraft`。
- A5 record-hash 修正：`MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImpl.validateWriteCommand` 同时要求 `binding.recordCategorySnapshotHash` 非空且等于 `task.routeBindingSnapshotHash`，并继续校验 slot hash、binding id、template 25、version、instance 身份；成功夹具改为使用正式 record hash。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest" test` -> PASS；BUILD SUCCESS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；BUILD SUCCESS，Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；BUILD SUCCESS，Tests run: 16, Failures: 0, Errors: 0, Skipped: 0。该命令为 P4 主审指定的 writer/dynamic port/completeness 聚焦验收。

### A3 GREEN - Batch Record Writer

- Review-driven BDD: 当前批次任务必须在 plan 阶段完成匹配 -> Given M0 4.1 要求 writer 先执行无副作用 plan/validate；When 当前 `batchExecutionId` 中不存在与逐工序正式批记录绑定完全一致的唯一 task；Then plan 返回定位到 `BATCH_EXECUTION_TASK` 的 `BATCH_RECORD_BINDING_REQUIRED` blocker，write 不得打开 execution 或保存字段审计。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；新增 M0 公共输入和 plan 阶段 task blocker 断言后，`testCompile` 精确失败于 `MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand` 缺少 `setWorkOrderId`，证明旧 plan command 未冻结 `workOrderId/productId/batchCode/applicantUserId/batchExecutionId`，且旧实现把 task 校验延迟到了 write。
- A3 实现：新增 `MesTeamLeaderActiveOrderReleaseBatchRecordWriter`、plan command/plan/result/impl；plan 只读取逐工序正式 `useType=BATCH + recordCategory=BATCH_RECORD` 绑定、`PROCESS_POOL_REPORT` 映射、当前 eDHR task、正式生产提交/分配/完成/生产组长复核及原始签名，不写 execution/audit。plan 已冻结当前 `batchExecutionId` 和匹配 task；存在 blocker 时 write 直接失败且不调用 backfill。
- A3 backfill 小适配：`MesTeamLeaderBatchRecordBackfillCommand` 增加成对的 `batchExecutionId/batchExecutionTaskId`；`openOrCreateByContext` 实际写入 `batchExecutionId + taskId`，打开后再次严格核对 work order、route process、report/definition/version/category 及当前 batch/task；返回 execution ID、audit batch ID、cell value hash、field audit head hash 和稳定幂等键。
- A3 正式来源约束：生产事件必须精确属于当前工单/路线/工序，`workOrderId=null` 不再放宽；填写人、服务端提交时间、签名 ID/user/snapshot 来自生产事件；审核人、审核时间、签名 ID/user/snapshot 来自 `APPROVED + PRODUCTION` 复核。历史 completion backfill execution 仅进入 `sourceObjectIds`，不进入本次 `batchRecordExecutionIds`。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest" test` -> PASS；BUILD SUCCESS，Tests run: 19, Failures: 0, Errors: 0, Skipped: 0（A3 writer 5、backfill 适配 8、历史 completion 回归 6）。该命令同时完成 2589 个 main source 和 419 个 test source 的稳定态编译。
- GREEN: 静态扫描 A3 生产文件不存在 `formBindings`、工序开始、`PROCESS_START`、当前登录用户或 `LocalDateTime.now`；不存在尾随空白；tracked 适配文件 `git diff --check` 通过（仅 Git 行尾转换提示）。
- AC-03：唯一逐工序正式批记录绑定、definition/version/report、非 `PROCESS_INSPECTION/LOSS_REPORT`、启用 `PROCESS_POOL_REPORT` 唯一单元格映射、完成数量和所有映射来源值均在 plan 阶段校验。
- AC-07/AC-10：结果返回当前 batch/task execution、字段审计批次/head hash、生产填写与生产组长确认的真实 signature evidence；`userId/signedAt/evidenceHash` 直接取正式来源并由测试精确断言，不使用申请人或当前时间替代。
- AC-13：缺签名、生产事件工单上下文缺失、当前 batch task 错配均在任何 backfill/audit 写入前 blocker/fail fast；测试验证 `backfillCompletedProcess` 不被调用。
- AC-14：重复 write 传入相同 completion aggregate hash/idempotency key 和当前 batch/task，复用相同 execution/audit 结果；测试验证两次命令幂等键一致。
- AC-15：输出包含 execution/audit IDs、来源对象 IDs、SHA-256 来源值 hashes、填写/审核 signature evidence；测试明确断言 aggregate hash 为 `agg-production-5001`、来源 event ID 为 `1001`，未再把 completion ID `7301` 误当 event ID。
- A3 修改范围：仅新增 A3 writer/plan/command/result/test，修改既有 batch record backfill command/result/service/test 以补当前 batch/task 上下文和正式审计证据，并追加本日志；未修改 active-order release application service、A4/A5 文件、前端或监督状态文件。
- A3 blocker：无。并行 Agent 曾同时重编译共享 `target` 产生瞬时缺 class，以及 A2 构造参数中间态导致一次非 A3 `testCompile` 失败；在无并行 Maven 且共享源码稳定后，上述 19 项最终命令已独立全绿。

### A3 M0 6.1 Ordering Correction

- 主 Agent review 指出上节把当前 eDHR task 匹配前移到 BatchRecord plan 的结论不符合 M0 6.1 固定顺序：三个 writer 必须先完成无副作用 plan/validate，之后才创建或复用 eDHR batch；因此 plan 阶段不存在可查询的 `batchExecutionId/batchExecutionTaskId`。上节关于“plan 已冻结当前 batch/task、task 错配返回 plan blocker”的描述已废止，不得作为最终合同或 GREEN 证据。
- BDD: eDHR batch 尚未创建时仍可完成正式来源计划 -> Given 活跃订单生产来源、正式批记录绑定、映射和签名齐全，但 `batchExecutionId=null` 且当前 batch task 尚不存在；When 执行 BatchRecord plan；Then plan 无 blocker、不得查询 batch task、不得调用 backfill。创建 batch 后调用 write，write 才查询并严格匹配当前 task；错配时在任何 backfill 前直接抛出。
- RED: 主 Agent 新增 `shouldPlanFormalSourcesBeforeTheCurrentBatchHasBeenCreated`；旧实现会因 plan command 要求非空 `batchExecutionId` 或在 plan 查询 task 而失败。
- 修正实现：BatchRecord plan 已移除 batch task 查询和 prepared task；`batchExecutionId` 仅作为兼容公共输入字段存在，允许为空且不参与 plan。write 在确认 plan 无 blocker 后按方法参数查询当前 batch tasks，逐工序严格匹配 batch/routeProcess/process/report/definition/version/binding/category/formSlotType，再把真实 `batchExecutionId + taskId` 传给 backfill；匹配数量不为 1 时抛出且不调用 backfill。
- 验证状态：按主 Agent 的统一串行验证要求，修正后尚未启动 Maven；等待主 Agent通知后运行精确 GREEN 并追加最终计数。上节 19 项 GREEN 只证明修正前其余 A3 行为，不证明本次 M0 6.1 顺序修正。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --self-test` -> PASS；backend-api-delivery 校验器自身契约正常。A5 按主 Agent write scope 只追加本执行日志，未新建独立 backend-api evidence 文件。

### A3-A5 Supervisor Serial Gate

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；主 Agent 在无其他 `yudao-module-mes` Maven 进程时串行强制重编译验证，Tests run: 37, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- 主审结论：A3 的 M0 6.1 顺序缺陷、A4 的非数值正式实测值丢失、A5 的未匹配损耗原因放行均已由对应回归测试锁定并修正；同一资料类型存在多工序正式文档的完成性用例通过，证明 checker 为“至少一条并逐条校验”，撤销此前中间误判。

### A2-INTEGRATE

- BDD: 三 writer 按固定顺序原子生成 -> Given 正式生产完成、PQC CONFIRMED 汇集、正损耗、三类传统报表与 RELEASE_APPROVE 候选均完整；When 生产组长申请放行；Then 先依次完成 BatchRecord/ProcessInspection/LossReport 的无副作用 plan，再创建批次并依次 write，完成性通过后才 precheck 和 submitForApproval，最终返回唯一 PENDING_RELEASE_APPROVAL 申请及非零签名证据。
- BDD: 预写 blocker 独立保存且没有生成副作用 -> Given 任一 writer plan 或 RELEASE_APPROVE 候选解析返回确定性 blocker；When 生产组长申请放行；Then 生成事务不创建 batch/execution/audit/release transaction/work task，独立短事务仅保存 BLOCKED 申请快照。
- BDD: 写入后完成性或 precheck 阻塞全部回滚 -> Given 三 writer write 已在生成事务内执行，但完成性或 release precheck 返回 blocker；When 生成事务退出；Then 本次 batch、execution、audit、release transaction 和待办全部回滚，再由独立事务保存 BLOCKED 申请，不留下部分资料。
- BDD: 正式双 100 与来源哈希不可降级 -> Given PQC 只有 SUBMITTED、生产完成缺回填/事件/分配/复核，或来源值/QA/映射/签名/负责人候选发生变化；When 申请放行；Then 非正式进度不能进入生成，`AO_RELEASE_SOURCE_V1` canonical hash 随正式来源变化且不含当前时间、申请备注或 blocker。
- BDD: 请求与业务双幂等 -> Given 同请求键重试或相同正式来源快照使用新请求键；When 并发或重复申请；Then 返回原申请且不重复创建 batch、execution、audit、release transaction 或 work task；正式来源变化后使用新请求键生成新的业务申请。
- A2 最小实现：将申请入口拆为无事务 facade、原子生成事务和申请持久化边界。生成事务按 M0 6.1 锁定 active order/work order/product/route/frozen published route version/process snapshots，重验逐工序正式生产 completion/backfill 与 `PQC task=CONFIRMED + aggregate detail`；依次执行三 writer plan，全部无 blocker 且路线级 `RELEASE_APPROVE` 候选有效后才 open/create batch，随后固定顺序执行 BatchRecord/ProcessInspection/LossReport write、完成性、precheck、submitForApproval 和 PENDING 回执。
- A2 事务证据：`MesTeamLeaderActiveOrderReleaseGenerationService.generate` 使用 `@Transactional(rollbackFor=Exception.class)`；PENDING 回执使用 `Propagation.MANDATORY`；确定性 blocker 以异常退出生成事务后，由 facade 调用 `Propagation.REQUIRES_NEW` 保存 BLOCKED 快照。数据库、JSON、writer 和非确定性基础设施异常不转成成功或通用 blocker；完成性/precheck blocker 测试证明不会提交待办或在原事务保存申请。
- A2 hash/幂等证据：`sourceSnapshotHash=SHA-256(UTF-8(canonical JSON))`，版本 `AO_RELEASE_SOURCE_V1`；覆盖租户、active order、锁定的工单/产品/路线/发布版本/工序快照、正式 production completion、三 writer 来源值/绑定/映射/签名及 RELEASE_APPROVE 规则/候选快照；对象键和数组稳定排序，时间截断到秒、decimal 去无意义尾零，排除申请时间、备注、blocker 和生成 ID。业务键固定为 `AO_RELEASE_SOURCE_V1|workOrderId|routeVersionId|sourceSnapshotHash`；并发唯一键冲突只在 request/business key 与正式 hash 全部一致时回读。
- A2 blocker/API 证据：后端公共 blocker 及 Response VO/Controller 映射均包含可选 `routeProcessId/processId/fieldCode/cellKey`；缺 writer 正式来源或缺唯一路线级 `RELEASE_APPROVE` 规则时，在 batch 前返回定位 blocker，不使用 `CLOSE`、当前申请人、工序开始、`formBindings` 或默认 `MAIN` 替代。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest" test` -> PASS；该稳定态命令重新编译全部 420 个 MES test source 后 Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。覆盖固定 plan/write/complete/precheck/submit 顺序、锁定正式主数据、成功三文档与非零签名、plan/完成性/precheck blocker、缺 RELEASE_APPROVE、SUBMITTED-only PQC、请求/业务幂等、并发冲突严格快照验证、facade 回滚后持久化和事务传播合同、canonical hash 排序/秒精度/decimal/正式值变化。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；A2+A3+A4+A5 串行 Tests run: 50, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- A2 覆盖：AC-05 正式双 100 重验；AC-06 当前工单/批号/产品/路线版本 batch open/create 严格回执；AC-11 三 writer 和完成性后唯一 RELEASE_APPROVE/PENDING；AC-13 预写无副作用、写后回滚及定位 blocker；AC-14 请求/业务双幂等和并发唯一冲突严格回读；AC-15 canonical source hash、writer execution/audit/source/signature 和申请/待办回执证据。
- A2 修改范围：申请 orchestration/facade/persistence/hash/shared blocker DTO 与 Controller 映射、只读锁定 mapper、小范围聚焦测试及本日志；未修改 A3/A4/A5 writer 生产实现、前端、`task-state.json` 或 `test-report.md`。A2-INTEGRATE blocker：无。

### A2-INTEGRATE Main Review Corrections

- BDD: 请求幂等不得绕过当前负责人授权 -> Given 已存在相同请求键回执；When 当前调用人不再是该活跃订单生产组长；Then 仍先锁定活跃订单并拒绝申请，不因旧回执直接放行，也不重新执行 writer plan 或创建批次。
- BDD: 输入边界在业务状态读取前失败 -> Given `idempotencyKey` 超过 128 字符或 `applyRemark` 超过 500 字符；When 调用申请接口；Then 在读取活跃订单、查询幂等回执和生成资料前 fail fast，不截断、不降级。
- BDD: 业务幂等回执必须匹配当前正式快照 -> Given 业务幂等键命中既有回执；When 回执的 active order、work order、冻结 route version、business key 或 source snapshot hash 任一与本次正式来源不一致；Then 拒绝复用且不打开新批次。
- BDD: 跨资料复用同一正式签名只计一次 -> Given 批记录和损耗单引用相同正式 `signatureId`；When 汇总资料摘要；Then `signatureEvidenceCount` 按签名身份全局去重，不因文档类型不同重复计数。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL；评审新增合同后共 55 项出现 6 个行为断言失败，精确锁定请求幂等早于负责人授权、超长请求键/备注未在状态读取前拒绝、业务回执未严格核对正式快照，以及签名按资料类型重复计数。
- 评审修正：申请命令长度先验校验；随后锁定并授权当前活跃订单，授权通过后才查询请求幂等；业务幂等回读严格核对 active order/work order/frozen route version/business key/source hash；摘要按正式 `signatureId` 全局去重。有效业务幂等测试夹具补齐冻结 route version；错误负责人用例移除按合同不会执行的冗余 Mockito 桩，未放宽生产校验。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；Tests run: 18, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；A2+A3+A4+A5 串行 Tests run: 55, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- A2-INTEGRATE 最终覆盖：AC-05/AC-06/AC-11/AC-13/AC-14/AC-15；未修改 A3/A4/A5 writer 生产实现、前端、`task-state.json` 或 `test-report.md`。最终 blocker：无。

### A1 Frontend Entry Hardening

- BDD: 申请载荷严格只有 M0 三字段 -> Given 生产组长在活跃订单页点击申请放行；When 前端调用正式 apply wrapper；Then 请求体仅含 `activeOrderId/idempotencyKey/applyRemark`，且前端不生成 batch、transaction、work task、资料或签名证据 ID/状态。
- BDD: BLOCKED 回执展示正式定位信息 -> Given 后端返回 `BLOCKED` 和含完整 locator 的 blockers；When 页面呈现阻塞项；Then 每项显示 `blockerType/reason/suggestion` 以及 `objectType/objectId/objectCode/routeProcessId/processId/fieldCode/cellKey`，不以前端文案猜测位置。
- BDD: 写入成功后刷新失败仍保留成功事实 -> Given apply 已返回合法正式回执；When 后续活跃订单列表刷新失败；Then 页面显示“申请已提交，但列表刷新失败”与真实刷新错误，并锁定当前订单的重复提交。
- BDD: apply 响应不确定时先读正式回执 -> Given apply 请求可能已到达服务端但前端收到网络或超时异常；When 前端处理该异常；Then 使用稳定 `activeOrderId` 调用正式活跃订单只读列表确认回执，已提交则锁定，明确未提交才保留同一 `idempotencyKey` 允许重试，无法确认则进入可见的不确定锁定态并提示人工核对。
- RED 计划命令：`node tests/e2e/team-leader-active-order-release-application-static.spec.js`；预期精确失败于 locator 类型/展示缺失、幂等键每次重新生成、apply 异常未读正式回执、刷新错误未固定包含成功事实以及无不确定锁定态。
- RED: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> FAIL；首个业务断言精确报 `blocker must type routeProcessId`，当前 `TeamLeaderActiveOrderReleaseBlockerRespVO` 只有六个基础字段，未对齐 M0 四个可选 locator；Node 已正常加载源文件和专用合同，失败不是路径、语法或 fixture 问题。
- Review RED: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> FAIL；新增“列表请求成功但未投影正式写回状态时仍不得解锁”合同后，精确失败于 `a successful list request must not unlock duplicate submission until it projects the formal write status`；旧实现在 `loadActiveOrders()` 只要返回就无条件删除本地写入锁，未校验列表中同一 `activeOrderId` 已投影回执 `result.status`。
- A1 最小实现：API 请求继续严格仅有 M0 三字段，响应 status 收紧为 `BLOCKED | PENDING_RELEASE_APPROVAL`，必填回执和 dossier summary 改为必填类型，blocker 补齐 `routeProcessId/processId/fieldCode/cellKey`。apply 页面按正式回执校验必填字段、两种状态、三类资料/签名计数与关联 ID，不在前端生成后端 ID、状态、hash 或签名证据。
- A1 错误分层：apply 返回合法回执后先记录 `CONFIRMED` 本地锁，再刷新列表；刷新失败改为固定显示“申请已提交，但列表刷新失败：<正式错误>”并保留 `CONFIRMED_REFRESH_FAILED` 锁，不转成写入失败或允许重复提交。
- A1 不确定回执：每个活跃订单的 `idempotencyKey` 在当次未决请求期间稳定复用；apply 异常后调用正式只读活跃订单列表按 `activeOrderId` 确认回执。回执可证明已提交时进入 `RECOVERED` 锁；明确无申请状态时保留同一幂等键允许用户主动重试；回执请求失败或旧/new BLOCKED 无法证明本次变化时进入 `UNCERTAIN` 锁，页面以可见 `el-alert` 提示人工核对。apply/回执请求由页面统一显示真实错误，wrapper 使用 `ignoreErrorMessage` 避免 Axios 与页面重复提示，不吞异常。
- A1 blocker 展示：每项显示正式 `blockerType/reason/suggestion`，并将 `objectType/objectId/objectCode/routeProcessId/processId/fieldCode/cellKey` 组合成可见定位行；必填原因不再 fallback 到 blocker type。
- GREEN: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS；任务专用合同通过。
- REGRESSION: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS；相邻班组长工作台静态合同通过。
- REGRESSION: `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs` -> PASS；SFC scoped style 编译合同通过。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0，无类型错误。
- GREEN: `git diff --check -- <A1 owned files>` -> PASS；无尾随空白或补丁格式错误。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --self-test` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence E:\IntRuoyi\doc\tasks\20260809-active-order-release-dossier-v4-delivery\execution-log.md` -> PASS；A1 按主 Agent write scope 将 frontend feature evidence 合并记录到本监督日志，未额外新建超出所有权的证据文件。

#### Feature

- Goal: 硬化生产组长活跃订单申请放行前端入口；非目标为新增接口、修改后端状态机、伪造资料证据或执行 A6 真实写入 E2E。
- Entry: `TeamLeaderWorkbenchPage.vue` 活跃订单“申请放行”；API: `POST /mes/pro/process-pool/team-leader/active-order/release/apply` 与只读 `GET /active-order/list`。

#### Acceptance

- AC-05：前端只发送当前活跃订单三字段请求，不替代后端重算。
- AC-11：只消费正式 `PENDING_RELEASE_APPROVAL` 回执，不伪造已放行状态或负责人待办ID。
- AC-13：正式 BLOCKED locator 可见，响应不确定/回执缺失 fail fast 并锁定。
- AC-14：稳定幂等键和只读回执确认防止响应丢失后无条件重复写入。

#### Verification

- Responsive: 未新增固定宽高；blocker/uncertain 复用现有全宽 `el-alert`，文字可自然换行。
- Accessibility: 继续使用现有 `el-button` disabled/loading/title，状态提示使用带 `show-icon` 的 `el-alert`。
- Loading/error/permission: 保留行级 submitting loading；三类错误分层可见；未改动原有按钮权限与双 100% 入口门禁。
- E2E path: A1 按 V4 仅完成 static + typecheck；生产组长真实申请和负责人处理由依赖 A1/A2 的 A6 执行，本节未用 mock/API-only 冒充真实 E2E。

#### Blockers

- A1 blocker: 无。页面文件原有其他并发任务改动，A1 仅在放行申请相关模板/状态/函数块增量修改，未回退或重写其他改动。

### A1 Main Stable Regression

- REGRESSION: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS；主 Agent 在并发文件稳定后复验任务专用合同。
- REGRESSION: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS；相邻工作台合同复验通过。
- REGRESSION: `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs` -> PASS；SFC scoped style 编译合同复验通过。
- GREEN: `pnpm ts:check` -> PASS；等待重叠的并发 `vue-tsc` 进程自然结束后由主 Agent独立重跑，退出码 0。

### Independent Verification Gate

- VERDICT: `BLOCKED / NOT COMPLETE`；A1-A5 聚焦实现和合同通过不等于 A6 真实跨角色验收完成。
- A6 prerequisite: 本地运行态/Playwright PASS；三类传统 `batchRecordReportId` 完整组合缺失、五类登录/签名凭据未证明，真实业务 Playwright 0、manifest 0、业务写入 0、残留 0。
- REGRESSION BLOCKED: 后端 55 项最终重跑在测试前读取到并发 Maven 产生的旧 Mapper class，随后强制全量重编译期间又出现多个无关 MES Maven 进程；主 Agent只终止自己的 Maven，会在共享模块稳定后串行复验，不用并发污染结果覆盖既有稳定 55/55 证据。
- REPORT: `doc/tasks/20260809-active-order-release-dossier-v4-delivery/verification-report.md`。

### Project Experience Consolidation

- 将“多类传统资料 E2E 必须在写入前按同一目标工序证明非空 report ID 完整组合，动态 form template 不可替代；账号/签名行存在不等于凭据可用”合并到既有 `docs/e2e-rules.md#写入型-e2e-任务自有模拟环境门禁` 和 `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`。
- 更新 `docs/experience-index.md` 关键词路由；未新建长期经验文档。
- STRUCTURAL: task-state JSON 解析通过；相关任务/经验/A1 文件 `git diff --check` 通过，仅有既有 LF/CRLF 警告。

### A6 Fixture Manifest And Real Playwright E2E

- BDD: 正式三资料真实页面闭环 -> Given 已确认的非生产测试租户、生产员工、生产组长、PQC 检验员、PQC 组长和放行负责人账号均可真实登录并具有有效电子签名，且任务自有产品、工单、发布路线版本、逐工序三类传统 `batchRecordReportId`、发布 QA 规程、PQC/损耗映射及 `RELEASE_APPROVE` 候选完整；When 依次从一线生产页面提交含正损耗的生产记录、生产组长页面确认、一线 PQC 页面提交、PQC 组长页面复核并汇集、生产组长活跃订单页面申请放行、放行负责人页面批准或驳回；Then 页面自然呈现生产/PQC 双 100%，三类正式文档、字段审计和去重签名可追溯，并写出字段完整、UTF-8、数组稳定排序且不含凭据的 manifest。
- BDD: 同一正式来源快照重复申请幂等 -> Given 首次真实页面申请已返回稳定申请、批次、放行事务和待办 ID；When 使用同一正式来源快照通过真实页面重复触发或恢复同一未决申请；Then 只读核验复用同一业务申请及资料，不新增 batch、execution、audit、release transaction 或 work task。
- BDD: 缺正式来源负向路径 -> Given 当前活跃订单任一工序缺逐工序传统批记录、过程检验或损耗报表 `batchRecordReportId`，或缺 QA/映射/签名/`RELEASE_APPROVE` 正式来源；When 生产组长从真实活跃订单页申请放行；Then 页面显示后端返回的精确 blocker 定位，数据库只读核验没有资料、事务或待办副作用，不以 `formBindings`、默认 `MAIN`、旧字段或 API-only 路径替代。
- BDD: 任务自有数据清理 -> Given A6 已记录每个正式写入回执和成功状态；When 真实 E2E 完成或异常退出；Then 仅通过真实 UI 清理 `AORD-V4-M0-A6-20260809-` 前缀数据，记录已删除与残留 ID，不直接 SQL 清理且不触碰既有共享 fixture。
- PREREQUISITE PASS: `E:\IntRuoyi` 为 Git 分支 `int_main` 基准；8081 由 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite 进程监听且首页 HTTP 200，48081 由 `E:\IntRuoyi` 的 int_main Java 运行包监听且 `/actuator/health` 返回 `UP`；未启动、停止或替换任何共享进程。
- PREREQUISITE PASS: Node `v24.12.0`、npm/npx `11.6.2`、仓库 Playwright `1.60.0`、Chrome 和 Edge 可解析；前端存在 `playwright.config.ts`、`test:e2e` 脚本与正式生产/PQC/组长页面组件。当前没有 A6/TLW/FRONTLINE/PQC/RELEASE/E2E 专用环境变量。
- PREREQUISITE PASS: 只读数据库核验 tenant `1` 的六角色账号 `964/1520/1301/659/512/1618` 启用，六个电子签名授权均为 `ENABLED`，活动签名图片分别为 `22/23/24/25/26/27`；本轮尚未获得或证明这些业务账号的登录密码和签名口令，因此仅证明账号/授权行存在，不声明真实登录可执行。
- PREREQUISITE BLOCKED: 只读全库核验 `mes_pro_route_flow_process_batch_record` 显示 `MAIN/BATCH_RECORD` 有非空 `batch_record_report_id`，但 tenant `1` 的 `PROCESS_INSPECTION/INTERNAL_RECORD` 176 行和 `LOSS_REPORT/INTERNAL_RECORD` 352 行、tenant `122` 对应 2 行和 4 行的 `batch_record_report_id` 全部为空；全库不存在同一工序三类 `MAIN + PROCESS_INSPECTION + LOSS_REPORT` 均具有非空传统 `batchRecordReportId` 的正式 fixture。路线 `922119` 当前发布版本为 `627/V27`，其特殊两类现存数据仍是 `form_slot_type + form_template_id`，不满足 M0 5.2 传统报表绑定合同。
- PREREQUISITE BLOCKED IMPACT: A2/A4/A5 会在资料生成前返回正式来源 blocker；无法合法执行“生产/PQC 双 100 -> 申请放行 -> 三资料/审计/签名 -> 负责人处理”正向链路，也无法产生非零正式 manifest ID。按 M0 5.2 fail-fast，A6 未创建空/占位 manifest，未新增把静态检查冒充真实 E2E 的 spec，未调用业务写 API、未执行 SQL 写入/清理、未修改任何产品代码或既有 fixture。

### A6 Executable Preflight Gate

- BDD: 显式授权和五角色秘密完整性 -> Given A6 真实 E2E 尚未开始；When 执行 preflight；Then 必须先校验显式授权 token、tenant、前端/浏览器/数据库目标、路线 fixture ID，以及生产员工、生产组长、PQC 检验员、PQC 组长、放行负责人五类账号的用户名、登录密码和签名口令环境变量；缺任一项时只输出缺少的环境变量名，不输出变量值，并在数据库、浏览器和任何业务写入前非零退出。
- BDD: 三类传统报表来源门禁 -> Given 显式环境变量完整；When preflight 只读核验目标路线工序；Then 每个工序必须分别具有唯一、非空 `MAIN/PROCESS_INSPECTION/LOSS_REPORT` 传统 `batch_record_report_id` 及有效 report/definition/APPROVED version，任何 `formBindings` 或 `form_template_id` 都不进入通过条件。
- BDD: QA、映射和负责人前置门禁 -> Given 三类传统报表有效；When preflight 继续只读核验；Then 每个目标工序具有当前产品/路线/发布版本的 PUBLISHED QA version、非空 items、必需 equipment，三类 report 分别具有 `PROCESS_POOL_REPORT/PQC_AGGREGATE_DETAIL/PRODUCTION_LOSS` 启用映射，且路线级唯一 `RELEASE_APPROVE` 规则能解析至少一个启用候选用户。
- BDD: 只读真实登录和监听顺序 -> Given所有数据门禁通过；When preflight 使用五类显式账号逐一登录；Then console/pageerror/requestfailed/目标响应监听均在首次导航前注册，只允许认证登录 POST 和页面/只读请求，不执行生产、PQC、申请放行、签名或清理业务写入。
- BDD: 结构化阻塞产物不含秘密 -> Given 任一 env、数据库、浏览器或登录前置失败；When preflight 结束；Then 以 UTF-8 JSON 输出 `BLOCKED`、精确 blocker 类型/检查项和零业务写副作用计数，递归扫描结果不含登录密码、签名口令、token 或秘密环境变量值，不创建空 manifest 或 placeholder success。
- RED: `node tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> FAIL，exit 1；首个断言精确失败于 `missing executable A6 V4 preflight gate`，证明静态合同已就绪而可执行 preflight 尚不存在，失败不是数据库、浏览器、凭据或业务路径误报。
- A6 最小正式实现：新增 `active-order-release-dossier-v4-preflight.cjs`，显式要求授权测试 tenant/目标 fixture/五角色登录及签名环境变量；按 env -> 只读数据库 -> 五角色只读真实 UI 登录的固定顺序执行。数据库合同严格使用逐工序传统 `batch_record_report_id` 和 report/definition/APPROVED version，明确排除 `formBindings/form_template_id`，并覆盖 PUBLISHED QA/items/equipment、PQC/损耗三类 mapping、唯一 `RELEASE_APPROVE` 正式候选。
- A6 写入防线：数据库仅允许 `SELECT/SHOW`；Playwright listener 和 admin-api write guard 在首次导航前安装，登录阶段仅允许认证 POST 和只读请求；结构化结果写入前对授权 token、登录密码和签名口令执行值级脱敏及泄漏断言。任一前置失败返回 `BLOCKED`、非零退出和零业务写副作用，不创建 manifest 或 placeholder success。
- GREEN: `node --check tests\\e2e\\active-order-release-dossier-v4-preflight.cjs` -> PASS；可执行门禁语法有效。
- GREEN: `node --check tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；静态合同语法有效。
- GREEN: `node tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；26 项显式 env、来源表/状态/mapping、`formBindings/form_template_id` 排除、env-before-DB-before-browser、listener/write guard 顺序、无秘密 BLOCKED 与零副作用合同全部通过。
- PREFLIGHT BLOCKED: `node tests\\e2e\\active-order-release-dossier-v4-preflight.cjs --result-path ..\\doc\\tasks\\20260809-active-order-release-dossier-v4-delivery\\a6-preflight-blocked.json` -> Node exit `2`；实际产物 `status=BLOCKED`、`blockerType=MISSING_EXPLICIT_ENV`、`missingEnvKeys=26`、`canRunRealE2E=false`，`browserBusinessWrites/businessApiWrites/sqlWrites=0`、`manifestCreated=false`，未访问数据库、未启动浏览器、未执行真实业务 E2E。
- A6 remaining blocker: 必须由获授权运行环境安全注入上述 26 项明确变量；env 完整后仍须通过已知当前数据会阻塞的同工序 `MAIN/PROCESS_INSPECTION/LOSS_REPORT` 三类非空传统 report ID、QA、mapping 和负责人候选门禁。真实 Playwright 业务路径、正式 manifest、真实业务 ID、console/page/backend 业务证据和 UI 清理仍为 `NOT RUN`，不得将 preflight GREEN 冒充 E2E 完成。
- Review BDD: BIT 签名授权按数值核验 -> Given MySQL CLI 可能将 `BIT(1)` 输出为二进制控制字符；When A6 preflight 查询五角色电子签名授权；Then SQL 必须先 `CAST(COALESCE(electronic_signature_enabled, 0) AS UNSIGNED)`，再按数值 `1` 核验，不能依赖原始字符串表现。
- Review BDD: 凭据合同不得自设长度 -> Given 获授权测试租户提供非空真实登录密码和签名口令；When 环境门禁校验五角色秘密；Then 仅校验显式非空，实际有效性分别由真实 UI 登录和后续业务签名路径证明，不因 M0 未冻结的最小长度拒绝。
- Review BDD: 前后端 base URL 均须显式且限定 int_main -> Given A6 只允许使用 `E:\IntRuoyi` 的 int_main 本地基准；When 环境门禁解析 base URLs；Then 必须显式提供 frontend `localhost/127.0.0.1:8081` 和 backend `localhost/127.0.0.1:48081`，禁止默认、推断或其它分支端口。
- Review BDD: 正式 report ID 支持仓库既有格式 -> Given 传统 report ID 可能包含仓库已使用的连字符格式如 `REPORT-040`；When preflight 构造只读 mapping 查询；Then 允许不含 SQL 引号/转义字符的正式字母数字、点、下划线、冒号、连字符 ID，不因安全白名单过窄转为运行错误。
- Review RED: `node tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> FAIL，exit 1；首个主审合同断言精确失败于 `preflight must require AORD_V4_M0_BACKEND_URL`，证明旧门禁只显式接收 frontend URL；同一 RED 合同还锁定 BIT unsigned cast、仅非空秘密和正式连字符 report ID。
- Review correction: 新增必填 `AORD_V4_M0_BACKEND_URL`，严格限定 `localhost/127.0.0.1:48081`；env 完整后先只读 GET frontend 和 backend `/actuator/health` 并要求 `UP`，再进入数据库门禁。此前 backend URL 未单列是因为浏览器业务请求经 Vite frontend proxy 发往 backend，但这不能满足本轮“base URLs 均显式”的独立运行态合同，现已消除隐式推断。
- Review correction: 电子签名启用列改为 SQL `CAST(COALESCE(a.electronic_signature_enabled, 0) AS UNSIGNED)` 且 Node 按数值核验；移除密码和签名口令最小长度，仅保留显式非空；结构化 details 改为递归仅脱敏值，避免任意口令字符破坏 JSON key/解析；正式 report ID 安全白名单补齐仓库既有点/下划线/冒号/连字符格式，非法值正式返回 `BLOCKED`。
- Review GREEN: `node --check tests\\e2e\\active-order-release-dossier-v4-preflight.cjs` -> PASS。
- Review GREEN: `node --check tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS。
- Review GREEN: `node tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；27 项显式 env、frontend/backend runtime-before-DB-before-browser、BIT unsigned cast、秘密只要求非空、正式 report ID 格式和无秘密 BLOCKED 合同通过。
- Review PREFLIGHT BLOCKED: `node tests\\e2e\\active-order-release-dossier-v4-preflight.cjs --result-path ..\\doc\\tasks\\20260809-active-order-release-dossier-v4-delivery\\a6-preflight-blocked.json` -> Node exit `2`；实际产物更新为 `MISSING_EXPLICIT_ENV/missingEnvKeys=27`（新增 `AORD_V4_M0_BACKEND_URL`），运行态 GET、数据库、浏览器和业务写均未开始，四项 side effects 仍为零。

### A6 Authorized Tenant/Route Read-Only Rerun

- AUTHORIZATION: 用户明确授权仅使用 `E:\IntRuoyi` 本机 int_main、`芋道源码` tenant 和目标名为“球囊扩张导管”的工艺路线；任何登录/签名口令只允许瞬时注入真实 UI，不写入命令、日志、manifest 或文件。共享来源路线只读，若需 fixture 只允许真实页面创建 `AORD-V4-M0-A6-20260809-*` 任务副本。
- PREREQUISITE PASS: Git 分支 `int_main`；8081 listener PID `51912` 的命令行属于 `E:\IntRuoyi\IntRuoyiFronted`，48081 listener PID `30464` 为 Java；本机前后端请求成功，Node `v24.12.0`、Playwright `1.60.0`、Docker MySQL 运行。未启动、停止或替换共享进程。
- READ-ONLY IDENTITY: `芋道源码 tenantId=1`；精确路线名 UTF-8 HEX `E79083E59B8AE689A9E5BCA0E5AFBCE7AEA1` 唯一命中 `routeId=900025/code=ROUTE-XLSX-00001`。最高版本为 `272/V10/DRAFT`，当前唯一 ACTIVE 发布版本为 `routeVersionId=271/V9`；M0 正式路径只能使用 ACTIVE V9，不能把草稿 V10 当发布版。
- READ-ONLY PROCESSES: 当前路线共有 23 个未删除工序，`routeProcessIds=926785..926807`（逐项连续），对应 `processIds=922894..922916`，最后工序 `routeProcessId=926807` 是唯一 key process。路线存在四个启用产品绑定，`productIds=[902231,902252,902262,907242]`，编码分别为 `YXN.037.011.1007/.1008/.1002/.1005`；因此用户只指定路线时不能唯一解析单一 `productId`，禁止选择第一条或按旧工单猜测。
- PREREQUISITE BLOCKED/TRADITIONAL: 共享路线 900025 的 `mes_pro_route_flow_process_batch_record` 未删除行总数为 0，23 个工序的 `MAIN/PROCESS_INSPECTION/LOSS_REPORT` 非空传统 `batchRecordReportId` 完整组合为 0。tenant 1 的目标产品名有 15 个 MAIN report（definition `54`、current APPROVED version `100`），唯一 current APPROVED PROCESS_INSPECTION report 为 `b48d2a150afc40deb456fb5fe9da551b`（definition `51`、version `99`）；唯一 LOSS_REPORT `ef191803cbef413089ed55a7bb5b9962` 缺 definition/version，不能作为正式绑定。MAIN 15 个与路线 23 个工序也不存在已冻结唯一逐工序选择关系。
- PREREQUISITE BLOCKED/ROUTE COPY: ACTIVE V9 的 `route_snapshot_json.configSnapshots` 只有 products/flowGraph/productBoms/batchUseConfigs/scheduleConfigs/scheduleUseConfigs，`batchRecordAttachmentOwners` 路径为 `NULL` 而非数组。按 `docs/e2e-rules.md#eDHR-任务专用路线副本-E2E-门禁`，禁止从该共享来源继续复制任务路线后再猜附件负责人；未打开复制页面、未创建副本。
- PREREQUISITE BLOCKED/QA+MAPPING+OWNER: route 900025 任意版本的 QA regulation 总数为 0，V9 的 PUBLISHED QA/items/equipment 为 0；tenant 1 启用的 `PROCESS_POOL_REPORT/PQC_AGGREGATE_DETAIL/PRODUCTION_LOSS` mapping 总数为 0；route 900025 的 `RELEASE_APPROVE` 规则为 0（仅有 CLOSE rule），唯一全租户 RELEASE_APPROVE 属于共享 route 922119，不能复用。
- READ-ONLY ACCOUNTS: 正式带签名角色候选中，`wangxin/userId=810` 具有 pressure-pump production/filler 角色、`shangmengying/userId=659` 具有 PQC 角色且二者均有 ENABLED 授权和 active signature image。正式 `mes_team_leader` 用户 `limin/149`、正式 PQC leader 用户 `jiangdan/617` 与 `majing/1467` 均缺电子签名授权/active image；可用的多个 distinct super_admin signed users只证明管理权限，不自动证明生产/PQC 业务角色。当前 executor 环境没有 AORD 登录/签名秘密变量，尚未通过真实 UI 登录，不声明账号可用。
- PLANNED UI WRITES (not started): admin 真实登录；只读确认 route copy 页面/精确来源；复制任务路线；保留 23 工序和唯一产品；逐工序精确绑定三类传统报表；发布/启用候选版本；配置 PUBLISHED QA/items/equipment、三类 cell mappings、RELEASE_APPROVE；分别登录五角色；创建任务工单/活跃订单；生产/PQC/组长/放行闭环；同快照幂等与缺来源负向；finally 通过页面作废业务批次并删除任务路线。由于上述正式来源及清理前置不满足，本清单业务写请求数为 0。
- DECISION: `STRUCTURED_BLOCKED / FAIL FAST BEFORE FIRST BUSINESS WRITE`。未运行 full preflight、未创建 Playwright business spec/manifest、未调用业务 API 写、未执行 SQL 写/清理、未修改共享路线或产品代码。

### A6 Pressure-Pump To Balloon-Catheter Read-Only Mapping

- BDD: 压力泵 MAIN 正式来源锁定 -> Given tenant 1 的“球囊扩张压力泵”共享路线；When 只读解析当前唯一 ACTIVE 发布版本、全部当前工序及逐工序批记录绑定；Then 每个 MAIN 来源必须同时给出 routeProcess/process/report/definition/current APPROVED version 身份，缺任一正式身份不得进入可复用候选。
- BDD: 过程检验传统报表与表单槽位分离 -> Given 压力泵路线可能同时存在 `PROCESS_INSPECTION` 行和动态表单配置；When 分析正式来源；Then 分别输出传统 `batchRecordReportId` 链路和 `formBindings/form_template_id` 链路，后者不得补齐、推断或冒充传统过程检验 report。
- BDD: 23 工序映射不猜测 -> Given 压力泵源工序和球囊扩张导管 V9 目标工序；When 按规范化名称、语义和顺序逐项比较；Then 每个目标工序只能标为 `EXACT/SEMANTIC_CANDIDATE/NO_MATCH/AMBIGUOUS`，并列出源/目标正式 ID、名称和 MAIN report；只有正式唯一对应才可计为自动映射，语义候选和歧义必须等待用户裁决。
- BDD: 报告归属和可复用性门禁 -> Given 任一映射候选；When 核对 report 产品归属、definition/current version/status 和源路线绑定；Then 只有产品语义适用且 definition current version 等于绑定 version、version 为 APPROVED 时才标为可能合法复用；无唯一来源时继续阻塞且不执行数据库、API 或页面写入。

### Main Independent A6 And Frontend Integration Verification

- INDEPENDENT: `node tests\\e2e\\active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；主 Agent 复核显式 env、正式来源、只读顺序、写保护和脱敏合同。
- INDEPENDENT: `node --check tests\\e2e\\active-order-release-dossier-v4-preflight.cjs` -> PASS。
- INDEPENDENT BLOCKED: `node tests\\e2e\\active-order-release-dossier-v4-preflight.cjs` -> exit 2；输出 `BLOCKED/MISSING_EXPLICIT_ENV`、27 个稳定排序变量名、`canRunRealE2E=false`，四项副作用为零；未写 result/manifest、未访问数据库或浏览器。
- INTEGRATION GREEN: A1 专用静态合同、相邻 workbench 合同、SFC style 编译合同全部 PASS。
- INTEGRATION GREEN: `pnpm ts:check` -> PASS，退出码 0。
- INTEGRATION BLOCKED: 最新后端 55 项串行复验尚未启动；检查时另一个任务正在同一 `yudao-module-mes` 执行 Maven，主 Agent 未停止或干预该进程，避免共享 `target` 再次污染证据。
- VERDICT: A6 可执行 preflight 通过独立验证，但 TC-A6-01、TC-INT-03、正式 manifest 和真实跨角色 Playwright 仍为 BLOCKED；不得标记 M0 completed。

### Stable Backend Integration Rerun

- PRECONDITION: 主 Agent 等待所有无关 Maven 自然结束，确认启动前无 `mvn/maven` 进程；测试运行中仅存在本任务 Maven 及其 surefire 子进程，未停止或干预其它任务。
- INTEGRATION GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；Tests run: 55, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS，Total time 01:06 min。
- RESOLVED: 此前共享 `target` 读取旧 class 的回归 caveat 已由本次稳定串行复验关闭；任务 blocker 只剩正式 fixture/三类传统 report ID 和 27 项获授权 A6 环境变量。

### Final Experience Consolidation Check

- 共享 Maven `target` 的并发写入、只停止任务自有 PID、等待稳定窗口后以 Surefire 结果复验，已由 `docs/powershell-memory.md#Maven-目标目录文件系统异常门禁` 和 `#Maven-javac/Lombok-class-写入长时间运行门禁` 完整覆盖；本轮只在 `task.md` 增加适用门禁，不重复扩写长期规则。
- collaboration 子 Agent 调度必须直接调用 `spawn_agent/followup_task/list_agents`、不得嵌套 `functions.exec` 或伪造 `functions.wait cell_id`，属于可复用工具编排经验，但当前 `docs/` 没有概念匹配的长期文档；按 `project-experience-consolidation` 规则未擅自新建经验文件，任务级事实继续保留在本日志。

### Blocker Recheck 2

- ENV RECHECK: 当前进程重新核验 A6 的 27 项显式授权环境变量，`PRESENT_COUNT=0`、`MISSING_COUNT=27`；只记录变量名和计数，未读取、打印或写入任何凭据值。
- FORMAL SOURCE RECHECK: 只读全库查询仍返回同一工序 `MAIN + PROCESS_INSPECTION + LOSS_REPORT` 三类非空传统 `batch_record_report_id` 完整组合数 `0`。tenant `1` 为 `MAIN 1224/1224`、`PROCESS_INSPECTION 176/0`、`LOSS_REPORT 352/0`；tenant `122` 为 `MAIN 15/15`、`PROCESS_INSPECTION 2/0`、`LOSS_REPORT 4/0`。
- RUNTIME RECHECK: int_main frontend `127.0.0.1:8081` HTTP 200，backend `127.0.0.1:48081/actuator/health` 为 `UP`；无 Maven 进程占用共享 target。
- BROWSER SESSION RECHECK: 使用应用内真实浏览器打开 `/`、`/login` 和 `/mes/pro/process-pool/team-leader`，页面均只呈现“瑛泰管理系统”启动页，未发现可确认的已登录测试租户/角色会话；未检查 cookie/local storage/密码，未点击、提交或调用任何写请求，标签页已关闭。
- VERDICT: 两项正式前置仍完全相同；没有合法路径执行五角色签名真实 E2E，也不能通过 UI 创建任务自有传统报表绑定。继续保持 `BLOCKED / NOT COMPLETE`，不写业务数据、不把 preflight 或静态合同冒充真实 E2E。

### Blocker Recheck 3

- AGENT AUDIT: collaboration 调度器实时返回 A1、A2、A6 均已结束；`task-state.json` 继续证明 A1-A5 为 `completed`、A6 为 `blocked`。A3-A5 已结束实例不再占用调度槽，其交付和 55/55 集成证据仍由任务状态与本日志承载。
- PREFLIGHT RECHECK: `node tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> `status=BLOCKED`、`blockerType=MISSING_EXPLICIT_ENV`、27 个缺失变量名、四项副作用为零；PowerShell 读取 `NODE_LASTEXITCODE=2`，没有访问数据库或浏览器。
- DATABASE RECHECK: 按 `docs/database-rules.md` 重新执行容器内只读 `SELECT`，凭据仅由容器环境变量引用且未输出。完整组合数仍为 `0`；tenant `1` 仍为 `MAIN 1224/1224`、`PROCESS_INSPECTION 176/0`、`LOSS_REPORT 352/0`，tenant `122` 仍为 `MAIN 15/15`、`PROCESS_INSPECTION 2/0`、`LOSS_REPORT 4/0`。
- BLOCKED AUDIT: 相同的两个外部前置条件已经在原始验收、第二次复核和本次第三次复核连续出现，且本轮没有可在授权范围内继续推进的正式路径。任务保持 `BLOCKED / NOT COMPLETE`；真实 Playwright、manifest、TC-A6-01、TC-INT-03 和最终 AC 放行仍未完成。

### A6 User-Authorized Resume

- USER INTENT: 用户明确要求在本机 `芋道源码` 中执行，目标为“球囊扩张导管”工艺路线的最新版本和全部工序；主账号固定为用户指定的 admin，其他必要账号允许从用户列表按正式角色、权限和电子签名授权选择。用户提供的登录口令只用于本轮运行，不写入任务文档、证据、manifest 或命令输出。
- RESUME SCOPE: A6 仅在本机 `8081/48081`、指定租户和指定路线范围内解析/创建任务自有 fixture，并通过真实页面执行生产、PQC、生产组长、放行负责人路径；禁止 SQL/API 写入、共享路线猜测、`formBindings` 替代传统报表或未记录的账号切换。
- PREFLIGHT PASS: `npx`、Node、仓库 Playwright runner 存在；frontend `8081` HTTP 200，backend `48081` health `UP`。正式路线版本、全工序三类传统 report、QA/映射/负责人、五角色登录和签名口令仍需逐项实时证明后才能开始业务写入。
- SKILL ROUTING: `development-plan-delivery`/`milestone-tdd-delivery` 均强制要求 `development-plan.md`，而本任务批准文件是 `dev-plan.md`；未重命名或伪造计划，改按现有 `task-state.json + dev-plan.md + test-plan.md` 继续，并使用 QA/Playwright 门禁执行 A6。

### Main Review Of Authorized A6 Rerun

- LOGIN EVIDENCE: 使用项目正式登录 preflight 从真实 UI 验证用户指定主账号可登录 `芋道源码`，`wangxin` 的用户提供通用凭据也可登录；`lvyujie/shangmengying/huzonggang/liuyueyue/sunxiaoqing/zhengxiaofang` 的同一通用凭据均被真实登录页拒绝。凭据值未写入命令证据、任务文档或输出。
- SIGNED ACCOUNT EVIDENCE: 只读比较已授权的通用凭据与现有密码散列，不打印散列或明文；在具有 ENABLED 电子签名授权和 active signature image 的用户中，只有 `pengyunfeng/wangsiyu/wangxin/zhaohaichen/zhaojie/zhaomingyu` 匹配。六人当前正式角色不能直接覆盖生产员工、生产组长、PQC 检验员、PQC 组长、放行负责人五类业务身份，角色/数据权限调整属于共享账号变更，未执行。
- ROUTE/PRODUCT REVIEW: `routeId=900025` 的最高版本 `V10/id=272` 是 DRAFT，正式最新发布版是 `V9/id=271/ACTIVE`，包含全部 23 工序。路线绑定四个启用产品 `902231/902252/902262/907242`；现有 V9 ACTIVE 排程唯一命中 `productId=902231/code=YXN.037.011.1007`，可作为明确业务选择的依据，但不能在用户未确认时自动代替正式产品选择。
- FORMAL REPORT REVIEW: 目标产品族现有 15 个 APPROVED MAIN reports 的表单名称与 23 个目标工序不形成正式一一映射；唯一 PROCESS_INSPECTION report 可解析，唯一 LOSS_REPORT 缺 definition/version。共享路线仍有 0 条逐工序传统绑定，禁止把 `formBindings`、表单槽位或默认 MAIN 作为替代。
- FORMAL CONFIG REVIEW: ACTIVE V9 快照的 `batchRecordAttachmentOwners` 为 NULL；路线 900025 的 PUBLISHED QA/items/equipment、三类启用 source mapping 和 `RELEASE_APPROVE` 规则均为 0。现有 `resource/过程检验记录.docx`、`resource/批记录节点-解析样本.docx` 与本机损耗单文档只能作为待确认输入，不能自行推导 23 工序的正式报表/QA/设备/负责人配置。
- SHARED DATA IMPACT: 继续使用共享路线需要补齐并发布 V10，这会替代当前 ACTIVE V9；安全的任务路线副本路径也因源快照缺附件负责人而被正式 E2E 门禁阻断。主 Agent 未修改、发布或停用任何共享路线版本，未修改账号角色/数据权限。
- VERDICT: `BLOCKED BEFORE FIRST BUSINESS WRITE`。用户提供的租户、路线和账号凭据已解决环境身份来源，但尚缺唯一产品选择、23 工序正式 MAIN/QA/设备配置、正式 LOSS report、附件负责人、三类 mapping、放行负责人及共享变更授权。真实 Playwright 业务写、manifest、TC-A6-01 和 TC-INT-03 仍为 NOT RUN；业务写、SQL 写、清理和残留 ID 均为 0。

### Pressure Pump To Catheter Formal-Source Mapping Review

- USER INTENT: 用户确认球囊扩张压力泵当前逐工序批记录表单和过程检验记录表单属于正式来源，要求主 Agent 帮助对应球囊扩张导管 V9 全部 23 工序并判断可否完整对应。
- READ-ONLY SOURCE: `922119 / RT000028 / 球囊扩张压力泵 / 627 / V27 / ACTIVE`，14 个工序；14 张 MAIN report 属于 definition `47`、current APPROVED version `130`。目标为 `900025 / ROUTE-XLSX-00001 / 271 / V9 / ACTIVE`，23 个工序。
- OFFICECLI L1: `officecli 1.0.143` 读取 `resource/过程检验记录.docx`；文档包含 1 张 54 行表格，检验依据为压力泵过程检验规程。49 条方法覆盖清洗/精洗/清洁/组装 I/II/III/硅化 I/II/III/光固/检测，包含压力泵旋转接头、压力表、延长管、活塞/活塞环和 8/30 atm 气密性等对象/参数。
- EXACT MATCH: 对目标 23 个工序名分别在压力泵 MAIN `sheet_layout_json` 和 `过程检验记录 V3.0` `recognized_schema_json` 中全文精确检索，二者均为 `0/23`。
- SEMANTIC REVIEW: 完整矩阵写入 `a6-pressure-pump-to-catheter-form-mapping-review.md`；结果为 9 个无直接对应、11 个弱候选、3 个条件候选、0 个无条件可复用。条件候选为点胶海波管→光固Ⅱ（仅当正式确认同一 UV 工艺）、球囊测漏及全检→检测（必须确认导管压力/设备/判定）、纸塑袋封口→单包装（必须确认导管热合参数/包装标准）。
- FORMAL IDENTITY: 目标产品已有 APPROVED PROCESS_INSPECTION report `b48d2a150afc40deb456fb5fe9da551b / definition 51 / version 99 / V6.0`，但当前 cell-link 表没有该类 report 的 `PQC_AGGREGATE_DETAIL` mapping；正式 report 身份不能证明 49 条压力泵方法适用于导管 23 工序。
- OFFICECLI VALIDATION NOTE: 对只读源文件 `resource/过程检验记录.docx` 执行 `officecli validate` 返回 23 个既有 Word XML schema 顺序问题（`themeFontLang`/`uiPriority` 等）；L1 `view text/get table` 已成功读取检验依据和 54 行表格内容，本轮未修改或重写源文档，故不把该 legacy 文件校验失败误记为业务映射失败。
- VERDICT: `NO FORMAL 23/23 AUTO-MAPPING`。压力泵表单可作为布局/结构来源，但原样复用会保留压力泵标题、`PP-ID-*` 依据、零件、设备和参数，形成错误导管记录。未执行路线、报表、QA、mapping、账号、SQL/API/UI 业务写入；manifest 和残留 ID 均为 0。

### DCC Catheter Formal-Source Mapping Refinement

- USER CONTINUE: 用户要求继续完成对应审查。主 Agent 保留“压力泵内容不能原样复用”的既有结论，并进一步检索 DCC 中 `product_code=PTCABC` 的导管专用受控文件，避免把压力泵内容缺口误判为导管没有正式来源。
- DCC IDENTITY: 导管工艺包 `controlledFileId=2054545668044057749 / originalFileId=9198354904952 / ACTIVE / file version V1.0`，文件内受控版本 `PP-PTCABC D/5`；导管过程检验规程候选 `controlledFileId=2054545668044064068 / originalFileId=9198354898506 / ACTIVE / file version V1.0`，文件内受控版本 `PQC-PTCABC-003 C/1`；导管过程检验记录 `controlledFileId=2054545668044064070 / originalFileId=9198354898508 / ACTIVE`，文件内版本 `A/0`。
- PRIVATE OBJECT READ: MinIO 匿名 URL 返回 `AccessDenied`；随后只在本机 `docker-minio-1` 内使用容器环境变量和 `mc cp` 读取上述精确 object key 到任务临时目录，未输出凭据、未改变 bucket/object、未调用业务写 API。SHA-256 分别为工艺包 `A6E5217E...D86899`、C/1 规程 `94041ED8...BF29`、A/0 记录 `4C3ABB88...12C4`。
- OFFICECLI L1: `C/1` 规程可解析为 65 段、4 张表，其中主检验表 32 行；`A/0` 记录可解析为 68 行主表。规程正式列出清洗、光固、外/内管拉伸、吹塑、球囊近端焊接、内管与尖端管焊接、压显影环、远端焊接、Rx 口焊接、涂层浸涂、海波管焊接、折叠压握的接受标准、方法、设备和抽样方案。
- PDF VISUAL: Poppler 对 70 页扫描工艺包执行 96 DPI 全页渲染并生成 7 张 contact sheet；逐页核对第 4-5 页工艺流程及第 16-56 页作业指导书/参数。正式命中包括光固、拉伸、剪裁、吹塑、球囊近端焊接、内管与尖端管焊接、显影环压握、球囊远端焊接、Rx 口焊接、涂层浸涂、海波管焊接、折叠压握、测漏/全检、包保护套和单包装热合。
- MAPPING RESULT: 完整 23 行矩阵写入 `a6-catheter-form-source-mapping-review.md`。生产来源 `20/23`（14 直接、6 从同一受控工序拆分）；过程检验为 9 道直接证据、7 道待 QA 唯一拆分、1 道条件候选；压力泵报表只保留正式布局身份，不复用其业务值。
- REMAINING MAIN GAPS: `6 外管切缝`、`12 裁剪圆角`、`20 球囊盘管（机器）` 在 DCC 工艺包、文件名/标题和当前路线既有绑定中均无正式来源；全库同 processId/同名工序也没有已绑定传统 batchRecordReportId。
- REMAINING PQC GAPS: `2 球囊裁剪`、`6 外管切缝`、`7 裁剪管材`、`8 穿显影环`、`12 裁剪圆角`、`20 球囊盘管（机器）`、`23 纸塑袋封口` 没有独立 PQC 条目；其中工序 8 只有压制后结果的条件候选。`11/13`、`14/15`、`19/21/22` 三组必须由 QA 冻结唯一行归属，禁止重复消费同一 PQC aggregate detail。
- DCC GOVERNANCE RISK: 数据库把 `B/6`、`C/0`、`C/1` 分别建为独立 `ACTIVE_CHAIN`，没有形成同一 master 修订链；B/6 和 C/0 对象当前不是有效 DOCX ZIP，C/1 可正常解析。本审查只能把 C/1 定为内容最新候选，正式 fixture 必须显式冻结 `controlledFileId=2054545668044064068`，不得运行时任取 ACTIVE 第一条。
- STRUCTURAL GREEN: `task-state.json | ConvertFrom-Json` -> PASS；新映射表含 23 条目标工序，生产/PQC 缺口计数与结论一致；所有数据库命令均为 `SELECT/DESCRIBE/SHOW`，业务写、manifest 和残留 ID 仍为 0。
- INDEPENDENT A6 REVIEW: 初审 `NOT RELEASED`；指出 17/23 只能算候选覆盖、工序 8 的证据不充分，以及三组共享 PQC 行存在重复消费风险。主 Agent 已按复审收紧为 9 直接 + 7 待唯一拆分 + 1 条件候选，并在矩阵/任务状态中显式列出共享组；未把治理要求冒充已完成的 QA 冻结证据。
- INDEPENDENT A6 RE-REVIEW: 第二轮仅发现工序 11 的逐行判断仍写成直接完成、与 `11/13` 共享组不一致；已修正为“生产可直接对应；PQC 待与工序 13 唯一拆分”。其余上一轮 finding 均已关闭。
- DEVICE EVIDENCE: 正式迁移 `20260708_mes_balloon_process_device_capacity.sql` 与当前数据库均为三项 MAIN 缺口提供启用设备/产能：`Z3810 -> B09212 导管切缝工装 / 1950`、`Z3850 -> B09262 球囊切管工装 / 2950`、`Z5600 -> B09326 导丝盘管设备 / 1650`。该证据只解决设备身份，不包含操作步骤、记录字段、接受标准、检验方法或抽样规则，故三项正式来源 blocker 保持不变。
- MATRIX CONSISTENCY: 将工序 13 的逐行结论统一为“生产可拆分；PQC 待与工序 11 唯一拆分”，使 `11/13` 与另外两组共享检验行采用同一未放行口径；矩阵仍为 9 道 PQC 直接、7 道待 QA 唯一拆分、1 道条件候选、6 道无适用 C/1 条目。
- INDEPENDENT A6 FINAL REVIEW: `RELEASED`。独立复核确认前两轮关于候选覆盖冒充正式映射、工序 8 证据不足、共享 PQC 行重复消费及工序 11/13 逐行口径不一致的问题均已关闭；该结论只放行只读映射审查文档，不代表正式 fixture 或真实业务 E2E 已完成。

### Supervised Resume Plan-File Gate

- USER AUTHORIZATION: 用户明确授权将 `dev-plan.md` 改名并继续；主 Agent 已使用文件移动补丁改为 `development-plan.md`，原 `dev-plan.md` 不再存在，内容未重写。
- PREFLIGHT GREEN: `development-plan.md`、`prd.md`、`test-plan.md`、`task-state.json` 四个必需文件均存在；`git diff --check` 未发现空白错误。
- SUPERVISOR INIT: `python -X utf8 C:\Users\BJB110\.codex\skills\development-plan-delivery\scripts\init_or_resume_task.py --cwd E:\IntRuoyi --task-dir E:\IntRuoyi\doc\tasks\20260809-active-order-release-dossier-v4-delivery` -> FAIL，精确原因为 `development-plan.md does not contain any milestone headings.`。
- BLOCKER: 当前计划以 `### A2-RED` 至 `### A6` 表达任务图，监督器只接受 `### 里程碑 N：标题`。技能规则要求计划无里程碑标题时停止，禁止自动重规划；本轮未修改产品代码、业务数据或 `task-state.json` 的既有阶段结论，仅追加该结构前置条件。

### Supervised Plan Structure Migration

- USER AUTHORIZATION: 用户明确授权转换计划结构并继续；主 Agent 只将原七个 A 节点改为 `### 里程碑 1..7`，补齐 `目标/涉及文件/交付物` 解析标签，原任务内容、写入范围、验收编号和依赖关系保持不变。
- GREEN: `sync_development_plan.py` -> PASS，`phase_count=7`；七个阶段均解析为恰好 1 个 owned-path 条目和 1 个 deliverable，未再把验收编号或验证步骤误解析成路径。
- STATE MIGRATION: 依据既有 `execution-log.md` 与 `test-report.md`，P1-P6 迁移为 `completed` 并为每个阶段及派生 AC 写入执行/独立测试证据引用；P7/A6 保持 `blocked`、`test_status=running`。监督器的默认线性依赖已按原任务图恢复为 P2/P3/P4 依赖 P1、P5 依赖 P2/P3/P4、P6 依赖 P5、P7 依赖 P5/P6。
- RESOLVED BLOCKER: `development-plan.md` 结构前置已从 `blocking_prereqs` 移除；剩余 blocker 均属于 P7 正式 fixture、共享配置授权和真实 E2E 前置，不把结构迁移冒充 A6 完成。

### P7 A6 Blocking Prerequisites Recheck 4

- BDD: P7 首次业务写入前置门禁 -> Given 当前用户授权仅锁定本机 int_main、tenant `1`、球囊扩张导管路线和已确认正式来源；When 重新核验 `task-state.json.blocking_prereqs` 五项前置；Then 只有唯一产品、附件负责人快照、23 工序正式生产/PQC 来源、三类传统报表/QA/映射/放行负责人及共享变更授权全部通过后才可发出第一个业务写请求，否则结构化阻塞且四类写副作用为零。
- BLOCKER 1 / PRODUCT: 只读查询返回 route `900025` 共 `5` 条未删除产品绑定，其中 item status 启用的唯一 productId 仍有 `4` 个：`902231/902252/902262/907242`；用户未在这四个中冻结唯一正式 productId，禁止选第一条或用旧排程推断。
- BLOCKER 2 / ROUTE COPY: ACTIVE `271/V9` 的 `route_snapshot_json.configSnapshots.batchRecordAttachmentOwners` 当前 `JSON_TYPE=NULL` 且 length `-1`，不是可保留的正式数组。按 eDHR 任务专用路线副本门禁，不得复制后猜测附件负责人。
- BLOCKER 3 / FORMAL CONTENT: 已放行的只读映射审查仍为 `23` 行；MAIN 内容覆盖 `20/23`，缺 `6/12/20`；PQC 为 `9` 直接 + `7` 待 QA 唯一拆分 + `1` 条件候选 + `6` 无适用 C/1，`2/6/7/8/12/20/23` 仍无独立 PQC 规则。映射审查文档 RELEASED 只表示分析口径通过，不代表 23 工序正式来源齐全。
- BLOCKER 4 / FORMAL CONFIG: 当前 route `900025` 未删除工序报表绑定总数 `0`、QA regulation 总数 `0`、该 route 三类必需启用 mapping 总数 `0`、启用 `RELEASE_APPROVE` 路线规则总数 `0`；tenant `1` 可解析到 current APPROVED version 的 `LOSS_REPORT` 总数也为 `0`。候选 `272/V10` 仍为 `DRAFT/active=0/published_time=NULL`。
- BLOCKER 5 / AUTHORIZATION AND ACTORS: 最新用户指令未扩大为发布共享 V10、调整共享账号角色/数据权限或代替 QA 冻结三组拆分规则的授权。当前 executor 实际运行 preflight 返回 `BLOCKED/MISSING_EXPLICIT_ENV`，缺失 `27` 个显式授权/fixture/五角色登录及签名变量，进程 exit `1`；未打印任何变量值。
- SIDE EFFECT AUDIT: 本轮只运行本机文件结构检查、容器内 `SELECT/SHOW`和无 result-path 的可执行 preflight；`browserBusinessWrites=0`、`businessApiWrites=0`、`sqlWrites=0`、`manifestCreated=false`。未启动业务 Playwright，未创建 fixture/spec/manifest/业务 ID，残留数据为 `0`。
- VERDICT: 五项 blocking prerequisite 全部仍未关闭，现有用户授权不足以满足首次业务写入门禁。P7/A6 继续 `BLOCKED / NOT COMPLETE`，TC-A6-01、TC-INT-03、正式 manifest 和 P7-AC1/P7-AC2/P7-AC3 均未完成。

### P7 A6 Target Correction To Pressure Pump

- USER CORRECTION: 用户明确纠正 P7/A6 目标为 `球囊扩张压力泵`，不是 `球囊扩张导管`。因此前述基于导管 route `900025`、product `902231/902252/902262/907242`、DCC 导管工艺包和 C/1 导管 PQC 的 blocker 标记为 `STALE_FOR_CURRENT_P7_TARGET`，不再作为当前 P7 首次业务写入依据。文档保留为历史审查证据，本轮未修改任何产品代码或任务状态文件。
- BDD: 压力泵 V27 首次业务写入门禁 -> Given 当前 P7 目标锁定 tenant `1` 下 `922119 / RT000028 / 球囊扩张压力泵 / routeVersionId=627 / V27 / ACTIVE`；When 只读复核产品、工序、三类传统绑定、QA/items/equipment、三类 source mapping、`RELEASE_APPROVE`和五角色账号/签名门禁；Then 任一正式前置未满足时必须在任何 SQL/API/UI 业务写入前阻塞，不创建 manifest 或假 PASS。
- ROUTE/PUBLISHED VERSION: 只读 `SELECT` 确认 route `922119` 为 `RT000028 / 球囊扩张压力泵`，version `627/V27` 为 `ACTIVE` 且 `active=1`，published time `2026-08-07 13:01:52`。
- PRODUCT BLOCKER: route `922119` 当前有 `4` 条未删除 product 绑定，其中 item status 启用的正式产品为 `3` 个：`901965/AW.107.02.01.2036`、`902149/AW.107.02.01.2010`、`924005/ID`；`902101/AW.107.02.01.1009` 为非启用。用户尚未在三个启用产品中冻结唯一 `productId`，禁止默认选择。
- PROCESS EVIDENCE: 当前 route process 表有 `14` 个未删除工序，`routeProcessIds=980661..980674`，工序为粗洗/精洗/清洗/清洁/组装I/光固I/硅化I/硅化II/组装II/检测/光固II/单包装/中包装/大包装，末工序大包装为 `key_flag=1/check_flag=1`。V27 snapshot 也有 `14` 个相同 `processId/name/sort`，但 snapshot `routeProcessIds=980645..980658`，与当前 route process 表 ID 不同；首次写入前必须冻结明确的 routeProcessId 口径。
- TRADITIONAL BINDING BLOCKER: route `922119` 当前绑定行为 `MAIN=14`、`PROCESS_INSPECTION=2`、`LOSS_REPORT=4`。`MAIN` 有 `14/14` 非空 `batch_record_report_id` 且对应 report 元数据可解析为 current APPROVED；但路线绑定行本身 `batch_record_definition_id/batch_record_version_id` 存在数为 `0`。`PROCESS_INSPECTION` 和 `LOSS_REPORT` 的非空传统 `batch_record_report_id` 均为 `0`，只有动态表单槽位数据；同一工序同时满足 `MAIN + PROCESS_INSPECTION + LOSS_REPORT` 三类非空传统 report 和 current APPROVED 元数据的数量为 `0`。
- QA BLOCKER: route `922119` + routeVersion `627` 有 `14` 条 QA regulation 行，但只有 `1` 条为 `PUBLISHED/PUBLISHED` 且 item count `3`（snapshot routeProcessId `980647` / 清洗）；其余 `13` 条均为 `RETIRED`且无 current published version/items。这不满足 14 工序全量 PUBLISHED QA/items/equipment 门禁。
- SOURCE MAPPING BLOCKER: route `922119` 上 `PROCESS_POOL_REPORT`、`PQC_AGGREGATE_DETAIL`、`PRODUCTION_LOSS` 三类启用 source mapping 均为 `0`，target report count 也均为 `0`。因此即便 MAIN 有报表 ID，也没有可用正式字段映射生成 A3/A4/A5 资料。
- RELEASE_APPROVE PARTIAL PASS: route `922119` 存在启用路线级 `RELEASE_APPROVE` rule `9000253153`，candidate `USER/1/admin`；只读核验 `admin` 为启用用户，电子签名授权 `ENABLED`且 active signature image count `1`。但 release owner 账号必须通过显式 env 和真实 UI 登录/签名路径证明，不能仅用 DB 行代替凭据可用性。
- ACCOUNT/ENV BLOCKER: 当前进程下 A6 可执行 preflight 仍在任何运行态请求、DB 访问和浏览器导航前返回 `BLOCKED/MISSING_EXPLICIT_ENV`，缺 `27` 个显式授权/fixture/五角色用户名/登录口令/签名口令变量，进程 exit `1`。tenant `1` 中具备启用电子签名和 active image 的用户行数为 `14`，但 DB 行不能代替五角色真实登录和签名口令证据。
- SIDE EFFECT AUDIT: 本轮仅读取文件、运行容器内 `SELECT`和无 result-path 的 preflight；未执行 SQL/API/UI 业务写入，未创建或更新 manifest，未修改 `task-state.json/test-report.md/development-plan.md`或产品代码，业务写入、SQL 写入和任务残留数均为 `0`。
- VERDICT: 压力泵 P7 目标已修正并重新核验，但尚不能进入首次业务写。当前精确 blocker 为：未冻结唯一启用产品（3 选 1）、当前 route process ID 与 V27 snapshot ID 口径未冻结、三类传统报表完整组合 `0/14`、PUBLISHED QA/items/equipment 只覆盖 `1/14`、三类 source mapping `0/0/0`、五角色真实登录/签名 env 缺 `27` 项。TC-A6-01、TC-INT-03、正式 fixture manifest 和 P7-AC1/P7-AC2/P7-AC3 仍为 `NOT RUN / BLOCKED`。

### P7 Independent Tester Gate

- TESTER SCOPE: 独立测试者只验证 P7 阻塞结论并只更新 `test-report.md`；未修改产品代码、执行日志、监督状态或业务数据。
- TEST RESULT: `node --check` 2/2 PASS，静态 preflight 合同 1/1 PASS；实际 preflight 以 exit `2` 返回 `STRUCTURED_BLOCKED/MISSING_EXPLICIT_ENV`，缺 27 项显式前置，四类副作用均为 0。
- INDEPENDENT VERDICT: `RELEASED BLOCKER EVIDENCE`，只放行“不能进入首次业务写”的证据；P7 AC 仍为 0/3，正式 manifest、业务 E2E spec、真实业务测试和最终只读业务断言均为 0。
- SUPERVISOR DECISION: 保持 `current_phase=P7/status=blocked/test_status=running`，不推进到完成门禁；独立证据引用已写入 P7 阶段与三个未完成 AC。

### P7 Target Correction Supervisor Sync

- USER CORRECTION: 用户明确指出当前要做的是 `球囊扩张压力泵`，不是 `球囊扩张导管`。主 Agent 接受该纠偏，导管 route `900025` 和 products `902231/902252/902262/907242` 在当前 P7/A6 目标下均为 stale evidence，不再作为当前阻塞依据。
- CHANGE GATE: `docs/changes/20260810-a6-target-pressure-pump-correction.md` 已创建；`validate_change_request.py --evidence` -> PASS。
- A6 READ-ONLY RESULT: 当前压力泵目标为 `922119 / RT000028 / V27 / routeVersionId=627 / ACTIVE`；启用产品 `901965/902149/924005` 未冻结唯一，14 工序 current routeProcessId 与 V27 snapshot 口径不一致，三类传统报表完整组合 `0/14`，QA 仅 `1/14 PUBLISHED`，三类 source mapping `0/0/0`，RELEASE_APPROVE 只具备 DB 候选但缺真实 UI 登录/签名证明，preflight 缺 27 项显式变量。
- INDEPENDENT REVIEW: 独立测试者对压力泵 blocker 证据给出 `RELEASED BLOCKER EVIDENCE`；语法 2/2 PASS、静态合同 1/1 PASS、实际 preflight `STRUCTURED_BLOCKED/MISSING_EXPLICIT_ENV` exit `2`、四类副作用 0。该结论仅放行压力泵阻塞证据，不放行 P7 完成。
- SUPERVISOR STATE: 主 Agent 已将 `task-state.json.blocking_prereqs`、`task.md`、`verification-report.md` 从导管旧口径替换为压力泵当前 blocker。P7 仍为 `blocked`，P7 AC `0/3 completed`，正式 manifest、业务 E2E、最终只读业务断言均为 0。

### P7 Pressure Pump Per-Process Source Correspondence

- USER CONTINUE: 用户要求继续，并强调目标为 `球囊扩张压力泵`；用户认为现有绑定到压力泵每个工序的批记录表单和过程检验记录表单就是正式来源。主 Agent 中断长时间无回传的 A6 复核，按当前库事实继续只读收敛；未启动第二个执行者。
- BDD: 压力泵逐工序正式来源对应 -> Given tenant `1` 的 route `922119 / RT000028 / 球囊扩张压力泵 / routeVersionId=627 / V27 / ACTIVE`；When 按 `process_id + sort` 将 V27 snapshot routeProcessId `980645..980658` 对齐当前未删除 routeProcessId `980661..980674`，并分别读取 `MAIN`、`PROCESS_INSPECTION`、`LOSS_REPORT` 来源；Then 只有逐工序来源能给出正式 report 身份或被合同明确允许的正式 formBindings 时才可进入真实 E2E，否则在首次业务写前阻塞。
- READ-ONLY SQL: 仅执行 `DESCRIBE/SHOW/SELECT`。容器内 MySQL 通过 `MYSQL_ROOT_PASSWORD` 环境变量读取，命令输出未记录密码或连接串；本轮未执行任何 `INSERT/UPDATE/DELETE`、业务 API、浏览器写入或 manifest 创建。
- ROUTE IDENTITY: route `922119` 当前唯一 active 发布版为 `627/V27/ACTIVE`，published time `2026-08-07 13:01:52`。V27 snapshot 的 routeProcessId 为已删除历史行 `980645..980658`，当前有效 routeProcessId 为 `980661..980674`；二者可按相同 `process_id` 和 `sort` 一一映射。
- PER-PROCESS MAIN: 14 个当前工序（粗洗、精洗、清洗、清洁、组装Ⅰ、光固Ⅰ、硅化Ⅰ、硅化Ⅱ、组装Ⅱ、检测、光固Ⅱ、单包装、中包装、大包装）均有 `MAIN/BATCH_RECORD` 非空 `batch_record_report_id`，report 元数据解析到 definition `47`、current APPROVED version `130`；路线绑定行自身 `batch_record_definition_id/batch_record_version_id` 仍为空，但可由 report 元数据回填身份。
- PER-PROCESS PROCESS_INSPECTION: 当前表和 V27 snapshot 中只有粗洗、精洗两个工序存在 `PROCESS_INSPECTION/INTERNAL_RECORD` 的 `formBindings`/槽位来源：`form_template_id=28 / 过程检验记录 / BATCH_SHARED / REQUIRED`。第 3-14 工序在当前配置和 V27 snapshot 中均没有过程检验记录槽位；所有 `PROCESS_INSPECTION` 行的传统 `batch_record_report_id` 均为空。
- PRESSURE PUMP PI REPORT CANDIDATE: `mes_pro_batch_record_report` 中存在压力泵 `PROCESS_INSPECTION` report `a08b8aa2ed5d4e4b874db88505db22ea / EBR_TN1_PROCESS_INSPECTION_DOC_1ebb5980_T01 / 过程检验单 / product_name=球囊扩张压力泵`，但该行 `batch_record_definition_id` 和 `batch_record_version_id` 均为空；不能满足当前 M0 preflight 对 traditional report identity + current APPROVED version 的通过条件。
- LOSS SEPARATION: `LOSS_REPORT` 仅作为不混淆证据复核。当前/V27 中只有前 4 个工序有 `form_template_id=25 / 损耗单` 槽位来源，传统 `batch_record_report_id` 仍为空；不得用损耗单或过程检验槽位补齐彼此。
- VERDICT: 用户说的“每个工序都有批记录表单”在 `MAIN` 维度成立；“每个工序都有过程检验记录表单”在当前库事实下不成立，只能证明前 2/14 工序有 `form_template_id=28` 的 formBindings。按当前项目术语合同，formBindings 也不能自动冒充传统 `PROCESS_INSPECTION.batch_record_report_id`；若要把 `form_template_id=28` 作为正式过程检验来源，需要用户明确授权修改 M0/V4 合同和实现口径。
- BLOCKER UPDATE: P7/A6 仍不能进入真实写入 E2E。当前缺口为：唯一启用 productId 未冻结；过程检验覆盖只有 `2/14` formBindings 且传统 report identity 为 `0/14`；PUBLISHED QA/items/equipment 只覆盖 `1/14`；三类 source mapping 仍为 `0/0/0`；五角色真实 UI 登录/签名 env 仍未完成。正式 fixture manifest、业务 E2E spec、真实业务测试、P7-AC1/P7-AC2/P7-AC3 均保持 `NOT RUN / BLOCKED`。

### P7 Formal Source Contract Recheck

- CONTRACT SOURCE: `prd.md:22` 明确“不把动态 formBindings、默认 MAIN 或工序开始配置当正式资料来源”；`development-plan.md:27` 对批记录 writer 交付物也写明“禁止 formBindings/MAIN”。该合同与用户当前“formBindings 即正式来源”的业务口径存在冲突，不能由主 Agent 静默改口径。
- PREFLIGHT CONTRACT: `active-order-release-dossier-v4-preflight.cjs:494` 要求 `batch_record_report_id IS NOT NULL`，并在 `:507` 明确每个 route process 需要唯一非空 `MAIN / PROCESS_INSPECTION / LOSS_REPORT` report identity；当前压力泵 `PROCESS_INSPECTION` 为 `0/14` traditional report identity，不满足该合同。
- WRITER CONTRACT: `MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java:529-532` 在缺唯一传统 `PROCESS_INSPECTION` 报表绑定时返回 `PROCESS_INSPECTION_REPORT_BINDING_REQUIRED`，处理建议是“通过批记录配置维护 batchRecordReportId 和 PROCESS_INSPECTION 类型”。因此现有生产代码不会把 `form_template_id=28` 当作 A4 正式目标报表。
- SUPERVISOR DECISION: 主 Agent 不进行 fallback、不调整业务口径、不以 `formBindings` 冒充传统报表，也不发起真实写入 E2E。P7 只能保持 `blocked`，直到用户明确授权修改 M0/V4 合同与实现口径，或业务侧补齐压力泵 14/14 工序的传统 `PROCESS_INSPECTION.batch_record_report_id`、QA、mapping 和五角色真实登录/签名前置。

### P7 Pressure Pump Route Slot Binding And Publish

- USER AUTHORIZATION: 用户要求“把 14 个工序都绑定过程检验记录表单和损耗单，然后发布一个新的版本”。本轮授权仅覆盖 tenant `1` 本机 int_main 路线 `922119 / RT000028 / 球囊扩张压力泵` 的路线候选版本、批记录配置表单槽位绑定和路线版本发布；不修改 M0/V4 writer 合同，不把本次配置冒充真实放行 E2E 完成。
- BDD: 压力泵 V27 补齐过程检验和损耗槽位 -> Given 当前生效版本为 `627/V27/ACTIVE` 且当前有效 14 个工序已有 MAIN 批记录表单；When 基于 V27 创建候选版，在每个工序保留现有 MAIN 绑定并补齐 `PROCESS_INSPECTION/form_template_id=28/过程检验记录` 与 `LOSS_REPORT/form_template_id=25/损耗单`；Then 新版本发布后 14 个工序均应同时存在 MAIN、PROCESS_INSPECTION、LOSS_REPORT 三类配置，且不覆盖 V27 已有 MAIN reportId。
- PREFLIGHT: 本机运行态 `http://127.0.0.1:8081` 与 `http://127.0.0.1:48081/actuator/health` 可用；MySQL 当前库为 `ruoyi-vue-pro`；`bpm_form_template_version` 中 `template_id=25` 最新发布版为 `id=27/V2.0/PUBLISHED`，`template_id=28` 最新发布版为 `id=32/V3.0/PUBLISHED`。
- PREFLIGHT: 当前 V27 有 14 个有效工序 `980661..980674`；当前 BATCH 配置已有 14/14 MAIN 绑定、2/14 PROCESS_INSPECTION 槽位和 4/14 LOSS_REPORT 槽位。目标写入方式为正式后端 API：`/mes/pro/route-version/create-candidate`、`/mes/pro/route/flow-config/batch-record/save`、`/mes/pro/route-version/submit-publish`。

### P7 V29 Supervisor Recheck

- CURRENT FACT: 只读 SQL 复核发现压力泵路线当前最新发布版已是 `632/V29/ACTIVE`，`631/V28` 与 `627/V27` 均已 `SUPERSEDED`；该变化来自共享运行态其它任务，本轮未发布路线版本、未写库。
- CONTRACT GUARD: P7 中断留下的 A4/A5 formBinding 合同偏移已回退；四个 writer 相关文件对 `FORM_BINDING_WRITER_REQUIRED`、template `28/25` 常量和新增 helper 的 diff 均为 0。当前仍按 `prd.md:22` 和 `test-plan.md:100` 执行：动态 formBindings 不得作为正式放行资料来源。
- READ-ONLY DB: `VERSION_ACTIVE|632|V29|ACTIVE|1|631`；当前工序 `14` 个，routeProcessId `9908090160..9908090173`；当前工序绑定统计为 `MAIN 14/14`、`PROCESS_INSPECTION form_template_id=28 14/14`、`LOSS_REPORT form_template_id=25 14/14`，但 `PROCESS_INSPECTION` 与 `LOSS_REPORT` 的传统 `batch_record_report_id` 均为 `0/14`，传统 `MAIN+PI+LOSS` 完整组合仍为 `0/14`。
- READ-ONLY DB: 产品绑定 4 条，其中启用状态产品仍为 `901965/902149/924005` 三个，未冻结唯一 product；V29 QA regulation 为 `0/14`，三类 source mapping `PROCESS_POOL_REPORT/PQC_AGGREGATE_DETAIL/PRODUCTION_LOSS` 总数为 `0`；路线级 `RELEASE_APPROVE` rule 仍为 1 条，但放行负责人真实 UI 登录/签名未证明。
- VERIFICATION: `node --check tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> PASS；`node --check tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；`node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS。completion gate -> FAIL，原因保持为 `blocking_prereqs is not empty`、`test_status is not passed`、`P7/P7-AC1/P7-AC2/P7-AC3 not completed`。
- STATE UPDATE: 主 Agent 已用监督器脚本将 `task-state.json.blocking_prereqs` 更新为 V29 当前事实，P7 仍为 `blocked`、P7 AC `0/3 completed`；未创建 manifest、业务 E2E spec 或业务 ID，SQL/API/UI 业务写入均为 0。

### P7 V29 Product And Contract Recheck

- PRODUCT READ-ONLY: 当前 route `922119` 的历史 active orders 中，启用产品唯一命中 `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`；另有 `902101` active order 但 item status 为 `1`，不属于启用产品。route 绑定层仍有启用产品 `901965/902149/924005` 三个，因此 P7 只能把 `902149` 作为推荐冻结对象，不能在未获确认时自动创建 V29 task-owned fixture。
- ACTIVE ORDER READ-ONLY: route `922119` 当前 active order 仍落在旧 routeVersion `627`，V29 `632` 下 active order count 为 `0`；因此真实 A6 仍需要创建任务自有 V29 fixture，不能复用旧 V27 活跃订单冒充 V29 验收。
- CONTRACT BLOCKER: 当前 PRD/test-plan 仍禁止 dynamic formBindings 作为正式放行资料来源；即便 V29 已有 PI/LOSS 槽位 14/14，A4/A5 writer 和 preflight 仍要求传统 `batch_record_report_id`、QA、mapping、签名证据链路。未获用户明确授权前，不修改 M0/V4 合同，不进入真实业务写入。
- STATE UPDATE: `task-state.json.blocking_prereqs` 已收窄产品 blocker：记录 `902149` 是现有启用 active orders 的唯一命中，但 V29 task-owned fixture 仍需显式冻结。

### P7 FormBinding Formal Target Contract Change

- USER AUTHORIZATION: 用户冻结测试物料为 `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`；明确要求过程检验记录表单和损耗单使用工艺路线 V29 已绑定的表单；授权真实 E2E 使用同一 admin 账号完成当前测试路径。凭据不写入文档、命令证据或 manifest。
- CONTRACT: `MAIN` 批记录继续只读逐工序传统批记录绑定。`PROCESS_INSPECTION` 的 `form_template_id=28` 与 `LOSS_REPORT` 的 `form_template_id=25` 可作为各自正式目标载体；二者不提供业务填写事实，正式内容来源仍分别是 CONFIRMED PQC aggregate + PUBLISHED QA，以及已签名生产损耗事实。
- QA CONTRACT: 当前产品按路线物料代码唯一映射 DCC 项目代码；过程检验使用该 DCC 项目、同一稳定 `processId` 对应的最新 PUBLISHED QA 版本，不以 V29 缺直接 QA 行静默降级为空标准。
- BDD: 路线动态表单被识别但自动写入尚未接通 -> Given V29 每个工序存在唯一有效 `PROCESS_INSPECTION/form_template_id=28` 或 `LOSS_REPORT/form_template_id=25` 绑定；When A4/A5 在无副作用 plan 阶段解析正式目标；Then 返回精确的动态 FormCenter 自动写入 blocker，定位到绑定 ID/key，不再误报传统 reportId 缺失，不调用传统 mapping/execution 写入。
- BDD: 动态正式资料成功生成 -> Given 动态绑定的已发布 template version、`FORM_TEMPLATE_VERSION` 精确字段映射、正式 PQC/损耗来源和原始签名均完整；When A4/A5 在生成事务中写入并提交 FormCenter instance；Then 当前 batch/task 关联的实例生效、字段值和来源 hash 可审计、签名证据等于原始记录，之后才允许完成性检查和放行待办。
- RED: `javac @p7-formbinding-javac.args` 后执行 `mvn -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> FAIL，18 tests / 2 failures；PI 实际返回旧 `PROCESS_INSPECTION_REPORT_BINDING_REQUIRED`，LOSS 也未返回专用动态自动写入 blocker，失败原因与合同变更预期一致。
- GREEN: 同一 response-file 编译当前两个 writer 与测试后，执行相同聚焦 `surefire:test` -> PASS，18 tests / 0 failures / 0 errors。A4/A5 已能精确识别 template 28/25 动态正式目标并在真实自动写入尚未实现时返回专用 blocker；传统 writer 回归保持通过，且 blocker 分支未调用传统 mapping/execution 写入。
## 2026-08-10 P7/A6 单账号动态表单前置门禁

- BDD: 用户明确授权单一 admin 承担五个 E2E 角色 -> Given `AORD_V4_M0_ACCOUNT_MODE=SINGLE_ADMIN_APPROVED` 且五个角色显式配置为同一启用账号和签名; When 执行只读 preflight; Then 允许复用该账号并逐角色完成真实页面登录，不把账号重复误报为 blocker。
- BDD: 压力泵 V29 动态正式目标与 QA 门禁 -> Given 14 个当前工序都有传统 MAIN、模板 28 过程检验和模板 25 损耗表单绑定; When 执行只读 preflight; Then MAIN 按传统 APPROVED 报表验证，PI/LOSS 按最新 PUBLISHED 模板版本与 `FORM_TEMPLATE_VERSION` 映射验证，QA 按当前工序的稳定 `process_id` 选择对应产品最新 PUBLISHED 版本。
- RED: `node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> 预期 FAIL，现有 preflight 尚无显式单 admin 模式、动态模板 28/25 正式目标和稳定工序最新 PUBLISHED QA 合同。
- BDD: 生产 reader 使用稳定工序最新 QA -> Given 当前 V29 routeProcessId 与历史已发布 QA 的 routeProcessId 不同但 `processId` 相同，且存在多个 PUBLISHED QA 版本; When 过程检验 writer 读取正式 QA 图; Then 按 `publishedAt`、版本 ID 选择最新 PUBLISHED 版本，不要求 V29 自身重复发布 QA，也不退回任意旧版本。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 预期 FAIL，mapper/reader 尚无 `selectPublishedListByStableProcess` 合同且仍按当前 routeVersionId/routeProcessId 精确读取。

## 2026-08-10 P7/A6 V29 真实只读门禁与稳定工序 QA reader

- GREEN: `node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；单 admin 明确授权模式、动态模板 28/25、稳定工序最新 PUBLISHED QA 和无秘密结果合同均通过静态验证。
- REAL PREFLIGHT: 在本机 `int_main` 前端 `8081`、后端 `48081` health `UP` 下，以用户冻结的 product `902149`、route `922119`、routeVersion `632` 和 14 个当前 routeProcessId 运行只读门禁；真实登录/签名秘密仅运行时注入，未写入命令证据、任务文档或结果 JSON。
- BLOCKED: 可执行 preflight -> exit `2`，`BLOCKED / DYNAMIC_FORM_TEMPLATE_SNAPSHOT_INVALID`；首个失败定位到 routeProcessId `9908090160` 的 `LOSS_REPORT`。只读 SQL 进一步确认 `LOSS_REPORT 14/14` 与 `PROCESS_INSPECTION 14/14` 的 `record_category_snapshot_hash`、`slot_config_snapshot_hash` 均为空。
- SIDE EFFECT AUDIT: `browserBusinessWrites=0`、`businessApiWrites=0`、`sqlWrites=0`、`manifestCreated=false`。门禁在任何业务写入前停止，未创建 fixture、业务 ID 或残留数据。
- DATA BLOCKERS: 模板 28/25 所需 `FORM_TEMPLATE_VERSION` 映射 `PQC_AGGREGATE_DETAIL/PRODUCTION_LOSS` 均为 `0`；13/14 最新 PUBLISHED QA 存在 `equipment_required` 项无正式设备关联；动态绑定候选仍为用户 `149/152`，与本轮 admin 执行账号不同。
- GREEN: 隔离编译 `MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl` 及依赖后运行 JUnit Console -> `2 tests / 2 successful / 0 failed`；已验证 V29 routeProcessId 可通过稳定 `processId` 选择较新的 PUBLISHED QA，不调用旧 routeVersion/routeProcess 精确 selector。
- STANDARD LIFECYCLE BLOCKER: 标准定向 Maven GREEN 命令在执行目标测试前被本任务范围外的 `MesProcessPoolPqcInspectionCorrectionCommand` Lombok getter/setter 编译错误阻断，共 40 个 missing-symbol；本轮未修改该并发任务代码，也未把隔离 GREEN 记作标准 Maven GREEN。

### Dynamic Route Binding Snapshot Hash Root Fix

- BDD: 候选路线保存冻结动态表单快照 -> Given 候选路线工序配置包含已发布动态模板、正式槽位策略和唯一填写候选；When 通过正式路线配置服务保存候选版本；Then 候选快照中的动态绑定必须同时包含不同的 64 位 `recordCategorySnapshotHash` 与 `slotConfigSnapshotHash`，后续发布投影不得生成空 hash。
- RED: 定向 `javac` 编译 `MesProRouteFlowConfigServiceImplTest` -> FAIL，新增断言无法解析 `MesProRouteFlowFormBindingSaveReqVO.getRecordCategorySnapshotHash/getSlotConfigSnapshotHash`；证明动态候选快照模型和保存链路尚未承载两类正式 hash，失败原因符合预期。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`43 tests / 0 failures / 0 errors`。候选版本保存现在由后端基于路线、工序、模板发布版本、槽位策略和候选身份生成不同的两类 SHA-256；候选快照解析、页面响应和发布投影链路均保留该正式证据。
- RUNTIME/DATA NOTE: 代码修复不回写既有已发布 V29，也不直接 SQL 修补历史快照。当前 int_main 稳定运行包仍未包含本次源码改动；必须在正式构建部署后通过页面创建并发布下一候选版本，才能让新路线绑定获得非空 hash。

### A4 Dynamic Form Revision

- BDD: 唯一 DCC 产品项目限定最新发布 QA -> Given 当前产品存在唯一启用且 `productMasterId` 精确相等的 DCC 项目，并存在同路线稳定 `processId` 的多个 PUBLISHED QA 版本; When A4 读取过程检验正式来源; Then 只选择该正式产品项目下同工序的最新 PUBLISHED QA 图，并把 DCC 项目身份纳入来源审计。
- BDD: 缺正式 DCC 产品项目时不读取 QA -> Given 产品只有名称相似的 DCC 项目或 DCC 项目未显式绑定该 `productMasterId`; When A4 执行无副作用 plan; Then 返回 `PQC_DCC_PROJECT_IDENTITY_REQUIRED`，不按名称、项目代码或 `ID/IDPR` 猜测，不读取 QA、不写 execution、不写 FormCenter instance。
- BDD: template 28 动态目标正式生成 -> Given CONFIRMED PQC 汇集、PQC/复核原始签名、唯一 DCC 产品项目、最新 PUBLISHED QA、唯一 template 28 已发布绑定和 `FORM_TEMPLATE_VERSION + FORMTPL:<versionId>` 精确 fieldCode 映射完整; When A4 写入当前 `batchExecutionId/batchTaskId` 的 `ROUTE_FORM` 任务; Then 写入并提交该任务已关联的 FormCenter instance，返回逐字段来源 hash、正式提交快照审计 ID/head hash 和原始签名，且不创建传统 execution。
- BDD: 动态模板、映射或签名缺失时无副作用阻塞 -> Given template version 不匹配/非 PUBLISHED、识别 fieldCode 不唯一、映射缺失或 PQC/复核签名缺失; When A4 执行 plan; Then 返回精确 blocker，FormCenter save/submit、传统 execution 和传统 field audit 调用均为 0。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；reactor 进入 `yudao-module-mes:testCompile` 后仅因 `MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort` 尚不存在出现 2 个缺符号错误，符合 template 28 正式自动写入端口尚未实现的预期原因；上游 23 个模块均成功，非 fixture、依赖或错误模块路径失败。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；`MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest` 2/2、`MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest` 4/4、`MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest` 12/12，合计 18/18，Failures 0，Errors 0。
- IMPLEMENTATION: 新增 A4 template 28 动态 FormCenter port 与 QA-DCC provenance port；reader 先按产品 `productMasterId` 解析唯一启用 DCC 项目，再按 `productId + routeId + stable processId` 选择同 DCC 项目身份下最新 PUBLISHED QA；writer 在 plan 阶段校验 DCC 项目、QA provenance、template 28 精确版本、`FORM_TEMPLATE_VERSION + FORMTPL:<versionId>` 映射和签名证据。
- IMPLEMENTATION: write 阶段仅允许写当前 batch task 已关联的 `EDHR_ROUTE_FORM` FormCenter instance；校验 tenant、task、binding、template version、actionCode、slot/hash 和业务上下文后保存草稿并提交 EFFECTIVE；返回 FormCenter instance ID、提交快照 ID、fieldAuditHeadHash、sourceSnapshotHash 和原始 PQC/复核签名 evidence，不创建传统 PROCESS_INSPECTION execution。
- REVIEW: 主 Agent 中断无最终回报的 A4 executor 后复核 surefire 报告与源码边界；P3 AC 标记 completed。P4/LOSS_REPORT template 25 动态写入仍为 needs_revision，不得由 A4 结果冒充完成；P7 真实 E2E 仍需等待 P4、运行包更新、新候选版本 hash、字段映射、QA 设备关联和 fixture。
- RED: 主审补充 DCC 来源门禁后追加 `productLinkedQaWithoutExplicitDccProvenanceIsNotExposedAsFormalQa` 与 `qaWithoutExplicitDccProvenanceBlocksBeforeBindingOrAnyTargetWrite`；`mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort` 缺符号；证明现有实现还没有显式 QA-DCC provenance 端口，不能只靠 `productId/routeId/processId` 把 PUBLISHED QA 当作正式 DCC QA。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，18 tests / 0 failures / 0 errors / 0 skipped。A4 已实现 template 28 FormCenter 动态正式写入端口成功路径：校验 task/binding/instance 身份、精确 PUBLISHED template version、`FORM_TEMPLATE_VERSION + FORMTPL:<versionId>` fieldCode 映射、必填字段、现有值冲突、提交后 EFFECTIVE 状态和 SUBMIT 快照 audit head；writer 成功路径返回 FormCenter instance id、审计快照 id/head hash、逐字段 source hash 和原始签名，不创建传统 execution。
- GREEN: 同一聚焦命令覆盖正式来源门禁：reader 只接受唯一 `productMasterId` 精确匹配的启用 DCC 项目；最新 PUBLISHED QA 候选必须经 `MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort` 验证后才暴露为正式 QA，缺显式 provenance 时返回 `PQC_DCC_QA_PROVENANCE_REQUIRED`，不加载 QA item/equipment、不解析绑定、不调用 FormCenter save/submit、不调用传统 execution/field audit。默认生产 provenance 端口当前严格阻塞，原因是当前 schema/数据尚无可验证 QA-DCC 项目关系；未猜测 `ID`/`IDPR`。
- REGRESSION: `mvn -rf :yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，18 tests / 0 failures / 0 errors / 0 skipped；补充验证从失败续跑到 `yudao-module-showroom` 与 `yudao-server` 后 reactor `BUILD SUCCESS`，但 A4 标准验收以 `-pl yudao-module-mes` 聚焦命令为准。
- SIDE EFFECT AUDIT: 本轮未执行 SQL/API/UI 业务写入，未创建 batch execution、batch task、传统 execution、FormCenter instance 或 manifest；仅修改 A4 reader/writer/动态表单端口/QA provenance 端口及对应 JUnit、execution-log。`test-report.md`、`task-state.json`、A2 编排、A5 损耗 writer、前端均未作为本轮 A4 实现目标修改。
- REMAINING BLOCKER: 当前真实产品 `902149` 与路线 V29 虽有 template 28/25 绑定，但库内尚缺“QA PUBLISHED version 属于该 DCC project”的显式 provenance/关系表或解析结果；因此正式运行会精确阻塞为 `PQC_DCC_QA_PROVENANCE_REQUIRED`，不会把名称候选 `ID` 或模板识别 `PQC-IDPR-001` 当作可猜测身份。

### P7 Pressure Pump V29 Form/QA Correspondence

- USER INTENT: 用户要求先把当前压力泵目标做正式来源对应；本轮仅只读核对 tenant `1`、product `902149 / AW.107.02.01.2010`、route `922119 / RT000028`、V29 `routeVersionId=632`，不写业务库、不运行真实业务 E2E、不修改生产代码。
- BDD: 压力泵 V29 正式来源对应表 -> Given 用户冻结的产品 `902149`、路线 `922119` 和当前 ACTIVE V29；When 按 14 个当前 routeProcessId 读取 MAIN 批记录、template 28 过程检验、template 25 损耗单、稳定 processId 最新 PUBLISHED QA 与 DCC 项目代码关系；Then 能对应的来源进入候选表，缺正式关系、字段映射、设备关联或快照 hash 时继续在首次业务写前阻塞。
- READ-ONLY SQL: 通过本机 `int-ruoyi-mysql` 容器只读 SELECT 复核，V29 `632` 为 `ACTIVE`；当前 routeProcessId 为 `9908090160..9908090173` 共 14 道工序。
- CORRESPONDENCE PASS: 14/14 工序都有 MAIN 批记录绑定；14/14 工序都有 `PROCESS_INSPECTION` 动态绑定，模板 `28`、最新发布模板版本 `32 / V3.0`；14/14 工序都有 `LOSS_REPORT` 动态绑定，模板 `25`、最新发布模板版本 `27 / V2.0`。
- QA CANDIDATE PASS: 按 product `902149` + route `922119` + 当前工序稳定 `process_id` 能找到 14/14 最新 PUBLISHED QA 候选；其中 1/14 设备关联完整，13/14 存在 `equipment_required` 项缺正式设备关联。
- BLOCKER / DYNAMIC SNAPSHOT: V29 现有 28 条 PI/LOSS 动态绑定的 `record_category_snapshot_hash` 与 `slot_config_snapshot_hash` 仍为空；这是旧 V29 发布前生成的历史数据，不做 SQL 回填。需运行包加载已修复的路线保存逻辑后，通过正式页面/接口创建并发布下一候选版本，生成非空 hash。
- BLOCKER / FIELD MAPPING: `FORM_TEMPLATE_VERSION` 维度启用映射为 0；template 28 缺 `PQC_AGGREGATE_DETAIL` 到 `FORMTPL:32` 的正式字段映射，template 25 缺 `PRODUCTION_LOSS` 到 `FORMTPL:27` 的正式字段映射。MAIN 批记录目标报表对应 `PROCESS_POOL_REPORT` 映射计数也为 0。
- BLOCKER / DCC PROJECT IDENTITY: 当前 `dcc_project_code` 无 `product_master_id=902149` 的启用项目，别名表也无 `902149` 或 `AW.107.02.01.2010` 的启用别名；现有启用 DCC 项目为 `ID/product_master_id=11` 与 `IDPR/product_master_id=13`，不能按当前严格合同自动当作 `902149` 的正式 DCC 项目来源。
- RESULT: 已完成“先对应”的只读候选表收敛；当前可对应的是三类表单绑定和 14 条 QA 候选，尚不可进入真实写入 E2E，因为 DCC 项目身份、动态模板字段映射、13 条 QA 设备关联和新版本动态绑定 hash 未满足。

### P7 Pressure Pump V29 Correspondence Artifact

- ARTIFACT: `doc/tasks/20260809-active-order-release-dossier-v4-delivery/p7-pressure-pump-v29-correspondence.md` 已生成逐工序对应表。
- READ-ONLY RESULT: 14/14 MAIN 批记录、14/14 template 28 过程检验、14/14 template 25 损耗单、14/14 stable process 最新 PUBLISHED QA 均已对应；清洗工序 QA 明确来自 DCC 受控文件 `PQC-ID-001 / G/0`，其余 13 条为旧 M0 派生或本地 fixture。
- REMAINING BLOCKERS: `AW.107.02.01.2010` 到 DCC 项目 `ID/IDPR` 的正式身份关系未命中；template 28/25 的 `FORM_TEMPLATE_VERSION` 字段映射为 0；V29 动态绑定 hash 为空；13 条 QA 设备关联缺口仍存在。

### P4 Loss Dynamic Form Main Review

- MAIN REVIEW: P4 executor completed LOSS_REPORT template 25 dynamic FormCenter gap without changing `task-state.json` or `test-report.md`.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest" test` -> FAIL，3 tests / 1 failure；record hash mismatch still reached `saveDraft` before the fix.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest" test` -> PASS，3 tests / 0 failures / 0 errors。
- MAIN REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，16 tests / 0 failures / 0 errors，BUILD SUCCESS。
- IMPLEMENTATION: LOSS dynamic port now fails before draft/submit when task/binding record hash mismatches, preserves slot hash/template/version/actionCode checks, writes only current task FormCenter instance, requires EFFECTIVE submit snapshot evidence, and lets dossier completeness accept dynamic LOSS evidence.
- GATE STATUS: P4 is not advanced yet. Three independent tester dispatch attempts failed to return a `test-report.md` update and did not start a new Maven process; main Agent interrupted the stalled tester agents rather than fabricating independent evidence.

### P7/A6 Read-Only Gate Refresh - 2026-08-11

- USER INTENT: 主线程要求继续 P7/A6，但仅执行最新只读前置门禁和证据刷新；禁止 SQL 写入、业务 API 写入、Playwright 业务写入、manifest 创建、task-state/test-report 修改。
- SCOPE: tenant 1；product `902149` / `AW.107.02.01.2010`；route `922119` / `RT000028` / `球囊扩张压力泵`；routeVersion `632` / `V29` / `ACTIVE`。
- BDD: P7 read-only gate -> Given P1-P6 已完成且 P7 仍需真实 fixture/E2E 前置；When 在不做业务写入的前提下运行静态合同、可执行 preflight 和只读 SELECT；Then 若显式环境、正式来源、动态映射、QA 设备、hash 或账号前置不齐，必须 STRUCTURED_BLOCKED 且副作用为 0。
- COMMAND: `node --check tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> PASS。
- COMMAND: `node --check tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS。
- COMMAND: `node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS，静态合同确认 SINGLE_ADMIN_APPROVED、BIT 字段 `CAST(... AS UNSIGNED)`、非空密码/签名口令、不要求自设长度、动态 PI/LOSS 合同、FORM_TEMPLATE_VERSION 映射检查仍存在。
- COMMAND: `node tests/e2e/active-order-release-dossier-v4-preflight.cjs --result-path ../doc/tasks/20260809-active-order-release-dossier-v4-delivery/a6-preflight-blocked.json` -> BLOCKED / exit 2；blocker=`ENVIRONMENT/MISSING_EXPLICIT_ENV`；缺 28 个显式 `AORD_V4_M0_*` 环境变量；sideEffects=`browserBusinessWrites=0,businessApiWrites=0,sqlWrites=0,manifestCreated=false`；未启动业务写入 E2E。
- SELECT: docker container `int-ruoyi-mysql` / schema `ruoyi-vue-pro` 只读查询；未直接 SQL 写入。
- SELECT RESULT: route `922119` 最新三版为 V29 `id=632 ACTIVE`、V28 `id=631 SUPERSEDED`、V27 `id=627 SUPERSEDED`；product binding 命中 `902149` / `AW.107.02.01.2010`。
- SELECT RESULT: V29 route processes `9908090160..9908090173` 共 14 条；route_process 逐工序 MAIN report id 非空 `14/14`。
- SELECT RESULT: `mes_pro_route_flow_process_batch_record` 三类计数为 MAIN `14/14`、PROCESS_INSPECTION template `28` / version `32` `14/14`、LOSS_REPORT template `25` / version `27` `14/14`。
- SELECT RESULT: dynamic binding hashes remain missing: PROCESS_INSPECTION rows `14`, record_category_snapshot_hash `0/14`, slot_config_snapshot_hash `0/14`; LOSS_REPORT rows `14`, record_category_snapshot_hash `0/14`, slot_config_snapshot_hash `0/14`。
- SELECT RESULT: template versions exist and are published: `32/template 28/V3.0/PUBLISHED` and `27/template 25/V2.0/PUBLISHED`。
- SELECT RESULT: FORM_TEMPLATE_VERSION dynamic mappings remain missing: PI mapping `FORM_TEMPLATE_VERSION:32 -> FORMTPL:32 / PQC_AGGREGATE_DETAIL` count `0`; LOSS mapping `FORM_TEMPLATE_VERSION:27 -> FORMTPL:27 / PRODUCTION_LOSS` count `0`。
- SELECT RESULT: latest PUBLISHED QA by stable process identity exists for `14/14` and all have items; equipment completeness remains blocker: only `1/14` QA rows have `missing_equipment_count=0`, total missing equipment links `44`。
- SELECT RESULT: DCC direct product identity remains missing: `dcc_project_code` direct match for `product_master_id=902149` or `AW.107.02.01.2010` count `0`。
- SELECT RESULT: RELEASE_APPROVE route-level rule exists uniquely: count `1`, rule `9000253153`, source `USER/1`。
- NOT COMPLETED: admin single-account UI/login/signature proof was not executed because explicit account/password/signature env variables are absent and main thread instructed stop expansion before further queries; no secret values were logged。
- STRUCTURED_BLOCKED: P7 cannot enter manifest or real business E2E. Blocking prereqs now are: missing explicit AORD env/account/signature/browser/db container variables; DCC product identity for `902149/AW.107.02.01.2010`; FORM_TEMPLATE_VERSION mappings for `32/PQC_AGGREGATE_DETAIL` and `27/PRODUCTION_LOSS`; non-empty dynamic binding hashes for V29 PI/LOSS; QA equipment mappings for 13/14 process QA rows; admin single-account UI/login/signature proof.

### P7 Pressure Pump Field Correspondence - 2026-08-11

- USER INTENT: 用户要求“先对应”，并明确目标为 `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`；过程检验记录表单和损耗单使用工艺路线当前绑定表单，QA 来源应按 DCC 项目代码对应的最新发布 QA 数据理解。
- BDD: 压力泵字段对应复核 -> Given V29 已发布且 14 个工序已有 PI/LOSS 动态表单绑定；When 按 `productId=902149 + routeId=922119 + stable processId` 只读匹配最新 PUBLISHED QA，并检查 FORM_TEMPLATE_VERSION 字段规则；Then 输出可执行对应结论，若当前接口或模板结构无法承载完整映射则在业务写入前阻塞。
- SELECT: `mes_md_item` -> product `902149` = `AW.107.02.01.2010 / 球囊扩张压力泵`。
- SELECT: route `922119` 最新发布版本为 `V29 / routeVersionId=632 / ACTIVE`；V29 14 个工序均有批记录、template 28/version 32 过程检验记录、template 25/version 27 损耗单绑定。
- SELECT: `bpm_form_template_version` -> template version 32 过程检验记录有 56 个识别字段；template version 27 损耗单有 8 个识别字段。
- SELECT: 当前 `mes_pro_batch_record_cell_link_rule` 中 `FORM_TEMPLATE_VERSION` 仅存在 `PRODUCTION_WORK_ORDER -> FORMTPL:32/27` 各 1 条；`PQC_AGGREGATE_DETAIL` 和 `PRODUCTION_LOSS` 映射仍为 0。
- READ SOURCE: `/mes/pro/batch-record-cell-link/rules/save` 在 `FORM_TEMPLATE_VERSION` 作用域下只允许 `PRODUCTION_WORK_ORDER` 来源，当前正式接口不能保存 `PQC_AGGREGATE_DETAIL` 或 `PRODUCTION_LOSS` 来源映射。
- READ SOURCE / SELECT: 当前 PROCESS_INSPECTION writer 按精确 sourceKey 要求 14 工序最新 QA 合计约 `794` 条必填映射，但模板 32 只有 `56` 个可写字段；当前 LOSS writer 要求 `15` 个唯一来源字段，但模板 27 只有 `8` 个可写字段。
- RESULT: 已更新 `p7-pressure-pump-v29-correspondence.md`，记录业务层可对应口径和当前实现/模板不能直接落库的结构性阻塞；未执行业务 API 写入、SQL 写入、Playwright 写入或 manifest 创建。

### P7 Dynamic Form Mapping Save - 2026-08-11

- BDD: 动态正式来源字段规则可配置 -> Given template 28/25 的目标是 `FORM_TEMPLATE_VERSION` 范围下的 `FORMTPL:<versionId>`，且 sourceType 是 `PQC_AGGREGATE_DETAIL` 或 `PRODUCTION_LOSS`; When 管理员通过正式 batch-record-cell-link 保存字段规则; Then 规则必须保留正式 sourceType/sourceFieldCode/target field，并且不能把动态来源降级为生产工单或普通批记录单元格。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest#saveRules_acceptsFormalDynamicSourceForFormTemplateVersionScope" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> expected FAIL；当前 `FORM_TEMPLATE_VERSION` 保存逻辑拒绝非 `PRODUCTION_WORK_ORDER` 来源，导致 PI/LOSS 动态正式映射无法通过正式接口落库。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest#saveRules_acceptsFormalDynamicSourceForFormTemplateVersionScope" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1/1；`PQC_AGGREGATE_DETAIL` 在 `FORM_TEMPLATE_VERSION` scope 下可保存为字段级正式来源，保留 `sourceType/sourceReportId/sourceFieldCode/sourceFieldName/targetCellKey`。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10/10；覆盖 `PQC_AGGREGATE_DETAIL` 与 `PRODUCTION_LOSS` 两类动态正式来源。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，44/44。
- EXECUTOR RECHECK: 2026-08-11 06:29 重跑 `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 44, Failures: 0, Errors: 0, Skipped: 0。
- IMPLEMENTATION: `MesProBatchRecordCellLinkServiceImpl` 新增 `PQC_AGGREGATE_DETAIL` 与 `PRODUCTION_LOSS` 作为 `FORM_TEMPLATE_VERSION` 的正式字段级 source type；非模板版本 scope 仍拒绝这些动态来源，生产工单自动预填逻辑保持原行为。
- REMAINING BLOCKER: 本次只打通正式规则表达入口，未猜测或写入 14 工序的真实字段映射；V29 仍缺实际 `PQC_AGGREGATE_DETAIL -> FORMTPL:32`、`PRODUCTION_LOSS -> FORMTPL:27` 规则数据、动态绑定 hash、DCC 产品身份关系和 13/14 QA 设备关联。

### P7 Pressure Pump Template Field Correspondence - 2026-08-11

- USER INTENT: 用户要求“先对应”压力泵现有工艺路线绑定的过程检验记录表单和损耗单；本轮仅做只读 SQL/源码复核并更新对应表，不写业务 SQL、不调用业务写 API、不创建 fixture/manifest。
- READ-ONLY SQL: template 28 当前发布目标为 `FORMTPL:32 / V3.0`，recognized fields `56`；template 25 当前发布目标为 `FORMTPL:27 / V2.0`，recognized fields `8`；现有 `PQC_AGGREGATE_DETAIL -> FORMTPL:32` 和 `PRODUCTION_LOSS -> FORMTPL:27` 启用规则均为 `0`。
- READ-ONLY SQL: 压力泵 14 个稳定工序最新 PUBLISHED QA 合计 `78` 条 item；按当前 PI writer 的逐 QA 明细规则，理论需要 `794` 条 source key 映射（item 明细 `696` + 14 道工序 header/DCC `98`），超过 template 28 的 56 个字段。
- CODE REVIEW: 当前 PI writer 要求 `inspectionType|itemCode|sampleNo|fieldCode` 精确 sourceCellKey；当前 LOSS writer 要求 15 个 `PRODUCTION_LOSS` 字段。二者均不能靠全局 `FORM_TEMPLATE_VERSION` 规则把 14 工序真实来源安全压进当前模板。
- ARTIFACT: `p7-pressure-pump-v29-correspondence.md#2026-08-11-字段级对应复核` 已追加可审核对应结论：业务来源能对应，当前可执行方案必须新增摘要 source writer 或扩展 template 28/25；禁止写猜测映射或用重复字段占位冒充完成。

### P7 Pressure Pump Main Correspondence Recheck - 2026-08-11

- USER INTENT: 用户要求“你帮我先对应”；本轮仅把压力泵 14 工序、当前路线绑定 PI/LOSS 表单和最新 PUBLISHED QA 做正式对应，不保存字段规则、不写 SQL、不创建业务 fixture。
- BDD: 先对应压力泵放行资料来源 -> Given 产品 `902149 / AW.107.02.01.2010`、路线 `922119 / RT000028`、V29 已发布且每工序绑定了 PI/LOSS formBindings; When 读取模板字段、QA 最新发布数据和现有字段规则结构; Then 输出可执行对应表，并在字段规则无法安全承载时阻塞写入。
- READ-ONLY SQL: docker container `int-ruoyi-mysql` / schema `ruoyi-vue-pro`；使用容器内 MySQL 环境变量连接，只输出字段、计数、版本和工序信息，不输出数据库口令。
- SELECT RESULT: template 28 最新发布目标 `FORMTPL:32 / V3.0`，recognized fields 56；template 25 最新发布目标 `FORMTPL:27 / V2.0`，recognized fields 8。
- SELECT RESULT: 14/14 工序均可按 `productId=902149 + routeId=922119 + stable processId` 对应最新 PUBLISHED QA；QA item 数分别为 3、3、3、3、3、15、3、3、5、13、15、3、3、3；设备缺口分别为 2、2、0、2、2、8、2、2、3、7、8、2、2、2。
- SCHEMA CHECK: `mes_pro_batch_record_cell_link_rule.source_cell_key` 为 `varchar(32)`；当前 PI writer 期望 `inspectionType|itemCode|sampleNo|fieldCode` 精确 key，多数真实 QA key 无法安全落库。
- CODE REVIEW: `FORM_TEMPLATE_VERSION` 是模板版本全局 scope，缺 routeProcess 维度；PI writer 还会拒绝当前工序不存在的 source key，因此不能把 14 工序不同 QA item 混入一套全局规则。
- CODE REVIEW: 当前正式 saveRules 对动态来源会重写 `source_cell_key=<sourceType>:<sourceFieldCode>`，与 PI writer 精确 key 和 LOSS writer 测试期望 key 不一致。
- RESULT: 已更新 `p7-pressure-pump-v29-correspondence.md#2026-08-11-主线对应确认`；业务来源已对应，字段规则写入保持阻塞，需先改合同/writer 或扩展模板/规则维度后才能进入真实写入 E2E。

### P7 Dynamic Summary Writer Main Integration - 2026-08-11

- USER INTENT: 继续推进 P7，基于用户已确认的压力泵路线绑定表单口径，让 template 28/25 动态表单可以承载过程检验与损耗摘要来源，而不是继续要求逐 QA 明细或 15 个损耗字段一对一落到全局模板规则。
- BDD: 动态表单摘要写入 -> Given 压力泵 V29 每工序已经绑定 template 28 过程检验记录和 template 25 损耗单；When release writer 读取正式 PQC/feedback/签名/产品主数据并发现目标是 FormCenter 动态表单；Then writer 使用 `SUMMARY|<field>` 的字段级正式来源生成摘要，写入当前 batch/task 的 FormCenter instance，并保留 source hash、field audit 和签名 evidence。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，45 tests / 1 failure；`MesTeamLeaderActiveOrderReleaseLossReportWriterTest.shouldTreatRouteBoundLossFormTemplateAsFormalTargetButBlockWhenDynamicMappingsAreMissing` 仍用旧字段 `reviewerSignedAt` 验证动态映射缺失，实际新链路先要求正式产品主数据和 `productLabel` 摘要映射。
- FIX: 更新 LOSS 动态映射缺失测试夹具，补正式产品主数据，使该用例真正验证 template 25 动态摘要映射缺失；断言字段改为 `productLabel`，不降低生产校验。
- GREEN: `mvn -q -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test; if ($LASTEXITCODE -eq 0) { Write-Output 'BUILD_SUCCESS_EXIT_0' } else { Write-Output ('BUILD_FAILED_EXIT_' + $LASTEXITCODE) }` -> PASS，`BUILD_SUCCESS_EXIT_0`。
- MAIN REVIEW: PI writer dynamic path now requires template 28 `PQC_AGGREGATE_DETAIL` summary rules and writes `batchCode/dccProjectCode/dccProjectName/qaVersionNo/itemSummary/resultSummary/equipmentSummary/overallJudgement/inspectorSignedInfo/reviewerSignedInfo` to FormCenter. LOSS writer dynamic path now requires template 25 `PRODUCTION_LOSS` summary rules, formal enabled product master data, and writes `productLabel/batchCode/productionSummary/lossSummary/lossDetailsSummary/fillerSignedInfo/reviewerSignedInfo/approvalSummary` to FormCenter.
- REMAINING P7 BLOCKERS: 当前库仍缺实际 `FORM_TEMPLATE_VERSION:32 -> FORMTPL:32 / PQC_AGGREGATE_DETAIL` 与 `FORM_TEMPLATE_VERSION:27 -> FORMTPL:27 / PRODUCTION_LOSS` 摘要字段规则；V29 PI/LOSS 动态绑定 hash 仍为空；`902149/AW.107.02.01.2010` 到 DCC 项目身份仍未直接命中；13/14 QA 设备关联仍不完整；manifest、真实 Playwright 业务 E2E 和最终只读断言仍未创建/执行。
## 2026-08-11 pressure pump dynamic field correspondence

BDD: 先对应压力泵动态过程检验与损耗字段 -> Given 用户明确产品为 902149 / AW.107.02.01.2010 / 球囊扩张压力泵，且 PI/LOSS 使用工艺路线 V29 已绑定表单 When 使用 tenant 1 admin 登录态读取正式路线、表单中心模板并通过正式规则保存接口提交摘要映射 Then 14 道工序应对应到 template 28 / template 25，字段规则应保存为 FORMTPL:32 与 FORMTPL:27 的摘要 source 映射。

RED: POST /mes/pro/batch-record-cell-link/rules/save for FORMTPL:32 -> FAIL, returned 1040509089 批记录表单布局 JSON 无效：FORMTPL:32；未记录口令或 token。

RED: POST /mes/pro/batch-record-cell-link/rules/save for FORMTPL:27 -> FAIL, returned 1040509089 批记录表单布局 JSON 无效：FORMTPL:27；未记录口令或 token。

Evidence: GET /mes/pro/route-version/list-by-route?routeId=922119 -> code 0, latest active V29 id=632；GET /mes/pro/route/flow-config?routeId=922119&routeVersionId=632&useType=BATCH -> code 0, 14 process configs with MAIN + PROCESS_INSPECTION + LOSS_REPORT bindings；GET /form-center/templates/28/versions/V3.0 -> code 0, PUBLISHED, recognizedFields=56；GET /form-center/templates/25/versions/V2.0 -> code 0, PUBLISHED, recognizedFields=8。

Blocker: 正式规则保存接口当前不能解析动态 FormCenter 的 FORMTPL:32 / FORMTPL:27 布局 JSON，因此不能通过正式接口落摘要字段规则；未使用 SQL 绕过、未发布新版本、未创建业务 E2E fixture。

Runtime blocker: 当前 48081 后端为本日早间旧 runtime jar，未证明已加载当前动态摘要 writer/cell-link 解析改动；主工作区同时存在多项并发脏改动，按 docs/local-runtime.md 运行态门禁，不能直接从脏主工作区重包重启冒充本任务运行态。下一步需要干净可验证构建/运行态刷新后重试正式规则保存。

## 2026-08-11 P7 runtime mapping supervisor follow-up

- SUPERVISOR: 按 development-plan-supervisor 恢复当前状态，current_phase=P7，P1-P6 completed，P7 blocked/test_status running；本轮只允许推进当前 P7，不重规划、不改 future scope。
- EXECUTOR: 已启动唯一 P7 executor p7_runtime_mapping_executor，目标为刷新可验证运行态后通过正式接口保存压力泵 V29 PI/LOSS 摘要字段规则，或返回精确 blocker。
- INTERRUPT: executor 超过监督等待窗口未回报且本机无 Maven/构建进程运行；主线程中断该 executor，未采纳其未完成工作。
- READ-ONLY RESULT: 当前 48081 仍是旧 int_main runtime jar；/mes/pro/batch-record-cell-link/workbench-context?templateId=28&versionNo=V3.0 与 template 25/V2.0 仍返回系统异常，规则未落库。
- STATE UPDATE: task-state.json 已补充 P7 blocker：旧 runtime 未证明加载当前动态 FormCenter 解析/摘要 writer，且主工作区存在并发脏改动，按 local-runtime 门禁不能直接从脏主工作区重包重启作为 P7 证据。
- REMAINING: P7 仍需干净可验证运行态刷新、正式规则保存、V29 新候选保存/发布生成动态绑定 hash、DCC 产品身份或代码合同、QA 设备关联、任务自有 fixture manifest、真实 Playwright E2E 和最终只读断言。

## 2026-08-11 P7 pressure pump correspondence main retry

- USER INTENT: 用户要求“你帮我先对应”；本轮使用本机 tenant 1/admin 登录态，目标仍为 `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`，只通过正式接口和本机正式迁移修复尝试推进字段对应，不记录口令或 token。
- BDD: 先对应压力泵 PI/LOSS 摘要字段 -> Given route `922119` 最新发布 `V29 / routeVersionId=632` 已有 14/14 MAIN、14/14 PROCESS_INSPECTION、14/14 LOSS_REPORT；When 读取 template 28/25、QA 发布证据并保存 `PQC_AGGREGATE_DETAIL` / `PRODUCTION_LOSS` 摘要字段规则；Then 规则必须通过正式 `batch-record-cell-link` API 落库，不能用 SQL 直接插入规则。
- SELECT/API: `GET /mes/pro/route-version/list-by-route?routeId=922119` -> code 0，latest active `V29 / id=632`。
- SELECT/API: `GET /mes/pro/route/flow-config?routeId=922119&routeVersionId=632&useType=BATCH` -> code 0，`14/14 MAIN`、`14/14 PROCESS_INSPECTION`、`14/14 LOSS_REPORT`。
- SELECT/API: template 28 `V3.0 / FORMTPL:32` -> code 0，PUBLISHED，recognized fields 56；template 25 `V2.0 / FORMTPL:27` -> code 0，PUBLISHED，recognized fields 8。
- SELECT/API: 14 个稳定工序均可按 `productId=902149 + routeId=922119 + stable processId` 读取对应 QA 发布证据；当前 project-status 只返回清洗工序 `regulationId=53 / versionId=54 / G/0` 为当前直接 PUBLISHED 状态，其余 13 条仍是历史 routeVersionId=448 的 PUBLISHED 证据，后续真实 E2E 仍需按 writer 的正式准入复验。
- RED: `GET /mes/pro/batch-record-cell-link/workbench-context?templateId=28&versionNo=V3.0` -> FAIL，后端日志首个数据库异常为 `Unknown column 'aggregation_strategy' in 'field list'`，运行库 schema 落后于当前 Mapper。
- GREEN: runtime schema repair -> PASS，通过本机 Docker MySQL 对 `mes_pro_batch_record_cell_link_rule` 添加正式迁移中已有的 nullable `aggregation_strategy varchar(32)`；复查 `information_schema.COLUMNS` 返回该列。
- GREEN: `GET /mes/pro/batch-record-cell-link/workbench-context?templateId=28&versionNo=V3.0` 和 template 25/V2.0 -> code 0；当前仅证明 schema 漂移已解除。
- RED: `GET /mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL:32` -> FAIL，`1040509089 批记录表单布局 JSON 无效：FORMTPL:32`。
- RED: `GET /mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL:27` -> FAIL，`1040509089 批记录表单布局 JSON 无效：FORMTPL:27`。
- CURRENT RULES: `FORMTPL:32` 仍仅有 1 条 `PRODUCTION_WORK_ORDER/batchCode -> 4:1 生产批号`；`FORMTPL:27` 仍仅有 1 条 `PRODUCTION_WORK_ORDER/batchCode -> 5:3 批号`；未写入 `PQC_AGGREGATE_DETAIL` 或 `PRODUCTION_LOSS` 摘要规则。
- RESULT: 已完成业务对应和 schema 漂移修复；正式字段规则保存仍阻塞于动态 FormCenter `form-cells` 布局解析，未使用 SQL 绕过、未发布新路线版本、未创建 fixture/manifest、未运行真实业务 E2E。

## 2026-08-11 P7 pressure pump correspondence root recheck

- USER INTENT: 用户再次要求“你帮我先对应”；本轮只复核并确认当前对应结果，不从脏主工作区重包重启，不用 SQL 直插规则。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`MesProBatchRecordCellLinkServiceImplTest` 11/11，说明工作区代码已具备动态 `FORMTPL` 解析/规则表达能力。
- API RECHECK: 使用 tenant 1/admin 登录态调用当前 `48081` 正式接口；`GET /mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL:32` -> `1040509089 批记录表单布局 JSON 无效：FORMTPL:32`；`FORMTPL:27` -> `1040509089 批记录表单布局 JSON 无效：FORMTPL:27`。未记录口令、token 或连接串。
- RESULT: “先对应”的业务映射保持确认：压力泵 V29 的 14 个工序 PI 用 template 28 / `FORMTPL:32`，LOSS 用 template 25 / `FORMTPL:27`；实际规则仍未落库，当前卡点是 `48081` 运行包未承载已通过测试的动态 FormCenter 解析能力。
## P7 FormCenter Real Jimu Layout Runtime Regression

- BDD: FormCenter recognized fields remain linkable when the published Jimu schema contains `sheetLayoutJson` but no explicit `cellRules` -> Given published template versions `FORMTPL:32` and `FORMTPL:27` have valid `recognized_schema_json` plus Jimu `sheetLayoutJson` without top-level `cellRules`, When an authorized administrator loads `/mes/pro/batch-record-cell-link/form-cells`, Then the service must expose the recognized non-signature fields as linkable targets instead of returning layout-invalid.
- RED: authenticated `GET /admin-api/mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL:32` and `FORMTPL:27` on `48081` -> FAIL, both returned business code `1040509089`; runtime log pointed to `MesProBatchRecordCellLinkServiceImpl.getFormTemplateCells(...:229)` because the Jimu layout branch produced zero linkable target cells.
- ROOT CAUSE: published FormCenter versions `32` and `27` contain valid `recognized_schema_json` and valid Jimu `sheetLayoutJson`, but their Jimu schema has no top-level `cellRules`. `resolveTemplateLayout` selected the Jimu layout solely because `sheetLayoutJson` existed, so the mapper never projected recognized fields and the final linkable-target invariant failed.
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` with the new regression before the fix -> FAIL, 12 tests / 1 error; `getFormCells_usesRecognizedFieldsWhenJimuLayoutHasNoCellRules` reproduced `1040509089` from `getFormTemplateCells`.
- FIX: `resolveTemplateLayout` now uses Jimu `sheetLayoutJson` only when the Jimu schema includes explicit non-empty `cellRules`; otherwise, a FormCenter template with valid `recognized_schema_json` uses the recognized-field projection. This keeps malformed templates fail-fast and does not use `formBindings` or SQL to infer batch-record sources.
- GREEN: targeted new regression `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_usesRecognizedFieldsWhenJimuLayoutHasNoCellRules" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS, 1 test / 0 failures / 0 errors.
- GREEN: focused service regression `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS, 12 tests / 0 failures / 0 errors.
- RUNTIME CHECK: current `48081` is `backend-runtime-control-20260811-125316.jar`, health `UP`, and its nested MES jar contains the earlier `recognizedProjection` constants; however it was built before this final branch fix. Formal `form-cells` still fails on that running jar until a clean, verifiable runtime refresh is performed.
- BLOCKER: No PI/LOSS summary rules were saved and no V29 route version was published in this executor pass. Per `docs/local-runtime.md`, the main workspace contains unrelated concurrent dirty changes, so I did not rebuild/restart `48081` from the dirty workspace and did not hand-patch the nested Spring Boot jar. Next minimum step is a clean runtime build containing this exact cell-link fix plus already-approved P7 changes, then retry formal `rules/save` for `FORMTPL:32` and `FORMTPL:27`.

## P7 Dynamic Summary And Work-Order Rule Coexistence - 2026-08-11

- BDD: PI 摘要规则与生产批号规则共存 -> Given 一个 `FORM_TEMPLATE_VERSION` scope 同时保存正式 `PRODUCTION_WORK_ORDER/batchCode` 规则和 `PQC_AGGREGATE_DETAIL` 摘要规则；When release writer 生成过程检验动态表单；Then writer 只消费 PQC 摘要规则，批号继续由生产工单预填规则负责，且不得要求或写入重复的 PQC batchCode 映射。
- BDD: LOSS 摘要规则与生产批号规则共存 -> Given template 25 同时保存正式 `PRODUCTION_WORK_ORDER/batchCode` 规则和 `PRODUCTION_LOSS` 摘要规则；When release writer 生成损耗动态表单；Then writer 只消费 LOSS 摘要规则，并仅把产品名称、型号规格、生产数量、损耗与签名备注、批准签名写入对应语义字段。
- BDD: FormCenter 生产工单预填与 release writer 分工 -> Given 一个模板版本同时存在生产工单规则、PQC 摘要规则或 LOSS 摘要规则；When `buildFormTemplateVersionPrefillData` 组装预填数据；Then 只执行 `PRODUCTION_WORK_ORDER` 规则，明确跳过两类 release writer 正式来源，未知 sourceType 仍立即失败。
- RUNTIME GREEN: tenant 1/admin 登录态正式接口 `GET /mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL:32` -> code 0，cells=56，linkable=56；`FORMTPL:27` -> code 0，cells=8，linkable=8；未记录 token、口令或连接串。
- GREEN: `POST /mes/pro/batch-record-cell-link/rules/save` for `FORM_TEMPLATE_VERSION:32 / FORMTPL:32` -> code 0，savedCount=10；保留 `PRODUCTION_WORK_ORDER/batchCode -> 4:1 生产批号`，新增 9 条 `PQC_AGGREGATE_DETAIL/SUMMARY|*` 摘要规则。
- GREEN: `POST /mes/pro/batch-record-cell-link/rules/save` for `FORM_TEMPLATE_VERSION:27 / FORMTPL:27` -> code 0，savedCount=6；保留 `PRODUCTION_WORK_ORDER/batchCode -> 5:3 批号`，新增 5 条 `PRODUCTION_LOSS/SUMMARY|*` 摘要规则。
- API VERIFY: `GET /mes/pro/batch-record-cell-link/workbench-context?templateId=28&versionNo=V3.0` -> total=10/workOrder=1/pqc=9/loss=0；`templateId=25&versionNo=V2.0` -> total=6/workOrder=1/pqc=0/loss=5。
- RED: `mvn.cmd -q -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> FAIL，Tests run 41 / Failures 1；原因是 `MesTeamLeaderActiveOrderReleaseLossReportWriterTest.shouldWriteRouteBoundLossFormTemplateAlongsideWorkOrderBatchCodeRule` 仍断言旧显示格式 `signedAt=2026-08-01 09:00:00`，与当前正式 `LocalDateTime.toString()` 摘要格式不一致。
- FIX: 校准 LossReportWriterTest 的批准签名摘要断言为 `signedAt=2026-08-01T09:00`，仍保留 userId=3001 和 signatureId=1201 断言；未改生产代码或运行库数据。
- GREEN: 同一聚焦 Maven 复跑后目标 surefire 报告通过：`MesProBatchRecordCellLinkServiceImplTest` 19/19，`MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest` 12/12，`MesTeamLeaderActiveOrderReleaseLossReportWriterTest` 10/10；Maven PID 已退出，无残留测试进程。
- REMAINING BLOCKER: 本轮仅完成“先对应”字段规则落库；P7 仍未完成 V29 下一候选保存/发布生成动态绑定 hash、902149/AW.107.02.01.2010 的 DCC provenance/13 道工序 QA 设备补齐、task-owned fixture manifest、真实 Playwright 业务 E2E 与最终只读业务断言。

## P7 Route Project Code And DCC QA Provenance - 2026-08-11

- BDD: 路线项目代码解析 DCC 项目 -> Given 压力泵当前路线版本快照包含物料代码 `ID`，DCC 存在唯一启用 `projectCode=ID` 项目且其 `productMasterId` 不是 MES 产品 `902149`; When 过程检验 reader 解析正式 DCC 身份; Then 按路线物料代码精确命中 DCC `ID`，不得把 `902149` 当 DCC `productMasterId`，也不得猜测 `IDPR`。
- BDD: DCC 项目限定最新发布 QA -> Given 当前产品、路线和稳定工序存在多个 PUBLISHED QA 候选; When reader 按路线已解析 DCC 项目筛选 QA; Then 只接受规程代码中精确项目段 `PQC-ID-*` 的候选并按 `publishedAt/versionId/regulationId` 选择最新版本，`PQC-IDPR-*`、M0 派生或缺显式项目段的候选返回 `PQC_DCC_QA_PROVENANCE_REQUIRED`。
- BDD: QA 来源缺失时在目标解析前阻塞 -> Given 压力泵 14 个稳定工序中只有 1 个最新 PUBLISHED QA 具备 `ID` 项目来源，其余 13 个缺正式 DCC 来源或设备关联; When release writer 执行无副作用 plan; Then 每个缺口按 PQC task/routeProcess 返回精确 blocker，且不解析表单绑定、不写 execution、不写 FormCenter instance。

## P7 Pressure Pump Template Identity Gate - 2026-08-11

- BDD: ID 产品不得写入 IDPR 过程检验模板 -> Given 产品 `902149 / AW.107.02.01.2010` 通过路线项目物料精确对应 DCC `ID / 球囊扩张压力泵`; When 读取路线当前绑定的 template 28 / version 32 / V3.0 正式字段; Then 若模板正文明确标识 `PQC-IDPR-001 / 按压式球囊扩张压力泵`，必须在任何放行业务写入前阻塞，不得把 `ID` 数据写入 `IDPR` 表单。
- API VERIFY: tenant 1/admin 正式登录后只读复核 `FORMTPL:32` -> code 0，56 个可映射字段；其中 `6:1` 的正式标签为 `PQC-IDPR-001（/）按压式球囊扩张压力泵组装过程检验规程`。未记录口令、token 或连接串。
- API VERIFY: 当前已保存 PI 规则仍为 total=10/workOrder=1/pqc=9，其中 `PQC_AGGREGATE_DETAIL.dccProjectCode -> 6:1`、`dccProjectName -> 4:3 型号/规格`；这会把 `ID` 项目身份写入 `IDPR` 静态规程区域，并把 DCC 项目名写入产品规格字段，语义不成立。
- BLOCKER: `902149` 的 DCC 项目身份与 template 28 当前发布内容不一致。P7 不得继续创建 fixture 或执行真实业务 E2E，直到通过正式模板/路线流程选择或发布与 `ID / 球囊扩张压力泵` 一致的过程检验记录模板，并重新保存字段映射与动态绑定快照；禁止以 `IDPR` 模板代替。

## P7 Route DCC And QA Provenance GREEN - 2026-08-11

- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；生产 reader 构造器尚无 `MesProRouteVersionMapper/MesMdItemService`，`InspectionSource` 尚无 `routeProjectCode`，严格证明旧实现仍把 MES productId 当作 DCC productMasterId。
- IMPLEMENTATION: reader 只读取命令指定的已发布 ACTIVE/SUPERSEDED 路线版本快照，要求目标 productId 存在于 `configSnapshots.products`，再以路线物料代码与唯一启用 DCC `projectCode` 精确相交；`ID` 不会命中 `IDPR`。writer 改为校验 `routeProjectCode == dccProject.projectCode`，不再要求 `902149 == DCC productMasterId=11`。
- IMPLEMENTATION: QA provenance 只接受当前 PUBLISHED regulation/version 且 regulation code 精确以 `PQC-<routeProjectCode>-` 开头；`PQC-IDPR-*` 和 `M0-*` 对 `ID` 均返回 `PQC_DCC_QA_PROVENANCE_REQUIRED`，成功证据使用长度前缀 canonical payload 计算 SHA-256。
- BUILD NOTE: 首次 GREEN 复跑被本任务早期隔离 javac 留在 `target/test-classes` 的旧生产 `.class` 遮蔽；只用当前 `target/classes` 对应类覆盖该可再生成任务产物，未清理共享 target、未触碰其他任务产物。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；21 tests / 0 failures / 0 errors / 0 skipped，标准 lifecycle BUILD SUCCESS。
- RUNTIME/DATA: 本轮未重启 48081、未写数据库、未修改路线或 QA。当前正式数据仍会只接受清洗工序 `PQC-ID-001`，其余 13 个 M0/fixture QA 将在写入前精确阻塞。

## P7 Process Inspection Template DCC Identity GREEN - 2026-08-11

- BDD: ID 产品不得写入 IDPR 动态表单实例 -> Given reader 已从正式路线快照解析 `routeProjectCode=ID`，且当前绑定模板可映射字段标签包含 `PQC-IDPR-001`；When writer 解析过程检验动态表单目标；Then 必须返回 `PROCESS_INSPECTION_TEMPLATE_DCC_IDENTITY_REQUIRED`，包含 expected=ID/actual=[IDPR] 定位信息，并且不创建或保存 FormCenter instance。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；测试编译明确指出 `resolveTarget` 尚无 expectedDccProjectCode 参数，旧接口不能验证绑定模板的 DCC 项目身份。
- IMPLEMENTATION: 动态表单端口从正式 recognized field 标签提取受控文件代码 `PQC-<projectCode>-<suffix>`，要求唯一项目代码与 reader 解析的路线 DCC projectCode 精确相等；缺失、多值或 ID/IDPR 不一致均在任何实例写入前阻塞。writer 显式传入 `source.routeProjectCode`，不增加 fallback 或别名匹配。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；24 tests / 0 failures / 0 errors / 0 skipped，四份 Surefire 报告时间为 2026-08-11 20:19:34..36，本任务 Maven 进程已退出。
- RUNTIME/DATA: 本轮未重启 48081、未写数据库、未修改路线/模板/QA。现有 template 28 V3.0 的 `PQC-IDPR-001` 将对压力泵 DCC `ID` 严格阻塞；必须通过正式模板发布与路线候选重绑解决，不能用字段映射覆盖静态受控文件身份。
- BDD: 已发布路线快照损坏必须立即失败 -> Given 过程检验 reader 读取命令指定的已发布路线版本；When `routeSnapshotJson` 非法或 `configSnapshots.products` 结构无效；Then 必须抛出包含 routeVersionId 与原因的 `IllegalStateException`，不得吞异常并伪装成“无 DCC 匹配”，也不得继续读取 PQC task。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest#malformedPublishedRouteSnapshotFailsFastInsteadOfLookingLikeNoDccMatch" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；1 test / 1 failure，旧实现未抛异常。
- IMPLEMENTATION: `parseRouteVersionProductIds` 对 JSON 解析失败、products 类型异常、非对象元素和无效 itemId 统一抛出显式 `IllegalStateException`，不包含快照原文；移除 catch 后返回空集合的静默降级。
- BUILD NOTE: 标准 lifecycle 已成功重编译 2621 个主源码与 435 个测试源码，但 `target/test-classes` 中仍有本任务早期隔离 javac 生成的旧 ReaderImpl class 遮蔽当前 `target/classes`；以当前主编译产物覆盖该任务自有可再生成 class 后执行 Surefire 聚焦回归，未清理共享 target。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" surefire:test` -> PASS；25 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS。
- INDEPENDENT TEST: tester 只更新 `test-report.md`，标准 Maven 同一四类聚焦回归 25/25 PASS；结论为本身份门禁切片可放行，P7 整体仍 BLOCKED / NOT COMPLETE。
- INTEGRATION GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseOrchestrationRedTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；68 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS。覆盖编排、批记录、PI、LOSS、完成性与 source hash 集成回归。

## P7 Frontend Integration Contract CRLF Regression - 2026-08-11

- BDD: Windows CRLF 不得破坏放行申请静态合同 -> Given 工作台 Vue 文件在 Windows 并发编辑后使用 CRLF；When A1 放行申请静态合同提取 blocker 警告块；Then 合同必须先把 CRLF 归一化为 LF，再验证正式 locator/reason/suggestion，不得把换行格式变化误报成页面功能缺失。
- RED: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> FAIL；页面实际仍存在 `releaseApplicationBlockers` 警告块，但 `extractBlock` 的 LF 锚点无法命中 CRLF 源文件，首个失败为 `missing block start`。
- FIX: 仅在该任务静态合同的 UTF-8 读取 helper 中归一化 `\r\n -> \n`；未改页面行为、接口或产品文案。
- GREEN: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS；相邻 `team-leader-workbench-static.spec.cjs` 与 `team-leader-workbench-sfc-style-compile-static.spec.cjs` 同时 PASS。
- GREEN: `pnpm ts:check` -> PASS，exit 0。

## P7/A6 Current Formal Prerequisite Gate - 2026-08-11

- SCOPE: tenant `1`；product `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`；route `922119 / RT000028`。本轮只读取当前 int_main 运行态和 MySQL 正式数据，不使用 SQL/API/UI 写入。
- BDD: A6 在正式来源不完整时必须停在首次业务写前 -> Given P7 要求 14 道工序使用 DCC 项目 `ID` 的最新 PUBLISHED QA、项目身份一致的过程检验模板及完整动态绑定快照；When 对当前 ACTIVE 路线执行实时只读前置门禁；Then 任一身份、QA、设备或 hash 不满足都必须 `STRUCTURED_BLOCKED`，不得创建 fixture、manifest 或启动业务 Playwright。
- RUNTIME READ-ONLY: `http://127.0.0.1:8081` -> HTTP `200`；`http://127.0.0.1:48081/actuator/health` -> `UP`。仅执行 `GET/HEAD`，未登录、未记录凭据。
- ROUTE READ-ONLY: route `922119` 当前仅有一个 ACTIVE 版本，仍为 `632 / V29 / ACTIVE`；其后续不存在更高版本，当前工序 `9908090160..9908090173` 共 `14` 道。
- DCC IDENTITY READ-ONLY: V29 产品快照包含 `902149`，并包含路线项目物料 `924005 / code=ID / 球囊扩张压力泵`；启用 DCC 项目唯一精确命中 `ID / 球囊扩张压力泵`。`IDPR` 是另一启用项目，名称为按压式球囊扩张压力泵，不得替代。
- PROCESS INSPECTION TEMPLATE READ-ONLY: template `28` 最新 PUBLISHED 仍为 `versionId=32 / V3.0`，正式识别内容命中 `PQC-IDPR-001`；与当前产品 DCC `ID` 不一致，过程检验模板身份门禁不通过。
- QA PROVENANCE READ-ONLY: 按 `productId=902149 + routeId=922119 + stable processId` 选出的 14 条最新 PUBLISHED QA 中，仅清洗工序 `9908090162` 命中 `PQC-ID-001-RP980647 / versionId=54 / G/0`；其余 `13/14` 为 `RRM-*`，不具备 `PQC-ID-*` 正式 provenance。
- QA EQUIPMENT READ-ONLY: 仅清洗工序设备关联完整；其余 `13/14` 工序仍有缺口，共缺 `44` 个 required-item equipment 关联，不能进入正式放行资料生成。
- DYNAMIC SNAPSHOT READ-ONLY: V29 `PROCESS_INSPECTION` 绑定 `14` 条、`LOSS_REPORT` 绑定 `14` 条；两类绑定的 `record_category_snapshot_hash` 与 `slot_config_snapshot_hash` 均为 `0/14` 非空。
- STRUCTURED_BLOCKED: 当前同时存在模板 DCC 身份不一致、QA provenance `1/14`、设备完整 `1/14`、动态快照 hash `0/14` 四类正式阻塞；P7-AC1/P7-AC2/P7-AC3 仍未满足，未启动 fixture manifest、真实业务 Playwright 或最终只读业务断言。
- SIDE EFFECT AUDIT: `browserBusinessWrites=0`、`businessApiWrites=0`、`sqlWrites=0`、`manifestCreated=false`；未改 `task-state.json`、`test-report.md`、开发计划、PRD、测试计划或生产代码。
- INDEPENDENT TEST: tester 对同一实时 int_main/正式数据只读门禁复核通过，`test-report.md` 记录 blocker evidence `PASS/RELEASED`；P7 仍 `BLOCKED / NOT COMPLETE`，P7 AC `0/3`，并独立确认 template IDPR、QA `1/14`、设备完整 `1/14`/缺 44、PI/LOSS hash `0/14` 与四类副作用为 0。
- MAIN REVIEW: executor 严格停在首次业务写前，未创建 manifest/fixture/业务 spec；tester 未修改生产代码或主状态。主线程同步 `task-state.json`，保持 P7 blocked/test_status running，不推进阶段。
- FINAL STRUCTURE: `task-state.json` UTF-8 JSON parse PASS；任务范围 `git diff --check` 无 whitespace error，仅现有 LF/CRLF 转换提示；无本任务 Surefire 或 vue-tsc 残留进程。

## 2026-08-13 PRD correction - correspondence only

- USER INTENT: 用户明确“这里只做对应关系，数据生成是生产组长点击申请放行的时候再生成；点击的时候不会出现数量不一致，也不存在复核人的时间逻辑问题”。
- BDD: 对应关系配置不生成数据 -> Given 用户在批记录单元格链接页面为当前工序配置一线生产元素到批记录表单单元格/重复行的对应关系；When 保存配置；Then 系统只保存 source-to-cell 规则，不创建批记录正式数据、field audit、签名证据、FormCenter instance、release dossier 或待办。
- BDD: 申请放行时按提交顺序使用重复行 -> Given 当前工序目标表单配置了由用户选择的重复行组，且一线生产历史已经完成；When 生产组长点击申请放行；Then 后端按提交顺序把每次提交写入下一条未使用重复行，未用到的重复行保持空白，重复行数量来自表单结构和用户配置而非全局固定 4 行。
- DOC UPDATE: `prd.md` 已补齐 Scope、Non-Goals、User Scenarios、Functional Requirements 和 Acceptance Criteria，明确配置阶段只做对应关系，真正数据生成在申请放行事务内发生。
- EXPERIENCE GATE: 已核对既有长期经验 `docs/backend-development.md#批记录单元格链接预填落库边界`，其中已明确配置保存、一线生产事实形成、申请放行资料生成是三个独立阶段；本轮仅把该门禁补入任务文档，不新建长期经验文档。
