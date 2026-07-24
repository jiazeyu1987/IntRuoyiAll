# Task: DCC 历史消息任务补发工具

## Goal

为 DCC 增加一个真实可用的历史消息任务补发工具，让已经留在 `dcc_controlled_file_message_job` 里的 `PENDING` / `FAILED` 任务可以通过后端管理接口重新投递到系统站内信，并回写最新发送状态。

## Scope

- 仅处理 DCC 后端历史消息任务补发。
- 抽取或复用现有 DCC 站内信发送逻辑，避免下发/培训/作废与补发各写一套。
- 提供一个管理端补发入口。
- 支持重放 `DISTRIBUTION` / `TRAINING` / `OBSOLETE` 三类历史消息任务。

## Non-Goals

- 不改前端页面。
- 不修改 `dcc_controlled_file_message_job` 表结构。
- 不补发非 DCC 业务消息。
- 不为历史补发新增 fallback、静默跳过或伪成功返回。

## Previous Task Check

- Previous same-repository task: `ruoyi-vue-pro/doc/tasks/20260519-dcc-notify-message-delivery/task.md`
- Status before this task: completed.
- Separate in-progress task observed: `ruoyi-vue-pro/doc/tasks/20260519-showroom-temp-preview-asset-local-verification/task.md`
- Isolation statement: the showroom preview verification task remains in progress but does not share write scope with `yudao-module-dcc`, so this DCC replay task can proceed independently.

## Milestones

- [x] M1: Create this backend task package before production code changes.
- [x] M2: Record BDD scenarios and RED evidence for missing replay capability.
- [x] M3: Implement minimal replay API and shared DCC message-delivery path.
- [x] M4: Run targeted backend tests and evidence validation.
- [x] M5: Run closeout preview, update task records, and prepare the scoped commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileMessageReplayServiceTest,DccControlledFileTaskActionApiTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileMessageOutboxTest,DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-message-job-replay\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-dcc-message-job-replay --mode preview`

## Current Status

Completed. Replay API, shared DCC message-delivery path reuse, targeted backend tests, evidence validation, and closeout preview all passed on 2026-05-19.

## Final Verification Result

- PASS: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileMessageReplayServiceTest,DccControlledFileTaskActionApiTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileMessageOutboxTest,DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-message-job-replay\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-dcc-message-job-replay --mode preview`
