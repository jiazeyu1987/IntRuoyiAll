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

### M1 融合运行态恢复与真实数据前置

- `GREEN: validated prebuilt int_main backend cold start -> PASS`；后端 PID `46644`，监听 `48081`，health `UP`，运行 Jar `backend-runtime-control-20260815-122829-word-route-e2e-0e653c709.jar`，SHA-256 `A9560D4A9B1BF109B7FE9E172F0A274055D71341C401A0928B1545A26CF7D702`，来源提交 `0e653c7099acd06521e54712c82573d082ace780`。
- `GREEN: int_main frontend restart -> PASS`；前端 PID `41700`，监听 `8081`，HTTP `200`，进程归属 `E:\IntRuoyi\IntRuoyiFronted`。
- `GREEN: node scripts/preflight/login-preflight.mjs -> PASS`；真实登录 `测试租户/aoteman`，进入 `/mes/pro/batch-record-form-list` 并看到“导入”；凭据未进入日志。
- `GREEN: node --check doc/tasks/20260815-batch-record-word-route-post-merge-e2e/post-merge-word-route-e2e.cjs -> PASS`。
- 只读 Playwright 前置首先纠正了两项测试脚本自身问题：移除系统并不存在的批记录删除权限前置；按项目现有 E2E 规范规范化浏览器缓存 token。两项修正均未放宽真实租户、页面入口或数据完整性门禁。
- `BLOCKED: Playwright readonly route baseline discovery -> 测试租户 6 条 ACTIVE 路线全部返回“工艺路线候选版本快照不完整”`；路线 ID/版本分别为 `980096/630`、`980090/621`、`980089/620`、`922275/482`、`922274/481`、`922273/480`。
- 只读证据：`output/playwright/batch-record-word-route-post-merge/preflight.json`；`writeRequests=[]`、`pageErrors=[]`、`consoleErrors=[]`。
- 阻塞影响：无法在 `测试租户/aoteman` 下冻结并证明三类独立配置，也不能安全进入未勾选/勾选工艺流程、候选发布和绑定保留验证。
- 需要用户决策：是否明确授权切换到本机默认租户 `芋道源码/admin`，仅创建并清理带 `E2E-WORD-ROUTE-20260815` 标识的任务专用路线副本。未授权前不得静默切换租户或写入 admin 基线。
- 当前目标业务写请求数：`0`；当前状态：`blocked`。

### 用户指定按压式压力泵 Word 基线

- 用户明确指定 Word：`C:\Users\BJB110\Desktop\文档\1\RE-PP-IDPR-01（A 1） 按压式球囊扩张压力泵生产记录--2026.02.02生效.doc`，文件存在，大小 `938132` 字节。
- `GREEN: 芋道源码/admin readonly preflight -> DCC 项目存在`；项目 ID `130`、项目代码 `IDPR`、项目名称“按压式球囊扩张压力泵”。
- `GREEN: 芋道源码/admin readonly preflight -> 当前同名路线=[]`；首次导入必须走 `CREATE_REQUIRED / REBUILD_V1`，不能直接冒充已有路线升版。
- 用户授权按该文件执行导入测试后，当前状态恢复为 `in_progress`；先创建 V1 候选和逐工序批记录绑定基线，再发布并执行 V2 升版保留验证。
- 当前运行 Jar `backend-runtime-control-20260815-203449-scheduler-seven-issues-jaruf0.jar` 的 `MesProBatchRecordReportServiceImpl.class` 与 `MesProRouteVersionPublishProjectionServiceImpl.class` 哈希分别与已验证融合包完全一致；本次关键融合代码运行态门禁 PASS。

### 指定 Word 首次真实导入结果

