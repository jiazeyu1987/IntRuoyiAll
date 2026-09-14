# Execution Log

## User Intent

- 用户要求：启动前后端。
- 用户追加要求：链接 Docker 的 MySQL。

## Rule Reading

- 已读取 `docs/local-runtime.md`。
- 已读取 `docs/database-rules.md`。
- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/branch-runtime-ports.md`。
- 已读取 `docs/experience-index.md`。
- 已读取 `docs/powershell-memory.md`。
- 已读取 `docs/powershell-preflight-lessons.md`。
- 已读取 `docs/powershell-encoding.md`。

## BDD / Runtime Scenario

- `BDD: local int_main services start -> Given int_main uses frontend port 8081 and backend port 48081, When the local frontend and backend are started through project commands, Then the frontend entry and backend health endpoint are reachable on the contracted ports.`
- `BDD: backend connects docker mysql -> Given Docker MySQL int-ruoyi-mysql exposes host port 23306 and contains ruoyi-vue-pro, When backend starts with JDBC URLs pointing to 127.0.0.1:23306, Then actuator health returns UP on 48081.`

## Command Intent Log

- `git -C E:\IntRuoyi status --short --branch`：检查根仓库状态，发现存在其他任务脏改动以及本任务记录改动。
- `docker ps -a --format ...`：确认 Docker MySQL 容器和端口映射。
- `docker exec int-ruoyi-mysql ... SELECT @@port ... INFORMATION_SCHEMA.SCHEMATA ...`：用容器内环境变量验证 MySQL 可登录且业务库存在，未打印实际密码。
- `Start-Process java -jar ... --spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:23306/...`：尝试用 Docker MySQL 端口启动后端。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`：验证后端健康状态。
- `Invoke-WebRequest http://127.0.0.1:8081/`：验证前端入口。
- `rg -n "本地后端数据库凭据|Access denied for user|dynamic-datasource create datasource" ...`：验证经验门禁关键词可定位。
- `git -C E:\IntRuoyi diff --check`：检查文档改动，结果无空白错误，仅有换行转换 warning。

## Milestone Updates

- 规则读取：完成。
- 任务文档：已创建并更新。
- 经验门禁：完成，适用本地重启脚本路径门禁、HTTP 健康检查门禁、本地后端数据库凭据门禁。
- Docker MySQL 检查：完成，容器 `int-ruoyi-mysql` 运行中，宿主机端口 `23306` 映射容器 `3306`，业务库存在。
- 服务启动：完成，前端 `8081` 与后端 `48081` 均在监听。
- 验证：完成，后端健康检查 `UP`，前端入口 `200`。

## Verification Evidence

- `GREEN: experience-preflight -> PASS, applicable gates recorded in task.md`
- `PORT 8081 LISTEN none`
- `PORT 48081 LISTEN none`
- `CHECK backend-jar EXISTS True`
- `CHECK frontend-vite EXISTS True`
- `JAVA C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe`
- `PNPM C:\Users\BJB110\AppData\Roaming\npm\pnpm.ps1`
- `DOCKER MYSQL int-ruoyi-mysql mysql:8.0.39 Up, 0.0.0.0:23306->3306/tcp`
- `DOCKER MYSQL PROBE -> port 3306, database ruoyi-vue-pro exists`
- `PORT 8081 PID 39008 NAME node.exe CMD node "E:\IntRuoyi\IntRuoyiFronted\node_modules\.bin\\..\vite\bin\vite.js" --mode env.local "--host" "127.0.0.1" "--strictPort"`
- `PORT 48081 PID 34940 NAME java.exe CMD ... --spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?... --spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?...`
- `BACKEND_STATUS=UP`
- `FRONTEND_STATUS=200 FRONTEND_LENGTH=3474`
- `GREEN: project-experience-consolidation -> PASS, merged local backend database credential gate into docs/local-runtime.md and routed keywords in docs/experience-index.md`
- `GREEN: git-diff-check -> PASS with line-ending warning only`

## Blockers

- 运行态无阻塞。
- 收尾提交/推送未执行：当前工作区存在其他任务脏改动，不能把 unrelated 前端改动混入本次启动记录。