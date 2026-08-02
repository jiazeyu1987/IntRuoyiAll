# 岗位需求分解矩阵 M0-M6 测试报告

## Scope

本报告验证 M0 revised gate、M1 activeOrderId authority、M2 production coefficient snapshots、M3 QA/PQC source model、M4 transfer/release source model、M5 日结/范围/路线配置切片，以及 M6 入口状态。2026-08-02 用户已调整 M0 门禁口径：M0 只负责识别、结构化冻结并归属 SOURCE blocker；M1-M5 已 accepted，当前进入 M6 migration/concurrency/performance/full-real-E2E gate。

## Current Result

`IN_PROGRESS / currentMilestone=M6`。M0、M1、M2、M3、M4、M5 已 accepted；M5 route config source gate、daily-close/scope static gate 和 backend target Maven 均已 GREEN；最新授权 `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` 为 `PASS`，0 SOURCE blocker，0 ENV blocker，0 RUNTIME blocker。M6 真实 E2E 已从泛化未实现占位推进为 62 AC coverage ledger：六角色入口/页面阶段已观察，AC-M04 的 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` 四个真实动作已 PASS，PQC `pqcRegulationItemsRendered` 动态规程项目渲染动作已 PASS，`activeOrderCleanupDeferred` 已结构化为 `E2E_CLEANUP` blocker；后端重复加入、并发唯一键和冲突路线前置拒绝均已 GREEN，`actionObserved=7`、`surfaceObserved=34`、`uncovered=21`、`pending=62`、`blockers=63`，但尚无 AC 达到真实动作/失败路径/只读核验 `ACCEPTED`。

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
| M6 real E2E coverage ledger | STRUCTURED_BLOCKED | 真实 E2E 已加载 62 AC，写出 6 个 phase evidence、6 个 action evidence（5 PASS + 1 BLOCKED）和 63 个 blocker；不再使用泛化未实现占位。 |
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
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after cleanup traceability implementation | STRUCTURED_BLOCKED，6 phase evidence / 5 action evidence / 38 surfaceObserved / 21 uncovered / 62 pending / 64 blockers；`activeOrderCleanupDeferred` action BLOCKED，原因是 activeOrderId 仍是后续 M6 共享夹具；`activeOrderUnauthorizedMutationBlocked` 仍为权限夹具 blocker |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with `RRM_UNAUTHORIZED_USERNAME=aoteman` | STRUCTURED_BLOCKED，6 phase evidence / 5 action evidence / 37 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`activeOrderUnauthorizedMutationBlocked` action PASS，`activeOrderCleanupDeferred` 仍为 `E2E_CLEANUP` blocker |
| 2026-08-02 | authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC regulation render slice | STRUCTURED_BLOCKED，6 phase evidence / 6 action evidence / 34 surfaceObserved / 21 uncovered / 62 pending / 63 blockers；`pqcRegulationItemsRendered` action PASS，覆盖 14 个工序、32 个正式 QA 规程项目和发布版本 ID 16..29 |

## Independent Gate Decision

`M0 = ACCEPTED_BY_REVISED_GATE`，`M1 = ACCEPTED`，`M2 = ACCEPTED`，`M3 = ACCEPTED`，`M4 = ACCEPTED`，`M5 = ACCEPTED`，`M6 = IN_PROGRESS`。Excel 全量目标仍需 M6 验收后才能宣称完成。

## M6 Required Coverage

- 迁移与 schema：确认 M1-M5 增量结构、索引、唯一键、历史兼容边界和迁移前置。
- 并发与幂等：确认加入活跃订单、报工分配、PQC 提交/确认、过程检验汇集、放行和批记录回填无重复终态。
- 性能与分页：确认看板、日结、PQC 列表、追溯和放行检查无 N+1、分页总数和索引计划可接受。
- 真实 E2E：用授权六角色账号走完整页面路径，覆盖成功链路、失败路径、权限隔离、签名和清理。
- 当前 blocker：`role-requirement-matrix-real-flow.e2e.js` 已具备 62 AC coverage ledger、6 个入口阶段证据、AC-M04 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` 四个 PASS 动作证据，以及 PQC `pqcRegulationItemsRendered` PASS 动作证据；权限隔离已由专用错误角色 `aoteman` 真实验证通过；清理追溯门禁已加入但因 activeOrderId 仍是后续 M6 共享夹具而记录 `E2E_CLEANUP` blocker。62 个 AC 均尚未达到完整真实动作、失败路径、权限隔离、只读核验和清理闭环 `ACCEPTED`；已记录为结构化 M6 BLOCKED，不作为通过。
- 收尾门禁：更新验证报告、清理任务自有临时产物；本轮仍按用户要求不执行 `git push`。

## Advancement Decision

允许进入 M6。下一步按 BDD + 严格 TDD 执行 migration/concurrency/performance/full-real-E2E gate；本轮不执行 `git push`。
