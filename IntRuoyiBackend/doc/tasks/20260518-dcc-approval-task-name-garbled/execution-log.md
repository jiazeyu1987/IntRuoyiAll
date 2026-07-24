# Execution Log: DCC 审批任务名称乱码修复

BDD: DCC 审批任务列表必须显示可读中文任务名 -> Given 当前用户打开真实 `DCC审批任务` 页面且存在 DCC 流程待办, When 页面读取 BPM 待办列表中的 `name` 字段, Then `DCC 审批任务` 列必须显示可读中文而不是 `????`.

BDD: DCC BPM 模型源与当前部署 XML 不能继续产生问号任务名 -> Given `dcc-controlled-file-approval` 模型会继续被当前系统用于生成审批待办, When 系统读取模型 editor source 或当前部署 BPMN XML, Then 四个固定阶段任务名必须保持为 `文控审核 / 审核会签 / 批准 / 文控批准`，不得再是 `?` 占位符。

- M1: Completed. Paused `20260518-workorder-erp-bom-garbled-item-fix` due user priority switch and created this backend task package before production changes.
- RED: `python doc/tasks/20260518-dcc-approval-task-name-garbled/scripts/check_dcc_approval_task_names.py` -> FAIL, active tasks, historic tasks, historic user-task activities, the model source, and the latest deployment all contained garbled DCC task names.
- RED: direct DB inspection on `ACT_RU_TASK`, `ACT_HI_TASKINST`, and `ACT_HI_ACTINST` -> FAIL, version `3` DCC task names were persisted as `????` / `??`.
- RED: direct DB inspection on `ACT_RE_MODEL -> ACT_GE_BYTEARRAY` and latest `ACT_RE_PROCDEF -> ACT_GE_BYTEARRAY` -> FAIL, the editor source and deployed BPMN XML still stored garbled user-task labels.
- GREEN: apply `sql/mysql/20260518_dcc_approval_task_name_fix.sql` inside the MySQL container with `utf8mb4` input -> PASS, runtime/historic task names plus BPMN byte streams were repaired in place.
- GREEN: `cmd /c restart-ruoyi.bat` -> PASS, local backend/frontend restarted successfully and Flowable cache refreshed against the repaired BPMN resource.
- GREEN: `python doc/tasks/20260518-dcc-approval-task-name-garbled/scripts/check_dcc_approval_task_names.py` -> PASS, 19 active tasks, 77 historic tasks, 77 historic user-task activities, the model source, and the latest deployment are all readable.

## Root Cause

- The garbled list cell was caused by corrupted runtime BPM metadata, not by the Vue table component.
- DCC process definition version `3` stored question-mark task labels in both the live task tables and the backing Flowable BPMN bytes.
- The Flowable model editor source for `dcc-controlled-file-approval` was corrupted in the same way, so future deployments would have kept reproducing the defect without repairing the source bytes too.

## Fix Summary

- Added `doc/tasks/20260518-dcc-approval-task-name-garbled/scripts/check_dcc_approval_task_names.py` to fail fast when DCC task names or BPMN bytes are still garbled.
- Added `sql/mysql/20260518_dcc_approval_task_name_fix.sql` as the tracked repeatable repair for live task names, historic task names, historic user-task activities, model source bytes, and deployment BPMN bytes.
- Applied the SQL repair to the running local MySQL and restarted the local services so current pages and future task generation both read the corrected metadata.
