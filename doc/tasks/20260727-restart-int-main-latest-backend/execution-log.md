# Execution Log

## User Intent

用户要求确认当前后端是否为融合后的最新版本；若不是，则重启为最新 `int_main` 后端。

## BDD

- `BDD: Latest merged backend runs on int_main port -> Given` 当前 `48081` Jar 来源无法证明等于最新 `origin/int_main`，`When` 从最新远端提交隔离构建并重启，`Then` 运行 Jar 与构建 Jar 哈希一致且 health 为 `UP`。
- `BDD: Existing backend is not stopped before artifact validation -> Given` 当前 `48081` 仍健康，`When` 最新构建尚未通过，`Then` 不停止旧进程。
- `BDD: Unknown process is never killed -> Given` `48081` 被进程占用，`When` 命令行不属于 `E:\IntRuoyi` 的 `int_main` 后端，`Then` fail-fast，不停止进程。

## RED/GREEN Evidence

- `GREEN: experience-preflight -> PASS`，已读取隔离构建 Jar、稳定日志目录、PowerShell Maven 参数和 worktree slot 门禁。
- `RED: runtime provenance check -> FAIL`，当前 PID `61040` 加载主工作区 `target` Jar，但本地 `HEAD` 与 `origin/int_main` 分叉，现有 Jar 缺少可验证的远端提交来源。
- `GREEN: isolated source preparation -> PASS`，构建 worktree `D:\IntRuoyiWorktree\20260727_int_main_latest_backend_runtime` 已登记 slot `6`（`8087/48087`），分支已快进到最新 `origin/int_main` 提交 `70a4b4141187eb282a054d3b70dcbcadca641cc2`，工作树干净。

## Current Evidence

- Current listener: `48081 -> PID 61040`.
- Current command: Java loads `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Current health: `UP`.
- Local Git state: `int_main` contains unrelated concurrent dirty changes and differs from `origin/int_main`; do not build from the main workspace.
- Clean build source: `70a4b4141187eb282a054d3b70dcbcadca641cc2`.

## Blockers

- None. Latest remote source and local build/runtime prerequisites are being verified before restart.
