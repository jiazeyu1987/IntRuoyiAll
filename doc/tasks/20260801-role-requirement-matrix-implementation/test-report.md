# 岗位需求分解矩阵 M0 独立测试报告

## Scope

本报告只验证 M0：契约、术语、权威来源、测试数据和真实 E2E 前置。当前不验证 M1-M6 生产实现，也不允许因静态合同通过而进入 M1。

## Tested Evidence

| 检查项 | 结论 | 证据 |
|---|---|---|
| 规划包必读文档 | PASS | 已读取 `prd.md`、`development-plan.md`、`test-plan.md`、`task-state.json` 和 `docs/acceptance/*`。 |
| BDD/TDD 结构 | PASS | 16 个 BDD 场景、62 个 AC、62 个 TC 已在规划包中形成映射。 |
| Source map 深化 | PASS_BLOCKED | ERP/QC/WMS/PQC/eDHR 代码锚点已补齐；结论仍是正式来源不足，M0 不准出。 |
| 入口合同 | PASS | `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` 通过。 |
| 真实 E2E 脚本语法 | PASS | `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` 通过。 |
| package script 前置 | PASS | `pnpm e2e:role-requirement-matrix:preflight:static` 通过。 |
| 真实 E2E 前置 | BLOCKED_EXPECTED | `pnpm e2e:role-requirement-matrix:real:check` 返回 31 个 SOURCE blocker；本轮已无 ENV/RUNTIME blocker。 |
| M0 本地测试夹具 | PASS_LOCAL_TEST | `m0-test-data.md` 记录用户授权租户、六角色账号、签名、压力泵路线、工单/调拨和 QC/IPQC 模板。 |
| M3/M4/M5 规划静态脚本 | RED_EXPECTED | 四个脚本均已存在，当前失败原因是业务来源或模型缺口，不是脚本缺失。 |
| 文档结构 | PASS | `task-state.json` 可解析，任务 Markdown 可 UTF-8 读取。 |

## Latest Rerun

| 日期 | 命令 | 结论 |
|---|---|---|
| 2026-08-02 | `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` | PASS |
| 2026-08-02 | `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` | PASS |
| 2026-08-02 | `pnpm e2e:role-requirement-matrix:preflight:static` | PASS |
| 2026-08-02 | `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` | RED：真实脚本尚未要求 `RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION` |
| 2026-08-02 | `pnpm e2e:role-requirement-matrix:real:check` | RED：用户授权的 `芋道源码` 本机租户仍被旧 ENV 禁止口径拦截 |
| 2026-08-02 | `pnpm e2e:role-requirement-matrix:real:check` | EXPECTED_BLOCKED：31 个 SOURCE 前置缺口；ENV/RUNTIME 前置通过 |
| 2026-08-02 | UTF-8 / JSON structural read | PASS |
| 2026-08-02 | `git diff --check -- ...` | PASS |

## Independent Gate Decision

`M0 = BLOCKED`。当前证据证明脚本入口和 fail-fast gate 已具备，但不证明 Excel 目标可实现完成。M0 未满足准出条件，原因如下：

- 统一 `activeOrderId` 的正式 schema/service 未具备。
- PQC 仍读取 `mes_pro_process_pool` 活跃行，且提交仍依赖最新生产事件。
- activeOrderId 到调拨、发货、补料/退料、批次/库存的正式关系源未冻结。
- QA 规程所有权、发布版本、PQC 任务身份、规程快照和逐件明细模型未冻结。
- PQC 前端仍硬编码检验项目、巡检类型和默认数量，未按发布规程动态渲染。
- activeOrderId 缺生产系数和计划数量快照，自动排产仍存在默认生产系数 `1` 的路径。
- 正式批记录绑定缺失槽位仍可默认 `MAIN`，`batchRecordFormNames` 与 `formBindings` 互不替代缺真实 E2E 证明。
- 放行检查仍存在 `buildSourceNotIntegratedItem` 来源未接入 blocker。
- 测试租户、六角色账号、权限、电子签名和任务专用本地夹具已确认；正式 ERP/activeOrder/QA/PQC source model 仍未完成。

## Advancement Decision

不允许进入 M1。后续只能继续补齐 M0 正式来源、真实账号/签名/样本和 E2E 前置；禁止用 mock、默认值、API-only、静态合同或截图替代真实 Playwright E2E。
