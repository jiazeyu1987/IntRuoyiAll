# Verification Report

## Result

PASS

## Scope

- `E:\IntRuoyi` 主工作区 `int_main`。
- 前端固定端口 `8081`。
- 后端固定端口 `48081`。

## Preflight

- 旧前端 PID `51912` 的命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 旧后端 PID `44052` 的运行 Jar 和 runtime-control 根目录归属 `E:\IntRuoyi`。
- `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 均为运行状态。
- MySQL `127.0.0.2:23306`、Redis `127.0.0.2:26379`、MinIO `127.0.0.1:9000` 均可达。
- 标准脚本需要的命令、目录、前端依赖和下载加密环境变量均存在；未记录密钥值。

## Restart Evidence

- 命令：`restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`。
- Maven reactor：`BUILD SUCCESS`，30 个模块成功，总耗时 `07:55 min`。
- 脚本结果：`Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`。

## Runtime Verification

- 前端 `http://127.0.0.1:8081/`：HTTP `200`。
- 后端 `http://127.0.0.1:48081/actuator/health`：`status=UP`。
- 标准状态脚本：full runtime `running`，前后端均为 `listening`，OnlyOffice HTTP `200`。
- 新前端 PID `38056`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 新后端 PID `24676`，运行 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-202548.jar`。
- 后端 Jar 修改时间早于 Java 进程启动时间，运行 Jar 不可变检查通过。
- 最终复核时前端当前监听 PID 为 `56568`，命令行仍归属 `E:\IntRuoyi\IntRuoyiFronted`；后端监听 PID 保持 `24676`。

## Experience Consolidation

- 已执行 `project-experience-consolidation` 工作流检查。
- 本次没有新的可复用失败模式或工程约束；已有 `docs\local-runtime.md` 与 `docs\experience-index.md` 已覆盖标准本地重启规则，因此未修改或新建长期经验文档。

## Blockers

- 无。

## Closeout

- `task-closeout-cleanup` preview：`status=ready`，仅保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、阻塞或警告。
- `task-closeout-cleanup` apply：`status=applied`，无删除项、阻塞或警告。
- 最终状态：`completed`。
