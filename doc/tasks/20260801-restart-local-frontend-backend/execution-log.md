# Execution Log

## User Intent

用户要求“重启前后端”。本任务按 `int_main` 主工作区本地运行态处理：前端 `8081`，后端 `48081`。

## Rule Bootstrap

- 读取 `docs/task-closeout-rules.md`。
- 读取 `docs/local-runtime.md`。
- 读取 `docs/worktree-restrictions.md`。
- 读取 `docs/experience-index.md`，命中本地运行态、PowerShell、任务收尾相关门禁。
- 读取 `docs/powershell-memory.md`。
- 读取 `docs/powershell-encoding.md`。

## BDD

- BDD: 重启本地前后端 -> Given `int_main` 主工作区使用固定端口 `8081/48081`，When 停止归属明确的旧本地进程并重新启动前后端，Then 前端首页返回 HTTP `200` 且后端 health 返回 `UP`。

## Git Baseline

- `git status --short --branch` 显示工作区已有大量非本任务改动。本任务不暂存、不提交、不覆盖这些改动，只新增当前任务文档并执行运行态重启。

## Milestone Updates

- 已完成规则读取与任务文档初始化。
- 启动前端口归属：
  - `8081`：PID `14800`，`D:\Programs\node.exe`，命令归属 `E:\IntRuoyi\IntRuoyiFronted` Vite。
  - `48081`：PID `54564`，Java 进程，运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260801-002326.jar`。
- 执行重启命令：`IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 -Component full`。
- GREEN: 标准本地 full 重启脚本 -> PASS，退出码 `0`。
- 重启后端口归属：
  - `8081`：PID `23752`，`D:\Programs\node.exe`，命令归属 `E:\IntRuoyi\IntRuoyiFronted` Vite。
  - `48081`：PID `52620`，Java 进程，运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260801-102211.jar`；数据库参数已脱敏，不写入任务记录。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，`status=UP`。
- GREEN: `Invoke-WebRequest http://127.0.0.1:8081/` -> PASS，HTTP `200`。
- 任务状态更新为 `ready_for_closeout`，等待 cleanup preview/apply 与最终状态处理。
- project-experience-consolidation：搜索 `docs/*memory*.md`、`docs/local-runtime.md`、`docs/powershell-memory.md` 中的本地重启、端口和运行态关键词；现有 `docs/local-runtime.md` 已覆盖本次门禁，没有新增可复用经验需要写入长期文档。
- cleanup preview：`task-closeout-cleanup --task-id 20260801-restart-local-frontend-backend --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 `<none>`。
- cleanup apply：`task-closeout-cleanup --task-id 20260801-restart-local-frontend-backend --mode apply` -> PASS，deleted_paths 为 `<none>`。
- 任务状态更新为 `completed`。