- `RED: Playwright 真实页面选择 IDPR、同时勾选批记录表单和工艺流程、上传用户指定 938132 字节 Word 并确认 -> FAIL`；POST `/admin-api/mes/pro/batch-record-report/recognize-uploaded` 返回业务错误“电子批记录 Word 解析失败：route_b_python_output_invalid”。
- 写入状态已在 `output/playwright/batch-record-word-route-post-merge/bootstrap.json` 记录：`routeCreated=false`、route/candidate ID 均为空、仅 1 次目标 POST、页面错误数 0。
- 根因核对：Word COM 以只读方式打开用户指定文件成功，但 `Tables=0`、`Paragraphs=1334`；当前 Route B 正式解析器按 `document.Tables` 提取批记录表格，输出 `tables=[]` 后按 fail-fast 规则拒绝导入。
- 对照只读核对：`(1)` 目录中的 937108 字节版本 `Tables=7`、`Paragraphs=3603`；该文件不是用户本次指定路径，未擅自替换导入。
- `GREEN: 导入失败后只读复核 -> 当前同名路线=[]`；DCC 项目仍为 `130 / IDPR / 按压式球囊扩张压力泵`，没有路线、候选或批记录残留需要清理。
- 当前状态：`blocked`。需要用户明确选择可解析对照文件，或授权把“零原生表格 Word 解析”纳入新功能范围并按 BDD/TDD 实现。

## 2026-08-17 芋道源码复验

