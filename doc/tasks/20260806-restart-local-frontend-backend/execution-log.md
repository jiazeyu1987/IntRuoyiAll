# Execution Log

## User Intent

- 用户要求：重启前后端。
- 执行口径：仅重启当前 `E:\IntRuoyi` 的 `int_main` 本地前端 `8081` 和后端 `48081`，不触碰其他 profile、worktree 或共享依赖。

## Preflight Evidence

- 已读取：`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/branch-runtime-ports.md`。
- 当前分支：`int_main`；当前分支未领先 `origin/int_main`。
- 端口预检：`8081` 由 PID `21760` 监听，`48081` 由 PID `44100` 监听；待补充完整命令行归属核验。

## BDD

- `本地前后端重启后可访问 -> Given 当前 int_main 端口由本工作区旧进程占用, When 停止旧进程并按标准配置启动前后端, Then 8081 返回 HTTP 200 且 48081 health 状态为 UP`

## RED/GREEN Evidence

- `RED: 待执行 -> FAIL, 重启前验证用于确认旧运行态需要被替换`
- `GREEN: 待执行 -> PASS`

## Milestone Updates

- 任务记录已建立；标准重启脚本参数和旧进程命令行核验待完成。

## Blockers

- None recorded.
