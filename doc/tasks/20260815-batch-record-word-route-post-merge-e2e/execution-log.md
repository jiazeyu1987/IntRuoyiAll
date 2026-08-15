# Execution Log

## 2026-08-15

- 用户意图：对融合后的批记录 Word 导入与工艺路线候选升版能力执行真实页面 E2E 验证。
- 范围：本机 `int_main` 的 `8081/48081`；真实前端路径；任务自有测试数据；API 仅用于只读后置核验。
- 门禁：已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/powershell-encoding.md` 和 `docs/task-closeout-rules.md`。
- `BDD: 未选择工艺流程时不重建路线 -> Given 已存在生效路线和任务自有 Word 文件；When 用户不勾选“工艺流程”并从批记录表单页导入；Then 只处理已选择的导入内容，生效流程图和路线版本保持不变。`
- `BDD: 选择工艺流程时创建候选升版 -> Given 已存在生效路线及正式逐工序批记录绑定；When 用户勾选“工艺流程”并按 Word 工序顺序导入；Then 系统生成或更新路线候选版本，发布前生效路线保持不变。`
- `BDD: 候选发布后保留既有绑定 -> Given 候选版本包含旧工序和 Word 新增工序；When 用户从真实页面发布候选；Then 旧工序正式批记录绑定身份、工序开始配置和表单槽位保持，新工序使用正式权限范围，工序结束不产生业务绑定。`
- 当前里程碑：M1 前置门禁。
- 经验门禁：已从 `docs/experience-index.md` 命中并读取 `docs/e2e-rules.md` 的融合运行态、写入型模拟环境、上传控件门禁，以及 `docs/backend-development.md` 的唯一候选和正式批记录绑定身份门禁；摘要已写入 `task.md`。
- 写请求状态：尚未执行任何业务写请求。

### M1 前置验证证据

- 代码来源：根仓库分支 `int_main`，HEAD `d67c40c2692bcbc6c424faf015195341c529bfee`，当前工作区状态项数量 `384`；未执行 Git 写操作。
- 端口归属：前端 `8081` PID `37616`，后端 `48081` PID `28436`，命令行归属均解析为 `E:\IntRuoyi`。
- 可用性：前端 HTTP `200`；后端 `/actuator/health` 返回 `UP`。
- 运行 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260815-032520-dcc-residual-fix.jar`，SHA-256 `77247FBD5EDD3265A4FBF634722DE3B766465D28BDF8243BE323881D38060E97`，修改时间 `2026-08-15T03:25:25+08:00`，早于后端进程启动时间 `2026-08-15T03:26:05+08:00`。
- 内嵌 MES Jar：`BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar`，SHA-256 `363678E367E2C88BF93BBC504A07044983C22D6D68044FDCAF73AAA5C56628DD`，保持未压缩嵌套方式。
- 候选生成类运行态与本地编译类 SHA-256 均为 `591BEE779F19F39FAC7CB23A66C36977308E6803B05DA74300130F39C5F834BD`，一致。
- 候选发布投影类运行态 SHA-256 为 `17F5B50EEA16BB9ECE9C89501EFC5D8D4C6C18C047E0A2B2230A87362071B814`，本地融合编译类为 `04B71DCFF2DDE4C04CD2F4AC1DB98E39E4B875B2E23F689C2C19B886D2F11B10`，不一致；源文件修改时间 `2026-08-15T03:39:05+08:00`，晚于运行 Jar。
- Playwright 前置：`npx` 可用，本机 Chrome 与 Edge 均存在，官方登录脚本存在。
- 真实页面登录：`node scripts/preflight/login-preflight.mjs` 使用本机 Chrome、测试租户和测试账号，进入 `/mes/pro/batch-record-form-list` 并看到“导入”，结果 PASS；凭据未写入任务日志。
- 页面/脚本盘点：现有 `edhr-word-import-upgrade-action-real-flow.e2e.js` 只覆盖批记录升版并主动取消全部路线重建项，且没有路线夹具清理；它不能证明本任务要求的“勾选工艺流程后候选升版、发布及三类绑定保留”。未将该旧脚本冒充目标 E2E。

### BLOCKER

- `BLOCKED: 运行 Jar 未加载融合后的候选发布投影修复 -> 影响：不能安全验证新工序正式权限范围、旧绑定身份保留和发布后正式路线结果。`
- `BLOCKED: 当前主工作区有 384 项未提交变更 -> 影响：按运行态门禁禁止从脏主工作区重新打包，也不能给当前运行包建立可追溯的融合版本身份。`
- 目标业务写请求数：`0`。未执行 Word 导入、路线候选创建/更新、发布或清理写操作。
- 当前状态：`blocked`；M2-M5 未开始。

### 用户解除阻塞授权

- 2026-08-15：用户明确授权将本次相关改动单独整理为可追溯版本，重建并重启本机 `int_main` 后继续真实 E2E。
- Git 边界：只提交本次 Word 导入/路线候选绑定保留相关生产代码、测试和任务证据；不提交、回滚或清理其余共享工作区改动。
- 当前状态更新为 `in_progress`；先复跑定向回归和分支端口保护检查，再提交、构建、重启。

### M1 可追溯版本提交前门禁

- `GREEN: mvn.cmd -pl yudao-module-mes "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3,MesProRouteVersionPublishProjectionServiceImplTest" test -> PASS`；Tests run: 12，Failures: 0，Errors: 0，Skipped: 0。
- `GREEN: node tests/e2e/batch-record-word-import-route-candidate-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/batch-record-word-import-production-upgrade-dedupe-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/batch-record-word-import-dcc-identity-static.spec.cjs -> PASS`；3/3。
- `GREEN: pnpm ts:check -> PASS`。
- `GREEN: scripts/preflight/branch-runtime-port-guard.ps1 -> PASS`；`int_main` 端口为前端 `8081`、后端 `48081`。
- 第一条 Maven 调用因 PowerShell 将未加引号的 `-Dsurefire.failIfNoSpecifiedTests=false` 误解析为生命周期阶段而在测试执行前失败；已用显式引号修正命令并得到上述 12/12 真实通过结果，未改生产代码规避。