- 用户意图：在 `芋道源码` 租户继续验证指定的按压式球囊扩张压力泵 Word 导入；仍使用用户指定原始路径，未切换到 `(1)` 对照文件。
- `GREEN: int_main runtime preflight -> PASS`；前端 `8081` HTTP `200`，后端 `48081` health `UP`，监听进程归属 `E:\IntRuoyi`。
- 当前运行 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260817-082151.jar`，SHA-256 `64C6933D692C3FBCA55050219D4FD1A50A3A16FFEB833B3D78CE186DB15E4716`。
- 关键运行类哈希：候选生成类 `FC69E877154C9AF6E6829879786A35AF6BBD26082BE9A89026A41C7B844065F0`；候选发布投影类 `04B71DCFF2DDE4C04CD2F4AC1DB98E39E4B875B2E23F689C2C19B886D2F11B10`；均与已验证融合包一致。
- `GREEN: node scripts/preflight/login-preflight.mjs -> PASS`；真实登录 `芋道源码/admin`，进入 `/mes/pro/batch-record-form-list` 并看到“导入”；凭据未写入任务日志。
- `RED: Playwright 真实页面选择 IDPR、同时勾选批记录表单和工艺流程、上传用户指定原始 Word 并确认 -> FAIL`；POST `/admin-api/mes/pro/batch-record-report/recognize-uploaded` 返回“电子批记录 Word 解析失败：route_b_python_output_invalid”。
- 本轮证据：`output/playwright/batch-record-word-route-post-merge/bootstrap.json`；`routeCreated=false`、路线/候选 ID 为空、目标写请求 `1` 次、`pageErrors=[]`，控制台记录该失败请求的 `502`。
- `GREEN: 导入失败后只读最终核验 -> exactRouteCount=0, exactBatchRecordReportCount=0`；没有同名路线、候选或批记录报表残留，不需要页面清理。
- 当前状态保持 `blocked`：原始 Word 仍为零原生表格结构，无法进入 V1 候选创建、发布和 V2 绑定保留验证。

### 用户确认正确 Word 路径

- 用户确认正确文件为：`C:\Users\BJB110\Desktop\文档\1\RE-PP-IDPR-01（A 1） 按压式球囊扩张压力泵生产记录--2026.02.02生效(1)\RE-PP-IDPR-01（A 1） 按压式球囊扩张压力泵生产记录--2026.02.02生效.doc`。
- 用户要求使用该文件在 `芋道源码/admin` 继续真实页面验证；当前状态更新为 `in_progress`。
- `GREEN: 正确 Word 输入门禁 -> PASS`；文件存在，大小 `937108` 字节；`npx`、前端 `8081`、后端 `48081`、官方登录前置均通过。
- `RED: Playwright 首次 V1 导入后置断言 -> FAIL`；真实页面导入本身成功，生成路线 `RT000036 / 980143`、V1 `681`、16 份批记录表单、15 道工序、15 条逐工序批记录绑定；失败原因为验证脚本错误要求“首次无现有路线也生成 DRAFT 候选”。
- 业务口径修正：根据需求和当前正式实现，无现有路线时直接创建 V1 `ACTIVE`；只有已有路线升版才生成 DRAFT 候选。验证脚本已改为按该口径断言。
- 清理核对发现原脚本把“路线列表未显示”错误记为清理完成；只读最终核验确认任务路线和 16 份报表仍存在。脚本已改为列表不可见时必须再做只读存在性确认，禁止假清理成功。该任务自有数据继续作为升版基线使用。
- `GREEN: Playwright 正确 Word 升版导入 -> 业务写入成功`；原路线保持 `980143`，V1 `681` 仍为 `ACTIVE`，生成 V2 `684` `DRAFT` 候选；批记录版本从 V1.0 `137` 升为 V2.0 `138`，导入结果仍为 16 份、15 道工序、15 条路线批记录绑定。
- `RED: 升版候选流程比较 -> FAIL`；仅因 START/END 边界返回顺序不同，边界业务身份和工序 ID 相同。验证脚本已改为按边界类型、顺序和工序身份排序比较。
- `RED: 升版候选绑定比较 -> FAIL`；V1/V2 的工序 ID、批记录表单 ID、权限范围相同，V2 候选额外生成 `slotConfigSnapshotHash`，V1 读取接口返回空。验证脚本已改为比较正式绑定身份，并仅要求已有非空冻结哈希不得变化。
- 当前环境阻塞：候选只读页面复核期间，另一条标准重启任务停止了共享 `48081` 后端，同一工作区仍有 Maven 构建进程；本任务未抢占、未终止并发进程，等待后端恢复后继续。

### 正确 Word 候选复核与发布阻塞

- `GREEN: Playwright --verify-upgrade -> PASS`；使用正确 Word 产生的路线 `RT000036 / 980143` 中，V1 `681` 保持 `ACTIVE`，V2 `684` 保持 `DRAFT`，路线当前生效版本仍指向 V1。
- `GREEN: 升版候选业务关系复核 -> PASS`；V1/V2 均为 15 道工序、14 条工序间流转边、2 条开始/结束边界、15 条逐工序正式批记录绑定；4 条工序开始附件配置保留，工序开始生产负责人为 0 条，表单槽位为 0 条，工序结束业务绑定为 0 条。
- `GREEN: DCC 项目身份链路 -> PASS`；导入预检通过正式产品绑定定位现有路线，项目为 `IDPR / 按压式球囊扩张压力泵`，没有按路线名称猜测或切换文件。
- `RED: Playwright 真实页面提交 V2 发布 -> FAIL, 后端返回“系统异常”`；目标写请求仅为 POST `/admin-api/mes/pro/route-version/submit-publish`，页面无 `pageerror`。
- 后端根因证据：`MesProRouteControlledContentAdapter.ensureHistoricalDraftCandidateRef` 抛出 `controlled content active ref does not exist for route: 980143`。Word 导入的首次路线创建路径直接插入路线及 V1，候选路径直接插入 V2，但未登记 V1 的统一受控内容生效引用；提交时无法为 V2 建立候选引用，事务回滚。
- `GREEN: 发布失败后 Playwright --verify-upgrade -> PASS`；V1 仍为 `ACTIVE`、V2 仍为 `DRAFT`、路线生效版本仍为 `681`，15 条正式绑定和 4 条工序开始配置未变化，证明失败没有覆盖生效路线。
- 验证脚本只读 API 等待时间从 30 秒调为 90 秒，以适应本机共享后端繁忙；不改变业务断言，不引入成功降级。
- 当前状态：`blocked`。发布链路缺口修复前，不能判定 M4 或整个融合 E2E 完成。
- 覆盖限制：本 Word 两次导入的工序集合相同，未证明“新增工序”发布后身份；当前路线 `formBindings=0`，只能证明空表单槽位保持为空；“不勾选工艺流程”尚未执行真实页面负向导入。

### Word 导入路线受控内容登记修复

- 用户意图：继续修复正确 Word 升版候选无法发布的问题，并在芋道源码真实页面完成复验。
- `BDD: Word 导入路线版本登记完整 -> Given 用户通过 Word 首次创建 ACTIVE 路线或对已有 ACTIVE 路线创建/更新 DRAFT 候选；When 导入事务完成；Then ACTIVE 与候选都登记到统一受控内容生命周期，后续提交发布不因引用缺失失败。`
- `BDD: 历史 Word 导入候选可审计补登记 -> Given 本任务已存在由旧入口创建、业务数据完整但受控内容引用缺失的 ACTIVE V1 和 DRAFT V2；When 用户再次通过 Word 更新该候选；Then 系统只补齐缺失引用并写入审计原因，不改写 ACTIVE 工艺流程或三类绑定。`
- 根因：批记录 Word 路线生成服务直接插入 V1/V2 版本对象，绕过普通工艺路线服务已有的统一受控内容登记入口；发布提交阶段才发现 ACTIVE 引用不存在。
- 设计约束：不在发布阶段做静默自愈；登记和历史补登记只发生在 Word 导入写事务中，已有引用必须身份一致，身份漂移直接失败。
- `RED: mvn.cmd -pl yudao-module-mes "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=MesProRouteControlledContentAdapterTest#recordActiveRegisteredIfMissing_shouldRegisterImportedActiveAndKeepMatchingExistingRef+recordActiveRegisteredIfMissing_shouldRejectActiveIdentityDrift+recordDraftCandidateRegisteredIfMissing_shouldRegisterImportedCandidateOnlyOnce,MesProBatchRecordRouteCandidateGovernanceTest#uploadedWordRouteVersionsMustRegisterControlledContentRefsBeforePublish" test -> FAIL`；测试编译准确指出 Word 导入所需的 ACTIVE/候选引用登记方法不存在，共 5 个缺失方法调用错误，符合预期 RED 原因。
- `GREEN: mvn.cmd -pl yudao-module-mes "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=MesProRouteControlledContentAdapterTest#recordActiveRegisteredIfMissing_shouldRegisterImportedActiveAndKeepMatchingExistingRef+recordActiveRegisteredIfMissing_shouldRejectActiveIdentityDrift+recordDraftCandidateRegisteredIfMissing_shouldRegisterImportedCandidateOnlyOnce,MesProBatchRecordRouteCandidateGovernanceTest#uploadedWordRouteVersionsMustRegisterControlledContentRefsBeforePublish" test -> PASS`；Tests run: 4，Failures: 0，Errors: 0，Skipped: 0。
- 实现结果：首次 Word 新建 V1 后立即登记 ACTIVE 引用；升版导入在锁定 ACTIVE/候选后确认引用，缺失时以明确审计原因补登记；已存在引用只有版本身份一致才复用，漂移时直接失败；V2 新建和更新路径都确认 DRAFT 候选引用。
- `RED: 相邻回归（24 tests） -> FAIL`；候选治理、生命周期适配和发布投影 21 条通过，3 条批记录数据库集成场景因精简测试 Spring 容器未注册新增的 `MesProRouteControlledContentAdapter`，在业务断言前报 `NoSuchBeanDefinitionException`。这是测试夹具缺口，不是生产行为失败；已将适配器作为该精简容器的 MockitoBean 补齐，未放宽业务断言。
- 第一次夹具补齐重跑遇到共享 `target` 增量状态不完整：Maven 报主类无需编译，但测试运行缺少 `MesProRouteFlowConfigTypeEnum` 和发布投影类。只读核对确认对应 `.class` 确实不存在；在无并发 MES Maven 进程时仅清理并重建 `yudao-module-mes/target`，未触碰历史损坏目录或其它任务产物。
- `GREEN: mvn.cmd -pl yudao-module-mes clean "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=MesProRouteControlledContentAdapterTest,MesProBatchRecordRouteCandidateGovernanceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3" test -> PASS`；Tests run: 24，Failures: 0，Errors: 0，Skipped: 0。
- `GREEN: 运行包热更新与重启 -> PASS`；只把已验证的受控内容适配器和 Word 路线生成类写入当前 int_main 运行包副本，内嵌 MES Jar 保持 `STORED`；新运行包 SHA-256 `D73D5C06F0A6E353EC28FD8ABFD836171B46A339C46833892A70E7A747D31E1A`，后端 PID `40132`，`48081` health `UP`。
- `GREEN: node scripts/preflight/login-preflight.mjs -> PASS`；使用本机 `.env` 正式默认登录来源登录 `芋道源码/admin` 并进入批记录表单页，凭据未写入日志。
- `RED: Playwright --upgrade 修复复测前置 -> FAIL`；验证脚本仍要求升版前没有候选，但本次复测目标正是旧入口已生成的唯一 V2 DRAFT。脚本已改为只允许唯一、来源为当前 ACTIVE 的 DRAFT，拒绝审批中/待发布候选，并要求导入后复用同一 V2、不得创建 V3。
- `RED: Playwright --upgrade 登录前置 -> FAIL`；第一次为浏览器协议未取得登录响应体，写请求 0；同流程重跑仍在登录请求等待超时，写请求 0。与官方已通过的登录前置对比后，定位为任务脚本选中了可能隐藏的租户输入；已改为只点击、填写可见租户控件，与官方登录路径一致。
- 后续核对发现本机后端由命令会话托管清理在约 8 分钟后终止，导致前端代理 500；随后另一条本机任务启动了新的 int_main 运行包。按并发所有权门禁未停止或替换该进程，只读比对确认其 Word 路线生成类和受控内容适配器 SHA-256 与本任务验证类完全一致，可以继续当前业务复测。
- 官方登录前置在后端恢复后又因重复点击已由 `.env` 预填的“芋道源码”租户输入而超时。任务脚本已改为先读取可见租户值，值一致时不重复触发下拉；只有不一致时才执行页面选择，不改变登录身份或账号来源。
- 脱敏表单诊断确认用户名、8 位密码和登录按钮均已正确就绪；租户搜索输入在 Element Plus 选中后按组件行为清空，实际选中值位于独立标签。脚本已改为按选中标签断言租户，避免把空搜索框误判成未选择。

### 正确 Word 修复后真实导入与重启阻塞

- `GREEN: 真实页面上传正确 Word 并选择“升版导入 + 批记录表单 + 工艺流程” -> PASS`；页面预检识别现有路线 `980143`、当前 V1 `681` 和唯一 V2 DRAFT `684`，确认动作明确为“更新 V2 草稿”，没有创建 V3 路线版本。
- `GREEN: POST /admin-api/mes/pro/batch-record-report/recognize-uploaded -> PASS`；请求于 `2026-08-17 14:34:18` 开始，`14:39:35` 正常完成，耗时 `277542 ms`，参数锁定原路线、原生效版本及候选版本；后端日志没有该请求的业务异常。
- 本轮批记录从 V2.0 升为 V3.0；路线仍更新既有 V2 草稿。页面自动化连接在长请求期间因浏览器安全检查暂时不可用而中断，但后端事务继续并正常完成；没有把浏览器中断记录成业务通过，仍要求后续真实页面发布和只读终态复核。
- `GREEN: 当前 48081 运行包关键类核对 -> PASS`；`MesProRouteControlledContentAdapter.class` 和 `MesProBatchRecordRouteGenerationServiceImpl.class` SHA-256 分别与本任务已验证修复类一致，后端 health 为 `UP`。
- `RED: Playwright --upgrade 重跑 -> FAIL`；第一次因共享 `48081` 在只读路线核对前停止监听而失败，第二次因重启后 `8081` 前端依赖目录不完整无法进入登录页而失败；两次都没有进入目标导入写请求。
- `RED: int_main frontend start -> FAIL`；Vite 配置加载依次报告缺少 `@babel/helper-validator-identifier`、`@babel/core`，确认 `node_modules` 为半完整状态，不是 Word 或路线业务错误。
- `BLOCKED: pnpm install --offline --frozen-lockfile -> 另一个非本任务 pnpm install 持续占用全局包仓库`；本任务已停止自己的竞争安装并等待 25 分钟，外部进程仍未退出。按并发所有权门禁未终止该进程，也未用工作树依赖或替代目录绕过。
- 当前影响：无法恢复 `8081` 真实页面，因而不能执行 V2 候选提交发布、发布后绑定核验、M2 负向路径和 M5 清理。当前状态：`blocked`。
