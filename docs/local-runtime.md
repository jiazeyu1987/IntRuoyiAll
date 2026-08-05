# IntRuoyi Local Runtime Rules

## 触发场景

- 启动、停止、重启或排查本机前端、后端、Vite、Java 服务时，必须先读取本文件。
- 排查 `8081`、`48081` 或其他 worktree 登记端口占用时，必须同时读取 `docs/worktree-restrictions.md`。
- 本文件只约束本机运行态；远端服务器必须读取 `docs/server-access.md`。

## 固定端口

PORT_CONTRACT_VERSION: 2026-07-26-branch-runtime-v3

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
- 附加 worktree 的 `slot` 只允许 `1..19`，必须由 `scripts\runtime\reserve-worktree-slot.ps1` 原子分配；`slot >= 20` 或命中任一基准端口时必须 fail fast。

## 2026-07-24 本地重启脚本路径门禁

- Trigger: 本地重启、E2E 复验、`restart-int-ruoyi-local.ps1`、`Missing int_main frontend path`、`yudao-ui-admin-vue3`、`IntRuoyiFronted`。
- Preflight check: 执行本地重启脚本前，确认脚本解析出的前端根目录与本项目规则一致，当前主工作区前端根目录必须是 `E:\IntRuoyi\IntRuoyiFronted`。
- Blocker: 脚本报 `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3` 时必须停止该脚本路径，记录失败；不得通过新建同名目录、软链、换端口或静默跳过前端路径检查继续。
- Verification: 记录脚本失败文本、端口归属 PID、`mvn.cmd -pl yudao-server -am -DskipTests package` 结果、重启后 `http://127.0.0.1:48081/actuator/health` 状态。
- Forbidden action: 禁止为了绕过脚本硬编码路径创建 `yudao-ui-admin-vue3` 假目录、修改端口、强杀未知进程或把 API-only 验证冒充 E2E。
- Evidence: `doc/tasks/fix-batch-exec-last-update-created-time/verification-report.md`。

## 2026-07-28 标准本地后端重启 tokenless Runner 门禁

- Trigger: `restart-int-ruoyi-local.ps1 -Component backend`、本地后端重启后测试管理提示 `Codex Runner token 无效或未配置`、`CODEX_TEST_RUNNER_TOKEN`、tokenless Runner、Runner 注册失败或 heartbeat 过期。
- Preflight check: 标准本地后端重启默认必须保持 tokenless Runner 模式；脚本不得生成、读取或复用 `.runtime/codex-test-runner/runner-token.txt`，不得在父进程或 Java 启动进程设置 `CODEX_TEST_RUNNER_TOKEN`；启动 Java 前必须显式清理继承来的 `Env:\CODEX_TEST_RUNNER_TOKEN`，确保 `yudao.codex-test.runner.token` 为空。Runner HTTP client 只能在环境变量真实存在时发送 `X-Codex-Runner-Token`，tokenless 模式不得发送伪 token 头。
- Blocker: 重启脚本仍包含 token 文件初始化、随机 token 生成、`CODEX_TEST_RUNNER_TOKEN` 注入、后端运行态 `yudao.codex-test.runner.token` 非空、Runner 注册请求仍带旧 token 头、注册探针业务码非 `0`、Runner `current_running_count` 无法归零，或空闲 heartbeat age 达到 `heartbeat-timeout-seconds` 时必须停止成功结论。
- Verification: 运行 `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q`、PowerShell parser、Runner 启动脚本静态合同、Runner HTTP client 静态合同；后端 health 为 `UP` 后使用不带 `X-Codex-Runner-Token` 的受控注册探针确认业务码 `0`，再等待至少一个 heartbeat 周期，确认实际 Runner 会话 heartbeat age 小于超时阈值且 `current_running_count=0`。
- Forbidden action: 禁止为了消除 token 报错重新生成 token 文件、只在当前 shell 设置 token、把 token 写入提交文件或日志、用 Runner 进程存在代替注册与 heartbeat 证明，或因 token 缺失切换端口/Runner/后端。
- Evidence: `doc/tasks/20260727-codex-runner-token-invalid/verification-report.md`；`doc/tasks/20260728-codex-runner-tokenless-cli/verification-report.md`；`doc/tasks/20260728-codex-runner-tokenless-local-restart/verification-report.md`。

## 2026-07-24 隔离构建 Jar 加载门禁

