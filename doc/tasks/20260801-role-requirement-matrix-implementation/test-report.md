# 岗位需求分解矩阵 M0-M3 测试报告

## Scope

本报告验证 M0 revised gate、M1 activeOrderId authority source gate、M2 production coefficient snapshots source gate 和 M3 QA/PQC source gate。2026-08-02 用户已调整 M0 门禁口径：M0 不要求清零需要 M1-M5 正式实现的 SOURCE blocker；M1 已清零 RRM-BLK-001..007，M2 已清零 RRM-BLK-026..028，M3 已清零 RRM-BLK-017..025，当前进入 M4。

## Tested Evidence

| 检查项 | 结论 | 证据 |
|---|---|---|
| 规划包必读文档 | PASS | 已读取 `prd.md`、`development-plan.md`、`test-plan.md`、`task-state.json` 和 `docs/acceptance/*`。 |
| BDD/TDD 结构 | PASS | 16 个 BDD 场景、62 个 AC、62 个 TC 已在规划包中形成映射。 |
| Source map 深化 | PASS_FROZEN | ERP/QC/WMS/PQC/eDHR 代码锚点已补齐；M0 冻结 31 个 SOURCE blocker，M1 已关闭 7 个，M2 已关闭 3 个，M3 已关闭 9 个，当前剩余 12 个归属到 M4-M5。 |
| 入口合同 | PASS | `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` 通过。 |
| 真实 E2E 脚本语法 | PASS | `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` 通过。 |
| package script 前置 | PASS | `pnpm e2e:role-requirement-matrix:preflight:static` 通过。 |
| 真实 E2E 前置 | BLOCKED_EXPECTED | `pnpm e2e:role-requirement-matrix:real:check` 返回 12 个 SOURCE blocker，无 ENV/RUNTIME blocker；剩余均为 M4-M5 下游 blocker。 |
| M0 本地测试夹具 | PASS_LOCAL_TEST | `m0-test-data.md` 记录用户授权租户、六角色账号、签名、压力泵路线、工单/调拨、QC/IPQC 模板和派生 QA 临时规程。 |
| M3/M4/M5 规划静态脚本 | PASS_WITH_DOWNSTREAM_BLOCKERS | 四个脚本均已存在；M3 静态合同已通过，M4/M5 剩余失败原因是业务来源或模型缺口，不是脚本缺失。 |
| blocker 汇总 | PASS_DOC | `blocker-inventory.md` 已结构化记录 12 个当前 SOURCE blocker；RRM-BLK-001..007、RRM-BLK-017..025、RRM-BLK-026..028 和历史 runtime blocker `RRM-BLK-032` 已标记验证关闭。 |
| M0 准出审计 | PASS_ACCEPTED | `m0-gate-audit.md` 已按用户批准的新口径确认 M0 准出，可进入 M1 activeOrderId 切片。 |
| M1 activeOrderId 权威来源 | PASS_ACCEPTED | RRM-BLK-001..007 已由 schema/service/controller/PQC source switch 测试和授权 `real:check` 验证关闭。 |
| M2 生产系数与计划数量快照 | PASS_ACCEPTED | RRM-BLK-026..028 已由 active order process snapshot schema/service、分配/完成链路、自动排产 fail-fast 测试和授权 `real:check` 验证关闭。 |
| M3 QA 规程与 PQC 闭环 | PASS_ACCEPTED | RRM-BLK-017..025 已由 QA/PQC schema、PQC task/source submit、逐件明细、前端动态渲染、类型检查和授权 `real:check` 验证关闭。 |
| 文档结构 | PASS | `task-state.json` 可解析，任务 Markdown 可 UTF-8 读取。 |

## Latest Rerun

