# AC-M11 生产报工数量与损耗边界校验执行日志

## User Intent

- 用户要求继续修复岗位需求矩阵中从系统代码分析发现的不符合项。
- 本轮选择 AC-M11 的窄切片：生产员工正式报工数量与损耗边界校验。

## BDD / TDD

- BDD: 拒绝损耗大于产出 -> Given 生产员工提交产出数量 10 且损耗数量 11, When 后端处理正式生产报工, Then 服务端拒绝提交且不得用 0 合格数量截断后继续生成报工/批记录/过程池事件。
- BDD: 拒绝负数数量 -> Given 生产员工提交负数产出或负数损耗, When 后端处理正式生产报工, Then 服务端拒绝提交并暴露真实校验错误。
- BDD: 合法损耗形成合格数量 -> Given 生产员工提交产出数量 10 且损耗数量 3, When 后端拆分生产报工 payload, Then 合格数量为 7 且损耗数量为 3。

## Gate Evidence

- 2026-08-05: 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/experience-index.md`。
- 2026-08-05: 已读取技能 `bug-regression-fix-loop`、`backend-api-delivery`、`bdd-tdd-acceptance-planner` 及其引用契约。

## Milestone Log

- 2026-08-05: 创建任务目录和最小任务文档，当前状态为 `in_progress`。

## Verification Evidence

- RED: 待执行。
- GREEN: 待执行。

## Blockers

- 暂无当前切片 blocker。共享工作区已有其它任务脏改动，后续不得宽泛暂存、提交或回滚。
