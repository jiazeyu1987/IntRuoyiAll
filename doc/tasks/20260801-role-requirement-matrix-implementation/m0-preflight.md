# M0 Preflight - 岗位需求分解矩阵

## Structural Validation

| 检查 | 命令 | 结果 |
|---|---|---|
| BDD/TDD acceptance plan | `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` | `PASS` |
| Roadmap node development plan | `python -X utf8 C:\Users\BJB110\.codex\skills\roadmap-node-dev-plan\scripts\validate_node_dev_plan.py --task-dir E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` | `PASS` |

## Runtime Preconditions

| 前置 | 结果 | 证据 |
|---|---|---|
| 前端 8081 | `PASS` | `node/vite` 监听 `8081`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。 |
| 后端 48081 | `PASS` | `java` 监听 `48081`，运行 jar 位于 `E:\IntRuoyi\output\runtime\int_main`，repo-root 指向 `E:\IntRuoyi\IntRuoyiBackend`；命令行已在本文档中省略凭据。 |
| 后端 health | `PASS` | `http://127.0.0.1:48081/actuator/health` 返回 HTTP 200。 |
| 前端登录页 | `PASS` | `http://127.0.0.1:8081/login?redirect=/index` 返回 HTTP 200。 |
| MySQL/Redis 端口 | `PASS_PARTIAL` | `23306`、`26379` 监听；仅确认本地依赖端口存在，未记录账号或连接密钥。 |
| 浏览器 | `PASS` | Chrome 和 Edge 可执行文件存在。 |
| Node/pnpm/Maven | `PASS` | Node `v24.12.0`，pnpm `10.22.0`，Maven `3.9.9`。 |

## E2E Script Preconditions

| 脚本或文件 | 结果 |
|---|---|
| `e2e:role-requirement-matrix:preflight:static` | `PASS`：`pnpm e2e:role-requirement-matrix:preflight:static` 通过。 |
| `e2e:role-requirement-matrix:real:check` | `BLOCKED_EXPECTED`：脚本存在并执行 fail-fast；在用户显式授权本机 `芋道源码` 租户并补齐 M0 夹具后，当前报告 31 个 SOURCE 前置缺口，无 ENV/RUNTIME blocker。 |
| `e2e:role-requirement-matrix:real` | `GATED`：脚本存在，但 M1-M5 未 accepted 且 M0 前置缺口未清，不允许执行全链路 E2E。 |
| `tests/e2e/role-requirement-matrix-real-flow.e2e.js` | `PASS`：文件存在，`node --check` 通过。 |
| `e2e:role-matrix-qa-regulation:static` | `RED_EXPECTED`：脚本存在；当前因 QA 规程正式 schema/版本模型和动态规程渲染缺失失败。 |
| `e2e:role-matrix-pqc-dynamic-form:static` | `RED_EXPECTED`：脚本存在；当前因 PQC 规程快照任务身份和动态项目渲染缺失失败。 |
| `e2e:role-matrix-transfer-start-check:static` | `RED_EXPECTED`：脚本存在；当前因 activeOrderId 调拨/批次/库存关系源和放行真实来源缺失失败。 |
| `e2e:role-matrix-daily-close-scope:static` | `RED_EXPECTED`：脚本存在；当前因日结入口和扩展责任范围模型缺失失败。 |
| `e2e:frontline-formal-submit:static` | `PASS`：脚本和文件存在。 |
| `e2e:team-leader-report-allocation:static` | `PASS`：脚本和文件存在。 |
| `e2e:edhr:release:check` | `PASS`：脚本和文件存在。 |
| `ts:check` | `PASS`：脚本存在。 |

## Real Data Preconditions

| 前置 | 结果 | 影响 |
|---|---|---|
| 测试租户 | `PASS_AUTHORIZED_LOCAL` | 用户明确授权本轮使用本机租户 `芋道源码`；真实预检要求 `RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION=USER_APPROVED_YUDAO_SOURCE_20260802`。 |
| 六角色账号 | `PASS` | 已随机选定并配置 `liuyueyue`、`lvyujie`、`sunxiaoqing`、`shangmengying`、`huzonggang`、`zhengxiaofang`；密码已按用户指定值重置，文档不记录明文。 |
| 权限 | `PASS_LOCAL_TEST` | 六个账号保留原 `approval_center_entry`，并新增本地测试用 `super_admin` 角色以覆盖菜单/页面前置。 |
| 电子签名 | `PASS_LOCAL_TEST` | 六个账号已启用本地电子签名授权，签名图片 ID 为 `22,23,24,25,26,27`。 |
| ERP 候选订单/调拨/发货/批次样本 | `PASS_LOCAL_FIXTURE` | 已创建 `RRM-20260801-PP-MO-001` 工单、两条调拨 `1,2` 和对应批次/库存；这些不是 ERP/activeOrder 正式来源。 |
| 正式路线/系数/SOP/逐工序批记录绑定 | `PASS_PARTIAL` | 使用压力泵路线 `922119` 最新 ACTIVE 版本 `448/V21`；粗洗/精洗存在正式批记录和 `PROCESS_INSPECTION` 绑定；未改生产系数。 |
| QA 规程样本 | `PASS_LOCAL_FIXTURE` | 已创建 QC/IPQC 模板 `5` 和指标 `5,6,7`；正式 QA 规程版本模型仍是 SOURCE blocker。 |

## M0 Gate Decision

`M0 = BLOCKED`。可以确认规划文档结构符合 BDD/TDD 与 62 项测试覆盖要求，且 `role-requirement-matrix` 真实 E2E 前置脚本和 M3/M4/M5 规划静态脚本入口已具备 fail-fast/RED 能力；本轮已补齐用户授权的本机租户、六角色账号、权限、签名、压力泵路线、工单/调拨和 QC/IPQC 夹具，但正式 SOURCE 缺口仍剩 31 项，不能进入 M1。

## Generated Evidence

- `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`：`BLOCKED`，31 个 SOURCE 前置缺口。
- `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`：真实 E2E 前置 Markdown 证据。
- `doc/tasks/20260801-role-requirement-matrix-implementation/m0-test-data.md`：本机租户/账号/路线/签名/QC 夹具证据。
- `doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md`：数据库夹具变更证据。

## Source Gate Expansion

- ERP/物料基础来源：预检显式核对 `mes_kingdee_production_material_list`、`mes_wm_transfer`、`mes_wm_transfer_detail`、`mes_wm_transfer_line`、`mes_wm_batch`、`mes_wm_material_stock`。
- ERP 关系 blocker：预检显式阻塞 activeOrderId 到调拨、发货、补料/退料、批次/库存追溯的正式关系缺口。
- QA/PQC blocker：预检显式阻塞 QA 规程唯一归属、规程版本、PQC 任务身份和逐件明细模型缺口。
- 生产系数 blocker：预检显式阻塞 activeOrderId 生产系数/计划数量快照缺口，以及自动排产缺失系数默认 `1` 的路径。
- 正式批记录绑定 blocker：预检显式阻塞 `normalizeRecordBindingSlotType` 和 eDHR 运行态缺失槽位默认 `MAIN`，并要求证明 `batchRecordFormNames` 与 `formBindings` 不互相替代。
