# 执行记录：重启本机后端加载 NAS 转移类别绑定修复

## BDD

BDD: 本机后端加载最新 DCC 修复 -> Given 当前 48081 后端仍运行修复提交前的旧 jar / When 执行本机 backend 重启脚本 / Then 48081 后端应重新打包并启动新的 runtime jar，健康检查返回 UP，运行 jar 时间晚于提交 `2e46186a62`。

BDD: 重启缺少前置条件必须失败 -> Given 本机 Docker、MySQL、Redis、必需环境变量或受保护展厅文件配置缺失 / When 执行重启脚本 / Then 脚本必须明确失败，不用旧后端或降级配置冒充成功。

## Evidence

- ENVIRONMENT: local `int_main` backend, `http://127.0.0.1:48081`, workspace `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`.
- OLD RUNTIME: PID `81232` ran `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260603-180330.jar`.
- TARGET COMMIT: `2e46186a62 2026-06-03 18:19:12 +0800 任务: 校验NAS转移类别目录绑定`.
- DEPLOY COMMAND: `.\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS, exit code `0`.
- ARTIFACT: `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260603-182800.jar`, `LastWriteTime=2026-06-03 18:28:00`, `Length=607961851`.
- GREEN: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/actuator/health` -> PASS, `{"status":"UP"}`.
- GREEN: Java process command line -> PASS, PID `60592` is running `backend-runtime-control-20260603-182800.jar` on port `48081`.
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-nas-transfer-local-backend-restart --mode preview` -> PASS, keep `task.md` / `execution-log.md`, delete `<none>`, blocked `<none>`, warnings `<none>`.
- PIPELINE FILES CHANGED: none.
- REQUIRED SECRETS: existing local DCC download encryption environment variables, local Docker MySQL/Redis/MinIO, and protected showroom file config were checked by the existing restart script.
- MANUAL APPROVALS: user requested `继续` for local backend restart.
- ROLLBACK: rerun the same restart script for latest local build; do not manually start the old jar unless explicitly accepting the old NAS transfer failure behavior.

## Blockers

- none
