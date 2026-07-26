# IntRuoyi Local Runtime Rules

## 触发场景

- 启动、停止、重启或排查本机前端、后端、Vite、Java 服务时，必须先读取本文件。
- 排查 `8081`、`48081` 或其他 worktree 登记端口占用时，必须同时读取 `docs/worktree-restrictions.md`。
- 本文件只约束本机运行态；远端服务器必须读取 `docs/server-access.md`。

## 固定端口

PORT_CONTRACT_VERSION: 2026-07-24-branch-runtime-v2

- `int_main` 前端专属端口：`8081`。
- int_main 后端专属端口：48081。
- int_main 默认本地仓库：E:\IntRuoyi。
- 前端本机入口：`http://127.0.0.1:8081` 或 `http://localhost:8081`。
- 后端健康检查：`http://127.0.0.1:48081/actuator/health`。
- 前端本机模式应使用 `IntRuoyiFronted\.env.local`：
  - `VITE_PORT=8081`
  - `VITE_BASE_URL=http://127.0.0.1:48081`
  - `VITE_PROXY_TARGET=http://127.0.0.1:48081`

## 分支运行端口矩阵

- `int_main_d`：前端 `8101`，后端 `48101`，对应 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`。
- `int_main`：前端 `8081`，后端 `48081`，对应 `E:\IntRuoyi`，保持原始本机默认设置不变。
- `int_batch`：前端 `8041`，后端 `48041`，对应 `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll`。
- `int_shedule`：前端 `8021`，后端 `48021`，对应 `E:\IntRuoyiBranch\Shedule\IntRuoyiAll`。
- `int_qms`：前端 `8061`，后端 `48061`，对应 `E:\IntRuoyiBranch\QMS\IntRuoyiAll`。
- 分支专属前端调试必须通过 `scripts\runtime\start-branch-frontend.ps1` 或对应 `IntRuoyiFronted\.env.branch-*` 模式启动，不得通过改写共享 `.env` 抢占端口。
- 分支专属后端调试必须通过 `scripts\runtime\start-branch-backend.ps1` 传入 `--server.port`，不得把后端 `application-local.yaml` 改成分支端口。
- 合并 `int_main` 或跨分支合并后必须运行 `scripts\preflight\branch-runtime-port-guard.ps1`，确认本矩阵未被覆盖、删除或改回 `8081/48081`。

## D Main Independent Runtime

- `int_main_d` is bound to `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`.
- Its fixed ports are frontend `8101` and backend `48101`.
- D-Main must never use `8081/48081`, which remain reserved for `E:\IntRuoyi`.

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

## 2026-07-24 本地重启脚本路径门禁

- Trigger: 本地重启、E2E 复验、`restart-int-ruoyi-local.ps1`、`Missing int_main frontend path`、`yudao-ui-admin-vue3`、`IntRuoyiFronted`。
- Preflight check: 执行本地重启脚本前，确认脚本解析出的前端根目录与本项目规则一致，当前主工作区前端根目录必须是 `E:\IntRuoyi\IntRuoyiFronted`。
- Blocker: 脚本报 `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3` 时必须停止该脚本路径，记录失败；不得通过新建同名目录、软链、换端口或静默跳过前端路径检查继续。
- Verification: 记录脚本失败文本、端口归属 PID、`mvn.cmd -pl yudao-server -am -DskipTests package` 结果、重启后 `http://127.0.0.1:48081/actuator/health` 状态。
- Forbidden action: 禁止为了绕过脚本硬编码路径创建 `yudao-ui-admin-vue3` 假目录、修改端口、强杀未知进程或把 API-only 验证冒充 E2E。
- Evidence: `doc/tasks/fix-batch-exec-last-update-created-time/verification-report.md`。

## 2026-07-24 隔离构建 Jar 加载门禁

