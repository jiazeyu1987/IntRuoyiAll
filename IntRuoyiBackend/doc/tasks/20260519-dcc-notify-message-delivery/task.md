# Task: DCC 站内信真实发送打通

## Goal

让 DCC 在真实下发/培训链路中不再只写本地 `dcc_controlled_file_message_job` 待发送记录，而是实际调用系统站内信发送 API，把消息真正投递到 `system_notify_message`，并正确回写消息任务状态。

## Scope

- 仅处理 DCC 后端消息投递链路。
- 补齐 DCC 下发/培训站内信模板种子。
- 在 DCC 消息任务创建后立即发送系统站内信并回写 `PENDING / SENT / FAILED`。
- 保持现有 DCC 下发、培训、手动放行、纸质确认的业务边界不变。

## Non-Goals

- 不改前端页面。
- 不新增 fallback、补偿分支或静默降级。
- 不重构 DCC 分发/培训业务模型。
- 不修改 `dcc_controlled_file_message_job` 表结构。

## Previous Task Check

- Previous same-repository task: `ruoyi-vue-pro/doc/tasks/20260519-unify-test-prod-publish-ui/task.md`
- Status before this task: completed.
- Separate in-progress task observed: `ruoyi-vue-pro/doc/tasks/20260519-showroom-temp-preview-asset-local-verification/task.md`
- Isolation statement: the showroom preview verification task is still in progress but does not share write scope with `yudao-module-dcc` or DCC SQL seeds, so this DCC backend task can proceed independently.

## Milestones

- [x] M1: Create this backend task package before production code changes.
- [x] M2: Record BDD scenarios and RED evidence for missing DCC notify delivery.
- [x] M3: Implement minimal DCC notify delivery service path and template seeds.
- [x] M4: Run targeted backend tests and evidence validation.
- [x] M5: Run closeout preview, update task records, and prepare the scoped commit.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_notify_sql_scripts.py -q`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileMessageOutboxTest,DccControlledFilePublicationFlowTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-notify-message-delivery\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-dcc-notify-message-delivery --mode preview`

## Current Status

Completed. DCC notify delivery now sends new distribution/training/obsolete station messages through the real system notify API, updates `dcc_controlled_file_message_job` to `SENT`, and ships the required notify template seeds. Targeted DCC tests, backend evidence validation, and closeout preview all passed on 2026-05-19.
