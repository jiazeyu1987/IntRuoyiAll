# IntRuoyi Local Runtime Rules

## 触发场景

- 启动、停止、重启或排查本机前端、后端、Vite、Java 服务时，必须先读取本文件。
- 排查 `8081`、`48081` 或其他 worktree 登记端口占用时，必须同时读取 `docs/worktree-restrictions.md`。
- 本文件只约束本机运行态；远端服务器必须读取 `docs/server-access.md`。

## 固定端口

PORT_CONTRACT_VERSION: 2026-07-24-branch-runtime-v1

- `int_main` 前端专属端口：`8081`。
- int_main 后端专属端口：48081。
- int_main 主线本地仓库：D:\ProjectPackage\IntRuoyi\IntRuoyiAll。
- 前端本机入口：`http://127.0.0.1:8081` 或 `http://localhost:8081`。
- 后端健康检查：`http://127.0.0.1:48081/actuator/health`。
- 前端本机模式应使用 `IntRuoyiFronted\.env.local`：
  - `VITE_PORT=8081`
  - `VITE_BASE_URL=http://127.0.0.1:48081`
  - `VITE_PROXY_TARGET=http://127.0.0.1:48081`

## 分支运行端口矩阵

- `int_main`：前端 `8081`，后端 `48081`，对应 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`，保持原始本机默认设置不变。
- `int_batch`：前端 `8041`，后端 `48041`，对应 `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll`。
- `int_shedule`：前端 `8021`，后端 `48021`，对应 `E:\IntRuoyiBranch\Shedule\IntRuoyiAll`。
- `int_qms`：前端 `8061`，后端 `48061`，对应 `E:\IntRuoyiBranch\QMS\IntRuoyiAll`。
- 分支专属前端调试必须通过 `scripts\runtime\start-branch-frontend.ps1` 或对应 `IntRuoyiFronted\.env.branch-*` 模式启动，不得通过改写共享 `.env` 抢占端口。
- 分支专属后端调试必须通过 `scripts\runtime\start-branch-backend.ps1` 传入 `--server.port`，不得把后端 `application-local.yaml` 改成分支端口。
- 合并 `int_main` 或跨分支合并后必须运行 `scripts\preflight\branch-runtime-port-guard.ps1`，确认本矩阵未被覆盖、删除或改回 `8081/48081`。

## 启动前检查

- 启动 `int_main` 前端前，检查 `8081` 占用。
- 启动 `int_main` 后端前，检查 `48081` 占用。
- 启动 `int_batch` 前端/后端前，检查 `8041/48041` 占用。
- 启动 `int_shedule` 前端/后端前，检查 `8021/48021` 占用。
- 启动 `int_qms` 前端/后端前，检查 `8061/48061` 占用。
- 如果端口被当前 `int_main` 旧进程占用，可记录进程 ID、命令行和归属依据后停止对应旧进程，再启动。
- 如果端口被同一 runtime profile 的旧进程占用，可记录进程 ID、命令行和归属依据后停止对应旧进程，再启动。
- 如果端口被未知进程、非 IntRuoyi 进程或其他 runtime profile 占用，必须 fail fast，不得强杀或换端口。
- worktree 必须按 `docs/worktree-restrictions.md` 的 profile + slot 规则使用独立端口。

## 2026-07-25 本机 Docker MySQL 连接门禁

- Trigger: 本机后端启动、`start-branch-backend.ps1`、`application-local.yaml` 指向 `127.0.0.1:3306`、MySQL 报 `Access denied for user 'root'@'localhost'`、需要“按 `E:\IntRuoyi` 相同方式连接 MySQL”。
- Preflight check: 启动后端前先确认 Docker 容器映射存在：`int-ruoyi-mysql` 必须映射 `127.0.0.1:23306 -> 3306`，`int-ruoyi-redis` 必须映射 `127.0.0.1:26379 -> 6379`；再确认目标后端 profile 端口仍按矩阵，例如 `int_batch` 使用 `48041`。
- Required runtime args: 分支后端必须通过显式 JVM 参数沿用 `E:\IntRuoyi` 的本机 Docker 运行方式，覆盖 master/slave datasource 到 `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?...`，并覆盖 Redis 到 `127.0.0.1:26379`；不得为了解决认证失败改写共享 `application-local.yaml`。
- Credential handling: MySQL 用户和密码只从既有本地运行脚本或容器环境读取；任务日志、经验文档和命令记录必须脱敏密码，不得新增明文凭据。
- Blocker: 如果 `23306/26379` 容器映射不存在、Docker 容器未运行、数据库名不是 `ruoyi-vue-pro`、或健康检查仍失败，必须停止并记录缺失前置条件；不得回退到本机 `3306`、猜测 root 密码、静默换端口或跳过后端。
- Verification: 记录 `docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"` 的 MySQL/Redis 映射、后端监听 PID、`http://127.0.0.1:<backendPort>/actuator/health` 返回 `{"status":"UP"}`、以及启动日志中的 `项目启动成功！`。
- Forbidden action: 禁止把 `127.0.0.1:3306` 认证失败当成服务不可启动的最终结论；必须先核对 `E:\IntRuoyi` 的 Docker MySQL 运行路径并显式使用 `23306/26379`。禁止通过改共享配置、假成功、API-only 替代健康检查或隐藏 datasource 异常来绕过。
- Evidence: `doc/tasks/20260724-run-int-batch-runtime/verification-report.md`。


