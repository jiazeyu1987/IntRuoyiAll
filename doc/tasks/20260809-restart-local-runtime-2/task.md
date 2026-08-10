# 20260809 Restart Local Runtime 2

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前后端运行态，保持前端端口 `8081`、后端端口 `48081`，并验证服务恢复。

## Milestones

- [x] 读取本地运行、worktree、端口、任务收尾和经验索引规则。
- [x] 检查本地依赖及 `8081`、`48081` 端口占用并确认进程归属。
- [x] 使用项目标准脚本重启前后端。
- [x] 验证前端 HTTP `200`、后端 health `UP` 和监听进程归属。
- [x] 完成任务清理和最终记录。

## Expected Verification

- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `8081`、`48081` 监听 PID 的命令行归属 `E:\IntRuoyi` 主工作区。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目标准重启脚本、固定端口和正式本地依赖。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md`；命中本地 full 重启、依赖容器、固定端口和进程归属门禁。
- 已读取 `docs\local-runtime.md`、`docs\worktree-restrictions.md`、`docs\branch-runtime-ports.md`、`docs\task-closeout-rules.md` 与 `docs\powershell-memory.md`。

## Verification Evidence

- 重启前 `8081` 监听 PID `51912`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 重启前 `48081` 监听 PID `44052`，运行 Jar 和 runtime-control 根目录归属 `E:\IntRuoyi`。
- 本地依赖容器 `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 均为运行状态，所需端口可达。
- 标准重启脚本所需命令、源码目录、前端依赖和下载加密环境变量均已就绪。
- 标准 full 重启脚本 -> PASS；Maven reactor `BUILD SUCCESS`，总耗时 `07:55 min`。
- 重启后前端 `8081` 返回 HTTP `200`，后端 `48081/actuator/health` 返回 `UP`，状态脚本报告 full runtime 为 `running`。
- 重启后 `8081` PID `38056` 归属 `E:\IntRuoyi\IntRuoyiFronted`；`48081` PID `24676` 归属 `E:\IntRuoyi\output\runtime\int_main`。
- 最终复核时 `8081` 当前监听 PID 为 `56568`，仍归属 `E:\IntRuoyi\IntRuoyiFronted`；`48081` 仍为 PID `24676`。
- 后端运行 Jar `backend-runtime-control-20260809-202548.jar` 修改时间早于进程启动时间，不可变检查通过。
- 经验沉淀检查完成；已有 `docs\local-runtime.md` 和经验索引已完整覆盖本次标准重启路径，无新增长期经验规则。
- `task-closeout-cleanup` preview/apply -> PASS；只保留三份核心任务记录，无删除项、阻塞或警告。
