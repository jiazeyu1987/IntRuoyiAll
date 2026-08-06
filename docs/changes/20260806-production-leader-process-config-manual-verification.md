# Change Request: Production Leader Process Config Manual Verification

## Request Summary and Source

- Date: 2026-08-06
- Source: 用户明确指令：`不用E2E,直接合并到主代码,我手动验证`
- Summary: 将生产组长工序配置统一表任务的最终完成门禁从“必须通过真实 Playwright 写入型 E2E”调整为“目标自动化回归通过后合并，由用户在主代码中手动验证真实页面路径”。

## Current Baseline Reviewed

- `doc/tasks/20260805-production-leader-process-config-unification/prd.md`
- `doc/tasks/20260805-production-leader-process-config-unification/test-plan.md`
- `doc/tasks/20260805-production-leader-process-config-unification/task-state.json`
- `doc/tasks/20260805-production-leader-process-config-unification/task.md`
- `doc/tasks/20260805-production-leader-process-config-unification/execution-log.md`
- `doc/tasks/20260805-production-leader-process-config-unification/test-report.md`
- `doc/tasks/20260805-production-leader-process-config-unification/verification-report.md`
- `doc/tasks/20260805-production-leader-process-config-unification/evidence/real-browser/result.json`

## Classification

- Requirement change
- Test acceptance scope change
- Release decision

## Impact Analysis

- Product impact: 不改变统一工序配置表、损耗、设备映射、参数上下限、目标值或实际平均值的业务实现口径。
- Design impact: 保留正式 `routeProcessId` 主线、后端约束、前端统一入口和无 fallback 设计；不新增兼容分支。
- Data impact: 不新增真实 E2E 写入测试数据；用户手动验证时自行选择测试数据并承担清理责任。
- API impact: 不改变 API 合同；已实现的统一列表、设备绑定、参数保存和统计接口继续作为正式合并内容。
- Test impact: 合并前保留数据库迁移、目标 Maven `-am`、前端类型检查、静态合同、真实 E2E 脚本语法和 `git diff --check`；取消真实 Playwright 写入路径、截图、trace 和样本数据闭环作为合并前门禁。
- Release impact: 允许在目标自动化验证通过后合并到 `int_main`；真实页面风险转由用户在主代码手动验收。
- Operations impact: 不启动本机 slot 4 运行态，不写入远端环境，不切换端口，不使用 API-only 冒充真实 E2E 通过。

## Decision

Accepted.

## Required Approvals

- 用户已在 2026-08-06 明确批准取消 E2E 完成门禁并直接合并到主代码。

## Downstream Skill Reruns

- `spec-driven-delivery`: 同步 PRD、test-plan、task-state、test-report 和 completion gate。
- `task-closeout-cleanup`: 在任务进入 `ready_for_closeout` 后执行 preview/apply，保留核心任务记录。
- `project-experience-consolidation`: 本轮无新增长期经验；上一轮已沉淀状态脚本串行写入门禁。

## Blockers and Next Action

- Remaining blockers: 无合并前 E2E blocker；真实 Playwright 写入型验证由用户手动执行，不再阻塞本任务合并。
- Next action: 更新任务合同和验证报告，重跑保留的目标自动化门禁，提交、推送并按 fast-forward 规则合并到 `int_main`。
