# Task: DCC 纸质发放确认留痕后端补齐

## Goal

在 DCC 后端的 `PAPER` 发放确认闭环上补齐留痕字段，让每条纸质发放记录可以保存并回显：

- 确认人
- 确认时间

## Scope

- 为 `dcc_controlled_file_distribution` 补充确认留痕字段。
- 在纸质发放确认接口中写入确认人和确认时间。
- 在受控文件详情的分发状态响应中返回这些字段。
- 保持现有 PAPER 允许确认、PUBLIC_FOLDER 禁止确认的行为不变。
- 不在本任务中新增额外的纸质签收表或份数模型。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260517-dcc-paper-distribution-ack-backend/task.md`
- Status before this task: completed for code delivery.
- Impact: the first PAPER acknowledge action already exists, so this task only
  adds audit persistence and read-side exposure.

## Milestones

- [x] M1: Create this backend task package before code edits.
- [x] M2: Record BDD scenarios and RED evidence for missing audit fields.
- [x] M3: Implement schema, service, and response support for audit fields.
- [x] M4: Run targeted backend verification and update evidence.
- [ ] M5: Commit only task-scoped files if verification fully passes.

## Expected Verification

- `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccPaperDistributionAckServiceTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260517-dcc-paper-distribution-audit-fields-backend\backend-api-evidence.md`

## Current Status

Completed for code delivery. The backend now stores `acknowledgedBy` and
`acknowledgedAt` on PAPER distribution rows and returns them in
`distributionStatuses`.

## Blocker And Impact

- Blocker: a task-scoped backend commit is not yet safe because the repository
  still contains unrelated dirty backend work outside this audit-field slice.
- Impact: code and tests are complete, but commit still needs a cleaner write
  set.

## Final Verification Result

- `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccPaperDistributionAckServiceTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 13 tests green.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260517-dcc-paper-distribution-audit-fields-backend\backend-api-evidence.md` -> PASS

## Cleanup Keep

- `doc/tasks/20260517-dcc-paper-distribution-audit-fields-backend/backend-api-evidence.md`
