# AC-M21 过程检验记录汇集修复执行日志

## User Intent

- 用户要求对 AC-M21「系统汇集过程检验记录」不符合项进行修复。
- 修复重点：从代码层补齐最终确认修订、结构化过程检验记录、任务/轮次/版本追溯、重复排除和跨租户隔离。

## Gate Reads

- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/powershell-encoding.md`。
- 已读取 `docs/backend-development.md`。
- 已读取 `docs/database-rules.md`。
- 已读取 `docs/experience-index.md`，命中 `MES PQC 项目级检验快照门禁`。
- 已读取技能：
  - `backend-api-delivery`
  - `database-schema-delivery`
  - `quality-assurance-test-suite`

## BDD / TDD

- BDD: Approved PQC review creates structured process inspection aggregation -> Given a submitted PQC inspection event with structured item results and a team leader approval, When the approval is completed, Then the system persists process inspection aggregate detail rows traceable to tenant, event, review, task, round, regulation version, item, piece and revision.
- BDD: Non-final or unapproved PQC submissions are excluded -> Given pending, rejected, self-review-blocked, old revision, duplicate, or cross-tenant PQC data, When aggregation runs, Then only the final approved revision is aggregated and all other data is excluded without default success.
- RED: pending.
- GREEN: pending.

## Milestone Updates

- Created task documentation and captured applicable PQC evidence gate.

## Verification Evidence

- Pending.

## Blockers

- Workspace baseline before this task was not clean: `git status --short --branch` reported branch `int_main` ahead of `origin/int_main` by 3 commits and unrelated untracked task directories. Current task will avoid unrelated paths; commit/push completion may need a separate baseline/coordination step.
