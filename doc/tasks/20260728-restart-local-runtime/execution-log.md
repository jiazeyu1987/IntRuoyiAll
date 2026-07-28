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
- RED: `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> FAIL, required Docker MySQL container was stopped and E drive bind mount was unavailable to Docker Desktop.
- Mount repair evidence:
  - Windows file exists: `E:\IntRuoyi\IntRuoyiBackend\sql\mysql\ruoyi-vue-pro.sql`.
  - Docker bind pre-repair: `BIND_MISSING`.
  - WSL E drive pre-repair: `/mnt/e/IntRuoyi` missing.
  - Docker Desktop log: `hostPathOfVolume /run/desktop/mnt/host/e/IntRuoyi... failed, skipping bind`.
  - Runtime repair: mounted E drive into Ubuntu WSL and Docker Desktop runtime namespace.
  - Docker bind post-repair: `BIND_OK`.
- Docker dependency recovery:
  - `docker start int-ruoyi-mysql` -> PASS.
  - `int-ruoyi-mysql`: `Up`.
  - `docker-minio-1`: `Up (healthy)`.
- GREEN: `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> PASS, restart command dispatched successfully.
- Backend health verification:
  - `http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`.
  - Listener: port `48081`, PID `42652`.
  - Runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-084231.jar`.
  - Runtime Jar stable: last write `2026-07-28 08:42:27` <= process start `2026-07-28 08:42:52`.
- Frontend HTTP verification:
  - `http://127.0.0.1:8081/` -> HTTP `200`, content length `3562`.
  - Listener: port `8081`, PID `43232`.
- Runner token verification:
  - `git check-ignore -v .runtime/codex-test-runner/runner-token.txt` -> `.gitignore:31:**/.runtime/`.
  - `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q` -> `15 passed in 2.19s`.
  - Runner register probe -> business code `0`, session `59`.
  - Runner heartbeat probe -> business code `0`.
  - DB readback for session `59` -> `ONLINE`, `current_running_count=0`, `heartbeat_age_seconds=1`.
- Experience consolidation:
  - Read `project-experience-consolidation` skill.
  - Existing destination selected: `docs/local-runtime.md`.
  - Added gate: `2026-07-28 Docker Desktop E 盘 bind 挂载门禁`.
  - Added route keyword line in `docs/experience-index.md`.
- Final document checks:
  - UTF-8 read check -> PASS for task docs, `docs/local-runtime.md`, and `docs/experience-index.md`.
  - `git diff --check -- doc/tasks/20260728-restart-local-runtime docs/local-runtime.md docs/experience-index.md` -> PASS; only CRLF normalization warnings.

## Final Status

- Status: completed.
- No fallback, port change, mock datasource, API-only success, or partial success claim was used.
