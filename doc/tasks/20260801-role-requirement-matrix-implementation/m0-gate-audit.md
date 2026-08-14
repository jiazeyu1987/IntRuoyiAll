# M0 Gate Audit - Role Requirement Matrix

## Purpose and Scope

本文件是 `岗位需求分解矩阵` 实施任务的 M0 独立准出审计。审计目标是判断当前证据是否允许主线程从 M0 进入 M1；不验证 M1-M6 生产实现完成，也不授权绕过 `M0 -> M1 -> ...` 顺序。

2026-08-02 用户已明确调整 M0 门禁口径：M0 只负责识别并结构化冻结 SOURCE blocker，不要求在 M0 清零这些需要 M1-M5 正式实现的 blocker。因此，本审计以变更后的 M0 gate 为准。

## Evidence Reviewed

- `doc/tasks/20260801-role-requirement-matrix-excel/development-plan.md`
- `doc/tasks/20260801-role-requirement-matrix-excel/test-plan.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/task-state.json`
- `doc/tasks/20260801-role-requirement-matrix-implementation/task.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/execution-log.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/verification-report.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/test-report.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/blocker-inventory.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`
- `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`

## Requirement-to-Evidence Checklist

| M0 requirement | Required proof | Current evidence | Audit result |
|---|---|---|---|
| 规划包和测试计划已作为权威输入读取 | `development-plan.md`、`test-plan.md` 可 UTF-8 读取 | 已读取并用于 M0 gate 复核 | PASS |
| 62 个 AC / TC 和 16 个 BDD 结构完整 | 规划包结构验证和测试计划映射 | `test-report.md` 记录结构 PASS | PASS |
| source map 明确正式来源或 blocker | M0 source map、真实代码路径和 blocker 清单 | `blocker-inventory.md` 记录 31 个 SOURCE blocker；历史 runtime blocker 已验证关闭 | PASS |
| 测试租户、六角色账号、签名和本地样本可用于 M0 预检 | 本地夹具证据，不记录密码 | `m0-test-data.md`、`m0-derived-qa-regulation.md` | PASS_LOCAL_TEST |
| 真实 E2E 前置脚本存在且可 fail fast | package script、真实脚本语法、静态入口合同 | `node --check`、`preflight:static` 均 PASS | PASS |
| `real:check` 无 ENV blocker | `result.json` categories 不含 `ENV` | 当前 `result.json` 为 31 个 `SOURCE`，无 `ENV` | PASS |
| `real:check` 无 RUNTIME blocker | `result.json` categories 不含 `RUNTIME` | 当前 `result.json` 无 `RUNTIME`；后端 health 返回 `UP` | PASS |
| SOURCE blocker 结构化冻结 | 每个 SOURCE blocker 有 ID、里程碑/AC/TC、代码路径、失败命令、原因、影响、正式解决方案、状态和日期 | RRM-BLK-001..031 已完整登记并映射到 M1-M5 | PASS |
| M1 activeOrderId blocker 归属 | active order schema/service 缺口不在 M0 清零，必须归属 M1 | RRM-BLK-001..007 已归属 M1 | PASS_DEFERRED_TO_M1 |
| M2 生产系数/计划数量 blocker 归属 | 生产系数和计划数量缺口不在 M0 清零，必须归属 M2 | RRM-BLK-026..028 已归属 M2 | PASS_DEFERRED_TO_M2 |
| M3 QA/PQC blocker 归属 | QA 规程、PQC task、逐件明细和前端动态渲染缺口不在 M0 清零，必须归属 M3 | RRM-BLK-017..025 已归属 M3 | PASS_DEFERRED_TO_M3 |
| M4 调拨/放行来源 blocker 归属 | 调拨、发货、补退料、批次、放行来源缺口不在 M0 清零，必须归属 M4 | RRM-BLK-008..016 已归属 M4 | PASS_DEFERRED_TO_M4 |
| M5 三类工艺路线配置分离 blocker 归属 | 批记录绑定、`formBindings` 和默认 `MAIN` 风险不在 M0 清零，必须归属 M5 | RRM-BLK-029..031 已归属 M5 | PASS_DEFERRED_TO_M5 |
| 任务状态不越级 | `task-state.json.currentMilestone=M1`，M0 accepted，M2-M6 仍按依赖阻塞 | 当前状态允许 M1 activeOrderId 切片启动，不允许 M2-M6 越级 | PASS |

## Blocker Classification

| Category | Count | Evidence | Impact |
|---|---:|---|---|
| ENV | 0 | `result.json` blocker categories | 环境、账号、签名和本地夹具不再是 M0 当前阻塞原因 |
| RUNTIME | 0 | `result.json` blocker categories | 历史 `backendHealth` blocker 已关闭；当前后端 `127.0.0.1:48081` health 为 `UP` |
| SOURCE | 31 | `blocker-inventory.md` / `role-requirement-matrix-real-e2e-evidence.md` | 正式模型、schema、服务和前端来源缺口已冻结并归属到 M1-M5；按新口径不阻塞 M0 exit |

## Advancement Decision

Gate result: `PASS_ACCEPTED`.

M0 按用户批准的新口径准出。主线程可进入 M1，但只能处理 RRM-BLK-001..007 对应的 activeOrderId 权威来源、schema、迁移预检、跨角色查询和 PQC 来源切换；不得新增 M2-M6 生产代码，不得把后续 AC 标记为 `GREEN`、`ACCEPTED` 或完成。

## Required Resolution After M0

- M1 必须先记录 BDD，再创建或确认 RED，再实现最小正式方案，随后 GREEN、REGRESSION 和适用 E2E。
- RRM-BLK-001..007 必须在 M1 中正式解决；不得用临时夹具、默认值、mock、API-only 或静态合同伪造 GREEN。
- RRM-BLK-008..031 保持 open，继续按 `blocker-inventory.md` 归属到 M2-M5；未到对应里程碑前不得越级实现。
- `task-state.json`、`task.md`、`execution-log.md`、`verification-report.md` 和 `test-report.md` 必须同步记录本次 M0 gate 变更。
