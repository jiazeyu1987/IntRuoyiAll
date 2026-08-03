# 岗位需求分解矩阵 M0-M6 实现任务

## Goal

按照规划包 `doc/tasks/20260801-role-requirement-matrix-excel/` 顺序实现源 Excel 的 23 项主需求和 39 项衍生需求，最终完成 62 个 AC，并通过 BDD、严格 TDD、真实 Playwright E2E、权限、并发、迁移、快照、性能和清理验证。

## Scope

- 新实现任务目录独立于规划任务目录；规划目录作为权威输入。本轮因用户明确调整 M0 gate，同步更新规划包中的 M0 Gate 定义。
- 严格按 `M0 -> M1 -> M2 -> M3 -> M4 -> M5 -> M6` 推进。
- 当前 M0 已按用户批准的新口径 accepted：M0 只负责识别、结构化冻结并归属 SOURCE blocker；M1 activeOrderId 权威模型切片、M2 生产系数/计划数量快照切片、M3 QA/PQC 切片、M4 调拨/放行来源切片、M5 日结/范围/路线配置切片已按 RED/GREEN、静态合同、目标 Maven 和 `real:check` 验证准出；当前进入 M6。
- 只有当前里程碑全部 AC 达到 `ACCEPTED`，且对应真实 Playwright E2E 或 E2E 前置检查通过，才允许进入下一里程碑。
- 本次按用户明确要求不执行 `git push`；如后续需要提交，只允许本地提交和本地验证。

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
- M6 迁移预检门禁已 GREEN：新增只读 `20260802_role_requirement_matrix_m6_migration_preflight.sql`、`e2e:role-matrix-migration-preflight:static` 和 `m6-migration-policy-gate.json`；静态合同、14 文件 release policy gate 与授权本地运行库 SQL 预检均已通过。
- M6 AC-M04 当前只达到部分动作和后端失败路径证据：真实页面 `joinActiveOrder` 已返回 `activeOrderId=12`，真实页面 `activeOrderConflictRouteRejected` 已证明错误路线 fail-fast 且不新增错误 activeOrder，真实页面 `activeOrderCrossRoleReadOnly` 已证明 PQC 只读读取同一 activeOrderId，专用错误角色 `aoteman` 已证明活跃订单写入被拒绝，清理追溯门禁已结构化为 `activeOrderCleanupDeferred`，后端重复加入、并发唯一键和冲突路线前置拒绝单测已 GREEN；但 activeOrderId 仍是后续 M6 共享夹具，AC-M04 仍缺正式清理窗口/可重建夹具和 M6 迁移/性能/上线门禁，不得标记为 `ACCEPTED`。
- M6 PQC 规程动态渲染已有真实动作证据：`pqcRegulationItemsRendered` 已通过 PQC 检验员页面登录态读取同一 activeOrderId 的 14 个路线工序、32 个正式 QA 规程项目、发布版本 ID 16..29 和计划巡检数量；D17 已进一步补强页面可见 `方法/标准/判定` 元信息，真实 E2E 记录 `visibleMetadataCount=1` 和正式 QA 规程快照样例。但 D17/D19/D24/D31 仍缺失败路径、提交/签名/复核和完整验收闭环，不得标记为 `ACCEPTED`。
- M6 AC-D27 逐件明细数量已有真实页面只读动作证据：`pqcPieceDetailQuantityPrepared` 已打开 PQC 逐件弹窗并证明 `plannedQuantities=[15]`、`uiQuantity=15`、`pieceRowCount=15`；但 AC-D27 仍缺提交后只读明细还原、失败路径、签名/复核、完整 N+1/查询计数和清理/上线门禁，不得标记为 `ACCEPTED`。
- M6 PQC 实际检验人切换门禁已通过真实动作验证：`pqcActualEmployeeSelected` 已接入正式 `/pqc/personnel` 和 `/pqc/switch-employee` 路径，本机 tenant 1 已写入 task-owned PQC `EMPLOYEE` scope row id 980013；full real E2E 证明 `actualEmployeeId=512` 不默认登录人 `659`。AC-D25/D31 仍不能标记为 `ACCEPTED`，因为还缺完整失败路径、签名/权限/清理和全量 M6 验收闭环。
- M6 AC-D32 PQC 组长提交看板筛选/分页已有真实动作 PASS：提交看板 read-model 已按产品、检验类型、轮次、复核状态和 `pqcTaskId` 精确关联过滤；`m6-pqc-d32-same-filter-local-seed.sql` 已准备同筛选条件下第二个正式 PENDING PQC task；full real E2E 证明筛选条件 `submitDate=2026-08-03 / workOrderCode=RRM-20260801-PP-MO-001 / employeeUserId=512 / processId=922985 / productKeyword=AW.107.02.01.2010 / inspectionType=PATROL / roundNo=1 / submissionReviewStatus=PENDING` 下 `total=2`，第 1/2 页事件分别为 `24`、`26`。AC-D32 仍不得标记为 `ACCEPTED`，因为还缺失败路径、权限/只读隔离、性能门禁、清理和 62 AC 全量准出。
- M6 AC-D29 PQC 正式提交事件链路已有真实动作 PASS：`submitPqcInspection` 已在正式提交时校验 source event 和 active pool 身份、更新 PQC task、插入逐件明细并调用 `createPqcInspectionEvent`；full real E2E 通过未占用签名池选择 `signatureId=23`，生成 `submittedTaskId=31`、`eventId=26`，候选签名 ID 为 `[25,22,23,24,26,27]`，已占用签名 ID 为 `[25,22]`。AC-D29 仍不得标记为 `ACCEPTED`，因为还缺失败路径、重复/并发提交证明、复核/只读追溯、清理和 62 AC 全量准出。
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
- M6 当前待办：并发、性能、全量真实 E2E、清理和上线验收门禁仍未完成；AC-M04 已有 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderUnauthorizedMutationBlocked` 四个真实 PASS 动作证据和 `activeOrderCleanupDeferred` 清理 BLOCKED 证据，PQC 已有 `pqcRegulationItemsRendered`、`pqcPieceDetailQuantityPrepared`、`pqcActualEmployeeSelected`、`pqcFormalSubmissionCreated`、`pqcLeaderSubmissionFilterPaginationConsistent` 五个真实 PASS 动作证据；M6 迁移预检静态/策略/运行库门禁已 GREEN，后端重复/并发/冲突路线单测 GREEN；当前剩余 blocker 为 `activeOrderCleanupDeferred`、`m6ConcurrencyGateDeferred`、`m6PerformanceGateDeferred` 和 62 个 `E2E_COVERAGE`，仍不能宣称 Excel 全量目标完成。
- M6 runtime ownership blocker 已关闭：当前 48081 listener PID `43876` 是本任务 RRM M6 runtime jar `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`，后端 health 为 `UP`，前端 8081 HTTP `200`，授权 `real:check` 为 PASS，0 SOURCE/ENV/RUNTIME blocker。
- M6 AC-D29 signature-pool blocker 已由真实页面验证关闭：固定签名 ID `25` 复用问题已由真实 E2E 脚本门禁解决，本轮实际选择未占用 `signatureId=23` 完成正式提交；若用户提供的签名池全部被占用，脚本仍会结构化输出 `E2E_PQC_SIGNATURE_POOL`，不得放宽后端 `signature_id` 唯一键。
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
- Current AC-D29 duplicate-submit status guard has target backend GREEN after isolating residual `target`; M6 still remains `in_progress` because full real E2E failure paths, permissions/read-only proof, concurrency/performance, cleanup, and 62 AC acceptance remain open.

## Applicable Gate Summary

- 严格 TDD 状态链：`PLANNED -> BDD_APPROVED -> TEST_ADDED -> RED_VALID -> IMPLEMENTING -> GREEN -> REFACTORED -> REGRESSION_PASS -> E2E_PASS -> ACCEPTED`。
- 缺测试类、缺脚本、No tests、编译失败、依赖缺失、服务未启动、账号缺失或测试数据缺失均只能记录 blocker，不算 RED。
- 用户可见行为必须通过正式登录页、正式菜单和真实页面路径执行 Playwright E2E；API 只能用于最终只读核验或任务数据清理。
- 后续实现任务不执行 Git push，除非用户另行明确要求。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；`real:check` 对缺正式来源保持 fail-fast / BLOCKED，不返回默认成功。
- `是否从根因和长期维护角度解决`：是，按规划包先冻结正式来源、source map、真实 E2E 前置和 blocker，再按 AC / TC 逐项进入后续里程碑；M6 仍需完成全量验收门禁。
- `是否存在临时补丁或绕过`：否；当前 `m0-derived-qa-regulation.md`、本机工单/调拨/签名数据、`m6-pqc-employee-scope-local-seed.sql` 和 `m6-pqc-d32-same-filter-local-seed.sql` 仅是本机真实 E2E 夹具，明确不替代 activeOrderId、QA 规程版本、PQC 任务或 ERP 关系正式模型。
