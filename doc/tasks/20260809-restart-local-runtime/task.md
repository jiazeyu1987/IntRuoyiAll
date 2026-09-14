# 20260809 Restart Local Runtime

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前后端运行态，保持前端端口 `8081`、后端端口 `48081`，并验证服务恢复。

## Milestones

- [x] 读取本地运行、worktree、任务收尾和经验索引规则。
- [x] 检查本地依赖、`8081`、`48081` 端口占用并确认进程归属。
- [x] 停止可确认归属的旧进程并启动前后端。
- [x] 验证前端 HTTP `200`、后端 health `UP` 和监听进程归属。
- [x] 完成任务清理预览、应用和最终记录。

## Expected Verification

- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `8081`、`48081` 监听 PID 的命令行归属 `E:\IntRuoyi` 主工作区。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目标准脚本、固定端口和正式本地依赖重启。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md`；本任务命中本地 full 重启、依赖容器、固定端口和进程归属门禁，按 `docs\local-runtime.md`、`docs\worktree-restrictions.md` 与 `docs\branch-runtime-ports.md` 执行。

## Verification Evidence

- 重启前 `8081` 监听 PID `40240`，命令路径归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 重启前 `48081` 监听 PID `22900`，运行 Jar 归属 `E:\IntRuoyi\output\runtime\int_main`。
- 本地依赖容器 `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 均为运行状态，所需端口可监听。
- 标准 full 重启脚本 -> PASS；Maven reactor `BUILD SUCCESS`，前后端启动命令已派发。
- Vite 在 `241015 ms` 后就绪；首次冷请求在 `30 s` 超时，待初始化完成后复核 `http://127.0.0.1:8081/` 返回 HTTP `200`，耗时 `4262 ms`。
- `http://127.0.0.1:48081/actuator/health` 返回 `UP`，耗时 `144 ms`。
- 重启后 `8081` PID `25476` 归属本项目前端；`48081` PID `26280` 运行独立 Jar `backend-runtime-control-20260809-003455.jar`。
- 后端运行 Jar 修改时间早于进程启动时间，不可变检查通过。
- 经验沉淀检查完成；本次为已有标准重启路径，未产生需要写入长期经验文档的新规则。
- cleanup preview/apply -> PASS；无删除项、无阻塞和警告，保留三份核心任务记录。
