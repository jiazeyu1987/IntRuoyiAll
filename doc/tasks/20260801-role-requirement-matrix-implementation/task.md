# 岗位需求分解矩阵 M0-M6 实现任务

## Goal

按照规划包 `doc/tasks/20260801-role-requirement-matrix-excel/` 顺序实现源 Excel 的 23 项主需求和 39 项衍生需求，最终完成 62 个 AC，并通过 BDD、严格 TDD、真实 Playwright E2E、权限、并发、迁移、快照、性能和清理验证。

## Scope

- 新实现任务目录独立于规划任务目录；规划目录作为权威输入。本轮因用户明确调整 M0 gate，同步更新规划包中的 M0 Gate 定义。
- 严格按 `M0 -> M1 -> M2 -> M3 -> M4 -> M5 -> M6` 推进。
- 当前 M0 已按用户批准的新口径 accepted：M0 只负责识别、结构化冻结并归属 SOURCE blocker；M1 activeOrderId 权威模型切片、M2 生产系数/计划数量快照切片、M3 QA/PQC 切片、M4 调拨/放行来源切片、M5 日结/范围/路线配置切片已按 RED/GREEN、静态合同、目标 Maven 和 `real:check` 验证准出；当前进入 M6。
- 只有当前里程碑全部 AC 达到 `ACCEPTED`，且对应真实 Playwright E2E 或 E2E 前置检查通过，才允许进入下一里程碑。
- 本次按用户明确要求不执行 `git push`；如后续需要提交，只允许本地提交和本地验证。
- 2026-08-04 用户明确调整本轮收尾门禁：当前切片结束后不再次执行完整真实 E2E，改为保留最新 canonical full real E2E 的 `STRUCTURED_BLOCKED` 证据，并用非 E2E 定向验证、本地提交和本地合入 `int_main` 作为本轮交付动作；该调整不代表 M6 或 62 个 AC 已 accepted。

## Non-Scope

- 不在 M6 完成前宣称 Excel 目标全部完成；M6 只能围绕迁移、并发、性能、真实 E2E、清理和上线验收门禁按 BDD + 严格 TDD 推进。
- 不在缺少正式来源、测试账号、运行服务、数据库、Redis、电子签名或真实样本时用 mock、默认值、API-only、静态合同或截图替代真实 E2E。
- 不引入 fallback、双读、兼容 shim、默认生产系数 `1`、默认订单、默认人员、默认数量、默认合格或占位成功。
- 不使用 `formBindings`、`MAIN` 槽位或 `工序开始` 替代正式逐工序批记录绑定。

## Milestones

- [x] M0：契约、术语、权威来源和 E2E 前置冻结。
- [x] M1：权威活跃订单与增量模型。
- [x] M2：生产事实、系数分配与正式批记录。
- [x] M3：QA 规程与 PQC 闭环。
- [x] M4：调拨、异常、完整性与放行。
- [x] M5：日结、范围、权限、审计与快照。
- [ ] M6：迁移、并发、性能、真实 E2E 与上线验收。

## Expected Verification

- M0 source map 明确 ERP 订单、调拨、发货、补料、退料、批次、QA 规程、生产系数、正式批记录绑定、异常、返工、报废、库存、签名、租户、角色和运行服务的正式来源或 blocker。
- BDD/TDD acceptance validator 对规划包通过。
- Roadmap validator 对规划包通过。
- 当前实现任务 `task-state.json` 可按 UTF-8 解析，M0/M1/M2/M3/M4/M5 accepted、M6 in_progress 和后续验收状态明确。
- 真实 E2E 前置检查覆盖前端、后端、数据库、Redis、浏览器、登录页、角色账号、权限、电子签名和任务数据来源；M0 准出要求 ENV/RUNTIME blocker 为 0，SOURCE blocker 必须全部结构化冻结并归属到 M1-M5。
- 当前任务 Markdown/JSON 均可 UTF-8 读取，`git diff --check` 无 whitespace error。
- 本次不执行 `git push`；如后续需要提交，只允许本地提交。
- 本轮合入前不再次运行完整真实 E2E；保留的当前完成门禁是目标后端 Maven、前端静态预检、真实脚本语法检查、`ts:check`、JSON 解析、branch-runtime guard 和 `git diff --check`。

