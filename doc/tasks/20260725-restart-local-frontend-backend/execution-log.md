# Execution Log

## User Intent

用户要求重启前后端程序。

## Rule Reads

- 读取 `docs/local-runtime.md`。
- 读取 `docs/task-closeout-rules.md`。
- 读取 `docs/branch-runtime-ports.md`。

## Milestone Updates

- 2026-07-25: 已创建任务记录，准备检查经验门禁与端口归属。

## Verification Evidence

待记录。

## Blockers

待记录。

## Restart Evidence 2026-07-25

- `GREEN: experience-preflight -> PASS`，命中 `docs/local-runtime.md` 本地重启路径门禁与本地后端数据库凭据门禁。
- 端口归属确认：旧前端 `8081` PID `30732` 为 `node.exe`，命令行指向 `E:\IntRuoyi\IntRuoyiFronted` Vite env.local。
- 端口归属确认：旧后端 `48081` PID `39380` 为 `java.exe`，命令行使用 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --server.port=48081`。
- 已停止旧 PID：`30732`、`39380`。
- 首次新启动 PID `19672`、`48420` 未监听端口；已按本任务归属停止，并改为带日志、全路径命令重启。
- 最终后端 PID：`52152`，监听 `48081`，health 返回 `UP`。
- 最终前端 PID：`46764`，监听 `8081`，入口返回 HTTP `200`，响应长度 `3458`。
- 项目经验沉淀检查：未新增长期经验；现有 `docs/local-runtime.md` 门禁已覆盖本地重启路径、端口归属与 health 验证要求。
- Cleanup 状态：任务已进入 `ready_for_closeout`；运行日志作为当前运行进程证据列入 `Cleanup Keep`。
## Cleanup Evidence 2026-07-25

- `task-closeout-cleanup preview -> PASS`：keep 包含 task、execution-log、verification-report 与本次运行日志；delete/blocked/warnings 均为 `<none>`。
- `task-closeout-cleanup apply -> PASS`：无删除项，当前为主工作区 `int_main`，非 linked worktree。
- Git closeout：未提交/推送。原因是工作区在本任务开始前已存在大量无关脏改动；本任务不将无关改动混入提交。