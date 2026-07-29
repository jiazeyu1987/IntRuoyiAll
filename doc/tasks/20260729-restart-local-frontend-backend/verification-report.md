# Verification Report

## Current Result

运行态验证 PASS；仓库收尾 BLOCKED。

## Runtime Evidence

- Frontend: `http://127.0.0.1:8081/` -> HTTP `200`。
- Backend: `http://127.0.0.1:48081/actuator/health` -> `status=UP`。
- Frontend PID: `39032`，归属 `E:\IntRuoyi\IntRuoyiFronted`。
- Backend PID: `38652`，归属 `E:\IntRuoyi\output\runtime\int_main`。
- Runtime Jar: `backend-runtime-control-20260729-081633.jar`。
- Runtime Jar SHA256: `1196e73c97cfce80694f21d918cfcf7d63f324e654967aa4bb355531a8c73beb`。
- Old PID `9040/52824` no longer exists。

## Tokenless Runner Evidence

- Restart script parser PASS。
- Runtime-control pytest: `15 passed`。
- Runner frontend static contracts PASS。
- Tokenless Runner Maven tests: `2 tests`, `BUILD SUCCESS`。
- Runner PID `32292` is the workspace `codex-test-runner.mjs --loop` process and has no token argument。
- Latest Runner session: id `73`, `ONLINE`, `current_running_count=0`, heartbeat age `4s` after waiting more than one heartbeat period。

## Closeout Blocker

并行 worktree 融合任务正在共享 `int_main` 上执行 merge，索引包含不属于本任务的大量暂存内容，且本地领先 `origin/int_main` 22 个提交。为避免修改、提交或推送并行任务内容，本任务不介入该 merge，保持 `ready_for_closeout`。Cleanup preview/apply 均已通过，无删除项。
