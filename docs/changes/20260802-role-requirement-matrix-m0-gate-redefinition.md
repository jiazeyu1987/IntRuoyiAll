# Change Request: Role Requirement Matrix M0 Gate Redefinition

## Request Summary and Source

- Date: 2026-08-02
- Source: 用户明确指令：`继续开发，就需要调整门禁口径：M0 只负责“识别并结构化冻结 SOURCE blocker”，不要求在 M0 清零这些需要 M1-M5 正式实现的 blocker`
- Summary: 调整 `岗位需求分解矩阵` 实施任务 M0 准出口径；M0 不再要求清零属于 M1-M5 正式实现范围的 SOURCE blockers。

## Current Baseline Reviewed

- `doc/tasks/20260801-role-requirement-matrix-excel/development-plan.md`
- `doc/tasks/20260801-role-requirement-matrix-excel/test-plan.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/task-state.json`
- `doc/tasks/20260801-role-requirement-matrix-implementation/task.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/execution-log.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/verification-report.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/test-report.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/m0-gate-audit.md`
- `doc/tasks/20260801-role-requirement-matrix-implementation/blocker-inventory.md`
- `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`

## Classification

- Requirement change
- Milestone gate change
- Test acceptance scope clarification

## Impact Analysis

- Product impact: 不改变 62 个 AC 的目标；只改变 M0 的准出判断，避免把后续里程碑的正式实现缺口误算为 M0 未完成。
- Design impact: M0 输出从“清零全部 SOURCE blocker”调整为“识别、结构化、冻结、归属和验证 SOURCE blocker”；M1-M5 仍必须按正式设计解除各自 blocker。
- Data impact: 不新增、删除或修改业务数据；现有本机测试夹具仍只作为 M0 预检证据。
- API impact: 本变更不改 API；M1 开始后才允许按 BDD/TDD 新增或修改 activeOrderId 相关 schema/service/API。
- Test impact: `pnpm e2e:role-requirement-matrix:real:check` 在 M0 可保持 `EXPECTED_BLOCKED`，前提是 blocker 全为已结构化 SOURCE 且 ENV/RUNTIME 为 0；M1-M5 各自 GREEN 仍要求对应 SOURCE blocker 被正式实现清零。
- Release impact: 当前任务仍未完成最终交付；只是允许从 M0 进入 M1。M6 仍负责完整真实 E2E、权限、并发、性能和上线验收。
- Operations impact: 不改变本机端口、运行服务、账号或凭据；继续禁止记录明文密码。

## Decision

Accepted.

## Required Approvals

- 用户已在 2026-08-02 明确批准调整 M0 门禁口径。

## Downstream Skill Reruns

- `readiness-docs-refresh`: 同步任务状态、验证报告、测试报告、blocker 清单和 M0 gate audit。
- `milestone-tdd-delivery`: M0 状态同步后，从 M1 开始按 BDD + 严格 TDD 执行第一个 activeOrderId 切片。

## Blockers and Next Action

- Remaining blockers: 31 个 SOURCE blockers 仍保持 open，但不再阻塞 M0 exit；它们按 `blocker-inventory.md` 归属到 M1-M5。
- Next action: 同步 `development-plan.md`、`task-state.json`、`task.md`、`m0-gate-audit.md`、`test-report.md`、`verification-report.md`、`execution-log.md` 和 `blocker-inventory.md`，然后运行结构验证和 M0 revised gate 验证。
