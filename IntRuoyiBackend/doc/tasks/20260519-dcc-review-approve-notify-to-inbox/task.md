# Task: DCC 审核批准待办改站内信

## Goal

让 DCC 审核/批准节点的待办通知从当前 BPM 短信链路切换为真实站内信链路，确保 DCC 审核人和批准人在任务分配时能收到系统站内信。

## Scope

- 仅改 DCC 审核/批准待办通知。
- 在 BPM 任务分配通知链路中识别 DCC 流程定义 `dcc-controlled-file-approval`。
- DCC 流程任务分配改为调用系统站内信 API。
- DCC 流程实例审批通过、驳回、待办超时通知统一改为站内信。
- 其他 BPM 流程保持现有通知方式不变。
- 补齐 DCC 审核/批准待办站内信模板种子。

## Non-Goals

- 不改前端页面。
- 不改 DCC 下发/培训/作废通知链路。
- 不把所有 BPM 任务分配通知统一改成站内信。
- 不新增 fallback、静默降级或双发短信+站内信。

## Previous Task Check

- Previous same-repository task: `ruoyi-vue-pro/doc/tasks/20260519-dcc-message-job-current-inventory/task.md`
- Status before this task: completed.
- Separate in-progress task observed: `ruoyi-vue-pro/doc/tasks/20260519-showroom-temp-preview-asset-local-verification/task.md`
- Isolation statement: the showroom preview verification task remains in progress but does not share write scope with this BPM/DCC notify slice, so this task can proceed independently.

## Milestones

- [x] M1: Create this task package before production code changes.
- [x] M2: Record BDD scenarios and RED evidence for DCC review/approve inbox delivery.
- [x] M3: Implement minimal DCC-only station-message path and seed updates.
- [x] M4: Run targeted backend tests and evidence validation.
- [x] M5: Run closeout preview, update task records, and prepare the scoped commit.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_notify_sql_scripts.py -q`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-bpm -am "-Dtest=BpmTaskConvertTest,BpmMessageServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am "-Dtest=DccNotifyTemplateSeedTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-review-approve-notify-to-inbox\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-dcc-review-approve-notify-to-inbox --mode preview`

## Current Status

Completed. DCC review/approve task-assigned notifications plus DCC process approve/reject/timeout notifications now use real station messages for `dcc-controlled-file-approval`, non-DCC BPM flows remain on SMS, and targeted verification, closeout preview, and local runtime seed synchronization all passed on 2026-05-20.
