# 岗位需求分解矩阵 M0-M6 测试报告

## Scope

本报告验证 M0 revised gate、M1 activeOrderId authority、M2 production coefficient snapshots、M3 QA/PQC source model、M4 transfer/release source model、M5 日结/范围/路线配置切片，以及 M6 入口状态。2026-08-02 用户已调整 M0 门禁口径：M0 只负责识别、结构化冻结并归属 SOURCE blocker；M1-M5 已 accepted，当前进入 M6 migration/concurrency/performance/full-real-E2E gate。

## Current Result

`IN_PROGRESS / currentMilestone=M6`。M0、M1、M2、M3、M4、M5 已 accepted；M5 route config source gate、daily-close/scope static gate 和 backend target Maven 均已 GREEN；最新授权 `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` 为 `PASS`，0 SOURCE blocker，0 ENV blocker，0 RUNTIME blocker。M6 真实 E2E 已从泛化未实现占位推进为 62 AC coverage ledger：六角色入口/页面阶段已观察，AC-M04 的 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` 四个真实动作已 PASS，PQC `pqcRegulationItemsRendered` 动态规程项目渲染动作已补强 AC-D17 页面可见 `方法 / 标准 / 判定` 元信息并记录 `visibleMetadataCount=1`，`pqcPieceDetailQuantityPrepared` 逐件数量动作和 `pqcActualEmployeeSelected` 实际检验人动作均已 PASS，AC-D29 `pqcFormalSubmissionCreated` 已接入真实门禁但因 PQC 页面提交按钮禁用保持 `E2E_PQC_SUBMISSION_UI` blocker，AC-D32 `pqcLeaderSubmissionFilterPaginationConsistent` 已接入真实门禁但因缺少正式 PQC submitted 样本保持 `E2E_PQC_SUBMISSION_DATA` blocker，`activeOrderCleanupDeferred` 已结构化为 `E2E_CLEANUP` blocker；并发/性能门禁已独立结构化为 `m6ConcurrencyGateDeferred` 与 `m6PerformanceGateDeferred`。后端重复加入、并发唯一键和冲突路线前置拒绝均已 GREEN，最新 full real E2E 为 `actionEvidence=10`、`gateEvidence=2`、`blockers=67`，但尚无 AC 达到真实动作/失败路径/只读核验 `ACCEPTED`。

2026-08-03 更新：AC-D29 PQC 正式提交生成工序池事件已完成代码、单测、静态合同和 `yudao-server` 可运行 Jar 构建门禁；`yudao-server-exec.jar` SHA256 为 `200527D05A5C9CC2F1E12A303EF9835BDB90E9775BC75B1D3EC94863693D6D25`。用户授权后已停止无关 DCC patched runtime PID `58452`，M6 runtime Jar `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` 已成为 48081 listener PID `28744`，后端 health 为 `UP`，前端 8081 为 HTTP `200`。授权 `real:check` PASS，但 full real E2E 仍 STRUCTURED_BLOCKED，新增 `pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_UI`，原因是 PQC 页面提交按钮仍禁用，因此 AC-D29 / AC-D32 仍不 `ACCEPTED`。

## Tested Evidence

| 检查项 | 结论 | 证据 |
|---|---|---|
| 规划包结构 | PASS | `doc/tasks/20260801-role-requirement-matrix-excel` 中 PRD、开发计划、测试计划形成 62 AC / 62 TC / 16 BDD 映射。 |
| M0 revised gate | PASS_ACCEPTED | 31 个 SOURCE blocker 已结构化冻结并归属到 M1-M5；ENV/RUNTIME blocker 为 0。 |
| M1 activeOrderId authority | PASS_ACCEPTED | RRM-BLK-001..007 已由 schema/service/controller/PQC source switch 测试和授权 `real:check` 验证关闭。 |
| M2 production coefficient snapshots | PASS_ACCEPTED | RRM-BLK-026..028 已由 active order process snapshot、目标数量服务、分配/完成链路和自动排产 fail-fast 测试验证关闭。 |
| M3 QA/PQC source model | PASS_ACCEPTED | RRM-BLK-017..025 已由 QA/PQC schema、PQC task/source submit、逐件明细、前端动态渲染和授权 `real:check` 验证关闭。 |
| M4 transfer/release source model | PASS_ACCEPTED | RRM-BLK-008..016 已由 active order transfer trace schema/service、放行完整性来源适配器、Maven 回归、静态合同和授权 `real:check` 验证关闭。 |
| M5 route config separation | PASS_STATIC_ACCEPTED | RRM-BLK-029..031 已由工艺路线静态合同、eDHR 后端运行态 fail-fast、后端 compile、前端回归和授权 `real:check` 验证关闭。 |
| M5 daily-close/scope static | PASS | 班组长工作台已有可见日结待处理面；scope model 静态覆盖 WORKSTATION / PRODUCTION_LINE / EQUIPMENT / ORDER。 |
| M5 backend target Maven | PASS | `MesTeamLeaderScopeServiceTest` 与 `MesProcessPoolTeamLeaderSchemaTest` 通过，7 tests、0 failures/errors、BUILD SUCCESS。 |
| 当前 M6 gate | IN_PROGRESS | M6 已启动，仍需迁移、并发、性能、真实 E2E、清理和上线验收。 |
| M6 AC-M04 active-order join/conflict/read-only/cleanup | PARTIAL_ACTION_AND_BACKEND_FAILURE_PASS | 真实页面 `joinActiveOrder` 已返回 `activeOrderId=12`；真实页面 `activeOrderConflictRouteRejected` 已证明错误路线 fail-fast 且不新增错误 activeOrder；真实页面 `activeOrderCrossRoleReadOnly` 已证明 PQC 只读读取同一 activeOrderId；`activeOrderCleanupDeferred` 已重新定位同一 activeOrderId 并记录共享夹具清理 blocker；后端重复加入、`DuplicateKeyException` 并发唯一键、冲突路线前置拒绝已由 6 个单测验证，但 AC-M04 仍缺正式清理窗口/可重建夹具和 M6 迁移/性能/上线门禁。 |
| M6 AC-M04 permission isolation | PASS_ACTION | `activeOrderUnauthorizedMutationBlocked` 已使用专用错误角色 `aoteman` 的独立浏览器上下文完成真实后端写入拒绝探测；响应 `code=403`、`message=没有该操作权限`，`E2E_PERMISSION` blocker 已清除。 |
| M6 AC-D17 PQC visible method metadata | PASS_ACTION_NOT_ACCEPTED | `FrontlineFixedTemplatePanel.vue` 已按正式 QA 规程快照在 PQC 项目行可见渲染 `方法 / 标准 / 判定`，真实 E2E `pqcRegulationItemsRendered` 记录 `visibleMetadataCount=1` 和正式样例 method/standard/result；仍缺失败路径、提交/签名/复核和完整 M6 门禁，因此不标记 ACCEPTED。 |
| M6 AC-D27 PQC piece detail quantity | PASS_ACTION | `pqcPieceDetailQuantityPrepared` 已通过真实 PQC 页面打开逐件弹窗并证明 `plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`；该证据只证明逐件数量准备，不代表 AC-D27 完整 ACCEPTED。 |
| M6 PQC actual employee switch | PASS_ACTION | `pqcActualEmployeeSelected` 已通过正式 `/pqc/personnel` 来源和 `/pqc/switch-employee` 路径接入真实 E2E；本机 tenant 1 已写入 task-owned PQC `EMPLOYEE` scope row id 980013，full real E2E 证明 `actualEmployeeId=512` 不默认登录人 `659`。 |
| M6 AC-D32 PQC leader submission filter/pagination | CODE_STATIC_JUNIT_GREEN_DATA_BLOCKED | 提交看板 mapper 已按产品、检验类型、轮次、复核状态和 `pqcTaskId` 精确关联筛选；前端 PQC 组长筛选与结果列已接入；node syntax、preflight static、mapper static、`ts:check`、`ProcessPoolTimelineFilterTest` 和授权 `real:check` 均 PASS。真实 E2E 因缺少至少两笔正式 PQC submitted 事件记录 `E2E_PQC_SUBMISSION_DATA`。 |
| M6 AC-D29 PQC formal submission event | CODE_UNIT_STATIC_BUILD_RUNTIME_GREEN_UI_BLOCKED | `submitPqcInspection` 已在正式提交时调用 `createPqcInspectionEvent` 生成可追溯 PQC event；`MesFrontlinePqcContextServiceTest` 6 tests PASS，真实 E2E 脚本语法、preflight static 和 mapper static PASS，`yudao-server` package PASS，M6 runtime Jar 已加载到 48081 且授权 `real:check` PASS。full real E2E 因 PQC 页面提交按钮禁用记录 `E2E_PQC_SUBMISSION_UI`，尚无真实提交 PASS。 |
| M6 migration preflight static/policy/runtime | PASS_RUNTIME_PREFLIGHT | 新增只读 `20260802_role_requirement_matrix_m6_migration_preflight.sql`、`e2e:role-matrix-migration-preflight:static` 和 `m6-migration-policy-gate.json`；静态合同、14 文件 release policy gate 与授权本地运行库 SQL 预检均已通过，且 `leftover_procedure_count=0`。 |
| M6 concurrency/performance gate ledger | STRUCTURED_BLOCKED | `gateEvidence=2`；`m6ConcurrencyGateDeferred` 覆盖 12 个 CONC AC，`m6PerformanceGateDeferred` 覆盖 4 个 PERF AC，已观察 `AC-D27`，`AC-D32` 因缺少正式 submitted 样本未完成分页证明，均仍结构化为后续 M6 门禁 blocker。 |
| M6 real E2E coverage ledger | STRUCTURED_BLOCKED | 真实 E2E 已加载 62 AC，写出 6 个 phase evidence、10 个 action evidence（7 PASS + 3 BLOCKED）、2 个 gate evidence 和 67 个 blocker；不再使用泛化未实现占位。 |
| 文档结构 | PASS | `task-state.json`、`result.json` 可解析；任务 Markdown 可 UTF-8 读取。 |

## Latest Verification

| 日期 | 命令 | 结论 |
|---|---|---|
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-matrix-route-config-separation:static` | PASS |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-matrix-route-config-separation-static.spec.cjs` | PASS |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` | PASS |
| 2026-08-02 | `mvn -pl yudao-module-mes -am "-DskipTests" compile` from `IntRuoyiBackend` | PASS，reactor BUILD SUCCESS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted ts:check` | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-matrix-daily-close-scope:static` | PASS |
| 2026-08-02 | `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，7 tests，0 failures/errors，BUILD SUCCESS |
| 2026-08-02 | `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` | BLOCKED，失败在既有无关源码编译错误 |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` | PASS，M6 真实前置齐全 |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` initial RED | BLOCKED，初始脚本停在 full-chain 62 AC 泛化未实现占位，后续已由 coverage ledger 结构化 blocker 取代 |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:edhr:release:check` | PASS，15 features / 12 check scripts / 12 syntax files |
| 2026-08-02 | M6 前端静态回归 | PASS，team-leader-workbench、frontline-formal-submit、frontline-team-config、team-leader-report-allocation、preflight、syntax、ts:check |
| 2026-08-02 | M6 后端定向 Maven 回归 before AC-M04 idempotency slice | PASS，58 tests，0 failures/errors，BUILD SUCCESS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after M6 coverage ledger RED/GREEN | PASS |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after M6 coverage ledger | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M6 coverage ledger | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after M6 coverage ledger | STRUCTURED_BLOCKED，6 phase evidence / 40 surfaceObserved / 22 uncovered / 62 pending / 62 blockers |
| 2026-08-02 | `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，5 tests，0 failures/errors，BUILD SUCCESS |
| 2026-08-02 | M6 后端定向 Maven回归 after AC-M04 idempotency slice | PASS，60 tests，0 failures/errors，BUILD SUCCESS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-M04 idempotency slice | STRUCTURED_BLOCKED，6 phase evidence / 1 action evidence / 39 surfaceObserved / 22 uncovered / 62 pending / 62 blockers；`joinActiveOrder` action PASS |
| 2026-08-02 | `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` after AC-M04 conflicting route slice | PASS，6 tests，0 failures/errors，BUILD SUCCESS |
| 2026-08-02 | M6 后端定向 Maven回归 after AC-M04 conflicting route slice | PASS，61 tests，0 failures/errors，BUILD SUCCESS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-M04 real-page conflict RED | FAIL，expected reason: 缺少 `verifyActiveOrderConflictRouteFailure` |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after conflict action implementation | PASS |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after conflict action implementation | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after conflict action implementation | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after conflict action implementation | STRUCTURED_BLOCKED，6 phase evidence / 3 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 62 blockers；`joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly` action PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-M04 permission isolation RED | FAIL，expected reason: 缺少 `verifyActiveOrderUnauthorizedMutationBlocked` |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after permission isolation implementation | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after permission isolation implementation | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after permission isolation implementation | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after permission isolation implementation | STRUCTURED_BLOCKED，6 phase evidence / 4 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderUnauthorizedMutationBlocked` action BLOCKED，原因是 `releaseOwner` 仍有活跃订单维护权限 |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after cleanup traceability implementation | STRUCTURED_BLOCKED，6 phase evidence / 5 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderCleanupDeferred` action BLOCKED，原因是 activeOrderId 仍是后续 M6 共享夹具；`activeOrderUnauthorizedMutationBlocked` 仍为权限夹具 blocker |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with `RRM_UNAUTHORIZED_USERNAME=aoteman` | STRUCTURED_BLOCKED，6 phase evidence / 5 action evidence / 37 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderUnauthorizedMutationBlocked` action PASS，`activeOrderCleanupDeferred` 仍为 `E2E_CLEANUP` blocker |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC regulation render slice | STRUCTURED_BLOCKED，6 phase evidence / 6 action evidence / 34 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`pqcRegulationItemsRendered` action PASS，覆盖 14 个工序、32 个正式 QA 规程项目和发布版本 ID 16..29 |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC actual employee switch gate | STRUCTURED_BLOCKED，6 phase evidence / 7 action evidence / 34 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`pqcActualEmployeeSelected` action PASS，`actualEmployeeId=512` 不默认登录人 `659`，剩余 blocker 为清理/coverage 闭环 |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` M6 concurrency/performance gate RED | FAIL，expected reason：真实 E2E 脚本缺少 `buildM6ConcurrencyPerformanceGateEvidence` |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after concurrency/performance gate | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after concurrency/performance gate | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after concurrency/performance gate | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after concurrency/performance gate | STRUCTURED_BLOCKED，6 phase evidence / 7 action evidence / 2 gate evidence / 33 surfaceObserved / 8 actionObserved / 21 uncovered / 62 pending / 65 blockers；新增 `m6ConcurrencyGateDeferred` 和 `m6PerformanceGateDeferred` |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` | PASS，M6 迁移预检静态合同通过 |
| 2026-08-02 | `run-release-migration-policy-gate.py` on 14-file M6 migration chain | PASS，`m6-migration-policy-gate.json` 写出 `status=passed`、`migrationCount=14` |
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` M6 formal batch-record scope RED | FAIL，expected reason：临时撤掉 MAIN/BATCH_RECORD 过滤后，静态合同拒绝过宽批记录绑定预检口径 |
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` after SQL scope fix | PASS，M6 迁移预检静态合同通过 |
| authorized local DB execution of `20260802_role_requirement_matrix_m6_migration_preflight.sql` | PASS，`leftover_procedure_count=0`，临时 SQL 文件已清理 |
| `run-release-migration-policy-gate.py` on 14-file M6 migration chain after SQL scope fix | PASS，`migrationCount=14`，预检 SQL sha256=`a4b225a7ef96e4281c63b90d344cb0ea1989ce6c9112a1f591a4d453d48f65bc` |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-D27 piece detail RED | FAIL，expected reason：真实 E2E 脚本缺少 `verifyPqcPieceDetailQuantityPrepared` |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after AC-D27 piece detail | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after AC-D27 piece detail | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after AC-D27 piece detail | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D27 piece detail | STRUCTURED_BLOCKED，6 phase evidence / 8 action evidence / 2 gate evidence / 32 surfaceObserved / 9 actionObserved / 21 uncovered / 62 pending / 65 blockers；`pqcPieceDetailQuantityPrepared` PASS，`plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`，`m6PerformanceGateDeferred` observed AC-D27 |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-D32 PQC submission filter RED | FAIL，expected reason：真实 E2E 脚本缺少 `verifyPqcLeaderSubmissionFilterPaginationConsistency` |
| 2026-08-02 | `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` AC-D32 mapper RED | FAIL，expected reason：提交看板 mapper 缺产品字段、`pqcTaskId` 精确关联和 AC-D32 筛选字段 |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after AC-D32 | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after AC-D32 | PASS |
| 2026-08-02 | `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` after AC-D32 | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted ts:check` after AC-D32 | PASS |
| 2026-08-02 | `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` after AC-D32 | BLOCKED，current-task Maven hang in `WinNTFileSystem.delete0` / `IncrementalBuildHelper.beforeRebuildExecution`；未宣称目标 JUnit GREEN |
| 2026-08-03 | `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，2 tests，0 failures/errors，BUILD SUCCESS；关闭此前 D32 target JUnit 前置 blocker |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after AC-D32 | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D32 | STRUCTURED_BLOCKED，6 phase evidence / 9 action evidence / 2 gate evidence / 66 blockers；`pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA`，缺至少两笔正式 PQC submitted 事件 |
| 2026-08-02 | `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` AC-D17 visible metadata RED | FAIL，expected reason：PQC 页面缺少可见 `data-pqc-inspection-meta` 方法/标准/判定元信息 |
| 2026-08-02 | `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` after AC-D17 | PASS |
| 2026-08-02 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after AC-D17 | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after AC-D17 | PASS |
| 2026-08-02 | `pnpm --dir IntRuoyiFronted ts:check` after AC-D17 | PASS |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after AC-D17 | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D17 | STRUCTURED_BLOCKED，6 phase evidence / 9 action evidence / 2 gate evidence / 66 blockers；`pqcRegulationItemsRendered=PASS` 记录 `visibleMetadataCount=1` 和正式 QA 规程样例 method/standard/result，D32 submitted-data、cleanup、coverage、concurrency、performance blocker 仍保留 |
| 2026-08-02 | D17 report-sync JSON parse / node check / QA static / preflight static / scoped diff-check | PASS，任务状态、真实 E2E result、D17 静态合同、预检静态合同和本轮 touched diff 均通过 |
| 2026-08-02 | D17 report-sync `pnpm --dir IntRuoyiFronted ts:check` rerun | BLOCKED_UNRELATED，当前工作区存在无关 DCC `controlled-file/detail/index.vue` controlled-print 类型错误；不作为 PQC/D17 失败，也不宣称本轮全量 ts GREEN |
| 2026-08-03 | `mvn -pl yudao-server -am "-DskipTests" package` from `IntRuoyiBackend` | PASS，reactor BUILD SUCCESS，`yudao-server-exec.jar` SHA256 `200527D05A5C9CC2F1E12A303EF9835BDB90E9775BC75B1D3EC94863693D6D25` |
| 2026-08-03 | M6 runtime reload on `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` | BLOCKED，48081 被无关 DCC patched runtime 自动占用，M6 Jar 日志为 `Web server failed to start. Port 48081 was already in use.`；仅停止任务自有失败启动 PID `5980` / `33980` |
| 2026-08-03 | `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` | PASS |
| 2026-08-03 | `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` | PASS |
| 2026-08-03 | `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` | PASS |
| 2026-08-03 | authorized runtime conflict handling for `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` | PASS，停止无关 DCC patched runtime PID `58452` 后，M6 Jar 成为 48081 listener PID `28744`；backend health `UP`，frontend 8081 HTTP `200` |
| 2026-08-03 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on M6 runtime Jar | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-03 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on M6 runtime Jar | STRUCTURED_BLOCKED，6 phase evidence / 10 action evidence / 2 gate evidence / 67 blockers；`pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_UI`，`pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA` |