- Trigger: 主工作区存在并行脏改动，但需要把本任务后端修复加载到 `int_main` 的 `48081` 做真实 E2E；或页面仍提示 `请求地址不存在:<接口>`、返回修复前旧业务错误，怀疑运行中 Jar 未加载新 Controller、Service 或 VO。
- Preflight check: 先确认 `48081` 监听 PID 的命令行属于预期源码或运行时 worktree、端口为 `48081`、`repo-root` 指向本项目；同时确认新 Jar 来自本次任务已验证的构建产物。对 fat jar 内嵌模块，必须只读检查 `BOOT-INF/lib/<module>.jar` 是否包含本次新增或修改的关键 class，例如新增 Resolver、Controller、VO 字段载体；若本地 `target/classes` 有新 class 但运行 Jar 内嵌模块没有，视为运行态未刷新。若只热替换某个内嵌模块，必须以当前运行 Jar 内的旧模块为底保留其它并行任务依赖类，仅替换本任务 class；写回 `BOOT-INF/lib/*.jar` 时必须保持 Spring Boot nested jar 未压缩（例如 `jar uf0`，zip `compress_type=0`），否则运行时可能出现 classpath resource missing。若 `48081` 实际运行的是 `D:\IntRuoyiWorktree\...` 下的 runtime jar，必须在该 runtime worktree 内补齐源码、测试、schema 夹具并重建该 Jar，不能只检查 `E:\IntRuoyi` 主工作区源码。
- Blocker: 如果 PID 归属不明、Jar 来源不明、目标 Jar 哈希与隔离构建 Jar 不一致、运行 Jar 内嵌模块缺少本次关键 class、热替换内嵌 jar 被压缩写入、或主工作区源码混有其他任务改动，必须停止，不得从脏主工作区重新打包冒充本任务运行态。
- Verification: 记录旧 PID、停止依据、新 PID、Jar SHA256、启动命令、`http://127.0.0.1:48081/actuator/health`、登录态目标接口业务响应、必要 schema 字段核对、内嵌模块关键 class 检查结果、嵌套 jar 压缩方式（`compress_type=0`），并在 E2E 后记录真实数据库状态。
- Route check: 目标接口需要登录时，未登录请求返回 `401` 只能证明安全过滤器生效，不能证明 MVC 路由已加载；必须使用本机登录态请求目标接口，业务码为 `0` 或预期业务错误，才可宣称新 Controller 已进入运行态。
- Forbidden action: 禁止强杀未知进程、随机换端口、用主工作区脏源码重新构建、只看 health 或未登录 `401` 就宣称修复已加载。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/verification-report.md`；`doc/tasks/20260803-controlled-file-category-missing/verification-report.md`。

## 2026-07-25 本地后端数据库凭据门禁

- Trigger: 启动 `int_main` 本地后端、`48081` 未监听、日志出现 `dynamic-datasource create datasource named [master] error` 或 `Access denied for user 'root'@'localhost'`。
- Preflight check: 启动后端前确认本地 MySQL `127.0.0.1:3306` 与 `application-local.yaml` 中的正式本地数据源配置一致；如果只做启动验证，可先启动并用日志判定真实失败原因，但不得改端口或切换数据源。
- Blocker: MySQL 拒绝当前配置账号、数据库不可达、或后端无法创建 `master` 数据源时，必须停止后端启动结论，不得声明 `48081` 已成功运行。
- Verification: 数据库前置条件修复后重新启动后端，记录 `48081` PID、命令行归属 `E:\IntRuoyi\IntRuoyiBackend`，并用 `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 断言 `status=UP`。
- Forbidden action: 禁止静默换端口、临时改 `application-local.yaml` 凭据、切换到 mock/空数据源、只启动前端就宣称前后端完成。
- Evidence: `doc/tasks/20260725-start-local-frontend-backend/verification-report.md`。

## 2026-07-28 Docker Desktop E 盘 bind 挂载门禁

- Trigger: `int-ruoyi-mysql` 启动报 `invalid mount config for type "bind"`、`bind source path does not exist`、`/run/desktop/mnt/host/e/IntRuoyi/.../ruoyi-vue-pro.sql`、Docker 临时容器挂载 `E:\IntRuoyi` 后目录为空、WSL 提示 `Failed to translate 'E:\IntRuoyi'`。
- Preflight check: 先用 Windows `Test-Path E:\IntRuoyi\IntRuoyiBackend\sql\mysql\ruoyi-vue-pro.sql` 验证源文件真实存在，再用临时只读容器验证 Docker 能看到该文件；若 Docker/WSL 看不到 E 盘，检查 `/mnt/e`、`/mnt/host/e` 或 Docker Desktop host path 日志。
- Blocker: Windows 文件存在但 Docker/WSL 挂载为空或缺失时，必须停止容器启动成功结论；不得复制 SQL 到假目录、删除 MySQL 数据卷、重建空库、改数据库端口或把缺失 bind 当成业务 schema 问题。
- Verification: 修复挂载后，临时只读容器必须返回 `BIND_OK`；再启动 `int-ruoyi-mysql`，重跑标准重启脚本，并验证 `48081` health `UP`、`8081` HTTP `200`。
- Forbidden action: 禁止用 C 盘临时副本、空初始化 SQL、容器重建换数据卷、mock 数据库或 API-only 成功绕过正式 E 盘项目路径。
- Evidence: `doc/tasks/20260728-restart-local-runtime/verification-report.md`。

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

