# 20260725 Start Local Frontend Backend

## Task Goal

启动本地 `int_main` 前端和后端服务，使用项目约定端口：前端 `8081`，后端 `48081`；后端连接 Docker MySQL。

## Milestones

- [x] 读取本地运行、端口和收尾规则
- [x] 检查任务文档与经验门禁
- [x] 检查 `8081/48081` 端口归属
- [x] 验证 Docker MySQL 容器和业务库
- [x] 启动后端与前端服务
- [x] 验证前端入口与后端健康检查

## Expected Verification

- Docker MySQL 容器 `int-ruoyi-mysql` 正在运行并映射宿主机端口 `23306`。
- Docker MySQL 内存在业务库 `ruoyi-vue-pro`。
- `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- `http://127.0.0.1:8081/` 返回前端页面。
- 记录启动命令、工作目录、端口、PID 和验证结果。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务使用 Docker MySQL 真实容器端口启动后端，不改写共享配置、不换运行端口。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 本地重启脚本路径门禁

- Trigger: 本地运行、重启或检查 `IntRuoyiFronted` 前端路径。
- Preflight check: 当前主工作区前端根目录必须是 `E:\IntRuoyi\IntRuoyiFronted`，不得使用旧路径 `E:\IntRuoyi\yudao-ui-admin-vue3`。
- Blocker: 若脚本报 `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3`，必须停止该脚本路径。
- Verification: 本次未调用本地重启脚本；前端启动命令在 `E:\IntRuoyi\IntRuoyiFronted` 下执行。
- Forbidden action: 不新建假目录、不改端口、不静默跳过前端路径检查。
- Evidence: `docs\local-runtime.md#2026-07-24-本地重启脚本路径门禁`。

### HTTP 健康检查与进程路径扫描门禁

- Trigger: PowerShell 本地 HTTP 健康检查与端口进程扫描。
- Preflight check: 后端 JSON 健康接口使用 `Invoke-RestMethod` 并断言结构化 `status`；进程扫描记录 PID 和命令行。
- Blocker: 健康状态不是 `UP`、前端 HTTP 状态不是 `200`、或端口被非主工作区进程占用。
- Verification: 本次后端 `BACKEND_STATUS=UP`，前端 `FRONTEND_STATUS=200`。
- Forbidden action: 不用端口替换、API-only 冒充前端验证或字节内容字符串误判。
- Evidence: `docs\powershell-preflight-lessons.md#2026-07-10 HTTP 健康检查与进程路径扫描门禁`。

### 本地后端数据库凭据门禁

- Trigger: 启动 `int_main` 本地后端、`48081` 未监听、日志出现 `dynamic-datasource create datasource named [master] error` 或 `Access denied for user 'root'@'localhost'`。
- Preflight check: 启动后端前确认本地 MySQL `127.0.0.1:3306` 与 `application-local.yaml` 中的正式本地数据源配置一致；若使用 Docker MySQL，则用启动参数明确指向 Docker 宿主机映射端口。
- Blocker: MySQL 拒绝当前配置账号、数据库不可达、或后端无法创建 `master` 数据源时，必须停止后端启动结论，不得声明 `48081` 已成功运行。
- Verification: 本次 Docker MySQL `int-ruoyi-mysql` 映射 `23306->3306`，容器内业务库 `ruoyi-vue-pro` 存在，后端健康检查 `UP`。
- Forbidden action: 禁止静默换端口、临时改 `application-local.yaml` 凭据、切换到 mock/空数据源、只启动前端就宣称前后端完成。
- Evidence: `docs\local-runtime.md#2026-07-25-本地后端数据库凭据门禁`。

## Verification Evidence

- Docker MySQL：`int-ruoyi-mysql`，镜像 `mysql:8.0.39`，状态 `Up`，端口映射 `0.0.0.0:23306->3306/tcp`。
- Docker MySQL 业务库：容器内 SQL 探针返回端口 `3306` 与库名 `ruoyi-vue-pro`。
- 前端启动：`8081` 已由 `node.exe` Vite 进程监听，PID `39008`，命令行位于 `E:\IntRuoyi\IntRuoyiFronted`。
- 后端启动：`48081` 已由 `java.exe` 进程监听，PID `34940`，命令行位于 `E:\IntRuoyi\IntRuoyiBackend`，JDBC URL 指向宿主机 Docker MySQL 端口 `23306`。
- 后端健康检查：`BACKEND_STATUS=UP`。
- 前端入口验证：`FRONTEND_STATUS=200`，`FRONTEND_LENGTH=3474`。
- 经验沉淀：已合并本地后端数据库凭据门禁到 `docs/local-runtime.md`，并在 `docs/experience-index.md` 增加关键词路由。

## Blockers

- 运行态无阻塞。
- 收尾提交/推送未执行：当前工作区存在其他任务的脏改动，需按任务边界单独处理，避免混入本次启动记录。