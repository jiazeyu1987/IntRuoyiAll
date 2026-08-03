# 岗位需求分解矩阵实现任务验证报告

## Scope

验证当前实现任务是否按规划包和用户批准的 revised M0 gate 推进。M0 负责 SOURCE blocker 识别、结构化冻结和归属；M1、M2、M3、M4、M5 已完成各自正式来源、日结/范围和路线配置切片；当前进入 M6 迁移、并发、性能、真实 E2E 与上线验收门禁。

## Current Result

`IN_PROGRESS / currentMilestone=M6`。M0-M5 已 accepted；M5 static gates、backend target Maven 和最新授权 `real:check` 均通过，0 SOURCE / 0 ENV / 0 RUNTIME。M6 真实 E2E 已补齐 62 AC coverage ledger、六角色入口阶段证据、AC-M04 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` PASS 动作证据，以及 PQC `pqcRegulationItemsRendered`、`pqcPieceDetailQuantityPrepared`、`pqcActualEmployeeSelected` PASS 动作证据；其中 AC-D17 已补强 PQC 页面可见 `方法 / 标准 / 判定` 元信息并在真实 E2E 记录 `visibleMetadataCount=1`。AC-D29 `pqcFormalSubmissionCreated` 已接入真实页面提交门禁，但因 PQC 页面提交按钮仍禁用保持 `E2E_PQC_SUBMISSION_UI` blocker；AC-D32 `pqcLeaderSubmissionFilterPaginationConsistent` 已接入 PQC 组长真实页面和只读分页核验，但因缺少正式 PQC submitted 样本保持 `E2E_PQC_SUBMISSION_DATA` blocker；权限隔离动作已由专用错误角色 `aoteman` 独立登录态真实验证通过，PQC 逐件数量动作证明 `plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`，PQC 实际检验人切换动作已通过 task-owned PQC `EMPLOYEE` scope 夹具证明 `actualEmployeeId=512` 不默认登录人 `659`，清理追溯动作 `activeOrderCleanupDeferred` 仍结构化为 `E2E_CLEANUP` blocker。M6 还新增 `m6ConcurrencyGateDeferred` 与 `m6PerformanceGateDeferred` 两个 gate evidence，把 12 个 CONC AC 和 4 个 PERF AC 明确归入后续门禁。后端已覆盖 AC-M04 重复加入、并发唯一键和冲突路线前置拒绝，提交看板 read-model 已补齐 AC-D32 筛选分页字段和精确 `pqcTaskId` 关联，但仍需完成每个 AC 的完整真实页面失败路径、权限隔离、只读核验、迁移、并发、性能、清理和上线验收。

2026-08-03 更新：AC-D29 PQC 正式提交生成工序池事件链路已完成代码、单测、静态合同和 `yudao-server` package；`yudao-server-exec.jar` SHA256 为 `200527D05A5C9CC2F1E12A303EF9835BDB90E9775BC75B1D3EC94863693D6D25`。用户授权后已停止无关 DCC patched runtime PID `58452`，M6 runtime Jar `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` 已成为 48081 listener PID `28744`，后端 health 为 `UP`，前端 8081 为 HTTP `200`。授权 `real:check` PASS，但 full real E2E 仍 STRUCTURED_BLOCKED，新增 `pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_UI`，原因是 PQC 页面提交按钮仍禁用，M6 仍为 in_progress。

## Verification Evidence

| 检查 | 结果 |
|---|---|
| BDD/TDD acceptance validator | PASS |
| Roadmap node development plan validator | PASS |
| M0 source map | PASS_FROZEN：31 个 SOURCE blocker 已归属 M1-M5 |
| M1 activeOrderId authority | PASS_ACCEPTED：RRM-BLK-001..007 已验证关闭 |
| M2 production coefficient snapshots | PASS_ACCEPTED：RRM-BLK-026..028 已验证关闭 |
| M3 QA/PQC source gate | PASS_ACCEPTED：RRM-BLK-017..025 已验证关闭 |
| M4 transfer/release source gate | PASS_ACCEPTED：RRM-BLK-008..016 已验证关闭 |
| M5 route config source gate | PASS_STATIC_ACCEPTED：RRM-BLK-029..031 已验证关闭 |
| M5 daily-close/scope gate | PASS_STATIC：日结可见面、生产线/设备/订单 scope、路线分离静态合同均通过 |
| Frontend/static regression | PASS：daily-close/scope static、route config separation static、ts:check、real-flow syntax、preflight static 均通过 |
| Backend regression | PASS：M5 目标 Maven 通过，7 tests、0 failures/errors、BUILD SUCCESS；M6 planned Maven regression after AC-M04 conflict-route slice 通过，61 tests、0 failures/errors、BUILD SUCCESS |
| Runtime preflight | PASS_CURRENT：最新 `result.json` 无 SOURCE/ENV/RUNTIME blocker |
| M6 AC-M04 active-order join/conflict/read-only/cleanup | PARTIAL_ACTION_AND_BACKEND_FAILURE_PASS：`joinActiveOrder` action evidence PASS，`activeOrderId=12`；`activeOrderConflictRouteRejected` action evidence PASS，错误路线被真实页面 fail-fast 拒绝且未新增错误 activeOrder；`activeOrderCrossRoleReadOnly` action evidence PASS，PQC 只读读取同一 activeOrderId；`activeOrderCleanupDeferred` 已重新定位同一 activeOrderId 后记录共享夹具清理 blocker；后端重复加入、并发唯一键和冲突路线前置拒绝 GREEN；仍缺正式清理窗口/可重建夹具和 M6 迁移/性能/上线门禁 |
| M6 AC-M04 permission isolation | PASS_ACTION：`activeOrderUnauthorizedMutationBlocked` action evidence 已使用专用错误角色 `aoteman` 独立浏览器上下文完成；后端写入接口返回 `code=403`、`message=没有该操作权限`，`E2E_PERMISSION` blocker 已清除 |
| M6 AC-D17 PQC visible method metadata | PASS_ACTION_NOT_ACCEPTED：PQC 页面现在可见展示正式 QA 规程项目的 `方法 / 标准 / 判定` 元信息，真实 E2E `pqcRegulationItemsRendered` 记录 `visibleMetadataCount=1` 和正式样例 method/standard/result；仍缺失败路径、提交/签名/复核和完整 M6 门禁 |
| M6 AC-D27 PQC piece detail quantity | PASS_ACTION：`pqcPieceDetailQuantityPrepared` action evidence 已通过真实 PQC 页面打开逐件弹窗；`plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`，但仍缺提交后明细只读还原、失败路径、签名/复核和完整性能证明 |
| M6 PQC actual employee switch | PASS_ACTION：`pqcActualEmployeeSelected` 已接入正式 PQC 人员来源和 switch-employee 路径；本机 tenant 1 task-owned PQC `EMPLOYEE` scope row id 980013 已验证，full real E2E 证明 `actualEmployeeId=512` 不默认登录人 `659` |
| M6 AC-D32 PQC leader submission filter/pagination | CODE_STATIC_JUNIT_GREEN_DATA_BLOCKED：提交看板 mapper / VO / frontend / E2E 已接入订单、产品、工序、检验类型、轮次、人员、日期和复核状态筛选，mapper static、preflight static、syntax、`ts:check`、`ProcessPoolTimelineFilterTest`、授权 `real:check` PASS；真实 E2E 因缺至少两笔正式 PQC submitted 事件保持 `E2E_PQC_SUBMISSION_DATA` |
| M6 AC-D29 PQC formal submission event | CODE_UNIT_STATIC_BUILD_RUNTIME_GREEN_UI_BLOCKED：`submitPqcInspection` 已校验 source event/pool identity、保存逐件明细并调用 `createPqcInspectionEvent`；`MesFrontlinePqcContextServiceTest` 6 tests PASS，真实 E2E 语法/preflight static/mapper static PASS，`yudao-server` package PASS，M6 runtime Jar 已加载到 48081 且授权 `real:check` PASS；full real E2E 因 PQC 页面提交按钮禁用记录 `E2E_PQC_SUBMISSION_UI`，尚无真实提交 PASS |
| M6 migration preflight static/policy/runtime | PASS_RUNTIME_PREFLIGHT：只读 SQL 预检、静态合同、14 文件 release policy gate 和授权本地运行库 SQL 执行均已通过；预检现在只按 MAIN/BATCH_RECORD 正式批记录口径检查空报表 ID，不误判 INTERNAL_RECORD 表单槽位 |
| M6 concurrency/performance gate ledger | STRUCTURED_BLOCKED：2 gate evidence；`m6ConcurrencyGateDeferred` 覆盖 12 个 CONC AC，`m6PerformanceGateDeferred` 覆盖 4 个 PERF AC，已观察 `AC-D27`，`AC-D32` 因缺正式 submitted 样本未完成分页性能证明 |
| M6 real E2E coverage ledger | STRUCTURED_BLOCKED：6 phase evidence，10 action evidence（7 PASS + 3 BLOCKED），2 gate evidence，pqcPieceDetailQuantityPrepared、pqcActualEmployeeSelected 已 PASS，pqcFormalSubmissionCreated 和 pqcLeaderSubmissionFilterPaginationConsistent 已结构化 blocker，剩余 67 cleanup/ui/data/gate/coverage blockers |

## Latest Local Verification

| 命令 | 结果 |
|---|---|
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-route-config-separation:static` | PASS |
| `node --check IntRuoyiFronted\tests\e2e\role-matrix-route-config-separation-static.spec.cjs` | PASS |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` | PASS |
| `mvn -pl yudao-module-mes -am "-DskipTests" compile` from `IntRuoyiBackend` | PASS，reactor BUILD SUCCESS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` | PASS |
| `pnpm --dir IntRuoyiFronted ts:check` | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-daily-close-scope:static` | PASS |
| `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，7 tests，0 failures/errors，BUILD SUCCESS |
| `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` | BLOCKED：既有无关源码编译错误，未进入目标测试 GREEN |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` | PASS，M6 真实前置齐全 |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` initial RED | BLOCKED，初始脚本停在 full-chain 62 AC 泛化未实现占位，后续已由 coverage ledger 结构化 blocker 取代 |
| `pnpm --dir IntRuoyiFronted e2e:edhr:release:check` | PASS，15 features / 12 check scripts / 12 syntax files |
| M6 前端静态回归 | PASS，team-leader-workbench、frontline-formal-submit、frontline-team-config、team-leader-report-allocation、preflight、syntax、ts:check |
| M6 后端定向 Maven 回归 before AC-M04 idempotency slice | PASS，58 tests，0 failures/errors，BUILD SUCCESS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after M6 coverage ledger RED/GREEN | PASS |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after M6 coverage ledger | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M6 coverage ledger | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after M6 coverage ledger | STRUCTURED_BLOCKED，6 phase evidence / 40 surfaceObserved / 22 uncovered / 62 pending / 62 blockers |
| `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，5 tests，0 failures/errors，BUILD SUCCESS |
| M6 后端定向 Maven 回归 after AC-M04 idempotency slice | PASS，60 tests，0 failures/errors，BUILD SUCCESS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-M04 idempotency slice | STRUCTURED_BLOCKED，6 phase evidence / 1 action evidence / 39 surfaceObserved / 22 uncovered / 62 pending / 62 blockers；`joinActiveOrder` action PASS |
| `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` after AC-M04 conflicting route slice | PASS，6 tests，0 failures/errors，BUILD SUCCESS |
| M6 后端定向 Maven 回归 after AC-M04 conflicting route slice | PASS，61 tests，0 failures/errors，BUILD SUCCESS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-M04 real-page conflict RED | FAIL，expected reason：缺少 `verifyActiveOrderConflictRouteFailure` |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after conflict action implementation | PASS |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after conflict action implementation | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after conflict action implementation | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after conflict action implementation | STRUCTURED_BLOCKED，6 phase evidence / 3 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 62 blockers；`joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly` action PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-M04 permission isolation RED | FAIL，expected reason：缺少 `verifyActiveOrderUnauthorizedMutationBlocked` |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after permission isolation implementation | PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after permission isolation implementation | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after permission isolation implementation | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after permission isolation implementation | STRUCTURED_BLOCKED，6 phase evidence / 4 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderUnauthorizedMutationBlocked` action BLOCKED，原因是 `releaseOwner` 仍有活跃订单维护权限 |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after cleanup traceability implementation | STRUCTURED_BLOCKED，6 phase evidence / 5 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderCleanupDeferred` action BLOCKED，原因是 activeOrderId 仍是后续 M6 共享夹具；`activeOrderUnauthorizedMutationBlocked` 仍为权限夹具 blocker |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with `RRM_UNAUTHORIZED_USERNAME=aoteman` | STRUCTURED_BLOCKED，6 phase evidence / 5 action evidence / 37 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderUnauthorizedMutationBlocked` action PASS，`activeOrderCleanupDeferred` 仍为 `E2E_CLEANUP` blocker |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC regulation render slice | STRUCTURED_BLOCKED，6 phase evidence / 6 action evidence / 34 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`pqcRegulationItemsRendered` action PASS，覆盖 14 个工序、32 个正式 QA 规程项目和发布版本 ID 16..29 |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC actual employee switch gate | STRUCTURED_BLOCKED，6 phase evidence / 7 action evidence / 34 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`pqcActualEmployeeSelected` action PASS，`actualEmployeeId=512` 不默认登录人 `659`，剩余 blocker 为清理/coverage 闭环 |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` M6 concurrency/performance gate RED | FAIL，expected reason：真实 E2E 脚本缺少 `buildM6ConcurrencyPerformanceGateEvidence` |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after concurrency/performance gate | PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after concurrency/performance gate | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after concurrency/performance gate | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after concurrency/performance gate | STRUCTURED_BLOCKED，6 phase evidence / 7 action evidence / 2 gate evidence / 33 surfaceObserved / 8 actionObserved / 21 uncovered / 62 pending / 65 blockers；新增 `m6ConcurrencyGateDeferred` 和 `m6PerformanceGateDeferred` |
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` | PASS，M6 迁移预检静态合同通过 |
| `run-release-migration-policy-gate.py` on 14-file M6 migration chain | PASS，`m6-migration-policy-gate.json` 写出 `status=passed`、`migrationCount=14` |
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` M6 formal batch-record scope RED | FAIL，expected reason：临时撤掉 MAIN/BATCH_RECORD 过滤后，静态合同拒绝过宽批记录绑定预检口径 |
| `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` after SQL scope fix | PASS，M6 迁移预检静态合同通过 |
| authorized local DB execution of `20260802_role_requirement_matrix_m6_migration_preflight.sql` | PASS，`leftover_procedure_count=0`，临时 SQL 文件已清理 |
| `run-release-migration-policy-gate.py` on 14-file M6 migration chain after SQL scope fix | PASS，`migrationCount=14`，预检 SQL sha256=`a4b225a7ef96e4281c63b90d344cb0ea1989ce6c9112a1f591a4d453d48f65bc` |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-D27 piece detail RED | FAIL，expected reason：真实 E2E 脚本缺少 `verifyPqcPieceDetailQuantityPrepared` |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after AC-D27 piece detail | PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after AC-D27 piece detail | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after AC-D27 piece detail | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D27 piece detail | STRUCTURED_BLOCKED，6 phase evidence / 8 action evidence / 2 gate evidence / 32 surfaceObserved / 9 actionObserved / 21 uncovered / 62 pending / 65 blockers；`pqcPieceDetailQuantityPrepared` PASS，`plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`，`m6PerformanceGateDeferred` observed AC-D27 |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` AC-D32 PQC submission filter RED | FAIL，expected reason：真实 E2E 脚本缺少 `verifyPqcLeaderSubmissionFilterPaginationConsistency` |
| `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` AC-D32 mapper RED | FAIL，expected reason：提交看板 mapper 缺产品字段、`pqcTaskId` 精确关联和 AC-D32 筛选字段 |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after AC-D32 | PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after AC-D32 | PASS |
| `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` after AC-D32 | PASS |
| `pnpm --dir IntRuoyiFronted ts:check` after AC-D32 | PASS |
| `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` after AC-D32 | BLOCKED：current-task Maven hang in `WinNTFileSystem.delete0` / `IncrementalBuildHelper.beforeRebuildExecution`，未宣称目标 JUnit GREEN |
| `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | PASS，2 tests，0 failures/errors，BUILD SUCCESS；关闭此前 D32 target JUnit 前置 blocker |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after AC-D32 | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D32 | STRUCTURED_BLOCKED，6 phase evidence / 9 action evidence / 2 gate evidence / 66 blockers；`pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA`，缺至少两笔正式 PQC submitted 事件 |
| `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` AC-D17 visible metadata RED | FAIL：PQC 页面缺少可见 `data-pqc-inspection-meta` 方法/标准/判定元信息 |
| `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` after AC-D17 | PASS |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` after AC-D17 | PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` after AC-D17 | PASS |
| `pnpm --dir IntRuoyiFronted ts:check` after AC-D17 | PASS |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after AC-D17 | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D17 | STRUCTURED_BLOCKED，6 phase evidence / 9 action evidence / 2 gate evidence / 66 blockers；`pqcRegulationItemsRendered=PASS` 记录 `visibleMetadataCount=1` 和正式 QA 规程样例 method/standard/result，D32 submitted-data、cleanup、coverage、concurrency、performance blocker 仍保留 |
| D17 report-sync JSON parse / node check / QA static / preflight static / scoped diff-check | PASS：任务状态、真实 E2E result、D17 静态合同、预检静态合同和本轮 touched diff 均通过 |
| D17 report-sync `pnpm --dir IntRuoyiFronted ts:check` rerun | BLOCKED_UNRELATED：当前工作区存在无关 DCC `controlled-file/detail/index.vue` controlled-print 类型错误；不作为 PQC/D17 失败，也不宣称本轮全量 ts GREEN |
| `mvn -pl yudao-server -am "-DskipTests" package` from `IntRuoyiBackend` | PASS，reactor BUILD SUCCESS，`yudao-server-exec.jar` SHA256 `200527D05A5C9CC2F1E12A303EF9835BDB90E9775BC75B1D3EC94863693D6D25` |
| M6 runtime reload on `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` | BLOCKED，48081 被无关 DCC patched runtime 自动占用；M6 Jar 日志为 `Web server failed to start. Port 48081 was already in use.`；仅停止任务自有失败启动 PID `5980` / `33980` |
| `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` | PASS |
| `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` | PASS |
| `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` | PASS |
| authorized runtime conflict handling for `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` | PASS，停止无关 DCC patched runtime PID `58452` 后，M6 Jar 成为 48081 listener PID `28744`；backend health `UP`，frontend 8081 HTTP `200` |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on M6 runtime Jar | PASS，0 SOURCE / 0 ENV / 0 RUNTIME |
| authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on M6 runtime Jar | STRUCTURED_BLOCKED，6 phase evidence / 10 action evidence / 2 gate evidence / 67 blockers；`pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_UI`，`pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA` |

## Resolved M5 Detail

- RRM-BLK-029：`normalizeRecordBindingSlotTypeDefaultMain` 已关闭；前端缺槽位不再默认 `MAIN`。
- RRM-BLK-030：`batchRecordFormNamesFormBindingsSeparation` 已关闭；静态合同和真实检查证明正式批记录表单与 `formBindings` 互不替代。
- RRM-BLK-031：`edhrRuntimeDefaultMainSlot` 已关闭；eDHR 运行态缺失或非法 `formSlotType` 时 fail-fast，不默认 `MAIN`。
- 日结可见面：班组长工作台新增 `data-role-matrix-daily-close`，按真实提交列表、活跃订单和加载错误展示待处理状态。
- 范围模型：`MesProcessPoolTeamLeaderScopeDO` / `MesTeamLeaderScopeService` 增加 `PRODUCTION_LINE`、`EQUIPMENT`、`ORDER` scope；对应 SQL 字段和增量迁移已补齐。

## Remaining M6 Detail

- M5 后端目标 Maven verification blocker 已关闭。
- 迁移、并发、性能、全量真实 E2E 和收尾/上线验收仍在 M6 中推进。
- 当前 M6 blocker：`role-requirement-matrix-real-flow.e2e.js` 已移除泛化未实现占位，改为从测试计划加载 62 AC 并输出逐项 `E2E_COVERAGE` blocker；当前已完成六角色入口/页面阶段观察、AC-M04 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` 真实动作观察、PQC `pqcRegulationItemsRendered` 正式规程项目渲染（含 AC-D17 页面可见方法/标准/判定元信息）、`pqcPieceDetailQuantityPrepared` 逐件数量、`pqcActualEmployeeSelected` 正式人员来源 PASS 动作证据、AC-D29 `pqcFormalSubmissionCreated` 正式提交事件门禁、AC-D32 `pqcLeaderSubmissionFilterPaginationConsistent` 数据 blocker、M6 迁移预检静态/策略/运行库门禁 PASS_RUNTIME_PREFLIGHT，以及 AC-M04 后端重复/并发/冲突路线失败路径。权限隔离已由专用错误角色 `aoteman` 真实验证通过；清理追溯脚本已能重新定位 activeOrderId，但当前 activeOrderId 仍是后续 M6 共享夹具，需要正式清理窗口或可重建夹具后才能清理；AC-D29 后端代码/单测/静态/构建/运行态已 GREEN，但 full real E2E 在真实页面记录 `pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_UI`，原因是 PQC 页面提交按钮仍禁用；并发/性能已显式结构化为 12 个 CONC AC 与 4 个 PERF AC 的 gate blocker，其中 `AC-D27` 已有逐件数量页面观察，`AC-D32` 因缺正式 submitted 样本未完成分页证明；尚未完成 62 AC 的完整真实页面失败路径、权限隔离、并发/性能证据和清理闭环。
- 62 项 AC 不能在 M5/M6 验收前标记为全部完成。
- 本轮按用户要求不执行 `git push`；后续如需提交，也只能本地提交并保留 no-push 记录，除非用户另行授权。

## Decision

M5 已准出，当前进入 M6。继续按 BDD + 严格 TDD 推进迁移、并发、性能、真实 E2E、清理和上线验收；禁止默认值、fallback、mock、API-only 或临时夹具替代验收。本轮不执行 `git push`。