## 2026-07-25 分支本地运行复用 Docker 依赖门禁

- Trigger: `int_batch`、`int_shedule`、`int_qms` 等分支工作区启动后端时出现本机 MySQL/Redis 认证或连接失败。
- Preflight check: 先确认用户授权的本地 Docker 依赖端口，再只修改该分支工作区的依赖连接配置；服务运行端口仍必须由分支脚本和端口矩阵控制。
- Current local Docker dependency convention: MySQL `127.0.0.1:23306/ruoyi-vue-pro`，Redis `127.0.0.1:26379`。
- Blocker: Docker MySQL/Redis 端口未监听、认证失败或 schema 不匹配时必须 fail fast，不得切换数据库、换端口、mock 成功或跳过后端启动。
- Verification: 记录 Docker 依赖端口监听、后端分支端口监听、`/actuator/health` HTTP 状态和前端入口 HTTP 状态。
- Forbidden action: 禁止为了复用 Docker 依赖而修改分支前端/后端服务端口；禁止打印或记录数据库密码、容器完整 env 或 secret-bearing 命令输出。


## 2026-07-24 本地重启脚本路径门禁

- Trigger: 本地重启、E2E 复验、`restart-int-ruoyi-local.ps1`、`Missing int_main frontend path`、`yudao-ui-admin-vue3`、`IntRuoyiFronted`。
- Preflight check: 执行本地重启脚本前，确认脚本解析出的前端根目录与本项目规则一致，当前主工作区前端根目录必须是 `E:\IntRuoyi\IntRuoyiFronted`。
- Blocker: 脚本报 `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3` 时必须停止该脚本路径，记录失败；不得通过新建同名目录、软链、换端口或静默跳过前端路径检查继续。
- Verification: 记录脚本失败文本、端口归属 PID、`mvn.cmd -pl yudao-server -am -DskipTests package` 结果、重启后 `http://127.0.0.1:48081/actuator/health` 状态。
- Forbidden action: 禁止为了绕过脚本硬编码路径创建 `yudao-ui-admin-vue3` 假目录、修改端口、强杀未知进程或把 API-only 验证冒充 E2E。
- Evidence: `doc/tasks/fix-batch-exec-last-update-created-time/verification-report.md`。

## 2026-07-25 分支本地运行复用 Docker 依赖门禁

- Trigger: `int_batch`、`int_shedule`、`int_qms` 等分支工作区启动后端时出现本机 MySQL/Redis 认证或连接失败。
- Preflight check: 先确认用户授权的本地 Docker 依赖端口，再只修改该分支工作区的依赖连接配置；服务运行端口仍必须由分支脚本和端口矩阵控制。
- Current local Docker dependency convention: MySQL `127.0.0.1:23306/ruoyi-vue-pro`，Redis `127.0.0.1:26379`。
- Blocker: Docker MySQL/Redis 端口未监听、认证失败或 schema 不匹配时必须 fail fast，不得切换数据库、换端口、mock 成功或跳过后端启动。
- Verification: 记录 Docker 依赖端口监听、后端分支端口监听、`/actuator/health` HTTP 状态和前端入口 HTTP 状态。
- Forbidden action: 禁止为了复用 Docker 依赖而修改分支前端/后端服务端口；禁止打印或记录数据库密码、容器完整 env 或 secret-bearing 命令输出。
## 禁止做法

- 禁止把 `int_main` 改到随机端口启动。
- 禁止非 `int_main` 使用 `8081/48081`。
- 禁止把 `int_batch`、`int_shedule` 或 `int_qms` 的分支端口写入 `int_main` 默认配置。
- 禁止通过修改共享 `.env` 或 `application-local.yaml` 来实现分支端口。
- 禁止端口占用时静默换端口、静默跳过服务或宣称启动成功。
- 禁止停止无法确认归属的进程。

## 验证方式

- 记录端口监听检查结果。
- 记录启动命令、工作目录、端口和进程 ID。
- 前端启动后验证 `http://127.0.0.1:8081/`。
- 后端启动后验证 `http://127.0.0.1:48081/actuator/health`。
- 分支启动后验证对应 profile 的前端入口和后端健康检查，例如 `int_batch` 使用 `http://127.0.0.1:8041/` 与 `http://127.0.0.1:48041/actuator/health`。
