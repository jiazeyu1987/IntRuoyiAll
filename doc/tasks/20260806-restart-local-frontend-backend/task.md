# 重启本机前后端

## Task Goal

重启当前 `int_main` 工作区的本地前端和后端，保持前端 `8081`、后端 `48081` 端口不变，并确认两条本地入口恢复可用。

## Milestones

- [x] 建立任务记录并完成本地运行态、进程归属和脚本参数预检。
- [x] 停止本工作区归属的旧前端和后端进程。
- [x] 使用标准本地配置启动前端和后端。
- [x] 验证前端 HTTP `200`、后端 health `UP`，并记录新 PID 与日志。
- [x] 完成任务记录、经验门禁复核和收尾状态更新。
- [x] 完成 task-closeout cleanup preview/apply，确认无任务附属产物需要删除。

## Expected Verification

- `Get-NetTCPConnection -LocalPort 8081,48081 -State Listen`
- `Invoke-WebRequest http://127.0.0.1:8081/`
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`
- 新监听 PID 的命令行分别归属 `E:\IntRuoyi\IntRuoyiFronted` 与 `E:\IntRuoyi\output\runtime\int_main` 或本工作区标准运行路径。

## Applicable Gates

- `docs/local-runtime.md`：`int_main` 固定使用 `8081/48081`，只可停止确认归属的旧进程。
- `docs/worktree-restrictions.md`：不得影响其他 profile、worktree 或未知进程。
- `docs/powershell-memory.md`：PowerShell 命令逐条执行并检查退出码，避免串联命令掩盖失败。
- `docs/task-closeout-rules.md`：完成后先标记 `ready_for_closeout`，再执行 cleanup preview/apply。
- `docs/experience-index.md`：复用本地运行重启和日志归属相关经验门禁。

## Current Status

completed

本机 `int_main` 前端和后端已按固定端口重启并完成核心运行态验证；task-closeout cleanup preview/apply 均通过，未删除任何任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只重启本工作区标准前后端。
- `是否从根因和长期维护角度解决`：是；使用项目标准启动入口和固定端口，不改共享配置。
- `是否存在临时补丁或绕过`：否。