| 日期 | 命令 | 结论 |
|---|---|---|
| 2026-08-02 | `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` | PASS |
| 2026-08-02 | `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` | PASS |
| 2026-08-02 | `pnpm e2e:role-requirement-matrix:preflight:static` | PASS |
| 2026-08-02 | `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` | RED：真实脚本尚未要求 `RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION` |
| 2026-08-02 | `pnpm e2e:role-requirement-matrix:real:check` | RED：用户授权的 `芋道源码` 本机租户仍被旧 ENV 禁止口径拦截 |
| 2026-08-02 | `pnpm e2e:role-requirement-matrix:real:check` | EXPECTED_BLOCKED：31 个 SOURCE 前置缺口；当时 ENV/RUNTIME 前置通过 |
| 2026-08-02 | `blocker-inventory.md` | HISTORICAL_PASS_DOC：旧口径下只结构化记录、不授权进入 M1；后续已由 revised M0 gate 覆盖 |
| 2026-08-02 | blocker status/date completeness | PASS_DOC：31 个 `Blocker Summary` ID 与 31 个 `Status Register` ID 完全一致 |
| 2026-08-02 | `m0-gate-audit.md` | HISTORICAL_FAIL_BLOCKED：旧口径要求 SOURCE 清零；后续已由 revised M0 gate 覆盖 |
| 2026-08-02 | `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` | PASS |
| 2026-08-02 | post-audit static / syntax / preflight rerun | PASS：入口合同、真实脚本语法、package script 前置均通过 |
| 2026-08-02 | post-audit `pnpm e2e:role-requirement-matrix:real:check` | HISTORICAL_EXPECTED_BLOCKED：当时为 31 个 SOURCE blocker；无 ENV/RUNTIME blocker，最新状态见下一行 |
| 2026-08-02 | pre-M1 `pnpm e2e:role-requirement-matrix:real:check` | HISTORICAL_EXPECTED_BLOCKED：31 个 SOURCE blocker；无 ENV/RUNTIME blocker；按新 M0 gate 可进入 M1 |
| 2026-08-02 | current-state planning package read | PASS：`development-plan.md` 532 行、`test-plan.md` 423 行；测试计划映射 62 AC / 62 TC / 16 BDD |
| 2026-08-02 | current-state M0 minimum rerun | PASS/EXPECTED_BLOCKED：health、静态合同、语法、package script、database evidence validator 均 PASS；`real:check` 仍为 31 SOURCE blocker、0 ENV、0 RUNTIME |
| 2026-08-02 | planning package validators rerun | PASS：BDD/TDD acceptance plan validator 和 roadmap node development plan validator 均通过；证明规划结构可作为后续 M1-M6 TDD 输入 |
| 2026-08-02 | blocker key consistency audit | PASS_AFTER_FIX：31 个 `result.json` SOURCE key 均可在 `blocker-inventory.md` 定位；31 个 `OPEN_BLOCKED`，1 个 `RESOLVED_VERIFIED` |
| 2026-08-02 | M0/M1 boundary guard documentation | HISTORICAL_PASS_DOC：旧口径下 M1-M5 命令只是 future gate；后续已由 revised M0 gate 覆盖 |
| 2026-08-02 | boundary guard minimum rerun | PASS/EXPECTED_BLOCKED：结构边界、blocker key、静态入口、脚本语法、package preflight、database validator、diff check 均 PASS；带授权 M0 夹具的 `real:check` 仍为 31 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | future RED plan register | HISTORICAL_PASS_DOC：`blocker-inventory.md` 已将 SOURCE blocker 分组为 6 个 RED 计划；当时 M1 已完成且只允许启动 M2 production coefficient snapshots 计划，当前已由 M2 accepted 覆盖 |
| 2026-08-02 | future RED plan minimum rerun | PASS/EXPECTED_BLOCKED：future RED 覆盖校验、UTF-8/JSON、静态入口、脚本语法、package preflight、database validator、diff check 均 PASS；带授权 M0 夹具的 `real:check` 仍为 31 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | design constraint and execution permission clarification | PASS_DOC：`task.md` 明确临时 QA/工单/调拨/签名仅为 M0 预检夹具，`blocker-inventory.md` 明确当前只允许 M0 证据和预检，不允许 M1-M6 实现闭环 |
| 2026-08-02 | design constraint minimum rerun | PASS/EXPECTED_BLOCKED：设计约束文档结构、result/inventory 一致性、静态入口、脚本语法、package preflight、database validator、diff check 均 PASS；授权 M0 夹具下 `real:check` 仍为 31 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | planning package supervisor script precheck | RED_EXPECTED_DOC：supervisor 脚本不识别当前表格型 milestone 和自定义 `task-state.json`；已记录为工具适配证据，不影响 revised M0 gate |
| 2026-08-02 | user challenge continuation recheck | HISTORICAL_PASS/EXPECTED_BLOCKED：旧口径下 M1 继续 blocked；后续已由用户批准 revised M0 gate 覆盖 |
| 2026-08-02 | revised M0 gate decision | PASS_ACCEPTED：用户明确调整门禁口径，M0 只负责识别并结构化冻结 SOURCE blocker；31 SOURCE 已归属 M1-M5，0 ENV，0 RUNTIME，可进入 M1 |
| 2026-08-02 | revised M0 gate validation rerun | PASS/EXPECTED_BLOCKED_FOR_DOWNSTREAM：change request validator、BDD/TDD validator、roadmap validator、revised gate consistency、static preflight、script syntax、package preflight、database evidence validator 和 diff check 均 PASS；授权 `real:check` 为 31 SOURCE / 0 ENV / 0 RUNTIME |
| 2026-08-02 | M1 schema RED | RED_EXPECTED：`MesProcessPoolTeamLeaderSchemaTest` 因 `NoSuchFieldException: routeId` 失败 |
| 2026-08-02 | M1 schema/service/controller GREEN | PASS：`MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest` 共 13 tests 通过 |
| 2026-08-02 | M1 PQC source RED | RED_EXPECTED：`MesFrontlinePqcContextServiceTest` 因构造器和 `MesProcessPoolActiveOrderMapper` 查询方法缺失编译失败；静态合同因缺 `ACTIVE_ORDER_AUTHORITY_SQL` 失败 |
| 2026-08-02 | M1 PQC source GREEN | PASS：`MesFrontlinePqcContextServiceTest` 共 6 tests 通过，静态合同通过 |
| 2026-08-02 | M1 combined regression | PASS：M1 组合 Maven 命令共 19 tests 通过，`node --check` 通过 |
| 2026-08-02 | latest authorized `pnpm e2e:role-requirement-matrix:real:check` after M1 | HISTORICAL_EXPECTED_BLOCKED_FOR_DOWNSTREAM：24 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-001..007 已移除，后续已由 M2 accepted 覆盖 |
| 2026-08-02 | M2 RED | RED_EXPECTED：M2 Maven 命令失败于缺少 active order process snapshot DO/mapper/target service |
| 2026-08-02 | M2 GREEN/regression | PASS：`MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest` 共 25 tests 通过 |
| 2026-08-02 | M2 static / syntax preflight | PASS：`node --check`、`node role-requirement-matrix-preflight-static.spec.cjs`、`pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` 均通过 |
| 2026-08-02 | latest authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` | EXPECTED_BLOCKED_FOR_DOWNSTREAM：21 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-026..028 已移除 |
| 2026-08-02 | M3 targeted Maven GREEN | PASS：`MesQaPqcSchemaTest,MesFrontlinePqcContextServiceTest` 共 8 tests 通过 |
| 2026-08-02 | M3 frontend regression | PASS：`pnpm --dir IntRuoyiFronted ts:check`、`e2e:role-matrix-qa-regulation:static`、`e2e:role-matrix-pqc-dynamic-form:static`、`e2e:role-requirement-matrix:preflight:static` 和 `node --check` 均通过 |
| 2026-08-02 | M3 real source gate first rerun | EXPECTED_BLOCKED_WITH_M3_FALSE_POSITIVE：13 SOURCE / 0 ENV / 0 RUNTIME；`hardcodedPqcInspectionItems` 为动态变量名扫描误判 |
| 2026-08-02 | M3 real-flow scanner refinement | PASS：`hardcodedItemPattern` 收窄后 `node --check` 和 PQC 动态表单静态合同通过 |
| 2026-08-02 | latest authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M3 | EXPECTED_BLOCKED_FOR_DOWNSTREAM：12 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-017..025 已移除 |
| 2026-08-02 | UTF-8 / JSON structural read | PASS |
| 2026-08-02 | `git diff --check -- ...` | PASS，仅 LF/CRLF 工作区警告 |

## Independent Gate Decision

`M0 = ACCEPTED_BY_REVISED_GATE`，`M1 activeOrderId authority = ACCEPTED`，`M2 production coefficient snapshots = ACCEPTED`，`M3 QA/PQC source gate = ACCEPTED`。当前证据证明脚本入口、fail-fast gate、本机测试夹具、SOURCE blocker 结构化冻结和里程碑归属已经具备，且 M1/M2/M3 已将 SOURCE blocker 从 31 个降至 12 个。该结论不表示 Excel 目标已实现完成，只表示可以进入 M4。

- M1 已清零 RRM-BLK-001..007：统一 `activeOrderId` 的正式 schema/service、跨角色唯一键和 PQC 来源切换。
- M2 已清零 RRM-BLK-026..028：生产系数、计划数量快照和自动排产默认系数路径。
- M3 已清零 RRM-BLK-017..025：QA 规程版本、PQC 任务身份、逐件明细、提交来源和动态前端渲染。
- M4 当前必须清零 RRM-BLK-008..016：activeOrderId 到调拨、发货、补料/退料、批次/库存和放行来源。
- M5 必须清零 RRM-BLK-029..031：正式批记录绑定、`formBindings`、默认 `MAIN` 和 `工序开始` 三类配置分离证明。
- 临时 QA 模板 `6 / RRM-20260801-QA-REG-PP-V21` 和本机工单/调拨/签名数据只作为 M0 夹具，不替代正式模型。

## Advancement Decision

允许进入 M4。M4 必须从 activeOrderId 调拨/发货/补退料/批次追溯和 eDHR 放行来源切片开始，按 BDD + 严格 TDD 先写/确认 RED，再做最小正式实现，随后 GREEN、REGRESSION 和适用真实 E2E；禁止用 mock、默认值、API-only、静态合同、临时 WMS 推断或截图替代真实 Playwright E2E。本轮未启动 M4 生产代码，M5-M6 仍不得越级实现。