## 2026-07-27 本地 OnlyOffice 容器下载地址门禁

- Trigger: 本地受控浏览、OnlyOffice `错误码 -4`、`下载失败`、`public-file-base-url`、`onlyofficeDocumentUrl`、Docker `onlyoffice` 容器访问 `48081`。
- Preflight check: 若 OnlyOffice 运行在 Docker 容器，本地 `application-local.yaml` 的 `yudao.dcc.preview.onlyoffice.public-file-base-url` 必须让容器访问 Windows Host 后端，默认应为 `http://host.docker.internal:${server.port}`；同时保留浏览器加载 OnlyOffice 的 `base-url` 为 `http://127.0.0.1:8080`。
- Blocker: `docker exec onlyoffice curl http://127.0.0.1:48081/actuator/health` 不可达但 `host.docker.internal:48081` 可达时，预览元数据下载 URL 不得继续使用 `127.0.0.1:48081`。
- Verification: 记录 `docker exec onlyoffice curl http://host.docker.internal:48081/actuator/health` HTTP `200`、旧容器内 `127.0.0.1:48081` 不可达、Jar 内 `application-local.yaml` 目标行、后端 health `UP`，并用静态契约覆盖该默认值。
- Forbidden action: 禁止把浏览器用的 OnlyOffice `base-url` 改成 `host.docker.internal`；禁止用 API-only 成功或未登录 `401` 冒充 OnlyOffice 容器已能下载文件；禁止修改测试服/生产配置来修复本地 Docker 网络问题。
- Evidence: `doc/tasks/20260727-local-onlyoffice-download-failed/verification-report.md`。

## 2026-08-04 DCC 上传预览 OnlyOffice 文档地址门禁

- Trigger: DCC 文件上传页、`OnlyOffice 预览地址未准备好`、`upload-preview` 响应、`onlyofficeBaseUrl`、`onlyofficeDocumentUrl`、上传 `.docx/.xlsx` 后提交前预览为空。
- Preflight check: 上传预览响应契约必须同时包含浏览器加载 OnlyOffice 的 `onlyofficeBaseUrl` 和带签名 token 的临时文件 `onlyofficeDocumentUrl`；`onlyofficeDocumentUrl` 应指向 `/admin-api/dcc/controlled-files/upload-preview/{fileId}/onlyoffice-file?token=...`，且文件类别从文件分类叶子节点自动绑定，不要求用户手填或选择。
- Blocker: 响应只有 `onlyofficeBaseUrl`、缺 `onlyofficeDocumentUrl`、前端 parser 将 `onlyofficeDocumentUrl` 当成 forbidden raw file capability 拒绝、页面继续显示 `OnlyOffice 预览地址未准备好`、或响应暴露原始 `fileId` 时必须停止验收。
- Verification: 前端静态合同必须覆盖 response parser、上传页 props 和 viewer props；后端 JUnit 必须覆盖签名文档地址和不暴露 `fileId`；真实 Playwright 必须通过上传页选择正式文件分类叶子节点、上传 `.xlsx/.docx`、断言 `previewKind=OFFICE`、`onlyofficeDocumentUrl` 有 token、页面无 `OnlyOffice 预览地址未准备好`，并清理临时上传 session。
- Forbidden action: 禁止让用户手工填写文件类别或预览地址；禁止用 API-only 上传、旧历史截图、未登录 `401`、只看 health、隐藏预览区错误或复用未清理临时文件冒充通过。
- Evidence: `doc/tasks/20260803-dcc-upload-onlyoffice-document-url/verification-report.md`。

## 2026-08-02 本机 Docker 开机自启门禁

