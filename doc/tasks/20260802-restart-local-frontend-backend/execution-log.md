# 本地主工作区前后端重启执行日志

## User Intent

- 用户要求：重启前后端。
- 目标工作区：`E:\IntRuoyi`。
- 目标端口：前端 `8081`，后端 `48081`。

## Rule And Preflight Evidence

- Read `docs/local-runtime.md` before local runtime restart.
- Read `docs/worktree-restrictions.md` before handling `8081/48081` port ownership.
- Read `docs/task-closeout-rules.md` and `docs/powershell-encoding.md` before task documentation and PowerShell orchestration.
- `scripts\runtime\show-branch-runtime.ps1` -> profile `int_main`, slot `0`, frontend `8081`, backend `48081`.
- Existing frontend listener before restart: PID `34496`, command line points to `E:\IntRuoyi\IntRuoyiFronted\node_modules\.bin\vite`.
- Existing backend listener before restart: PID `29052`, command line points to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081`.
- Preconditions: `IntRuoyiFronted\package.json`, `IntRuoyiFronted\node_modules\.bin\vite.cmd`, `IntRuoyiBackend\pom.xml`, and `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` all exist.

## Restart Log

- Stopped old frontend listener PID `34496` after confirming it pointed to `E:\IntRuoyi\IntRuoyiFronted\node_modules\.bin\vite`.
- Stopped old backend listener PID `29052` after confirming it pointed to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081`.
- Started backend with `scripts\runtime\start-branch-backend.ps1`; new backend listener PID `38348` on `48081`.
- Started frontend with `scripts\runtime\start-branch-frontend.ps1`; new frontend listener PID `12608` on `8081`.
- Runtime logs: `output\runtime\int_main\restart-20260802-local\backend-48081.out.log`, `backend-48081.err.log`, `frontend-8081.out.log`, `frontend-8081.err.log`.

## Verification Evidence

- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`.
- `Invoke-WebRequest http://127.0.0.1:8081/` -> HTTP `200`.
- Listener check after restart: `8081` owned by PID `12608`; `48081` owned by PID `38348`.
- Project experience consolidation: no new durable lesson added; existing `docs/local-runtime.md` and `docs/worktree-restrictions.md` already cover this standard restart path.
- Closeout status before cleanup: `ready_for_closeout`.

## Closeout Evidence

- `task_closeout.py --task-id 20260802-restart-local-frontend-backend --mode preview` -> ready, no delete items, no blockers.
- `task_closeout.py --task-id 20260802-restart-local-frontend-backend --mode apply` -> applied, no delete items, no blockers.
- Final task status: completed.