## Current Status

in_progress

## Current Milestone

M6

## Milestone Boundary Guard

- M0 按 2026-08-02 用户批准的新口径准出：M0 只负责识别并结构化冻结 SOURCE blocker，不要求清零属于 M1-M5 正式实现范围的 SOURCE blocker。
- M1 已按当前 activeOrderId source gate 准出：RRM-BLK-001..007 均由代码、迁移、服务测试和 `real:check` 验证关闭。
- M2 已按 production coefficient snapshots source gate 准出：RRM-BLK-026..028 均由代码、迁移、服务测试、静态合同和 `real:check` 验证关闭。
- M3 已按 QA/PQC source gate 准出：RRM-BLK-017..025 均由 QA 规程/PQC 任务 schema、服务测试、前端动态渲染静态合同、类型检查和 `real:check` 验证关闭。
- M4 已按 transfer/release source gate 准出：RRM-BLK-008..016 均由调拨追溯 schema/service、放行完整性来源适配器、既有放行服务回归和 `real:check` 验证关闭。
- M5 route configuration separation source gate 子项准出：RRM-BLK-029..031 均由工艺路线前端静态合同、eDHR 后端运行态解析器、后端 compile、前端回归和 `real:check` 验证关闭。
- M5 日结/范围静态门禁已 GREEN：班组长工作台提供可见日结待处理面，scope 模型包含工位、生产线、设备和订单范围。
- M5 后端目标 Maven 已 GREEN：`mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，7 tests、0 failures/errors、BUILD SUCCESS。
- M6 当前进行中：只允许处理迁移、并发、性能、全量真实 E2E、任务数据清理和上线/readiness gate；不得把 M6 未验证项提前标记为 ACCEPTED。
- M6 迁移预检门禁已 GREEN：新增只读 `20260802_role_requirement_matrix_m6_migration_preflight.sql`、`e2e:role-matrix-migration-preflight:static` 和 `m6-migration-policy-gate.json`；静态合同、含 AC-M21/AC-D37 新增迁移的 16 文件 release policy gate 与授权本地运行库 SQL 预检均已通过。
- M6 AC-M04 当前已有真实动作、权限、清理和后端失败路径证据：真实页面 `joinActiveOrder` 已返回 `activeOrderId=12`，真实页面 `activeOrderConflictRouteRejected` 已证明错误路线 fail-fast 且不新增错误 activeOrder，真实页面 `activeOrderCrossRoleReadOnly` 已证明 PQC 只读读取同一 activeOrderId，专用错误角色 `aoteman` 已证明活跃订单写入被拒绝，`activeOrderCleanupCompleted=PASS` 已通过真实后端移出接口清理 activeOrderId=12 并回读 active order count=0；后端重复加入、并发唯一键、冲突路线前置拒绝和显式 `ACTIVE -> REMOVED` 条件更新均已 GREEN。AC-M04 仍不得标记为 `ACCEPTED`，因为还缺 AC 级完整失败路径、权限/只读 breadth、清理-readiness 和 62 AC 全量准出。
- M6 PQC 规程动态渲染已有真实动作证据：`pqcRegulationItemsRendered` 已通过 PQC 检验员页面登录态读取同一 activeOrderId 的 14 个路线工序、32 个正式 QA 规程项目、发布版本 ID 16..29 和计划巡检数量；D17 已进一步补强页面可见 `方法/标准/判定` 元信息，真实 E2E 记录 `visibleMetadataCount=1` 和正式 QA 规程快照样例。但 D17/D19/D24/D31 仍缺失败路径、提交/签名/复核和完整验收闭环，不得标记为 `ACCEPTED`。
- M6 QA 规程维护入口已补真实 action 证据并关闭页面证据 blocker：`qaRegulationPublishedVersionReadOnly=PASS` 通过 QA 账号打开真实入口，观察到正式发布版本、产品/路线版本/工序、首检/巡检/末检、逐工序批记录绑定和发布不可变选择器；published-version API 返回 `apiStatus=200`、`apiCode=0`、`publishedVersionId=29`、首检/巡检/末检各 1 条规则和批记录绑定 `大包装工序生产记录`。AC-M09/AC-D15..D23 仍未准出，因为完整草稿发布、缺项失败路径、历史版本和清理/并发/性能/全量覆盖仍未完成。
- M6 AC-D12/AC-D38 日结看板性能证据已补到后端查询计数：`dailyClosePerformanceReadOnly=PASS` 在生产组长真实页面读取 `data-role-matrix-daily-close` 看板，`requestBudget={submissionPageRequests:0, activeOrderListRequests:0, submissionDetailRequests:0}` 证明卡片读取不触发隐藏列表/详情 N+1；目标 JUnit `ProcessPoolTimelineFilterTest#shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary` 进一步证明日结提交摘要只产生 count+page 且 detail 查询为 0，`MesTeamLeaderActiveOrderServiceTest#shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance` 证明活跃订单卡片读取不加载/重建逐工序快照。AC-D12/AC-D38 仍不得标记为 `ACCEPTED`，因为完整失败路径、权限、清理、运行态分页漂移和上线门禁仍未全部闭环。
- M6 AC-D32 时间轴/列表性能证据已补 PARTIAL_GREEN：新增 `20260804_mes_process_pool_timeline_performance_indexes.sql`，生成 `mes_pro_process_pool_event.pqc_task_id` 并补事件、PQC task、最新复核三组索引；mapper 改为 `pqc_task.id = pool_event.pqc_task_id`，避免提交看板分页 JOIN 逐行 `JSON_EXTRACT`。mapper static、preflight static、17-file migration policy gate、授权本地 DB schema 复核和授权 `real:check` 均 PASS；目标 JUnit 已证明第 1/2 页只产生 count+page 查询且 detail 查询为 0；最新 full real E2E 还记录 `pqcLeaderSubmissionFilterPaginationConsistent=PASS`、`total=3`、`firstEventId=39`、`secondEventId=45` 和 `requestBudget={submissionPageRequests:3, submissionDetailRequests:0, activeOrderListRequests:0}`。AC-D32 仍缺完整失败路径、权限/只读隔离、清理和 62 AC 全量准出，不得标记为 `ACCEPTED`。
- M6 AC-D27 逐件明细数量、requestBudget 和后端 bulk-query 证据已补：`pqcPieceDetailQuantityPrepared` 已打开 PQC 逐件弹窗并证明 `plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`，最新 full real E2E 还记录 `requestBudget={pieceDetailRequests:0, processSnapshotRequests:0, pqcPersonnelRequests:0}`；目标 JUnit `MesFrontlinePqcContextServiceTest#shouldPreparePqcPieceDetailContextWithBulkQueriesOnly` 证明逐件明细上下文使用批量路线/任务/规程读取，不再按工序执行 `selectPendingByActiveOrderProcess` 或按件执行 task detail 查询。AC-D27 仍缺提交后只读明细还原、失败路径、签名/复核、清理和上线门禁，不得标记为 `ACCEPTED`。
- M6 并发和性能总门禁均已转为 PASS：性能门禁只有在 AC-D12/AC-D38、AC-D27、AC-D32 的真实 requestBudget/page-total 证据和六项后端查询计数/索引证明全部为 true 时才标记 `m6PerformanceGateVerified=PASS`；AC-M23 放行终态并发切片已补 `selectByIdForUpdate` 锁定重读，目标 release-service JUnit 和完整 release-service 回归 GREEN，full real E2E 现记录 `m6ConcurrencyGateVerified=PASS` 且 `missingConcurrencyAcceptanceIds=[]`。最新 canonical full real E2E 为 `phaseEvidence=6/actionEvidence=20/gateEvidence=2/blockers=62/failedActions=0`，当前 blocker 分类仅为 `E2E_COVERAGE=62`；CONC/PERF AC 仍不能标记为 `ACCEPTED`，因为各 AC 的失败路径、权限/只读、清理-readiness 和全量覆盖仍未完成。
- M6 放行追溯准备 blocker 已关闭：真实页面 prefill 会自动打开“打开或创建 eDHR 批次执行”弹窗，E2E 脚本已先复用该弹窗，避免再次点击底层 `打开/创建` 按钮被 Element Plus overlay 拦截；最新 full real E2E 证明 `edhrReleasePreparedViaBatchExecutionPage=PASS`，创建/打开 `batchExecutionId=900000000926`、`releaseTransactionId=104`，`edhrReleaseTraceabilityReadOnly=PASS`，放行负责人只读页面看到 10 个检查项和 7 个 PRECHECK 事件，放行追溯写请求数为 0。AC-M22/AC-M23 仍不得标记为 `ACCEPTED`，因为还缺完整失败路径、权限隔离、终态闭环、清理和 62 AC 全量准出。
- M6 放行预检 `failure_reason` 字段超长缺陷已关闭：`MesOrderReleaseCompletenessServiceImpl` 对大量 PQC task / 偏差 / 返工 / 报废 / 库存来源 ID 使用“总数 + 示例”正式摘要，`MesOrderReleaseCompletenessServiceTest` 证明 120 个未确认 PQC task 的失败原因保留业务含义且长度 `<= 500`；目标 release 回归 22 tests PASS，新 48098 runtime jar `backend-runtime-control-20260804-124630-rrm-m6-release-precheck-summary-fix.jar` 已加载，最新 full real E2E 不再出现 `Data too long for column 'failure_reason'`，只剩 62 个 `E2E_COVERAGE` blocker。
- M6 活跃订单调拨追溯投影切片已通过目标/静态/类型验证：班组长加入活跃订单表单新增可见 `调拨单ID列表`，前端将校验后的 `transferIds` 随正式页面提交；后端在新增、复用、并发复用和恢复活跃订单路径上调用 `recordTransferTracesForActiveOrder`，从正式 `mes_wm_transfer` / line / detail 读取并投影到 `mes_pro_process_pool_active_order_transfer_trace`，保留幂等键和来源快照，不引入直接 SQL 写入、mock 或默认成功。最新 canonical full real E2E 状态仍按上一轮记录保持 `phaseEvidence=6/actionEvidence=20/gateEvidence=2/blockers=62/failedActions=0`，本轮按用户要求不再重跑完整真实 E2E。
- M6 PQC 实际检验人切换门禁已通过真实动作验证：`pqcActualEmployeeSelected` 已接入正式 `/pqc/personnel` 和 `/pqc/switch-employee` 路径，本机 tenant 1 已写入 task-owned PQC `EMPLOYEE` scope row id 980013；最新 full real E2E 解析 `pqcLeader` reviewerUserId=512 并排除复核人，选择 `actualEmployeeId=659`，记录 `nonLoginCandidateAvailable=false`，避免生成自我复核事件。AC-D25/D31 仍不能标记为 `ACCEPTED`，因为还缺完整失败路径、签名/权限/清理和全量 M6 验收闭环。
- M6 AC-D32 PQC 组长提交看板筛选/分页已有当前真实动作 PASS：提交看板 read-model 已按产品、检验类型、轮次、复核状态和 `pqcTaskId` 精确关联过滤；`m6-pqc-d32-same-filter-local-seed.sql` 已准备同筛选条件下第二个正式 PENDING PQC task；最新 current-state full real E2E 证明 `total=3`，第 1/2 页事件分别为 `39`、`45`，并记录 `requestBudget={submissionPageRequests:3, submissionDetailRequests:0, activeOrderListRequests:0}`。AC-D32 仍不得标记为 `ACCEPTED`，因为还缺失败路径、权限/只读隔离、清理和 62 AC 全量准出。
- M6 AC-D33 PQC 提交详情追溯和权限隔离已有当前真实动作 PASS：latest canonical full real E2E 使用任务自有 PQC 签名 ID `98003520` 生成 `submittedTaskId=33` / `eventId=83`，PQC 组长详情返回同一 task/signature/actual employee、逐件明细和原始 payload；专用错误角色 `aoteman` 调用 `/submission/detail` 返回业务 `403 没有该操作权限`。AC-D33 仍不得标记为 `ACCEPTED`，因为还缺完整失败路径、只读核验、清理、并发/性能和 62 AC 全量准出。
- M6 AC-D29 PQC 正式提交事件链路已有当前 signature-backed 真实动作和后端并发守卫证据：`submitPqcInspection` 已在正式提交时校验 source event 和 active pool 身份、条件式更新 PQC task、插入逐件明细并调用 `createPqcInspectionEvent`；latest canonical full real E2E 通过未占用任务自有签名池选择 `signatureId=98003520`，生成 `submittedTaskId=33`、`eventId=83`，并通过跨页提交看板查找确认正式 PQC event 已落库；独立 worktree 已按 TDD 修复 stale `PENDING` 并发重复提交，目标 Maven 11 tests / 0 failures/errors。AC-D29 仍不得标记为 `ACCEPTED`，因为还缺真实页面失败路径、复核/只读追溯、清理和 62 AC 全量准出。
- M6 AC-D34/D35 PQC 复核失败路径已有目标和当前真实动作证据：`reviewSubmission` 已锁定 event 和 latest review，重复终态抛 `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`；确认人等于实际检验人时抛 `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN` 且不插入 review。目标 Maven 4 tests / 0 failures/errors，相邻 controller/timeline 回归 15 tests / 0 failures/errors。latest canonical full real E2E 已证明 `pqcLeaderDuplicateTerminalReviewBlocked=PASS`（eventId=83 / reviewId=60 / expectedErrorKey=`PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`）和 `pqcLeaderSelfReviewBlocked=PASS`（eventId=47 / reviewerUserId=512 / actualEmployeeId=512 / expectedErrorKey=`PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN` / stillPending=true）。AC-D34/D35 仍不得标记为 `ACCEPTED`，因为还缺完整退回/只读核验、清理、并发/性能和 62 AC 全量准出。
- M6 AC-D30 PQC 退回后修订链已有后端门禁和当前真实页面动作证据：`updateOriginalRecord` 已锁定 event 并要求 latest submission review 为 `REJECTED`，缺失或 `APPROVED` 最新复核均抛 `PRO_PROCESS_POOL_REVISION_REJECTED_REVIEW_REQUIRED`，且不插入 revision/diff。目标 Maven `MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest` 13 tests / 0 failures/errors；相邻 review/timeline/aggregation 回归 27 tests / 0 failures/errors。latest canonical full real E2E 证明 `pqcLeaderRejectedCorrectionChain=PASS`：eventId=84 / reviewId=61 / revisionSignatureId=98003522，读模型显示修改历史。AC-D30 仍不得标记为 `ACCEPTED`，需要继续完成失败路径、清理、并发/性能和 62 AC 全量准出。
- M6 AC-M21/AC-D37 过程检验汇集已有目标、当前真实页面动作和只读核验证据：PQC record 新增 `PENDING/AGGREGATED` 汇集状态、复核 ID 和汇集时间；`APPROVED` PQC review 插入后通过条件式 `PENDING -> AGGREGATED` 更新，`REJECTED` 和 `PRODUCTION_SUBMIT` 复核不汇集，缺记录/已汇集/并发零行更新均 fail-fast。目标 Maven 6 tests / 0 failures/errors，汇集/复核目标 9 tests / 0 failures/errors，PQC event + schema 4 tests / 0 failures/errors，相邻 controller/timeline/PQC/schema 回归合计 25 tests / 0 failures/errors。latest canonical full real E2E 已证明 eventId=83 的 PQC 组长确认返回 reviewId=60，读模型和页面显示 `AGGREGATED`，且 `pqcProcessInspectionAggregationReadOnly=PASS` 证明已确认事件为 `AGGREGATED`、自我复核被拒的 eventId=47 仍为 `PENDING`。AC-M21/AC-D37 仍不得标记为 `ACCEPTED`，因为还缺清理、并发/性能和 62 AC 全量准出。
- `blocker-inventory.md`、`source-map.md`、`verification-report.md` 中出现的 M6 只表示当前待验收范围，不表示 Excel 全量目标已完成。
- 在 M6 未达到 GREEN/REGRESSION/E2E/launch gate 证据前，不得把 62 项 AC 或最终 Excel 目标标记为完成。