## Independent Gate Decision

`M0 = ACCEPTED_BY_REVISED_GATE`，`M1 = ACCEPTED`，`M2 = ACCEPTED`，`M3 = ACCEPTED`，`M4 = ACCEPTED`，`M5 = ACCEPTED`，`M6 = IN_PROGRESS`。Excel 全量目标仍需 M6 验收后才能宣称完成。

## M6 Required Coverage

- 迁移与 schema：确认 M1-M5 增量结构、索引、唯一键、历史兼容边界和迁移前置。
- 并发与幂等：确认加入活跃订单、报工分配、PQC 提交/确认、过程检验汇集、放行和批记录回填无重复终态。
- 性能与分页：确认看板、日结、PQC 列表、追溯和放行检查无 N+1、分页总数和索引计划可接受。
- 真实 E2E：用授权六角色账号走完整页面路径，覆盖成功链路、失败路径、权限隔离、签名和清理。
- 当前 blocker：`role-requirement-matrix-real-flow.e2e.js` 已具备 62 AC coverage ledger、6 个入口阶段证据、AC-M04 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` 四个 PASS 动作证据、PQC `pqcRegulationItemsRendered`（含 AC-D17 页面可见方法/标准/判定元信息）、`pqcPieceDetailQuantityPrepared`、`pqcActualEmployeeSelected` 三个 PASS 动作证据、AC-D29 `pqcFormalSubmissionCreated` 正式提交事件门禁、AC-D32 `pqcLeaderSubmissionFilterPaginationConsistent` 数据 blocker，以及 M6 迁移预检静态/策略/运行库门禁 PASS_RUNTIME_PREFLIGHT；权限隔离已由专用错误角色 `aoteman` 真实验证通过；清理追溯门禁已加入但因 activeOrderId 仍是后续 M6 共享夹具而记录 `E2E_CLEANUP` blocker；AC-D29 后端代码/单测/静态/构建/运行态已 GREEN，但 full real E2E 在真实页面记录 `pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_UI`，原因是 PQC 页面提交按钮仍禁用；并发/性能已显式结构化为 12 个 CONC AC 与 4 个 PERF AC 的 gate blocker，其中 `AC-D27` 已有逐件数量页面观察，`AC-D32` 因缺正式 submitted 样本未完成分页证明。62 个 AC 均尚未达到完整真实动作、失败路径、权限隔离、只读核验和清理闭环 `ACCEPTED`；已记录为结构化 M6 BLOCKED，不作为通过。
- 收尾门禁：更新验证报告、清理任务自有临时产物；本轮仍按用户要求不执行 `git push`。

## Advancement Decision

允许进入 M6。下一步按 BDD + 严格 TDD 执行 migration/concurrency/performance/full-real-E2E gate；本轮不执行 `git push`。
