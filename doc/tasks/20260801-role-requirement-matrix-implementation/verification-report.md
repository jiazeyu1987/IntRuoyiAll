# 岗位需求分解矩阵实现任务验证报告

## Scope

验证当前实现任务是否按规划包推进。当前阶段为 M0，只验证契约、source map、测试数据和真实 E2E 前置，不声明 M1-M6 生产实现完成。

## Current Result

BLOCKED。规划包 BDD/TDD/测试覆盖结构验证通过，`role-requirement-matrix` 真实 E2E 前置脚本和 M3/M4/M5 规划静态脚本入口已补齐并通过入口合同；本轮已补齐用户授权的本机租户、六角色账号、权限、签名、压力泵路线、工单/调拨和 QC/IPQC 夹具，但正式来源前置仍未满足，不能进入 M1。

独立测试报告已补齐：`doc/tasks/20260801-role-requirement-matrix-implementation/test-report.md`。该报告明确 M0 为 `BLOCKED`，不允许进入 M1。

## Verification Evidence

| 检查 | 结果 |
|---|---|
| BDD/TDD acceptance validator | PASS |
| Roadmap node development plan validator | PASS |
| M0 source map | BLOCKED |
| M0 source map 深化 | PASS：ERP/QC/WMS/PQC/eDHR 锚点已补齐，结论仍为 BLOCKED |
| 本机前端/后端/health/login/browser 基础前置 | PASS |
| `role-requirement-matrix` 真实 E2E 脚本入口 | PASS |
| `role-requirement-matrix` 真实 E2E 前置检查 | BLOCKED：31 个 SOURCE 缺口；无 ENV/RUNTIME blocker |
| M3/M4/M5 规划静态脚本入口 | PASS |
| M3/M4/M5 规划静态脚本业务断言 | RED_EXPECTED |
| 测试租户、六角色、电子签名、本地任务夹具 | PASS_LOCAL_TEST |

## Latest Local Verification

| 命令 | 结果 |
|---|---|
| `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` | PASS |
| `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` | PASS |
| `pnpm e2e:role-requirement-matrix:preflight:static` | PASS |
| `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` | PASS |
| `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` | RED：真实脚本缺少显式本机租户授权门禁 |
| `pnpm e2e:role-requirement-matrix:real:check` | RED：`芋道源码` 本机租户被旧 ENV 禁止口径拦截 |
| `pnpm e2e:role-requirement-matrix:real:check` | EXPECTED_BLOCKED：31 个 SOURCE 前置缺口；ENV/RUNTIME 前置通过 |
| `pnpm e2e:role-matrix-qa-regulation:static` | RED_EXPECTED：QA 规程正式 schema/版本模型缺失 |
| `pnpm e2e:role-matrix-pqc-dynamic-form:static` | RED_EXPECTED：PQC 规程快照任务身份缺失 |
| `pnpm e2e:role-matrix-transfer-start-check:static` | RED_EXPECTED：activeOrderId 调拨/批次/库存关系源缺失 |
| `pnpm e2e:role-matrix-daily-close-scope:static` | RED_EXPECTED：日结入口和扩展责任范围模型缺失 |
| `git diff --check -- ...role-requirement-matrix... doc/tasks/20260801-role-requirement-matrix-implementation` | PASS |
| UTF-8 / JSON structural read | PASS |

## Blocking Detail

- 当前活跃订单表和服务仍以生产组长范围为中心，不满足统一 activeOrderId。
- PQC 仍读取 `mes_pro_process_pool` 活跃行，且提交时仍依赖最新生产事件，未切到统一活跃订单服务和发布规程任务。
- ERP 调拨/发货/补料/退料/批次与活跃订单的正式关系源未冻结。
- QA 规程唯一所有权、发布版本、PQC 任务身份、规程快照和逐件明细正式模型未冻结。
- PQC 前端仍硬编码检验项目、巡检类型和默认数量，当前只能作为业务 RED 证据，不能作为可验收配置。
- 统一 activeOrderId 仍缺生产系数和计划数量快照，自动排产仍存在缺失系数默认 `1` 的路径。
- 正式逐工序批记录绑定表存在，但前端和 eDHR 运行态仍有缺失槽位默认 `MAIN` 路径，且 `batchRecordFormNames` 与 `formBindings` 互不替代尚未通过真实 E2E 证明。
- eDHR 放行的检验、偏差、返工、报废和库存仍是来源未接入 blocker。
- 真实 E2E 主链路前置脚本已存在；任务本地租户、六角色账号、权限、电子签名和 RRM 本地样本已补齐。
- 真实 E2E 前置脚本已扩大 SOURCE 覆盖，当前明确阻塞 31 项 SOURCE 缺口：activeOrderId 到调拨/发货/补料/退料/批次关系、QA 规程归属、PQC 任务身份和逐件明细模型、生产系数快照、批记录槽位默认 `MAIN` 风险等。
- M3/M4/M5 对应规划静态脚本已创建；当前分别因 QA 规程、PQC 动态表单、调拨/放行真实来源、日结/范围缺口产生业务 RED，后续不能跳过 GREEN。

## Decision

M0 状态标记为 `blocked`。本轮不进入 M1，不新增生产代码，不用静态合同或 API-only 代替真实 Playwright E2E；当前新增的真实 E2E 前置脚本只作为 M0 fail-fast gate。本轮按用户明确要求不执行 `git push`。
