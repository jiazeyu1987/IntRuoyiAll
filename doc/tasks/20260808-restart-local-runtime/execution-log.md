# Execution Log

## User Intent

- 用户要求：重启前后端代码。

## Rule Reads

- 已读取 `docs\task-closeout-rules.md`。
- 已读取 `docs\local-runtime.md`。
- 已读取 `docs\powershell-encoding.md`。
- 已读取 `docs\worktree-restrictions.md`。

## Milestone Evidence

- BDD: 本地前后端重启 -> Given `int_main` 主工作区使用固定端口 `8081/48081`, When 停止可确认归属旧进程并重新启动前后端, Then 前端入口返回 HTTP `200` 且后端 health 返回 `UP`。
- PRECHECK: 端口归属 -> `8081` 旧 PID `51364` 为 `E:\IntRuoyi\IntRuoyiFronted` Vite，`48081` 旧 PID `62116` 为 `E:\IntRuoyi` runtime Jar，均可确认为当前主工作区运行态。
- BLOCKER: 标准 full 重启首次执行 -> FAIL，`int-ruoyi-mysql` 容器未运行，影响后端正式本地依赖启动。
- RECOVERY: 本地依赖容器 -> 启动 `int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1`；随后 `23306`、`26379`、`9000` 端口恢复可访问，MinIO healthy。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> PASS，Maven reactor `BUILD SUCCESS`，`Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`。
- GREEN: runtime wait -> PASS，`FrontendOk=True / HTTP 200`，`BackendOk=True / status=UP`。
- GREEN: runtime owner check -> PASS，`8081` PID `40240` 为 `E:\IntRuoyi\IntRuoyiFronted` Vite，`48081` PID `22900` 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260808-214737.jar`。
- GREEN: runtime Jar immutable check -> PASS，Jar `LastWriteTime=2026-08-08 21:47:35`，Java process `StartTime=2026-08-08 21:47:50`。
- GREEN: final hot HTTP verification -> PASS，frontend `HTTP 200` in `2801 ms`，backend health `UP` in `190 ms`。
- CLOSEOUT: cleanup preview -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
- CLOSEOUT: cleanup apply -> PASS，deleted_paths `<none>`，当前为主 worktree，未执行 merge 或 worktree removal。
- EXPERIENCE: project-experience-consolidation -> PASS，复用现有 `docs\local-runtime.md`，新增 `2026-08-08 标准本地 full 重启依赖容器退出门禁`；更新 `docs\experience-index.md` keyword 路由。
- VERIFY: `rg -n "Required Docker container is not running|本地 full 重启依赖容器退出" docs\experience-index.md docs\local-runtime.md` -> PASS。
- VERIFY: `git diff --check -- docs/local-runtime.md docs/experience-index.md doc/tasks/20260808-restart-local-runtime/...` -> PASS；仅提示 Git 未来可能 CRLF 规范化，无 whitespace error。
- VERIFY: `python -X utf8 -c ...` -> PASS，任务文档与经验文档均 UTF-8 可读。
