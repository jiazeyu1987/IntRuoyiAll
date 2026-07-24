# Task: DCC 审批任务名称乱码修复

## Goal

修复真实 DCC 审批任务列表中 `DCC 审批任务` 列显示 `????` 的问题，确保当前待办与后续新生成待办都能显示可读中文任务名。

## Scope

- 先确认上一条后端任务状态，并在必要时显式阻塞后再启动本任务。
- 在生产修改前创建本任务文档、执行日志与缺陷回归证据。
- 复现真实运行库 `ACT_RU_TASK`、`ACT_HI_TASKINST` 与 DCC BPM 模型资源中的问号任务名。
- 先补可重复执行的失败回归检查，再做最小持久化修复。
- 只修复 DCC `dcc-controlled-file-approval` 流程的任务名称、模型源 XML 与当前部署 XML，不引入 fallback，不改无关 BPM 行为。
- 修复后重启本地后端，验证当前审批任务列表与运行时模型都恢复为可读中文。

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-workorder-erp-bom-garbled-item-fix/task.md`
- Status before this task: blocked by higher-priority live DCC approval-task garbled-name defect.
- Impact: the paused ERP BOM garbled-text task does not block this DCC BPM runtime encoding repair.

## Milestones

- [x] M1: Pause the previous backend task and create this task package.
- [x] M2: Record BDD scenarios and RED runtime evidence for the garbled task-name reproduction.
- [x] M3: Add the minimal tracked repair for DCC task names and BPM XML bytes.
- [x] M4: Apply the runtime fix, restart the backend, and run GREEN verification.
- [x] M5: Update closeout evidence and prepare a task-scoped backend commit.

## Expected Verification

- `python doc/tasks/20260518-dcc-approval-task-name-garbled/scripts/check_dcc_approval_task_names.py`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT t.ID_, t.NAME_, t.TASK_DEF_KEY_ FROM ACT_RU_TASK t JOIN ACT_RE_PROCDEF d ON d.ID_ = t.PROC_DEF_ID_ WHERE d.KEY_ = 'dcc-controlled-file-approval' ORDER BY t.CREATE_TIME_ DESC;"`
- Real frontend path `http://localhost:8081` renders readable Chinese in the `DCC审批任务` list.
- Corrected DCC BPM model source and deployed BPMN bytes no longer contain question-mark placeholders for stage task names.

## Current Status

Completed. The live Flowable runtime names, historical names, model editor source, and latest deployed BPMN XML were all repaired in place, and the local services were restarted against the corrected bytes.

## Blocker And Impact

- Blocker: none currently.

## Final Verification Result

- `python doc/tasks/20260518-dcc-approval-task-name-garbled/scripts/check_dcc_approval_task_names.py` -> PASS
- `cmd /c restart-ruoyi.bat` -> PASS
- `http://127.0.0.1:48081/v3/api-docs` -> PASS (`200`)
- `http://127.0.0.1:8081` -> PASS (`200`)
- Live DCC runtime task sample -> PASS, latest rows now render `文控审核` / `文控批准`
