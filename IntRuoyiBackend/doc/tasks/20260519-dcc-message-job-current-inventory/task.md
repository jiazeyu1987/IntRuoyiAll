# Task: DCC 当前消息任务待补发清单盘点

## Goal

盘点当前本地运行库里 DCC `dcc_controlled_file_message_job` 的 `PENDING` / `FAILED` 消息任务，给出可直接用于补发的 jobId 清单及对应业务上下文。

## Scope

- 仅查询当前本地 `ruoyi-vue-pro` 运行库数据。
- 输出 DCC 消息任务的状态、业务类型、业务编号、接收人和时间信息。
- 尽量补充对应受控文件标题/版本等上下文，方便后续补发。

## Non-Goals

- 不修改任何生产代码。
- 不执行实际补发。
- 不修改数据库数据。
- 不改前端页面。

## Previous Task Check

- Previous same-repository task: `ruoyi-vue-pro/doc/tasks/20260519-dcc-message-job-replay/task.md`
- Status before this task: completed.
- Separate in-progress task observed: `ruoyi-vue-pro/doc/tasks/20260519-showroom-temp-preview-asset-local-verification/task.md`
- Isolation statement: the showroom preview verification task remains in progress but does not share write scope with this DCC data-inventory task, so this read-only inventory can proceed independently.

## Milestones

- [x] M1: Create this task package before inspection work.
- [x] M2: Confirm local runtime database connection and query the DCC message-job table.
- [x] M3: Correlate pending/failed jobs with controlled-file business context.
- [x] M4: Record the inventory result and direct replay candidates.

## Expected Verification

- `SELECT` queries against local MySQL `ruoyi-vue-pro`
- No write SQL executed

## Current Status

Completed. Runtime DCC message-job inventory was queried from local MySQL on `127.0.0.1:23306/ruoyi-vue-pro`, and the pending replay candidate list is recorded below.

## Final Verification Result

- PASS: local datasource verification found the active runnable MySQL instance on `127.0.0.1:23306/ruoyi-vue-pro`
- PASS: read-only query of `dcc_controlled_file_message_job` returned current status distribution and all `PENDING/FAILED` rows
- PASS: no write SQL executed during this task

## Inventory Summary

- Pending replay candidates: `14`
- Failed replay candidates: `0`
- Current breakdown:
  - `PENDING / DISTRIBUTION = 7`
  - `PENDING / TRAINING = 7`

## Direct Replay Candidate Job IDs

- Distribution jobs: `1, 3, 5, 7, 9, 11, 14`
- Training jobs: `2, 4, 6, 8, 10, 12, 13`

## Notes

- Historical backlog is concentrated on one recipient `1567 = 璞霖备用卡`, plus one newer training backlog row for `1 = 瑛泰管理员`.
- The newest controlled file backlog belongs to `2054545668044042274 / DCC-FORMAL-1779159313337-Training / 1.0`.