## Blockers

- M0 source map 已完成并 accepted：31 个 SOURCE blocker 已结构化冻结到 `blocker-inventory.md`，其中 M1 已验证关闭 7 个、M2 已验证关闭 3 个、M3 已验证关闭 9 个、M4 已验证关闭 9 个、M5 已验证关闭 3 个；当前真实预检无 SOURCE/ENV/RUNTIME blocker。
- M1 已验证关闭：RRM-BLK-001..007 已通过 active order schema/migration/service/controller/PQC source switch 代码和 `real:check` 清零。
- M2 已验证关闭：RRM-BLK-026..028 已通过 active order process snapshot schema、目标数量服务、分配/完成链路和自动排产 fail-fast 代码清零。
- M3 已验证关闭：RRM-BLK-017..025 已通过 QA 规程版本、PQC task、逐件明细、提交来源和前端动态渲染代码与 `real:check` 清零。
- M4 已验证关闭：RRM-BLK-008..016 已通过 activeOrderId 调拨/发货/补退料/批次追溯 schema、放行检验/偏差/返工/报废/库存来源适配器、Maven 回归、静态合同和 `real:check` 清零。
- M5 子项已验证关闭：RRM-BLK-029..031 已通过正式批记录绑定与 `formBindings` / 默认 `MAIN` / `工序开始` 分离代码、静态合同、后端 compile 和 `real:check` 清零。
- M5 日结/范围静态已验证：`pnpm --dir IntRuoyiFronted e2e:role-matrix-daily-close-scope:static` PASS。
- M5 后端目标 Maven 已验证：`mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS，7 tests、0 failures/errors、BUILD SUCCESS。
- M6 当前待办：全量真实 E2E 覆盖、清理-readiness 和上线验收仍未完成；AC-M04 已有五个真实 PASS 动作证据（加入、冲突、跨角色只读、错误角色拒绝、最终清理）且后端显式 remove 修复已 GREEN；AC-D12/AC-D38 已有日结看板只读、requestBudget 0/0/0 和后端 count+page/零 detail 查询计数证据；AC-D27 已有逐件数量、requestBudget 0/0/0 和后端 bulk-query 证据；AC-D32 已有同条件分页、generated-column/index/query-shape、count+page/zero-detail 查询计数和真实列表 requestBudget 证据；性能总门禁已记录 `m6PerformanceGateVerified=PASS`；QA 当前 `qaRegulationPublishedVersionReadOnly=PASS`；PQC 当前 canonical full real E2E 已有 `pqcRegulationItemsRendered`、`pqcPieceDetailQuantityPrepared`、`pqcActualEmployeeSelected`、`pqcFormalSubmissionCreated`、D33/D34/D37 组长动作和 `pqcLeaderRejectedCorrectionChain` PASS 动作证据；放行当前 canonical full real E2E 已有 `edhrReleasePreparedViaBatchExecutionPage=PASS` 和 `edhrReleaseTraceabilityReadOnly=PASS`。M6 迁移预检静态/策略/运行库门禁已 GREEN，AC-M04 后端重复/并发/冲突路线/显式 remove 单测 GREEN，AC-M18 进度超目标并发阻塞与重复回填抑制 proof 已被 canonical `m6ConcurrencyGateVerified` 识别，AC-M23 放行终态锁定重读和重复放行签名阻塞 proof 已 GREEN，AC-D29 duplicate-submit 状态守卫和 concurrent stale `PENDING` 后端守卫目标 Maven GREEN，AC-D30 退回后修订链 latest rejected review 后端门禁与当前真实动作 GREEN，AC-D34 重复终态复核守卫、AC-D35 自我确认阻塞守卫和 AC-M21/AC-D37 过程检验汇集后端状态门禁及事件类型隔离目标 Maven / 相邻 controller/timeline/PQC/schema 回归 GREEN；当前 canonical result 为 `phaseEvidence=6/actionEvidence=20/gateEvidence=2/blockers=62/failedActions=0`，剩余 blocker 均为 62 个 `E2E_COVERAGE`，仍不能宣称 Excel 全量目标完成。
- M6 活跃订单调拨追溯投影当前已关闭目标实现 blocker：`transferIds` 通过真实班组长页面输入进入 active-order add payload，后端从正式调拨单、调拨行、调拨明细投影追溯记录；本轮不重跑完整真实 E2E，只记录目标 Maven、静态预检、语法检查和 `ts:check` 证据。
- M6 runtime ownership blocker 已关闭：当前 48098 listener PID `70004` 是本任务 RRM M6 runtime jar `backend-runtime-control-20260804-124630-rrm-m6-release-precheck-summary-fix.jar`，后端 health 为 `UP`，前端 8098 HTTP `200`，授权 `real:check` 为 PASS，0 SOURCE/ENV/RUNTIME blocker。
- M6 AC-D29 signature-pool 选择门禁已验证且当前 blocker 已关闭：固定签名 ID 复用问题已由真实 E2E 脚本门禁解决，`isPqcSignaturePoolRole` 将 PQC 工序池签名候选限定为 `pqcInspector + pqcExtra*` 任务自有 ID；latest canonical full real E2E 使用 `signatureId=98003520` 完成正式提交，没有 `E2E_PQC_SIGNATURE_POOL` blocker，不放宽后端 `signature_id` 唯一键。
- M6 AC-D30 当前签名池 blocker 已关闭：2026-08-04 latest canonical full real E2E 显示 `pqcFormalSubmissionCreated=PASS`、`pqcLeaderRejectedCorrectionChain=PASS`，正式提交使用 `signatureId=98003520`，修订链使用 `revisionSignatureId=98003522`；基础签名池耗尽记录仅保留为历史 fail-fast 证据。
- 本地 M0 夹具不等于正式来源实现：QC/IPQC 模板和从 `过程检验记录 V3.0` 逆推的临时 QA 模板不是正式 QA 规程版本模型，工单/调拨夹具不是 activeOrderId 关系源。

## M0 Evidence

- `source-map.md`
- `m0-preflight.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`
- `m0-test-data.md`
- `m0-derived-qa-regulation.md`
- `database-schema-evidence.md`
- `blocker-inventory.md`
- `m0-gate-audit.md`

## M1 Evidence

- `backend-api-evidence.md`
- `database-schema-evidence.md`
- `execution-log.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`

## M2 Evidence

- `backend-api-evidence.md`
- `database-schema-evidence.md`
- `execution-log.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`

## M3 Evidence

- `backend-api-evidence.md`
- `database-schema-evidence.md`
- `execution-log.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`

## M4 Evidence

- `backend-api-evidence.md`
- `database-schema-evidence.md`
- `execution-log.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`

## M5 Evidence

- `execution-log.md`
- `test-report.md`
- `verification-report.md`
- `blocker-inventory.md`
- `source-map.md`
- `role-requirement-matrix-real-e2e-evidence.md`

## M6 Evidence

- `backend-api-evidence.md`
- `execution-log.md`
- `test-report.md`
- `verification-report.md`
- `role-requirement-matrix-real-e2e-evidence.md`
- `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`
- `m6-migration-policy-gate.json`
- `m6-pqc-d32-same-filter-local-seed.sql`
- Current AC-D29 duplicate-submit status/concurrent stale `PENDING` backend guards, AC-D30 rejected-correction latest review backend gate plus current real-page rejected correction action, AC-D34 duplicate terminal review backend guard, AC-D35 self-review backend guard, AC-D33 detail traceability/permission isolation, QA published-version page proof, AC-D12/AC-D38 daily-close read-only/requestBudget/backend query-count evidence, AC-D27 piece-detail requestBudget/bulk-query evidence, AC-D32 timeline generated-column/index/query-shape/query-count/requestBudget proof, AC-M04 active-order cleanup await/backend remove proof, active-order transfer trace projection from visible `transferIds`, AC-M23 release terminal locked-reread proof, AC-M22/AC-M23 release preparation/read-only traceability page proof, release precheck `failure_reason` summary fix, and AC-M21/AC-D37 process-inspection aggregation backend status/type-isolation/read-model/page-visibility contracts have target GREEN/PASS evidence after independent worktree verification; latest canonical full real E2E is STRUCTURED_BLOCKED with `phaseEvidence=6` / `actionEvidence=20` / `gateEvidence=2` / `blockers=62` / `failedActions=0`, with `m6ConcurrencyGateVerified=PASS`, `m6PerformanceGateVerified=PASS`, `qaRegulationPublishedVersionReadOnly=PASS`, `activeOrderCleanupCompleted=PASS`, `dailyClosePerformanceReadOnly=PASS` / `requestBudget=0/0/0`, `pqcPieceDetailQuantityPrepared=PASS` / `requestBudget=0/0/0`, `pqcFormalSubmissionCreated=PASS`, `pqcLeaderRejectedCorrectionChain=PASS`, `pqcLeaderSubmissionFilterPaginationConsistent=PASS` / `requestBudget=3/0/0`, and `edhrReleaseTraceabilityReadOnly=PASS` / `checkItemCount=10` / `eventCount=7` / `mutationRequestCount=0`. M6 still remains `in_progress` because full real E2E failure paths, permissions/read-only breadth, cleanup-readiness, and 62 AC acceptance remain open; 2026-08-04 用户明确要求本轮不再额外重跑完整真实 E2E。

## Applicable Gate Summary

- 严格 TDD 状态链：`PLANNED -> BDD_APPROVED -> TEST_ADDED -> RED_VALID -> IMPLEMENTING -> GREEN -> REFACTORED -> REGRESSION_PASS -> E2E_PASS -> ACCEPTED`。
- 缺测试类、缺脚本、No tests、编译失败、依赖缺失、服务未启动、账号缺失或测试数据缺失均只能记录 blocker，不算 RED。
- 用户可见行为必须通过正式登录页、正式菜单和真实页面路径执行 Playwright E2E；API 只能用于最终只读核验或任务数据清理。
- 后续实现任务不执行 Git push，除非用户另行明确要求。
- 2026-08-04 用户明确取消本轮完整真实 E2E 复跑要求；必须记录为范围变更，不得把“未重跑”写成“已通过”，也不得据此把 M6 或 62 个 AC 标记完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；`real:check` 对缺正式来源保持 fail-fast / BLOCKED，不返回默认成功。
- `是否从根因和长期维护角度解决`：是，按规划包先冻结正式来源、source map、真实 E2E 前置和 blocker，再按 AC / TC 逐项进入后续里程碑；M6 仍需完成全量验收门禁。
- `是否存在临时补丁或绕过`：否；当前 `m0-derived-qa-regulation.md`、本机工单/调拨/签名数据、`m6-pqc-employee-scope-local-seed.sql` 和 `m6-pqc-d32-same-filter-local-seed.sql` 仅是本机真实 E2E 夹具，明确不替代 activeOrderId、QA 规程版本、PQC 任务或 ERP 关系正式模型。
