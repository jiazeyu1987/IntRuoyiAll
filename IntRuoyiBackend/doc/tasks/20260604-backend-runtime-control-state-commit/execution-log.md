# 执行记录：提交后端运行控制台状态

BDD: 后端运行状态可被独立提交 -> Given 后端工作区当前改动集中在 `runtime/runtime-control` / When 执行后端提交 / Then 提交必须只包含运行控制台状态文件与本任务记录。

BDD: 提交门禁必须显式绑定任务目录 -> Given 后端仓库 pre-commit 要求 `TDD_TASK_DIR` / When 未设置该变量提交 / Then 提交必须失败；设置为本任务目录后提交才能通过。

VERIFY: `git status --short --branch` -> PASS，当前后端待提交改动仅包含 `runtime/runtime-control` 状态文件。

VERIFY: `node -e "JSON.parse(...)"` -> PASS，15 个运行状态 JSON 可解析。

RED: `git commit -m "任务: 提交后端运行控制台状态"` -> FAIL, `TDD compliance failed: set TDD_TASK_DIR to the task directory path before committing.`

GREEN: `python .\tool\verify_tdd_compliance.py --task-dir doc/tasks/20260604-backend-runtime-control-state-commit --all-changed` -> PASS, `TDD compliance passed`.

CLEANUP: `task_closeout.py --task-id 20260604-backend-runtime-control-state-commit --mode preview` -> PASS, `ready`, delete `<none>`, blocked `<none>`.
