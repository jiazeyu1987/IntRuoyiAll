# Verification Report

## Result

- PASS：`int_shedule` 已精确收敛到 `origin/int_main`，56 个任务产生的未跟踪 tar 乱码图片已按精确路径清理，后端与前端均保持启动并可访问。

## Git Verification

- Branch: `int_shedule`
- HEAD: `e9eca0b386a7a01b28421084a937792245609d8f`
- Target: `origin/int_main=e9eca0b386a7a01b28421084a937792245609d8f`
- Tracked worktree: `git -c core.safecrlf=false diff --name-only --no-renames` 返回 0 个路径。
- Runtime port guard: PASS，前端 `8021`、后端 `48021`。

## Runtime Verification

- Docker MySQL dependency: `23306` listening.
- Docker Redis dependency: `26379` listening.
- Backend PID: `50472`
- Backend command: current workspace `yudao-server-exec.jar --server.port=48021 --spring.profiles.active=local`
- Backend health: `http://127.0.0.1:48021/actuator/health` -> `UP`
- Frontend PID: `34312`
- Frontend command: current workspace Vite `--mode branch-shedule --port 8021 --strictPort`
- Frontend entry: `http://127.0.0.1:8021/` -> HTTP `200`

## Scope Note

- 本任务是 Git 融合与本地运行操作，没有修改生产业务行为，因此未新增生产代码测试。
- 前后端保持运行；运行日志保留在本任务目录。

## Closeout Verification

- `project-experience-consolidation`: PASS，经验合并到现有 `docs/worktree-memory.md`，并补充 NUL 安全的未跟踪 tar 产物复核与精确路径清理规则。
- `tar-mojibake-artifact-cleanup`: PASS，仅删除 `IntRuoyiBackend/.image/` 下 56 个任务产生的未跟踪乱码副本。
- `task-closeout-cleanup final preview`: PASS，无 delete/blocked/warnings。
- `task-closeout-cleanup final apply`: PASS，核心任务记录与四个运行日志已保留。