- Trigger: 调整本机 Docker 开机自启、Docker Desktop 自动恢复容器、`docker update --restart`、清理非项目 Docker 运行项。
- Preflight check: 先用 `docker inspect` 记录容器名、镜像、Compose project/service、restart policy 和状态；只允许与 IntRuoyi 明确相关的容器或已记录的本地依赖保留自启策略。
- Blocker: 容器归属不明、Compose project 不属于 IntRuoyi/Yudao/已记录依赖、或无关容器 restart policy 不是 `no` 时必须阻塞自启合规结论。
- Verification: `docker inspect` 复查所有本机容器，非 IntRuoyi 容器的 `HostConfig.RestartPolicy.Name` 必须为 `no`；记录未停止、未删除、未重建容器。
- Forbidden action: 禁止通过停止、删除、重建容器来冒充禁用自启；禁止让 `docker_ragflow` 等无关栈保留 `always`、`unless-stopped` 或 `on-failure` 自启策略。
- Evidence: `doc/tasks/20260802-docker-autostart-policy/verification-report.md`。

## 2026-08-05 本机 Docker 未使用镜像清理门禁

- Trigger: D 盘空间不足、`D:\Docker\DockerDesktopWSL\disk\docker_data.vhdx` 过大、`docker system df` 显示大量未使用镜像、用户要求通过 Docker 命令清理未使用镜像。
- Preflight check: 先记录 `docker system df`、`docker container ls -a` 和当前 D 盘可用空间；仅在用户明确要求清理镜像时执行 `docker image prune -a -f`，输出较大时用 `Tee-Object` 流式写入任务证据文件，不要先整体捕获到 PowerShell 变量。
- Blocker: Docker CLI 不可用、Docker Desktop/WSL 引擎异常、无法确认命令只清镜像、或用户未授权清理 volume/container/build cache 时必须停止；不得手工删除 `docker_data.vhdx`。
- Verification: 再次运行 `docker image prune -a -f` 应返回 `Total reclaimed space: 0B`；记录清理前后 `docker system df`，并确认容器仍存在、volume 未执行 prune。
- Forbidden action: 禁止执行 `docker system prune --volumes`、`docker volume prune`、删除容器、删除 VHDX、或把 VHDX 文件未立刻缩小误判为清理失败；VHDX 回收/压缩必须作为单独授权任务处理。
- Evidence: `doc/tasks/20260805-docker-unused-image-cleanup/verification-report.md`。

## 2026-08-02 DCC 上传预览本机 MinIO 前置门禁

