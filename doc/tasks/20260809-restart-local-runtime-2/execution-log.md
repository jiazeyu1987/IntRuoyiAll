# Execution Log

## User Intent

- 用户要求：重启前后端。
- 目标范围：`E:\IntRuoyi` 主工作区 `int_main`，前端 `8081`，后端 `48081`。

## Milestone Log

- 2026-08-09：读取本地运行、worktree、端口、PowerShell、任务收尾和经验索引规则。
- 2026-08-09：确认标准脚本为 `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full`。
- 2026-08-09：重启前 `8081` PID `51912` 归属本项目前端，`48081` PID `44052` 归属本项目 runtime Jar，可按同一 profile 旧进程处理。
- 2026-08-09：确认 MySQL、Redis、MinIO 容器运行，`127.0.0.2:23306`、`127.0.0.2:26379`、`127.0.0.1:9000` 可达。
- 2026-08-09：确认 `java`、`mvn`、`pnpm`、`docker`、前端 `node_modules`、后端 POM 和下载加密环境变量均已就绪；未记录任何密钥值。
- 2026-08-09：执行 `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`，Maven reactor `BUILD SUCCESS`，重启命令成功派发。
- 2026-08-09：等待真实 HTTP 就绪，前端返回 `200`、后端 health 返回 `UP`；新监听 PID 与旧 PID 均不同。
- 2026-08-09：执行项目经验沉淀检查；标准重启依赖、固定端口、进程归属和健康检查均已有长期规则覆盖，无需修改或新建经验文档。
- 2026-08-09：将任务状态设为 `ready_for_closeout`，运行 cleanup preview/apply；仅保留三份核心任务记录，无删除项、阻塞或警告。
- 2026-08-09：cleanup 完成后将任务状态更新为 `completed`。

## Behavior/Test Scope

- 本任务只重启本地运行态，不修改生产代码，不适用 BDD 和严格 TDD。
- 验证采用正式固定端口、HTTP 健康检查和监听进程命令行归属检查。

## Command Intent

- 重启前只读检查 Docker 依赖、端口监听 PID、进程命令行和当前 HTTP 状态。
- 仅在 PID 可确认属于 `E:\IntRuoyi` 的 `int_main` 旧运行态时，允许标准脚本停止并重启。

## Verification Evidence

- 重启前状态脚本：前端 HTTP `200`、后端 HTTP `200`、OnlyOffice HTTP `200`。
- Maven：`BUILD SUCCESS`，30 个模块成功，总耗时 `07:55 min`。
- 重启后前端：`http://127.0.0.1:8081/` -> HTTP `200`。
- 重启后后端：`http://127.0.0.1:48081/actuator/health` -> `UP`。
- 状态脚本：`status=running`，前后端均为 `listening`，OnlyOffice HTTP `200`。
- 新进程：前端 PID `38056`；后端 PID `24676`。
- 后端运行 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-202548.jar`，Jar 修改时间早于进程启动时间。
- Cleanup preview/apply：PASS；`delete=<none>`、`blocked=<none>`、`warnings=<none>`。
- 最终复核：前端当前监听 PID `56568`，命令行仍归属 `E:\IntRuoyi\IntRuoyiFronted`；后端监听 PID 保持 `24676`。

## Blockers

- 无。
