# 20260808 Restart Local Runtime

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前后端运行态，保持前端端口 `8081`、后端端口 `48081`，并验证服务恢复。

## Milestones

- [x] 读取本地运行、worktree、任务记录和 PowerShell 编码规则。
- [x] 检查 `8081`、`48081` 端口占用并确认旧进程归属。
- [x] 停止可确认归属的旧前端/后端进程。
- [x] 启动本项目本地前端和后端。
- [x] 验证前端 HTTP `200`、后端 health `UP`，记录证据。

## Expected Verification

- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 端口监听 PID 的命令行归属 `E:\IntRuoyi` 主工作区。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按项目固定端口和运行规则重启。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md`；本任务命中本地运行、worktree 端口和 PowerShell 编排门禁，已按 `docs\local-runtime.md`、`docs\worktree-restrictions.md`、`docs\powershell-memory.md` 执行。

## Verification Evidence

- `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> PASS，Maven reactor `BUILD SUCCESS`，启动命令已派发。
- 本地依赖容器 `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 已恢复运行，用于正式本地后端依赖。
- 前端 `http://127.0.0.1:8081/` -> HTTP `200`，热态复核耗时 `2801 ms`。
- 后端 `http://127.0.0.1:48081/actuator/health` -> `UP`，热态复核耗时 `190 ms`。
- 后端 PID `22900` 运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260808-214737.jar`，运行 Jar 修改时间早于进程启动时间，Jar 不可变检查通过。
- cleanup preview/apply -> PASS；无删除项、无阻塞，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 经验沉淀 -> PASS；已将依赖容器刚退出的 full 重启门禁合并到 `docs\local-runtime.md`，并在 `docs\experience-index.md` 增加可检索路由。