- Trigger: DCC 原版上传、上传升版、`/admin-api/dcc/controlled-files/upload-preview`、`SdkClientException`、`Connect to 127.0.0.1:9000 failed: Connection refused`、`docker-minio-1`。
- Preflight check: 跑 DCC 写入型上传 E2E 前，先确认 `docker-minio-1` 正在运行且 healthy，`http://127.0.0.1:9000/minio/health/ready` 返回 HTTP 200，`/data/yudao` bucket 目录存在；同时确认后端文件配置 endpoint 指向 `http://127.0.0.1:9000`、bucket 为 `yudao`，不得输出 access key 或 secret。
- Blocker: MinIO 容器未运行、9000 未监听、ready health 非 200、bucket 缺失、后端 `48081` 未 UP，或标准 backend 重启仍在 Maven package 阶段时必须停止 DCC 完整链路结论。
- Verification: MinIO ready 200、后端 health `UP`、前端 8081 HTTP 200 后，再用 Playwright 真实页面跑到 `upload-preview`，目标请求无 500，结果 JSON 中 `targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- Forbidden action: 禁止用 API-only 上传、SQL 改状态、mock 文件服务、切换存储实现、复用历史会话或复用半失败文件号冒充上传链路恢复；禁止在共享 Maven package 正在运行时抢占启动后端或强停未知进程。
- Evidence: `doc/tasks/20260802-dcc-minio-object-storage-runtime/verification-report.md`。

## 2026-07-27 本地后端标准输出阻塞与日志目录门禁

- Trigger: 本地后端 health 为 `UP`，但登录、租户查询或业务 API 长时间无响应；线程栈集中阻塞在 Logback `OutputStreamAppender`；或 task-closeout 无法删除仍被 Java 进程占用的 stdout/stderr 日志。
- Preflight check: 启动长期运行的本地后端前，确认 stdout/stderr 有持续消费或重定向到不会被当前任务 cleanup 删除的稳定运行目录；运行态排查必须同时核对 `48081` PID/命令行、真实登录态业务接口和线程阻塞点，不能只看 health。
- Config check: `application-local.yaml` 本地默认不得把自研 `cn.iocoder.yudao.module.*.dal.mysql` mapper 包设为 `debug`；后端应用日志默认必须进入 `output/runtime/<profile>/logs`，标准本地重启脚本必须显式传入 `--logging.file.name` 和匹配的 `yudao.runtime-control.storage-guard.log-dir`。
- Blocker: health 为 `UP` 但登录态接口挂起，或待清理日志仍被有活动客户端连接的共享后端占用时，必须停止成功或清理结论；不得强杀共享进程、强删日志或在缺少完整运行环境变量时重启。
- Verification: 记录 listener PID、Jar 路径、活动客户端归属、登录预检和目标业务接口结果；cleanup 前用独占文件打开检查日志句柄，只有进程已安全迁移或停止且文件不再锁定后才能删除。涉及本地日志配置修改时，还必须运行 `mvn -pl yudao-server "-Dtest=LocalRuntimeLoggingConfigTest,RuntimeControlLocalConfigTest" test` 与 `python -X utf8 -m pytest script/tests/test_runtime_control_local_config.py script/tests/test_runtime_control_scripts.py -q`。
- Forbidden action: 禁止把 health `UP` 等同于请求链路可用；禁止让长运行进程把日志写入任务 cleanup 候选目录；禁止因首次重启缺少安全配置而改配置、降级或跳过启动校验。
- Evidence: `doc/tasks/20260727-form-template-button-alignment-design/execution-log.md`。

## 2026-07-27 本地后端运行 Jar 不可变门禁

- Trigger: 本地后端运行中再次执行 Maven `package`、接口突然出现多个无关依赖的 `NoClassDefFoundError`、Jimu MiniDAO 模板资源缺失、或运行命令直接引用 `yudao-server\target\yudao-server-exec.jar`。
- Preflight check: 长期运行的后端必须从 `output\runtime\<profile>\` 的独立 Jar 副本启动；记录进程启动时间、运行 Jar 路径和 Jar 修改时间。启动后允许 `target` 继续构建，但运行 Jar 不得被覆盖。
- Blocker: 监听进程直接引用 Maven `target` Jar，或运行 Jar 修改时间晚于进程启动时间时必须重启到独立运行副本；不得把跨 Hutool、Spring、Tomcat、Freemarker 的连续类/资源缺失当作多个业务缺陷分别修补。
- Verification: 后端启动后确认运行 Jar 位于稳定运行目录且 `runtimeJar.lastWriteTime <= process.startTime`；随后执行一次 `mvn -pl yudao-server -am "-DskipTests" package`，目标登录态业务接口仍返回 HTTP 200、health 为 `UP`、运行 PID 和运行 Jar 路径保持不变。
- Forbidden action: 禁止长期运行进程直接使用会被 Maven 重建的 `target\yudao-server-exec.jar`；禁止通过复制缺失类、吞异常、跳过 Jimu 更新或返回空详情掩盖运行归档损坏。
- Evidence: `doc/tasks/20260727-batch-record-list-detail-500/verification-report.md`。

## 2026-07-28 本地前端 pnpm 链接损坏门禁

- Trigger: `restart-int-ruoyi-local.ps1 -Component frontend/full`、`pnpm dev -- --strictPort`、Vite 报 `Cannot find module '@babel/helper-validator-identifier'`、`failed to load config from IntRuoyiFronted\vite.config.ts`、或 `pnpm install --frozen-lockfile` 显示 `Already up to date` 但前端仍无法启动。
- Preflight check: 先确认 `IntRuoyiFronted\node_modules` 存在、`pnpm list <missing-package> --depth 5` 能从 lockfile 依赖树定位缺失包，再用真实包路径执行 Node 解析探针，例如从 `node_modules\.pnpm\@babel+types@7.26.0\node_modules\@babel\types` 加载 `@babel/types` 或 `@babel/helper-validator-identifier`。
- Blocker: lockfile 依赖树中不存在缺失包、`pnpm install --frozen-lockfile` 修改 lockfile 或失败、强制重建后真实包路径仍不能解析、或 `pnpm approve-builds`/构建脚本审批缺失导致 Vite 运行依赖不可用时必须停止启动成功结论。
- Verification: 记录首次前端启动失败日志、`pnpm install --frozen-lockfile` 结果、必要时 `pnpm install --frozen-lockfile --force` 结果、真实包路径 Node 解析探针 PASS、重新启动前端后 `http://127.0.0.1:8081/` HTTP `200`。
- Forbidden action: 禁止手工复制缺失包、手工创建 `node_modules` symlink/junction、修改 `package.json` 或 lockfile 绕过、换端口、跳过前端、或把后端 health `UP` 冒充前后端均已启动。
- Evidence: `doc/tasks/20260728-start-local-frontend-backend-runtime/verification-report.md`。

## 禁止做法

- 禁止把 `int_main` 改到随机端口启动。
- 禁止非 `int_main` 使用 `8081/48081`。
- 禁止任何附加 worktree 使用 `slot >= 20` 或其他 profile 的基准端口。
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
