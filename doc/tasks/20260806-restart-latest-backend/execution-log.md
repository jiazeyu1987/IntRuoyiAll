# Execution Log

## User Intent

- 用户要求：重启到最新的后端。
- 执行口径：将本机 `int_main` 后端 `48081` 重启为当前 `origin/int_main` 最新提交对应的后端 Jar。

## Preflight Evidence

- Read: `docs/local-runtime.md`。
- Read: `docs/worktree-restrictions.md`。
- Read: `docs/backend-development.md`。
- Read: `docs/task-closeout-rules.md`。
- Read: `docs/powershell-encoding.md`。
- Read: `docs/powershell-memory.md`。
- Current `HEAD` and `origin/int_main`: `e71769ece854c56d911779b44afd5d57247ba9b5`。
- Main workspace has unrelated dirty files from parallel tasks, so this restart will build from a clean detached worktree instead of dirty `E:\IntRuoyi` sources。
- Existing `48081` listener: PID `47520`, command line points to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-active-order-candidate-batch-20260806-213525.jar` with `--server.port=48081` and local profile。

## Execution Evidence

- Build worktree: `D:\IntRuoyiWorktree\restart-latest-backend-20260806` detached from `origin/int_main` at `e71769ece854c56d911779b44afd5d57247ba9b5`.
- Build command: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS, `BUILD SUCCESS`, finished `2026-08-06T22:04:20+08:00`.
- Built Jar: `D:\IntRuoyiWorktree\restart-latest-backend-20260806\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`, SHA256 `CD2F95C0CB7D819E064B5B81ECEBACC32D6428A8C4A767442C6421D5403A980E`.
- Runtime Jar copied to: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-latest-replan-shift-hours-20260806-220545.jar`, SHA256 `CD2F95C0CB7D819E064B5B81ECEBACC32D6428A8C4A767442C6421D5403A980E`.
- Old backend stopped only after rechecking `48081` owner PID `47520` and command line under `E:\IntRuoyi\output\runtime\int_main`.
- New backend started with PID `936`, local profile, `--server.port=48081`, and log file `E:\IntRuoyi\output\runtime\int_main\logs\yudao-server.log`.

## Verification Evidence

- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS, `{"status":"UP"}`.
- `Get-NetTCPConnection -LocalPort 48081 -State Listen` -> PASS, owner PID `936`.
- New PID `936` command line points to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-latest-replan-shift-hours-20260806-220545.jar`.
- `task-closeout-cleanup --mode preview` -> PASS, no delete or blocked items.
- `task-closeout-cleanup --mode apply` -> PASS, no delete or blocked items.
- Temporary build worktree `D:\IntRuoyiWorktree\restart-latest-backend-20260806` removed with `git worktree remove --force`; `Test-Path` verified false.
- Experience consolidation: updated `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁` and `docs/experience-index.md` with the clean detached build plus start-after-build restart rule.

## Blockers

- None.