- Trigger: 主工作区存在并行脏改动，但需要把本任务后端修复加载到 `int_main` 的 `48081` 做真实 E2E；或页面仍提示 `请求地址不存在:<接口>`，怀疑运行中 Jar 未加载新 Controller。
- Preflight check: 先确认 `48081` 监听 PID 的命令行属于预期源码或运行时 worktree、端口为 `48081`、`repo-root` 指向本项目；同时确认新 Jar 来自本次任务已验证的构建产物。若 `48081` 实际运行的是 `D:\IntRuoyiWorktree\...` 下的 runtime jar，必须在该 runtime worktree 内补齐源码、测试、schema 夹具并重建该 Jar，不能只检查 `E:\IntRuoyi` 主工作区源码。
- Blocker: 如果 PID 归属不明、Jar 来源不明、目标 Jar 哈希与隔离构建 Jar 不一致，或主工作区源码混有其他任务改动，必须停止，不得从脏主工作区重新打包冒充本任务运行态。
- Verification: 记录旧 PID、停止依据、新 PID、Jar SHA256、启动命令、`http://127.0.0.1:48081/actuator/health`、登录态目标接口业务响应、必要 schema 字段核对，并在 E2E 后记录真实数据库状态。
- Route check: 目标接口需要登录时，未登录请求返回 `401` 只能证明安全过滤器生效，不能证明 MVC 路由已加载；必须使用本机登录态请求目标接口，业务码为 `0` 或预期业务错误，才可宣称新 Controller 已进入运行态。
- Forbidden action: 禁止强杀未知进程、随机换端口、用主工作区脏源码重新构建、只看 health 或未登录 `401` 就宣称修复已加载。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/verification-report.md`。

## 2026-07-25 本地后端数据库凭据门禁

- Trigger: 启动 `int_main` 本地后端、`48081` 未监听、日志出现 `dynamic-datasource create datasource named [master] error` 或 `Access denied for user 'root'@'localhost'`。
- Preflight check: 启动后端前确认本地 MySQL `127.0.0.1:3306` 与 `application-local.yaml` 中的正式本地数据源配置一致；如果只做启动验证，可先启动并用日志判定真实失败原因，但不得改端口或切换数据源。
- Blocker: MySQL 拒绝当前配置账号、数据库不可达、或后端无法创建 `master` 数据源时，必须停止后端启动结论，不得声明 `48081` 已成功运行。
- Verification: 数据库前置条件修复后重新启动后端，记录 `48081` PID、命令行归属 `E:\IntRuoyi\IntRuoyiBackend`，并用 `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 断言 `status=UP`。
- Forbidden action: 禁止静默换端口、临时改 `application-local.yaml` 凭据、切换到 mock/空数据源、只启动前端就宣称前后端完成。
- Evidence: `doc/tasks/20260725-start-local-frontend-backend/verification-report.md`。

## 2026-07-25 分支本地运行复用 Docker 依赖门禁

- Trigger: `int_batch`、`int_shedule`、`int_qms` 等分支工作区启动后端时出现本机 MySQL/Redis 认证或连接失败。
- Preflight check: 先确认用户授权的本地 Docker 依赖端口，再只修改该分支工作区的依赖连接配置；服务运行端口仍必须由分支脚本和端口矩阵控制。
- Current local Docker dependency convention: MySQL `127.0.0.1:23306/ruoyi-vue-pro`，Redis `127.0.0.1:26379`。
- Blocker: Docker MySQL/Redis 端口未监听、认证失败或 schema 不匹配时必须 fail fast，不得切换数据库、换端口、mock 成功或跳过后端启动。
- Verification: 记录 Docker 依赖端口监听、后端分支端口监听、`/actuator/health` HTTP 状态和前端入口 HTTP 状态。
- Forbidden action: 禁止为了复用 Docker 依赖而修改分支前端/后端服务端口；禁止打印或记录数据库密码、容器完整 env 或 secret-bearing 命令输出。

## 2026-07-25 D-Main 本地启动源码与依赖门禁

- Trigger: D-Main 本地启动、`int_main_d`、`8101/48101`、`vite command not found`、Java 包名包含 `runtime`、后端打包提示 `*.runtime不存在`。
- Preflight check: 后端打包前先确认被引用的 `runtime` Java 包未被 `.gitignore` 的 `**/runtime/` 误忽略；若同源工作区存在正式实现，必须同步正式源码并用 `git check-ignore -v` 记录忽略来源，提交时对合法源码使用 `git add -f`。前端启动前确认 `IntRuoyiFronted/node_modules/.bin/vite` 存在；缺失时执行 `pnpm install --frozen-lockfile`。
- Blocker: 缺失源码只能用同源正式实现补齐；若找不到正式实现或 `pnpm install --frozen-lockfile` 修改 lockfile/失败，必须阻塞，不得造空实现、改用旧 Jar、换端口或跳过前端。
- Verification: 记录 Maven RED/GREEN、`yudao-server-exec.jar` 生成结果、`git check-ignore -v` 输出、`pnpm install --frozen-lockfile` 退出码、后端 health `UP` 和前端 HTTP `200`。
- Forbidden action: 禁止因为目录名是 `runtime` 就放任合法 Java 源码被忽略；禁止复制 `node_modules`、复用旧 Jar、改共享 `.env`/`application-local.yaml`、API-only 冒充前端启动成功。
- Evidence: `doc/tasks/20260725-start-d-main-runtime/verification-report.md`。
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
