# Execution Log

## User Intent

用户要求运行当前工作区的前后端程序，并指定后端按 `E:\IntRuoyi` 相同的 MySQL 连接方式运行。

## Rule Reads

- 已读取 `docs\task-closeout-rules.md`
- 已读取 `docs\local-runtime.md`
- 已读取 `docs\branch-runtime-ports.md`
- 已读取 `docs\frontend-development.md`
- 已读取 `docs\backend-development.md`
- 已读取 `docs\worktree-restrictions.md`
- 已读取 `docs\powershell-encoding.md`

## Milestone Updates

- 2026-07-24：确认当前路径 `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll` 对应 `int_batch` profile，矩阵端口为前端 `8041`、后端 `48041`。
- 2026-07-24：前端已启动，监听 `8041`，PID `30620`（`node.exe`）；访问 `http://127.0.0.1:8041/` 返回 HTTP `200`。
- 2026-07-24：本机 `127.0.0.1:3306` 的 `mysqld.exe` 拒绝配置中的 `root` 凭据。经比对 `E:\IntRuoyi` 已验证的本机运行记录，后端使用 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro` 与 Docker Redis `127.0.0.1:26379`，保持 `--server.port=48041`。
- 2026-07-24：后端已启动，监听 `48041`，PID `25760`（Java）；启动日志显示数据源初始化、数据库查询和 Quartz 同步均已完成。
- 2026-07-25：已将本机 Docker MySQL 连接方式沉淀到 `docs/local-runtime.md#2026-07-25-本机-docker-mysql-连接门禁`，并在 `docs/experience-index.md` 增加关键词路由，防止后续再次错误连接本机 `3306`。

## Verification Evidence

- `Get-NetTCPConnection -LocalPort 8041 -State Listen`：`0.0.0.0:8041`，PID `30620`，状态 `Listen`。
- `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8041/`：HTTP `200`。
- `Get-NetTCPConnection -LocalPort 48041 -State Listen`：`[::]:48041`，PID `25760`，状态 `Listen`。
- `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48041/actuator/health`：`{"status":"UP"}`。
- 后端日志：`.runtime\20260724-run-int-batch-runtime\backend-23306.out.log` 包含 `项目启动成功！`。
- `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"`：`int-ruoyi-mysql` 映射 `23306->3306`，`int-ruoyi-redis` 映射 `26379->6379`。
- `rg -n "23306|int-ruoyi-mysql|Access denied for user root localhost|start-branch-backend" docs/local-runtime.md docs/experience-index.md`：可定位新增门禁与经验索引路由。

## Blockers

无。服务按本任务目的保持运行，不进入 closeout 流程。
