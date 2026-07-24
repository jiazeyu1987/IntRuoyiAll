# Task: DCC 纸质发放确认后端闭环

## Goal

在 `ruoyi-vue-pro` 的 DCC 后端中补一个最小纸质发放闭环：允许对
`distributionMedium = PAPER` 的分发记录执行“确认纸质发放”，并把该分发记录
状态更新为 `ACKNOWLEDGED`。

## Scope

- 新增纸质发放确认后端服务与控制器入口。
- 只作用于 `PAPER` 分发记录。
- 使用现有 `dcc_controlled_file_distribution.status` 状态流，不新增纸质签收表。
- 保持现有分发详情 contract 基本不变，只新增动作入口。
- 不在本任务中引入纸质份数、签收人、回收等扩展模型。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260517-dcc-distribution-rule-replace-unique-fix/task.md`
- Status before this task: completed for code delivery.
- Impact: medium config/save is green, so this task can focus on the first
  PAPER acknowledgment action.

## Milestones

- [x] M1: Create this backend task package before code edits.
- [x] M2: Record BDD scenarios and RED evidence for missing PAPER acknowledge support.
- [x] M3: Implement the minimal backend acknowledge action.
- [x] M4: Run targeted backend verification and update evidence.
- [ ] M5: Commit only task-scoped files if verification fully passes.

## Expected Verification

- `mvn --% -pl yudao-module-dcc -Dtest=DccPaperDistributionAckServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260517-dcc-paper-distribution-ack-backend\backend-api-evidence.md`

## Current Status

Completed for code delivery. The backend now exposes a dedicated PAPER
distribution acknowledge action and updates the matching distribution row to
`ACKNOWLEDGED` when an authorized user confirms paper distribution.

## Blocker And Impact

- Blocker: a task-scoped backend commit is not yet safe because the repository
  still contains unrelated dirty backend work outside this paper-ack slice.
- Impact: the backend action is implemented and verified, but commit still
  needs a cleaner write set.

## Final Verification Result

- RED:
  - `mvn --% -pl yudao-module-dcc -Dtest=DccPaperDistributionAckServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    -> FAIL initially, because the service class and dedicated error code did
    not exist.
- GREEN:
  - `mvn --% -pl yudao-module-dcc -Dtest=DccPaperDistributionAckServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    -> PASS, 4 tests green.
- Runtime outcome:
  - endpoint path:
    `POST /admin-api/dcc/controlled-files/{id}/paper-distributions/{distributionId}/acknowledge`
  - real browser-backed frontend call later returned backend `code=0`

## Cleanup Keep

- `doc/tasks/20260517-dcc-paper-distribution-ack-backend/backend-api-evidence.md`
