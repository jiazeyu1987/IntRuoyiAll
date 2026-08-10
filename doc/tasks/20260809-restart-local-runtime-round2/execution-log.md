# Execution Log

## User Intent

- 用户要求：重启前后端。

## Rule Reads

- 已读取 `AGENTS.md`。
- 已读取 `docs\task-closeout-rules.md`。
- 已读取 `docs\local-runtime.md`。
- 已读取 `docs\worktree-restrictions.md`。
- 已读取 `docs\powershell-encoding.md`。
- 已读取 `docs\powershell-memory.md`。
- 已读取 `docs\branch-runtime-ports.md`。
- 已读取 `docs\experience-index.md` 中本地重启相关路由。

## Milestone Evidence

- BDD: 本地前后端重启 -> Given `int_main` 主工作区使用固定端口 `8081/48081`, When 确认旧进程归属后运行标准 full 重启, Then 前端入口返回 HTTP `200` 且后端 health 返回 `UP`。
- PRECHECK: `8081` 旧 PID `25476` 为 `E:\IntRuoyi\IntRuoyiFronted` Vite；`48081` 旧 PID `51896` 为 `E:\IntRuoyi\output\runtime\int_main` 独立运行 Jar，均确认属于当前 `int_main` 运行态。
- PRECHECK: `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 均为运行状态，`23306/26379/9000` 已监听；标准重启脚本、前端 Vite 依赖、后端工程均存在。检查未读取或记录任何凭据值。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> PASS；Maven reactor 30/30 `BUILD SUCCESS`，总耗时 `20:46 min`，输出 `Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`。
- OBSERVATION: Vite 冷启动耗时 `209697 ms`；首次 HTTP 探针在页面冷转换期间等待 `15 s` 后超时，未作为成功证据。
- GREEN: frontend hot verification -> PASS，`http://127.0.0.1:8081/` 返回 HTTP `200`，耗时 `8602 ms`。
- GREEN: backend health -> PASS，`http://127.0.0.1:48081/actuator/health` 返回 `UP`，耗时 `191 ms`。
- GREEN: runtime owner check -> PASS，`8081` PID `51912` 归属 `E:\IntRuoyi\IntRuoyiFronted`；`48081` PID `52880` 归属 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-140430.jar`。
- GREEN: runtime Jar immutable check -> PASS，Jar 最后写入时间 `2026-08-09T14:04:30.1327415+08:00` 早于 Java 进程启动时间 `2026-08-09T14:04:47.8788087+08:00`。
- EXPERIENCE: project-experience-consolidation -> PASS；检索并复用 `docs\local-runtime.md` 与 `docs\experience-index.md` 的现有标准重启门禁，本次未发现新的故障模式或通用规则，不修改长期经验文档。
- CLOSEOUT: cleanup preview -> PASS；保留 `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 `<none>`。
- CLOSEOUT: cleanup apply -> PASS；`deleted_paths` 为 `<none>`，当前为主 worktree，未执行 merge 或 worktree removal。
