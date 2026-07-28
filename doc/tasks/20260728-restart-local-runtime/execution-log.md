# Execution Log

## Intent

- User request: 重启前后端。
- Scope: 本地 `E:\IntRuoyi` 主工作区 `int_main` 前端 `8081` 与后端 `48081`。
- Ownership: 仅本次任务目录和本次重启产生的运行证据；不修改已有并行任务文件。

## Rule Reads

- `docs/task-closeout-rules.md` -> PASS。
- `docs/local-runtime.md` -> PASS。
- `docs/worktree-restrictions.md` -> PASS。
- `docs/experience-index.md` -> PASS。
- `docs/powershell-memory.md` -> PASS。
- `docs/powershell-encoding.md` -> PASS。

## Git Baseline

- `git status --short --branch` before task docs:
  - `## int_main...origin/int_main`
  - `A  doc/tasks/20260728-commit-current-code/execution-log.md`
  - `A  doc/tasks/20260728-commit-current-code/task.md`
- Decision: existing staged task files are outside this restart task and will not be modified.

## BDD

- BDD: Restart local frontend and backend -> Given the `int_main` local runtime uses ports `8081/48081`, When the standard local restart script runs for `full`, Then old same-profile processes are replaced and both frontend and backend respond on their fixed ports.

## Execution Evidence

- Port ownership before restart:
  - `8081`: not listening.
  - `48081`: not listening.
- Restart command result:
  - Command: `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
  - Exit: FAIL.
  - Failure summary: `Required Docker container is not running: int-ruoyi-mysql`.
- Docker dependency read-only check:
  - `int-ruoyi-mysql`: `Exited (255) 35 minutes ago`.
- BLOCKER: local-runtime-preflight -> Required local Docker MySQL container is stopped; backend cannot start without the formal local datasource dependency.
- Backend health verification: BLOCKED by missing Docker MySQL runtime dependency.
- Frontend HTTP verification: BLOCKED because standard full script stopped before frontend dispatch after backend preflight failed.

## Final Status

- Status: blocked.
- No fallback, port change, mock datasource, API-only success, or partial success claim was used.
