# IntRuoyi E2E Rules

## 触发场景

- 编写、修改、运行或评审 Playwright E2E、真实用户路径验证、截图验收、登录后联调时，必须先读取本文件。
- 涉及登录、租户、账号时，还必须读取 `docs/login-access.md`。
- 涉及本机端口或 worktree 端口时，还必须读取 `docs/local-runtime.md` 和 `docs/worktree-restrictions.md`。

## 基本规则

- E2E 必须使用 Playwright 操作真实前端页面。
- API 只能用于最终状态核验或只读辅助检查，不得代替真实用户路径。
- 默认本机入口为 `http://localhost:8081` 或 `http://127.0.0.1:8081`。
- 写入型 E2E 必须使用已确认的测试租户和账号，并创建带任务标识、可追踪、可清理的数据。
- 只读验证必须说明使用的数据来源和只读范围。

## 缺入口处理

- 发布、审计或独立验证任务发现前端无入口时，必须 fail fast，不得临时扩大范围新增入口。
- 功能或修复任务只有在入口属于用户批准范围，且已完成 BDD + TDD 时，才允许补入口。
- 计划或验收文档列出的 E2E 命令必须先核对 `package.json` 实际 script 和测试文件存在性；脚本或测试文件缺失时记录为 E2E 前置缺口，不得把命令解析失败、静态合同或 API wrapper 测试写成真实 E2E 通过。


## 静态合同与真实 E2E 同步门禁

### E2E 脚本入口存在性门禁

- Trigger: 任务验收文档指定 `pnpm test:e2e ...`、`pnpm test <target>`、Playwright spec 文件或新增真实用户路径 E2E。
- Preflight check: 运行前读取当前前端 `package.json` 的 scripts，确认命令名存在、命名 runner 能识别目标、spec 文件存在，并记录实际工作目录；PowerShell 下若 `pnpm --dir` 或 `pnpm -C` 解析异常，改用显式 `workdir` 复核，不把第一次命令解析失败当作业务 E2E 结果。
- Preflight check: 验收文档包含写入型用户路径时，还必须同时确认真实页面入口、前端 route、权限 meta、页面主按钮和写 API wrapper 全链路存在；只有 API wrapper 或只读追溯页存在时，不得宣称写路径已实现。
- Blocker: `ERR_PNPM_NO_SCRIPT`、named target unknown、spec 文件缺失、真实页面入口缺失、菜单权限或测试租户账号缺失时必须停止并记录具体前置缺口。
- Verification: 证据必须区分静态合同 PASS、TypeScript PASS、Playwright 真实路径 PASS 和 E2E BLOCKED；真实 E2E 只有在 Playwright 操作真实页面并完成目标断言后才能记为 PASS。
- Forbidden action: 禁止新增虚假 script 包装静态测试冒充真实 E2E，禁止 API-only 替代页面路径，禁止把前端 API wrapper 存在宣称为页面入口已验收。
- Evidence: `doc/tasks/20260730-process-pool-f5-f6-implementation/execution-log.md`。

### DCC 文控审批处理入口门禁

- Trigger: 验证 DCC 文控上传、原版上传、上传审批、电子签名审批、升版发布、发布申请、文件作废/废止、旧版自动失效、`OBSOLETE`、`SUPERSEDED`、`DccControlledFileDetail`、`/approval-center?moduleCode=DCC`、`PROCESS_IN_MODULE`、`approve-task`、`DCC_PUBLISH` 或 `APPROVE_USER_SELECT` 链路。
- Preflight check: 浏览器审批前必须证明审批账号能从真实页面进入非只读处理态，并看到“审批阶段进度”、当前 `approvalTodoTask` 对应的签名按钮和目标写接口；真实 E2E 不能只断言处理区标题或“审批要求”文案，必须同时断言当前任务按钮可见，并排除“暂无待处理审批任务”“当前没有待处理审批任务”等空任务提示；同时核对 `DccControlledFileDetail.beforeEnter` 不会把非 viewer 处理态重定向到受控浏览。遇到 DCC “作废/废止”需求时，必须先确认用户要的是手动当前版本作废审批链路，还是升版发布后旧版本自动失效链路；若用户提到“升版本”“老版本自动作废/失效”“不走审批”，验收口径是旧 V1 `SUPERSEDED`、新 V2 `ACTIVE`、master 当前有效版本指向 V2，不要求创建 `OBSOLETE` 审批。发布申请前还必须核对发布申请人拥有 `form:instance:create`、`form:instance:submit`、`system:user:query` 和用户选择弹窗所需的用户查询权限；发布 BPM 审批如果后续节点是 `APPROVE_USER_SELECT`，必须在 BPM 流程详情页等待 `/bpm/process-instance/get-next-approval-nodes` 返回并选择下一节点审批人。受控浏览 viewer 模式的版本追溯入口是 `data-testid="dcc-controlled-preview-version-button"` 打开的版本信息弹窗，变更原因显示在详情基础信息的“提交备注”；受控浏览 traceability 模式是 `/dcc/controlled-file/detail/{id}?traceability=1&from=browser` 的追溯详情页，需验证内嵌“版本历史”表与升版原因；viewer 模式还必须渲染当前有效版的最终目录路径、`publishedFileId`、`stampedFileId` 或等价发布文件信息，不能只在非 viewer 详情路径展示该 linkage 卡片。脚本等待详情接口时必须精确匹配 `/admin-api/dcc/controlled-files/{id}` 的 pathname，避免误抓 `/preview`、`/preview-metadata`、`/access-explanation` 等同 ID 子接口。
- Blocker: DCC 审批中心行只能打开 `viewer=1` 只读预览、非 viewer 详情被路由守卫重定向、页面未渲染签名按钮、处理区只显示空任务提示但没有当前审批动作、只有 `approve-task` API wrapper 但无页面入口、BPM 原生行直接审批返回业务 `403`、发布申请弹窗提示缺审批人、用户选择弹窗因缺 `system:user:query` 报无权限、或 BPM 发布审批返回“下一个任务的审批人未配置”时必须停止并记录 E2E BLOCKED。若用户明确要求手动作废审批链路但运行态缺少已发布 `DCC / DCC / CONTROLLED_FILE / OBSOLETE` 业务审批策略，也必须记录 BLOCKED；若用户明确要求升版自动失效链路，则缺手动作废策略不能阻塞该链路验收。
- Verification: 证据需包含审批中心 DCC 行、跳转后的实际 URL、详情页处理态控件、当前任务按钮文本、签名弹窗、`/dcc/controlled-files/{id}/approve-task` 响应、Flowable 当前任务和 DCC 文件状态；原版上传链路还需包含同一 `file_number` 仅一条 V1.0 `NEW` 文件、状态 `ACTIVE`、master 当前生效版本指向该 V1.0、上传审批完成任务数不少于 4，且不存在升版/修订行；发布/升版自动失效链路还需包含 `bpm_form_action_instance.status=EFFECTIVE`、发布 BPM 完成任务数、旧版本 V1 `SUPERSEDED`、新版本 V2 `ACTIVE`、master 当前生效版本指向 V2、V1 successor 指向 V2；受控浏览链路还需包含 ACTIVE browser-page 只返回/默认打开 V2、V1 不作为当前有效行返回、viewer 版本信息弹窗或 traceability 详情内嵌版本历史可见 V1/V2、详情提交备注/升版原因可见、viewer 页面可见最终目录路径以及 published/stamped 文件 ID。若 blocked，记录路由守卫源码行、页面实际落点、viewer/traceability 模板缺口和任务自有残留数据。
- Forbidden action: 禁止用 BPM 原生审批行替代 DCC 上传审批、直接 API、SQL 改状态、移除断言、绕开路由守卫、只读 viewer 截图、跳过发布申请审批人选择、或把发布 BPM 审批人的 `APPROVE_USER_SELECT` 通过默认值/空值冒充配置完成。禁止把升版后的旧版 `SUPERSEDED` 误判为必须走 `OBSOLETE` 审批；禁止在用户明确要求升版自动作废/失效时，继续用缺手动作废审批策略作为当前链路失败结论。
- Evidence: `doc/tasks/20260802-dcc-upload-revision-e2e/verification-report.md`，DCC 上传升版 E2E 先暴露处理态、发布申请权限和 BPM 下一审批人选择缺口，补齐非 admin 角色权限并改为真实 DCC/BPM 页面路径后完成完整链路验证；`doc/tasks/20260802-dcc-upload-original-e2e/verification-report.md`，DCC 原版上传 E2E 验证 V1.0 `NEW` 文件审批后直接 `ACTIVE`，master 指向原版且无升版行；`doc/tasks/20260802-dcc-controlled-file-obsolete-e2e/verification-report.md`，DCC “作废/废止”需求经用户澄清后按升版自动失效链路验收，真实 Playwright 证明 V1 `SUPERSEDED`、V2 `ACTIVE`、master 指向 V2、受控浏览不再返回 V1 当前有效行，手动作废 OBSOLETE 策略缺失仅作为非当前链路 blocker 记录。

### DCC 升版发布 UX 闭环门禁

- Trigger: DCC 升版/修订发布、版本历史、升版原因、变更说明、发布完成结果、master 当前版本、受控浏览落位、BPM `markers` pageerror 或只读复验已完成发布链路。
- Preflight check: 若复用既有已发布升版数据做只读复验，必须记录数据来源、只读范围、非 admin 账号和目标写请求数为 0；同时区分受控浏览行的“预览”viewer 路径与文件编号追溯详情路径。静态合同必须先锁定版本历史标题、升版原因/变更说明列、发布完成结果摘要和 BPM marker 安全 helper；真实页面复验必须分别打开 V2 追溯详情、受控浏览 viewer 版本历史弹窗和 BPM 流程图。
- Blocker: 只用 API/DB 证明状态、只截图详情页但未打开受控浏览 viewer、版本历史弹窗仍叫“版本信息”、版本历史表只显示 V1/V2 但看不到升版原因/变更说明、发布完成摘要无法同时证明旧版失效/新版生效/master 切换/受控浏览落位、BPM 流程图仍出现未解释 `pageerror`、或只读复验无法证明目标写请求为 0 时必须停止。
- Verification: 证据至少包含聚焦静态契约 PASS、真实 Playwright result JSON、发布完成摘要截图、受控浏览版本历史弹窗截图、BPM 流程图截图、`pageErrors=[]`、目标 DCC 写请求为 0，以及最终报告中的 V1/V2 ID、master 当前版本、受控浏览 published/stamped 文件 ID。
- Forbidden action: 禁止用 admin、API-only、SQL 状态修改、旧 result 覆盖、本轮未打开的旧截图、忽略 `markers` pageerror、隐藏流程图高亮、删除版本历史断言或把只读 traceability 路径冒充 viewer 受控浏览落位。
- Evidence: `doc/tasks/20260802-dcc-revision-ux-final-fixes/verification-report.md`，DCC 升版发布 UX 三项缺口通过聚焦静态契约和真实只读 Playwright 复验证明：发布完成摘要、受控浏览版本历史弹窗、BPM marker pageerror 均闭环，且目标写请求为 0。

### 规划型 E2E 前置与业务 RED 分离门禁

- Trigger: 根据 Excel、PRD、开发文档或测试方案落地多里程碑功能，且后续里程碑需要提前建立 BDD/TDD/E2E gate。
- Preflight check: M0 或首个前置里程碑必须先补齐计划中声明的 script、spec、真实前置检查和证据输出；只有脚本入口、测试文件、工作目录和命令解析均有效后，后续失败才可记录为业务 RED。
- Blocker: 脚本缺失、测试文件缺失、命令无法解析、真实租户/账号/签名/任务数据缺失、正式 source map 未冻结时，必须记录为前置 blocker；不得把缺入口或缺环境写成业务 RED，也不得进入下一里程碑。
- Verification: 证据需要同时记录“入口合同 PASS”“真实前置 BLOCKED 及当前缺口数量”“规划静态脚本业务 RED”，并在后续扩展 source gate 后同步清理旧口径，避免任务文档保留过期 blocker 数量或“脚本缺失”结论。
- Forbidden action: 禁止为了制造 RED 临时写无效脚本、把静态合同当真实 E2E、在 M0 未通过时提前实现 M1-M6 生产代码，或用 API-only/默认值/占位成功绕过正式来源冻结。
- Evidence: `doc/tasks/20260801-role-requirement-matrix-implementation/execution-log.md`，岗位需求分解矩阵 M0 将脚本缺失前置转为可执行入口合同，并把后续 M3/M4/M5 脚本固定为业务 RED。

### Playwright 浏览器可执行文件门禁

- Trigger: `browserType.launch: Executable doesn't exist`、`npx playwright install` 提示、`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH`、本机 Chrome/Edge 已安装但 Playwright 缓存浏览器缺失。
- Preflight check: 先检查本机正式浏览器路径，例如 `C:\Program Files\Google\Chrome\Application\chrome.exe` 或 Edge；若存在，可通过 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 显式传给登录预检和任务 E2E，并在任务日志记录路径来源。
- Blocker: 本机没有可用 Chrome/Edge、指定路径不存在、或浏览器版本无法启动时，必须记录 E2E 前置缺口；不得把浏览器缺失写成产品失败。
- Verification: 复跑官方登录预检或目标真实 E2E，证明确实使用该可执行文件完成真实页面断言。
- Forbidden action: 禁止静默下载或切换未知浏览器缓存、禁止用 API-only 代替页面验证、禁止把 Playwright 浏览器缓存缺失冒充业务页面不可达。
- Evidence: `doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/verification-report.md`，D-Main 真实 E2E 使用本机 Chrome 完成 DCC 产品目录排序验证。

### Playwright 目标链路与外部资源异常归因门禁

- Trigger: Playwright 捕获到 `console error`、`requestfailed` 或非 2xx 响应，且失败 URL 包含外部头像、图片、CDN、非当前页签接口或其它非本轮目标链路资源。
- Preflight check: 采集失败请求的完整 URL、状态码和资源类型；按本机前端、当前后端、目标业务 API 与目标读写接口定义目标链路。多页签页面必须按当前验收页签精确限定目标接口，例如审批中心“已办”只把 `/approval-center/done` 和 `viewType=DONE` 列为目标链路，页签切换中被浏览器中止的 TODO 请求只能单独记录为非 DONE 审批中心请求，不得冒充 DONE 失败。只有在确认非目标 URL 未造成目标控件缺失且目标行为断言独立通过后，才允许将其单独记录为非目标链路异常；不得用域名白名单批量忽略错误。
- Blocker: 任一本机目标业务请求失败、出现未解释的 `pageerror`、外部或非目标请求失败导致目标页面或控件不可用、无法确认目标写请求数量，或失败请求归属不明确时必须停止。
- Verification: 证据必须同时记录目标链路错误数、非目标同域请求或外部异常 URL 与状态码、`pageerror` 数量、目标 UI 断言和目标写请求数量；只读/取消确认路径必须明确证明写请求为 0。
- Forbidden action: 禁止全局关闭 console/network 断言、忽略全部第三方域名、把页面 HTTP 200 当作目标功能通过，或省略外部异常证据。
- Evidence: `doc/tasks/20260801-dcc-list-auto-classify-local-e2e/verification-report.md`，DCC 列表只读 E2E 将外部头像 502 与本机/DCC 目标链路分开归因，并证明目标链路错误数和 DCC 写请求数均为 0。

### Playwright 快照与 daemon 收尾门禁

- Trigger: 使用 Playwright CLI / headed browser 验证登录页、发布控制台、版本变更说明或任何可能包含输入框内容的真实页面。
- Preflight check: 运行前把输出目录限定到当前任务或 releaseTag；验收后扫描 `.playwright-cli\page-*.yml`、trace、截图、视频和 CLI daemon 进程，判断是否包含登录预填字段、账号、密码、token 或任务敏感数据。
- Blocker: 任务输出目录存在未脱敏 `page-*.yml`、trace、视频或截图，或存在命令行可证明属于当前任务的 `cliDaemon.js <task-or-release>` 进程仍占用输出目录时，任务不得 closeout。
- Verification: 删除或脱敏任务自有 Playwright artifact；若目录被锁，只停止命令行明确属于当前任务的 daemon 和子进程；最终记录任务输出目录 `Test-Path=False` 或 artifact 清单为空。
- Forbidden action: 禁止提交原始 Playwright snapshot；禁止为了清理目录误停其他并发 E2E/Playwright 任务；禁止用旧页面快照代替本轮真实页面验证。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260730-head-test-only-release\execution-log.md`，发布验收后清理当前任务 `.playwright-cli` 快照，并只停止 `cliDaemon.js r260731c-r2 --headed` 任务归属进程树。

### Worktree / int_main 运行态 URL 门禁

- Trigger: 主工作区默认端口被并行任务占用、旧 jar 未加载当前接口、真实 E2E 需要使用已登记 worktree slot 端口运行，或 worktree 融合后需要在 `E:\IntRuoyi` 的 `int_main` 主端口复验。
- Preflight check: 同时显式传入前端和后端 URL；附加 worktree 必须来自同一 runtime slot，融合后主运行态只允许 `8081/48081` 且端口命令行归属 `E:\IntRuoyi`。脚本应只允许这两种合法模式：`int_main 8081/48081` 或成对 `int_main slot 1..19`。
- Blocker: 只传一个 URL、端口既不是 `8081/48081` 又不属于同一 slot、未确认端口监听命令行归属目标 worktree/主工作区、或后端业务接口返回配置缺失/404 时必须停止并记录真实原因，不得静默切换端口或 API-only。
- Verification: 记录 base URL、backend URL、端口归属、前端 HTTP 200、后端 health UP、关键目标接口业务响应、真实页面断言，以及任务结束后的任务自有数据清理结果。
- Forbidden action: 禁止强停并行 48081、随机换端口、只看 health 就宣称目标 Controller 已加载、用未配对的 frontend/backend URL 造成前端访问旧后端，或让融合后 E2E 脚本拒绝合法 `int_main 8081/48081` 主运行态。
- Evidence: `doc/tasks/20260726-edhr-release-dossier-requirement-switches/execution-log.md`，48081 旧 jar 返回新增接口 404 后，使用 slot 5 的 8086/48086 成对 URL 完成真实 E2E；`doc/tasks/20260727-edhr-visual-fill-config-implementation/execution-log.md`，融合后先在 slot 2 通过，再修正脚本允许 `int_main 8081/48081` 并完成主端口真实 E2E。

### Playwright 全新上下文登录导航竞争门禁

- Trigger: 真实 E2E 登录阶段出现 `Execution context was destroyed, most likely because of a navigation`，脚本在首次打开登录页后执行 `page.evaluate(() => localStorage.clear())`，或登录页存在自动重定向。
- Preflight check: `browser.newContext()` 创建的非持久化上下文默认没有上一轮 cookie、localStorage 或 sessionStorage；登录脚本应在首次导航前按需调用 `context.clearCookies()`，然后只导航一次登录页。若任务确需清理持久化 storage，必须使用受控持久化上下文并在应用加载前完成，不得在 Vue 路由已启动后清理。
- Blocker: 首次 `page.goto()` 后页面正在自动跳转、`page.evaluate` 因执行上下文销毁失败、登录请求尚未发出，或通过捕获该异常继续执行时必须停止并修正登录前置顺序。
- Verification: `node --check <real-e2e-script>` 通过后，使用官方登录身份和真实前后端 URL 重跑完整 Playwright 路径，必须得到登录接口成功、目标页面断言 PASS、任务自有 fixture 清理为 0。
- Forbidden action: 禁止吞掉导航异常、循环重试登录掩盖脚本竞争、复用带未知登录态的持久化 profile、或把已生成的旧截图/旧 `result.json` 当成本轮 E2E PASS。
- Evidence: `doc/tasks/20260730-edhr-frontline-fill-tabs/execution-log.md`，一线填写真实 E2E 在全新 context 的首次登录页导航后清 storage 触发执行上下文销毁，改为导航前清 cookie 且单次打开登录页后通过。

### Windows 换行与脚本行为同步

- Trigger: 修改 `tests/e2e/*static.spec.js`、真实 `*.e2e.js` 脚本、Windows worktree 融合后出现静态合同在目标 worktree 自身失败、CRLF/LF 差异或废弃弹窗流程断言。
- Preflight check: 先在目标 worktree 和当前工作区分别运行同一静态合同；读取源码时对只检查模板片段的静态合同统一归一化 CRLF 为 LF；定位 Vue/SFC 弹框、函数或组件块时优先按稳定 class、data 属性、组件名或下一个函数/组件声明回找边界，不用缩进数量精确匹配；负向断言必须先收窄到目标函数/模板块，避免把同文件无关编辑表单、弹窗或其它能力中的合法字段误判为失败；确认真实 E2E 脚本与当前页面真实用户路径一致。
- Blocker: 若静态合同在目标 worktree 自身也失败，必须先判断是合同过期、换行误判、正则范围过宽还是产品实现失败；不得把目标 worktree 自身失败直接当作融合漏项。
- Narrow fix: 若当前任务只修一个窄范围页面缺陷，而同一个宽静态合同存在无关既存失败，先保留失败证据，再新增或运行聚焦本缺陷的独立静态合同；不得为了通过宽合同顺手改无关产品逻辑或断言。
- Verification: 更新静态合同后必须重跑目标 worktree 涉及的全部静态合同；涉及真实 E2E 脚本行为变更时，至少用静态合同断言真实脚本等待的 API、点击的按钮和禁止的旧弹窗步骤。
- Forbidden action: 禁止为通过静态合同改产品文案或 DOM 顺序；禁止保留真实脚本里的废弃确认弹窗、签名密码输入或 API-only 替代页面点击。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`。

### 真实 E2E 阶段归因门禁

- Trigger: 复用一个覆盖多阶段的真实 E2E 验证窄范围改动，脚本在目标页面保存或目标断言后继续进入路线、批次、审批、清理等后续阶段。
- Preflight check: 运行前标出本任务必须证明的阶段和后续阶段边界；脚本结果 JSON 必须记录阶段性证据字段，例如目标弹窗可见、目标保存响应、任务自有数据清理状态。
- Blocker: 如果目标阶段之前失败，当前任务验证不得放行；如果目标阶段已通过但后续阶段失败，必须记录后续失败位置和清理结果，不得把整条 E2E 宣称为 PASS。
- Verification: 当前任务报告同时写入整条命令退出状态、目标阶段证据、后续失败断言文本、清理恢复结果，以及为何该失败不属于本次行为变更。
- Forbidden action: 禁止删除后续断言来制造整条 PASS；禁止把目标阶段通过冒充 full-chain 通过；禁止在失败后遗漏共享配置恢复或任务自有数据清理。
- Evidence: `doc/tasks/20260728-assist-role-responsibility-mode/verification-report.md`，填写配置保存阶段已返回 `adminSave.assistRowCount/assignmentCount`，后续路线绑定断言失败并完成配置恢复和路线清理。

### 真实 E2E 主链路与扩展诊断产物隔离门禁

- Trigger: 同一任务目录内同时运行主验收链路、resume 复核、权限负向验证、traceability/viewer linkage/诊断脚本，或多个脚本默认写同一个 `e2e-result.json`、`result.json`、`verification-report.md`、`final-readonly-db-verification.json`。
- Preflight check: 运行前必须明确本轮用户要求的主链路范围与可选扩展断言边界；主链路结果文件、扩展诊断结果文件和固定最终证据文件必须使用不同路径，或在脚本启动前确认无同任务目录写入进程会覆盖默认结果。若完成门禁同时读取 Markdown evidence 和 Playwright `result.json`，二者必须来自同一个 task root 和同一轮 run，不能回退读取主工作区、其它 worktree 或历史 run 的同名结果；若 evidence 记录目标请求或响应身份，`result.json.targetRequestEvidenceFlushed` 必须为 `true`，`result.json.targetRequests` 每一项必须是 JSON object，`result.json.targetRequests` 与 Markdown 中的 URL、Method、HTTP Status、Business Code 等关键请求字段必须逐项一致，且每个 `targetRequests[*].label`、`targetRequests[*].url` 与 `targetRequests[*].method` 必须存在并非空，`targetRequests[*].httpStatus` 与 `targetRequests[*].businessCode` 都必须存在并可解析为数字；`result.json.targetResponseIdentities.<LABEL>` 每一项必须是 JSON object，`result.json.targetResponseIdentities.<LABEL>.field` 必须存在且非空，`result.json.targetResponseIdentities.<LABEL>.value` 必须存在且可解析为正整数，`result.json.targetResponseIdentities.<LABEL>.sourceRequestLabel` 必须存在且非空，并绑定回同一个 canonical `<LABEL>`，且 `targetResponseIdentities` key 集合必须等于同一 artifact 内 `targetRequests[].label` 观测集合。扩展断言必须通过显式 opt-in 环境变量开启，默认不得影响主链路验收结论。
- Blocker: 若扩展诊断脚本仍在运行、默认结果文件被其它进程改写、可选断言失败覆盖主链路 PASS、报告中的文件号/状态与最新主链路结果不一致、Markdown evidence 与 `result.json` 的 status/root ID/关键闭环字段不一致，Markdown 目标请求成功但 `result.json.targetRequestEvidenceFlushed` 不是 true、`result.json.targetRequests` 缺失、`targetRequests[*]` 非 JSON object、指向其它后端、方法/HTTP 状态/业务码不一致、`label` 缺失/为空、`url` 缺失/为空、`method` 缺失/为空、`httpStatus` 缺失/非数字、`businessCode` 缺失/非数字，或 `result.json.targetResponseIdentities.<LABEL>` 非 JSON object、缺少 `field`、缺少可解析正整数 `value`、缺少对应 `sourceRequestLabel`、`sourceRequestLabel` 为空 / 串用其它 label / 与 `targetRequests[].label` 观测集合不一致，必须停止收尾并恢复到清晰的主链路证据；不得把被扩展诊断覆盖的 `BLOCKED`、旧文件号或旧 `result.json` 当作当前验收结论。
- Verification: 收尾前延迟复查一次结果文件和任务文档，记录无当前任务脚本进程、默认结果和固定最终结果均为预期状态，且 `verification-report.md`、`task.md`、`execution-log.md` 的文件编号、文件 ID、master ID、状态、浏览路径、目标请求以及 Markdown evidence 与 `result.json` 的核心身份字段一致。
- Forbidden action: 禁止多个并行 Playwright 脚本共享同一个最终结果路径；禁止扩展诊断失败后直接改口主场景 BLOCKED 或 PASS；禁止用旧 resume 结果覆盖新建任务文件；禁止让 completion gate 为了“找得到结果”跨 task root 读取旧 run 或其它 worktree 的 `result.json`；禁止 Markdown 手工写目标请求成功而 `result.json.targetRequests` 指向旧后端、缺少真实请求、缺少实际 label、缺少实际 URL、缺少实际方法、缺少可解析 HTTP 状态或缺少可解析业务码；禁止只用响应身份 key 替代 `field` / `value` 结构化采集，禁止只用响应身份 key、field、value 三元组替代来源请求 label 绑定或跳过请求/响应身份集合一致性；禁止把可选 viewer linkage、签核追溯、权限负向验证混入用户明确限定的主验收范围。
- Evidence: `doc/tasks/20260802-dcc-original-release-e2e-current/execution-log.md`，DCC 原版发布主链路 PASS 后，可选 viewer linkage / traceability 诊断多次覆盖默认结果和报告，最终通过显式关闭扩展断言、固定主链路结果文件并延迟复查结果稳定性收口。
- Evidence: `doc/tasks/20260802-dcc-traceability-ux-fixes/verification-report.md`，签核追溯 UX 复验先识别默认 ACTIVE 源缺待签名按钮导致错误密码诊断不适用，再显式绑定任务自有 wrong-password 结果文件，最终同时证明页面 UX、只读一致性和 `dccWriteRequests=[]`。

### 真实 E2E 页面加载判据门禁

- Trigger: 真实 Playwright 验证只读详情页、批次执行详情、当前工序高亮、页面顶部批号/执行号/标题文案可能与接口字段不一致。
- Preflight check: 脚本必须先等待目标业务接口命中目标对象 ID，再等待本次需求真正依赖的页面控件或状态渲染；只读页面可用任务组、状态 class、颜色、按钮可见性等目标控件作为页面加载判据。
- Blocker: 页面不稳定展示内部执行号、生产批号或标题文本时，不得让这类文本等待替代目标行为断言；若目标业务控件未渲染或接口未命中目标 ID，必须失败并截图记录。
- Verification: 证据需包含目标接口 ID、目标页面控件状态、截图路径、关键样式/交互断言和 MES 写请求数；修正等待条件后必须重跑真实 Playwright。
- Forbidden action: 禁止为了通过 E2E 删除目标页面断言、改成 API-only、等待无关菜单/标题文本、或把页面未渲染解释成接口已通过。
- Evidence: `doc/tasks/20260729-edhr-parallel-start-process-highlight/verification-report.md`，真实脚本改为接口命中目标批次后等待工序组渲染，并断言三 个当前工序黄底。

### 真实 E2E 用户列配置与列表可见性门禁

- Trigger: 真实 Playwright 验证报工列表、排产工单、统一列表模板或任何支持用户自定义显示字段的表格，尤其默认用户列配置可能隐藏单据编号、报工单号、内部 ID 或状态列。
- Preflight check: 列表断言前先确认当前用户实际可见列；若目标编号列被隐藏，必须改用页面可见的业务唯一组合（如工序编码、人员工号、数量、来源生产工单号、进度文本）证明列表更新，并用 DB 或只读 API 复核隐藏编号与正式记录绑定。
- Blocker: 页面已显示目标业务行但脚本只因隐藏编号列缺失而失败时，不得判定产品失败；若可见业务组合也不足以唯一证明目标行，必须记录当前可见列并补充只读后置核验。
- Verification: 证据需同时包含真实页面可见字段断言、隐藏编号的只读 DB/API 绑定证据、目标页面路由和当前用户列配置影响说明。
- Forbidden action: 禁止把用户列配置隐藏导致的编号不可见写成业务未更新；禁止为通过 E2E 强行重置用户列配置、改用 API-only 替代页面列表、或断言不可见列文本。
- Evidence: `doc/tasks/20260802-test-server-feedback-import-not-working/verification-report.md`，报工列表当前列配置未显示报工单号，但页面显示 5 条导入明细，DB 复核 `FB-000157` 至 `FB-000161` 与导入记录绑定，排产工单页面 `/mes/pro/schedule-order` 显示目标工单进度。

### 真实 E2E 动态事件查询与确认响应门禁

- Trigger: 真实 E2E 在页面写入后需要只读发现新生成事件、提交记录、分配记录、审计记录或其它运行态 ID，或确认按钮依赖后端写接口完成后继续断言。
- Preflight check: 只读发现接口必须携带后端分页接口要求的完整查询条件，例如日期、租户、业务对象、提交编码或任务自有前缀；确认类动作必须等待对应写接口响应，断言 HTTP 成功且业务 `code=0`，再进入后续 UI 或只读核验。
- Blocker: 只按列表默认条件查询导致接口 500、跨日/跨页误选、用外部预填 eventId 替代页面提交后动态发现、等待瞬时 toast 而未等待写接口响应，或确认接口业务码非 0 时必须停止。
- Verification: 静态合同应锁定真实 E2E 对必填查询条件、动态 ID 占位符和确认接口响应断言的使用；真实 E2E 证据需记录动态发现的 ID、确认接口路径、业务响应校验和后置只读核验结果。
- Forbidden action: 禁止把 toast 文案、列表第一行、硬编码事件 ID、API-only 写入、或忽略业务 `code` 的 HTTP 200 当作确认完成。
- Evidence: `doc/tasks/20260731-team-leader-workbench-prd-plan/execution-log.md`，生产组长真实 E2E 事件发现补齐 `submitDate`，确认报工改为等待 allocation confirm 响应并断言业务码。

### Schema-backed E2E 迁移与字段可选态门禁

- Trigger: 真实 E2E 验证新增 schema 字段支撑的页面能力、工作台上下文字段、单元格链接、字段矩阵、合成来源字段、`source_type`、`source_field_code`、`sourceFields`、或页面接口返回 `Unknown column` / `系统异常`。
- Preflight check: 浏览器路径前先核对当前后端连接库已应用本任务正式迁移；若页面展示合成字段矩阵，E2E 必须断言可见文本和可交互态同时存在，例如 `.is-source-selectable`、选中态、目标单元格选择和主动作按钮 enabled。
- Blocker: 缺迁移列、接口 500、字段文字可见但没有可选 class、点击字段后选中态不变、或只读账号需要写入保存才能证明行为时必须停止并记录；不得把“页面看得到字段”当成可选择或可保存通过。
- Verification: 证据需包含 schema 列核对结果、真实前端入口 URL、租户/用户标签、字段白名单数量、目标页可见断言、可选/选中态断言、主动作按钮状态，以及是否发送 MES 写请求。
- Forbidden action: 禁止用 API-only、mock response、绕过页面直连 URL、忽略 schema 缺列、只断言文本不断言可选态、或在 `芋道源码/admin` 基线数据上保存规则冒充写入 E2E。
- Evidence: `doc/tasks/20260726-work-order-field-cell-link/verification-report.md`。
## 禁止做法

- 禁止 mock 数据冒充真实 E2E。
- 禁止 API-only 代替前端路径。
- 禁止直接 SQL 或接口直塞绕过页面。
- 禁止修改生产租户、admin 基线数据或无关真实业务记录。
- 禁止为了测试额外添加产品上不需要的前端控件。

## 验证方式

- 记录 Playwright 命令、入口 URL、租户/用户标签、目标页面和关键断言。
- 写入型 E2E 记录测试数据标识和清理方式。
- 失败时记录实际失败位置、页面状态、网络响应或控制台错误。


## 全局开关类 E2E 恢复门禁

- Trigger: Playwright 验证全局开关、共享配置、租户级开关、系统级配置或任何影响后续用户路径的运行态状态切换。
- Preflight check: 切换前读取并记录原始状态；脚本必须有 `finally` 恢复逻辑，恢复后再用独立 API 或页面断言确认状态回到原始值。
- Blocker: 关闭/开启断言通过但恢复失败、恢复后接口值不一致、或页面仍显示变更后的状态时，必须立即执行受控恢复并记录失败位置；不得把产品断言 PASS 当作完整 E2E PASS。
- Verification: 证据必须同时包含变更态断言、恢复动作结果、恢复后页面或接口复验；恢复使用 API 时必须说明它是 cleanup，不得替代真实页面变更路径。
- Forbidden action: 禁止留下全局开关关闭、禁止记录密码/token、禁止用未复验的 `finally` 假设恢复成功。
- Evidence: `doc/tasks/20260725-edhr-global-recordbook-switch/verification-report.md`。



## 官方登录前置与 admin-only 全量验证门禁

- Trigger: E2E 脚本调用 `scripts/preflight/login-preflight.mjs`、执行 `芋道源码/admin` 只读全量验证、或工作区融合后发现真实 E2E 登录前置脚本缺失/目标文案过期。
- Preflight check: `scripts/preflight/login-preflight.mjs` 必须存在于工作区根目录并通过真实前端登录；目标文本必须使用当前页面真实可见文案，不得沿用历史菜单标题。密码只能通过临时环境变量或命令参数传入，任务日志和证据必须脱敏。若 Playwright 默认浏览器缓存缺失，先检查本机稳定 Chrome/Edge 可执行文件并通过 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 显式传入；只有缺少可用浏览器时才记录 E2E BLOCKED，不得临时下载浏览器或把浏览器缺失写成业务页面失败。
- Blocker: 若只授权 `芋道源码/admin`，写入型、多用户、签名、放行、发布或需测试租户数据清理的 E2E 必须记录 BLOCKED；不得在 admin 基线租户上创造测试写入数据，也不得用 API-only、直连历史 execution 填写页或 mock 代替。
- Verification: 管理员只读验证应优先覆盖登录前置、目标页面文案、关键目标接口业务码、批次详情、只读预览、伴随单据、表单日志、权限可见性和无 MES 写请求；当前活动填写必须走正式页面按钮或 `openTask` 返回上下文，历史只读必须走 tracking 模式。若先因浏览器缓存或运行库迁移缺失失败，必须记录 RED 原因、解除动作和复跑 GREEN 证据。
- Forbidden action: 禁止删除或跳过官方登录 preflight；禁止把缺失 preflight 脚本当成 E2E 通过；禁止在真实脚本中保留历史默认密码；禁止把过期固定批次/任务 ID 当作长期前置。
- Evidence: `doc/tasks/20260725-full-e2e-admin-validation/verification-report.md`；`doc/tasks/20260730-banzuzhang/verification-report.md`。

### eDHR 管理员主区域已提交内容门禁

- Trigger: Playwright 验证批记录管理员在批次详情主区域查看已提交批记录内容、无已提交内容时显示空表单、`review-timeline.executionReviews.formViewModel`、`selectedEmptyTaskPreviewFormViewModel`、或排查主区域是否读取草稿/快照。
- Preflight check: 浏览器路径前先用当前后端登录态确认目标批次 `review-timeline` 业务码成功。若验证已提交内容，目标 `execution.status` 必须属于已提交/已批准/完成态，且 `formViewModel.cellValuesJson` 含可页面断言的非空单元格值。若验证无已提交内容，必须明确记录 `execution.status=0` 或无 submitted execution，并确认 `task/preview` 仅用于取得正式模板壳。
- Blocker: 目标批次只有草稿执行记录但页面展示草稿 `cell_values_json`、空表单直接绑定 `selectedTaskPreview.formViewModel` 未清空单元值、历史样本 `review-timeline` 返回 `eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录`、本地库只有过期冻结快照样本、或缺少可写测试账号/签名密码时，必须记录 BLOCKED；不得用历史 execution 直连、API-only、草稿 cell_values_json、旧样本截图或 admin 写入替代页面验证。
- Verification: 证据需包含成对 frontend/backend URL、租户/用户标签、批次 ID、任务 ID、execution ID/status、`review-timeline` HTTP/业务码、主区域只读原表或空表单断言、submitted 场景 `/task/preview` 请求数为 0、空表单场景 `task/preview` HTTP/业务码且 `executionCreated=false`、MES 写请求数为 0、artifact JSON 和截图路径。
- Forbidden action: 禁止把“草稿有 cell_values_json”解释为管理员应显示内容；禁止把 task preview 的单元值冒充已提交内容；禁止在 admin 基线租户上临时造提交样本；禁止跳过 `review-timeline` 当前接口门禁后宣称提交后显示通过。
- Evidence: `doc/tasks/20260729-admin-submitted-content-e2e/verification-report.md`；`doc/tasks/20260729-edhr-fill-submitted-form-content/verification-report.md`。
## eDHR 批次执行数据库夹具与证据文件门禁

- Trigger: 运行 `edhr-batch-execution-real-flow.e2e.js`、复跑 eDHR 批次执行真实 E2E、或脚本默认写入 `doc/tasks/<task-id>/real-e2e-evidence.md`。
- Preflight check: 默认从本机 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro` 读取授权租户、账号、批次执行、批次任务、工作任务和执行 ID；写型验证若需调整责任人或夹具数据，必须先记录原始值、影响行数和回滚 SQL。读取既有批次任务时，还必须核对 `form_slot_type` 与目标报表 `form_slot_type` 一致，且 `slot_config_snapshot_hash` 非空，否则详情页可能返回 blocked 响应或前端禁用“打开填写”。`EDHR_BATCH_E2E_TASK_ID`、`EDHR_BATCH_E2E_EVIDENCE_FILE`、浏览器路径等只允许作为可选运行参数，不得作为工单、批次、填写值或签名密码的必需来源。
- Blocker: 本地数据库不可达、授权租户/账号不存在、无当前账号可打开的待办工作任务、目标租户未获当前任务明确授权、写入影响行数不是预期值、`form_slot_type`/槽位快照与正式报表不一致、或证据路径会覆盖非当前任务历史 PASS 证据时，必须停止，不得进入浏览器或伪造通过。
- Verification: 记录 E2E 命令、证据文件路径、入口 URL、租户/账号标签、数据库来源、批次执行 ID、任务 ID、执行 ID、DB 写入行数、回滚方式，以及脚本 PASS/BLOCKED 结果；打开执行页后如默认处于“填写辅助模式”，需要切到“原表模式”再断言批记录单元格输入控件显示已落库值。
- Forbidden action: 禁止把工单/批次/密码等业务数据重新改成必需环境变量；禁止记录明文密码；禁止用 mock、API-only、默认成功、生产/未授权租户或未记录的数据库直改替代真实前端路径。
- Evidence: `doc/tasks/fix-batch-record-fill-rule/execution-log.md`，2026-07-25 脚本已改为数据库夹具读取，并在用户授权的 `芋道源码/admin` 下完成真实前端 E2E。

### eDHR 工作任务 FormCenter 动态表单夹具门禁

- Trigger: 运行或修改 `edhr-work-task-process-advance-real.e2e.js`、个人工作台 `edhr-work-task/my-page` 到 FormCenter 动态表单的真实 E2E，或出现 `生产工单不存在`、`当前工艺路线工序未配置默认批记录报表`、`eDHR 批次工序任务被阻塞`。
- Preflight check: 夹具必须创建任务自有真实 `mes_pro_work_order` 并贯穿 `batch_execution/work_task`；FormCenter 动态路线表单任务必须 `batch_record_report_id` 为空、`form_binding_key` 非空、`form_template_id/form_template_version_id/form_center_instance_id` 完整；首工序全部同工序任务必须 `root_process_flag=true`，下一工序必须写入 `predecessor_route_process_id`。若 E2E 验证切换填写人后打开 FormCenter 槽位，必须确认运行态渲染来自 `task/open` 响应的模板快照和实例草稿，普通填写人不应依赖模板管理接口 `/form-center/templates/{id}/versions/{versionNo}` 成功。
- Blocker: 缺少真实工单、把 FormCenter binding key 塞进 `batch_record_report_id`、动态任务缺 FormCenter 上下文、首工序非 root、下一工序无 predecessor、或页面点击未限定目标可见行时必须停止修复夹具；不得放松后端 `task/open` 校验。
- Verification: 真实 E2E 必须从个人工作台按目标批次和任务编码所在 `.el-table__body-wrapper tbody tr:visible` 点击“处理”，提交 FormCenter 抽屉后用 DB 断言当前任务完成、effect applied、下一工序 fill count 符合业务规则，并在 finally/收尾中清理 `EDHR-ADV-%` 任务自有数据；切换填写人路径还必须记录 `task/open` payload 的 `taskId + assistUserId`、响应里的 FormCenter 模板快照字段、页面表单控件渲染结果，以及没有因 `form:template:query` 权限缺失导致 403 或空表单。
- Forbidden action: 禁止用固定不存在工单 ID、API-only submit、直连详情 URL、点击页面第一个“处理”按钮、把动态表单降级为传统批记录、或保留明文 MySQL 密码参数。
- Evidence: `doc/tasks/20260727-edhr-process-fill-advance-optimization/verification-report.md`。

## eDHR 作废 BPM 审批真实 E2E 门禁

- Trigger: Playwright 验证 eDHR 批次作废、`void-batch-execution/approval-resolution`、`void-batch-execution/request`、审批中心 `BPM_REQUIRED`、或作废后工作台待办闭环。
- Preflight check: 作废弹窗打开前就启动 `approval-resolution` 响应等待，因为页面可能在打开弹窗时解析审批策略；提交作废前再等待 `request` 响应。若策略为 `BPM_REQUIRED`，必须通过审批中心真实页面审核，并按 `act_ru_task.PROC_INST_ID_` 的实际 `ASSIGNEE_` 映射 `system_users.username` 登录审批人。
- Blocker: 未捕获 `approval-resolution`、审批待办不属于当前 `processInstanceId`、审批人账号无法映射、审批中心列表未出现目标行、或作废后仍有 TODO/DOING/OVERDUE 工作任务时必须停止。
- Verification: 证据需包含成对 frontend/backend URL、作废列表页行级点击、`approval-resolution` 与 `request` HTTP 200、审批中心行级“审核”点击、`tasks/review` payload 锁定同一 `processInstanceId`、批次状态 `VOIDED`、变更事件 `EFFECTIVE`、活动工作任务取消、负责人工作台 `my-page/stats` 排除、旧任务链接 fail-fast、artifact JSON 路径。
- Forbidden action: 禁止把 `approval-resolution` 当作提交后才发生的请求；禁止硬编码固定审批人；禁止用接口直审、SQL 改状态、API-only 或前端隐藏替代真实审批中心路径。
- Evidence: `doc/tasks/20260727-edhr-batch-void-work-task-closure/verification-report.md`。

## eDHR 跨系统路线产品夹具门禁

- Trigger: 真实 E2E 需要从批记录 Word 导入路线、绑定 DCC 项目代码/MES 物料、创建金蝶生产订单、同步 MES 工单并生成员工待办。
- Preflight check: 脚本必须把任务批记录夹具名、目标表单名和路线产品名分开配置；写入前先校验 DCC 项目名与项目代码、MES 物料编码/名称、`batchFlag`、路线产品绑定、金蝶物料编码和计量单位是否一致。
- Blocker: 任一环节缺失或不一致时必须在导入/创建工单前 fail fast，记录缺失的正式前置；不得先创建冲突 DCC 项目代码、不得用另一产品名冒充任务夹具、不得调用 MES 手工工单接口绕过金蝶同步。
- Verification: 证据应记录本地未跟踪配置路径、租户/账号标签、路线产品名、项目代码、MES item、路线 ID、金蝶生产订单创建结果、MES 工单同步结果和员工待办打开结果；密码/token 必须脱敏。
- Forbidden action: 禁止把任务批记录名直接当路线产品名、禁止用 API-only/样本接口/直接 SQL 造待办、禁止把金蝶物料不存在或 MES 物料未启用批次绑定解释为页面 E2E 失败。

## eDHR 任务专用路线副本 E2E 门禁

- Trigger: 真实 E2E 需要验证目标批记录表单生成员工待办，但共享工艺路线当前激活版本未绑定目标 `batchRecordReports`。
- Preflight check: 先通过认证只读接口确认目标工单可用来源路线、来源 ACTIVE 版本 `configSnapshots.batchRecordAttachmentOwners` 为数组、目标工序、目标报表 ID/编码和当前绑定状态；若需要写入，只能在用户授权范围内通过真实页面复制任务专用路线、创建候选版本、逐工序绑定正式批记录报表、提交发布并启用副本。
- Blocker: 缺少来源路线、来源 ACTIVE 附件负责人快照、目标工序、目标报表唯一编码、候选版本草稿、电子签名发布能力或任务专用路线清理能力时必须停止；不得修改共享路线、选择任意第一条路线、把表单槽位 `formBindings` 当批记录报表绑定，或用 API-only 造路线/批次。
- Verification: E2E 必须按精确任务路线编码创建批次，创建前只读确认任务路线 ACTIVE/候选发布快照仍保留 `batchRecordAttachmentOwners` 数组，创建后只读确认目标 `batchRecordReportId` 的批次任务真实存在；finally 必须恢复报表配置、作废任务批次并删除任务路线副本。
- Forbidden action: 禁止在共享路线缺正式批记录绑定或复制路线缺附件负责人快照时继续创建批次后再解释员工无待办；禁止用当前登录人、旧路线绑定、动态表单槽位或默认附件负责人推导批记录任务。

## eDHR 同名批记录报表精确选择门禁

- Trigger: 路线候选版本或其它 Element Plus 下拉需要选择批记录报表，且正式报表目录可能存在同名报表。
- Preflight check: 下拉选项必须展示足以区分的报表编码或唯一业务键；Playwright 选择时必须按目标编码/ID 定位选项，保存后再按读回 ID 核验。
- Blocker: 如果页面只展示报表名称、脚本只能命中第一条同名选项、保存后读回 ID 与目标不一致，必须停止并修复展示/选择合同。
- Verification: 静态合同覆盖编码展示与脚本精确选择；真实 E2E 记录目标 `reportCode`、读回 `batchRecordReportId` 和目标 ID 一致。
- Forbidden action: 禁止按下拉数组下标、名称首个匹配、隐藏 value 猜测或 API-only 选中替代真实页面选择。

## eDHR 任务批次清理幂等门禁

- Trigger: 写入型 E2E 的 finally/cleanup-only 需要清理任务自有批次，而批次列表页面会排除已作废、关闭、归档等终态批次。
- Preflight check: cleanup 先通过真实批次列表页面定位非终态任务自有批次并执行作废；如果列表未命中，只允许用只读详情确认目标批次已处于终态。
- Blocker: 列表未命中且只读详情不是终态、目标批次不属于当前任务标识、或清理需要 SQL/API 写操作时必须停止。
- Verification: cleanup 证据记录批次 ID/编码、列表定位结果、作废动作或 `already-voided` 只读确认，以及最终终态。
- Forbidden action: 禁止把列表排除终态误判为权限缺失后绕过页面清理；禁止对未作废批次用 API-only 或 SQL 执行作废。


## eDHR 历史执行只读验证门禁

- Trigger: Playwright 需要从 eDHR 批次详情、批记录、记录本或执行记录入口打开 `/mes/pro/feedback/edhr-execution/form`，尤其是复验历史 `executionId`、`batchTaskId`、`workTaskId`、`returnPath` 或 `viewMode`。
- Preflight check: 先区分“当前活动填写”与“历史执行只读追踪”。当前活动填写必须通过页面按钮或正式 `openEdhrBatchTask` 流程获取后端返回的当前 execution/workTask 上下文；历史执行只读必须使用 `viewMode=tracking`，并使用具备对象 VIEW 权限的只读账号标签。
- Blocker: 若页面提示“当前用户不是该 eDHR 工作任务责任人”、“非当前活动表单”或 `BATCH_RECORD_EXECUTION:<id>:VIEW` 权限不足，先记录页面正文和账号/租户标签，停止该路径结论；不得把历史 executionId 直接拼成填写 URL 继续跑。
- Verification: 只读 tracking E2E 必须断言 `eDHR 追踪详情`、追踪表单区域、返回批次详情时保留 `batchExecutionId` 与 `batchTaskId`，并断言无 MES 写请求；填写页 toolbar/返回按钮可用性用真实填写路径或静态合同补充覆盖。
- Forbidden action: 禁止用 API-only、管理员写入、旧 executionId 直连填写页、忽略对象级权限、或把 read-only tracking 当作写入路径 fallback。
- Evidence: `doc/tasks/post-merge-jiluben-e2e-20260725/verification-report.md`。
## eDHR 终态批次个人待办门禁

- Trigger: 个人控制台、eDHR 工作任务、`edhr-work-task/my-page`、`edhr-work-task/stats`、`workTaskId` 打开提示“当前 eDHR 批次状态不允许该操作”，或数据库中 `mes_pro_edhr_work_task.status=TODO/OVERDUE` 但关联批次已关闭、归档、驳回或作废。
- Preflight check: 先只读核对 `mes_pro_edhr_work_task.batch_execution_id` 与 `mes_pro_edhr_batch_execution.status`；若批次为终态，`openTask` 阻断是正确保护，应检查个人待办列表、统计和审批中心候选待办是否从源头排除终态批次。
- Blocker: 若真实页面仍展示终态批次任务或统计仍计入终态批次任务，必须修复列表/统计查询；不得放松 `openTask` 的终态批次 fail-fast 校验。
- Verification: 后端回归需覆盖“同一用户同时有正常批次和终态批次 TODO 时，个人待办与统计只返回正常批次”；真实 E2E 用责任人账号进入个人控制台，断言目标终态任务不在 `my-page` 响应和页面正文中，且没有“当前 eDHR 批次状态不允许该操作”。
- Forbidden action: 禁止为了让按钮可点而允许终态批次进入填写页；禁止用前端隐藏、吞 toast、API-only 打开或改任务状态替代列表源头过滤。
- Evidence: `doc/tasks/20260726-edhr-personal-console-open-task-status/verification-report.md`。
## eDHR 单据填写人显示值门禁

- Trigger: Playwright 验证 eDHR 批次详情右侧单据卡片、特殊节点操作区、损耗单、过程检验单、参数记录表、`fillableUsers`、填写人显示值。
- Preflight check: 页面断言前先通过同一登录会话的详情接口读取目标任务 `fillableUsers`，以接口当前 `displayName/nickname/username` 为页面期望值；特殊节点还要确认选中任务后右侧操作区使用同一 task 的 `fillableUsers`，不得硬编码配置页历史 `candidateSourceNames` 格式。
- Blocker: 若详情接口 `fillableUsers` 为空、只返回角色/部门 ID、页面卡片或特殊节点操作区没有显示填写人，或页面显示值与详情接口当前显示值不一致，必须停止并记录接口任务、页面可见区域和账号/租户标签。
- Verification: 真实 E2E 同时记录批次编码/ID、命中任务、接口填写人、页面卡片或特殊节点操作区可见文本和无 MES 写请求检查；接口 `fillableUsers` 正确但页面未渲染不得判定通过。
- Forbidden action: 禁止把旧配置页候选名称、当前登录人、创建人、更新人或账号拼接格式当作页面期望值；禁止把 API-only 或仅详情接口断言当成页面填写人显示通过。
- Evidence: `doc/tasks/20260725-edhr-route-form-filler-e2e/real-e2e-evidence.md`；`doc/tasks/20260727-edhr-special-node-filler-from-route-start/verification-report.md`。
## eDHR 路线表单跳过口径门禁

- Trigger: 修改或验证 eDHR 批次详情右侧路线表单卡片、损耗单、过程检验单、参数记录表、`isOptionalTask`、`canSkipOptionalTask`、`requiredPolicy`、`requiredFlag`、`SKIP` 动作、无 `OPEN_FORM` 的只读查看动作，或错误“必填路线表单不允许跳过”。
- Preflight check: 先核对详情任务的 `requiredPolicy` 和 `allowedActions`；只有 `requiredPolicy === 'OPTIONAL'` 且后端返回 `SKIP` 动作时，前端才允许显示或执行“跳过表单”。若账号无 `OPEN_FORM` 但任务存在 `formCenterInstanceId/formTemplateId` 等查看上下文，必须通过真实卡片点击验证“查看表单”只读抽屉，而不是直连历史 execution 代替。动态表单卡片选中态可以调用统一 `/task/preview`，但后端必须先按完整 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId` 分流到 FormCenter 模板预览，已保存布局读取 `jimuSchemaJson.sheetLayoutJson/layout/rows`，未保存布局但有正式识别字段时按 `recognizedSchemaJson` 生成只读布局，禁止误走批记录 `batchRecordReportId` / Jimu 报表来源。
- Blocker: 若前端用 `requiredFlag=false`、非必填进度口径、表单槽位类型、当前载体选择或本地状态推断可跳过，必须停止并改为后端 `requiredPolicy + allowedActions` 口径。
- Verification: 至少运行聚焦静态合同，断言 `isOptionalTask` 通过 `isOptionalRouteFormTask` 对齐 `requiredPolicy === 'OPTIONAL'`，并断言必填损耗单点击路径调用打开填写而非跳过接口；涉及动态表单卡片中心预览时，必须断言前端只对完整 FormCenter 上下文加载预览，后端从 `FormTemplateVersionDO.jimuSchemaJson` 已保存布局或 `recognizedSchemaJson` 识别字段生成 `FormViewModel` 且不调用批记录报表 JSON。涉及无填写权限但有查看权限时，真实 E2E 必须断言卡片主动作是“查看表单”、抽屉动作按钮全部禁用，未触发 `/task/open`、`/task/special-node/skip` 或表单中心写请求，且页面没有“必填路线表单不允许跳过”红色错误。
- Forbidden action: 禁止为了避开“必填路线表单不允许跳过”而吞掉后端错误、隐藏按钮错误、改文案、API-only 直开历史 execution，或把必填表单改成可跳过。
- Evidence: `doc/tasks/20260725-edhr-loss-form-open-action/verification-report.md`；`doc/tasks/20260728-edhr-dynamic-form-view/verification-report.md`。
## eDHR 右侧表单卡片标题门禁

- Trigger: 修改或验证 eDHR 批次详情右侧当前工序表单卡片标题、`edhr-batch-detail__rail-process-form-name`、`edhr-batch-detail__rail-execution-code`、`resolveTaskDisplayName`、`resolveTaskCardDisplayName`、草稿 `DRAFT` 标识、`EDHRB-` 批次编号展示。
- Preflight check: 先区分页面顶部批次上下文和单据卡片任务标题；批次编号只能作为批次上下文展示，不得作为每张卡片主标题。卡片标题基础名称必须来自当前 task 的表单名称解析，草稿标识只按 `task.status === EDHR_BATCH_TASK_STATUS_DRAFT` 追加 ASCII `*`，非草稿不追加，名称无效时不得追加。右侧卡片仍必须逐 task 展示，不得为了消除重复标题合并、去重或隐藏真实表单任务。
- Blocker: 若右侧当前工序表单卡片列表仍包含 `edhr-batch-detail__rail-execution-code` 或卡片级 `detail?.batchExecutionCode`，若标题 helper 读取批次编号，若草稿判断不用任务自身 `DRAFT` 状态，或状态标签、填写人、门禁原因、打开/查看/接管/跳过动作被一起删改，必须停止并修复。
- Verification: 至少运行 `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js`、`node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js`、`node tests/e2e/edhr-batch-process-companion-forms-static.spec.js`、`node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` 和 `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`；真实登录态、端口和可读批次数据齐备时，还必须用 Playwright 走批次详情页面确认标题、草稿 `*` 和无控制台错误。
- Forbidden action: 禁止用批次编号、表单槽位、`formBindings`、当前登录人、默认 `MAIN` 或后端接口临时改造来替代表单任务名称；禁止通过合并不同表单任务、隐藏产品信息/损耗单/过程检验记录、API-only 直查详情或 mock 页面宣称标题已修复。
- Evidence: `doc/tasks/20260728-edhr-batch-card-title-draft-marker/verification-report.md`。
## eDHR 右侧红框元信息隐藏门禁

- Trigger: 修改 eDHR 批次详情右侧栏、单据卡片、`edhr-batch-detail__primary-fill-meta`、`primaryFormFillMetaItems`、填写人/提交时间摘要、工艺路线配置右侧 `data-flow-panel="selected-field-detail"` 或截图红框区域。
- Preflight check: 先区分“单据卡片内填写人”与“右侧独立填写元信息红框”；删除红框时必须同时确认 `edhr-batch-detail__rail-process-form-filler` 和 `resolveTaskCardFillersText(task)` 仍保留。若修改工艺路线 `batchRecordFormNames` 字段明细，必须确认字段值、链接和节点红绿边框都使用显式槽位匹配，不得把缺少 `formSlotType` 的其它表单默认归入 `MAIN`。
- Blocker: 若源码仍存在 `primary-fill-meta`、`primaryFormFillMetaItems`、`showPrimaryFormFillMeta`、`resolvePrimaryFormFillersText` 或 `resolvePrimaryFormSubmitTimesText`，不得声明红框已删除；若单据卡片填写人被一起删除，必须停止并修复。若 `batchRecordFormNames` 仍通过带默认 `MAIN` 的 `normalizeRecordBindingSlotType` 过滤右侧明细或节点绑定状态，不得声明批记录表单红框过滤完成。
- Verification: 至少运行 `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` 和 `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js`，一个确认红框无残留，一个确认单据卡片填写人保留。涉及工艺路线批记录表单字段明细时，还必须运行 `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js`。
- Forbidden action: 禁止把右侧独立红框移动到其他一级区域伪装删除；禁止为了通过宽静态合同顺手修改与红框无关的审批/提交逻辑。
- Evidence: `doc/tasks/20260725-hide-edhr-right-fill-meta-redbox/bug-regression-evidence.md`；`doc/tasks/20260726-batch-record-detail-panel-form-filter/bug-regression-evidence.md`。
## Element Plus 下拉选择门禁

- Trigger: Playwright 在 Element Plus `el-select` 中选择租户、工单、工艺路线、角色、用户或其他写入型业务对象。
- Preflight check: 优先按页面可见业务唯一文本定位选项，例如租户名称、工单编码、路线编码/名称/ID；填入搜索词后必须等待目标 `.el-select-dropdown__item:visible` 出现并点击该选项。Element Plus 的 placeholder 可能由外层组件展示而不写入真实 `input[placeholder]`，真实 E2E 定位搜索型 `el-select` 时应先用 DOM 快照确认可见 `input.el-select__input[role="combobox"]` 或控件作用域内稳定选择器，再填值触发远程搜索。若 `el-select` 位于 `el-popover`、抽屉内局部弹层或 click-outside 容器中，必须确认下拉面板归属不会触发外层误关闭，必要时使用受控可见状态和 `:teleported="false"` 静态合同锁定。
- Blocker: 如果只按 `input[placeholder=...]` 找不到控件、只填输入框后按 Enter 未触发真实选项选择、目标选项未出现、页面显示文本与脚本断言字段不一致，或选择项点击导致外层 Popover 在确认动作前误关闭，必须停止并记录输入框 DOM 快照、下拉可见文本、弹层状态和相关接口响应，不得继续提交写入。
- Verification: 对写入结果使用 UI 响应和最终只读 API/DB 核验；涉及发布版/草稿版差异时，必须核验落库版本 ID、版本号、快照 JSON 和当前草稿仍存在。Popover 内下拉还必须验证“选择后保持打开、确认成功后显式关闭”。
- Forbidden action: 禁止把接口数组下标、隐藏 value、输入框残留文本、API-only 选中或坐标点击当作真实页面选择。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/execution-log.md`；`doc/tasks/20260726-route-flow-copy-popover-stability/execution-log.md`；`doc/tasks/20260730-standard-template-list-search-alias/`，顶部菜单搜索框视觉上显示 placeholder，但真实 DOM 只有 `input.el-select__input[role="combobox"]`，最终真实 E2E 改用 combobox 后通过。

### Element Plus 上传控件门禁

- Trigger: Playwright 通过 Element Plus `el-upload`、隐藏 `input[type=file]`、拖拽上传区或 Word/附件导入弹窗执行真实文件上传。
- Preflight check: `setInputFiles` 后必须断言可见上传列表出现目标文件名，或断言页面已发出目标上传请求；未看到文件列表时不得直接点击提交并长时间等待响应。
- Blocker: 文件名未进入上传列表、上传请求未触发、导入按钮只触发表单校验、或页面停留在空上传控件时必须记录 BLOCKED；不得改用 API-only 上传替代真实页面路径。
- Verification: 证据需包含真实文件路径、页面入口、上传接口、文件列表断言、请求触发断言、最终响应或阻塞截图。
- Forbidden action: 禁止只因为 `input.files.length > 0` 就认定 Element Plus 组件状态已接收文件；禁止等待接口超时后不记录文件列表状态。
- Evidence: `doc/tasks/20260727-shared-word-parser-real-e2e/verification-report.md`。

### Element Plus 表单值断言门禁

- Trigger: Playwright 需要断言 Element Plus `el-input`、`el-input-number`、`el-select` 搜索输入框或表单项中的当前值，尤其字段值来自页面初始化模板、后端回填或选择项目后的自动绑定。
- Preflight check: 先确认目标值是普通文本节点、选中标签，还是原生 `input/textarea` 的 `value`；若是输入框值，必须定位到对应 `el-form-item` 作用域内的 `input/textarea`，使用 `inputValue()` 或等价 DOM value 断言，不得只用 `getByText` 查找输入框内部值。
- Blocker: `getByText` 找不到输入框值但页面实际已回填、断言误判为业务缺失、或无法区分 label 文本与 value 文本时必须停止并改用表单控件值断言。
- Verification: 真实 E2E 需同时证明目标表单项可见、输入框 value 等于预期业务值，并保留目标接口或页面状态证据；若修复旧 E2E，先记录旧断言 RED，再重跑目标真实路径 GREEN。
- Forbidden action: 禁止把 Element Plus 输入框 value 当作普通可见文本节点断言；禁止为通过测试把输入框值复制成额外隐藏/旁路文本；禁止用 API-only 代替真实页面回填验证。
- Evidence: `doc/tasks/20260805-dcc-project-mdm-binding/verification-report.md`，QA 规程配置选择 `IDI` 后规程名称已在 `el-input` value 中回填，旧 `getByText` 断言误判，改为读取 `规程名称` 表单项 input value 后真实 E2E 通过。

### Element Plus 选择框显示门禁

- Trigger: 修改 Element Plus `el-select` 多选字段、`el-input-number` 数字步进控件、`el-switch` 旁状态标签、弹窗内多列配置表单、角色/人员/租户/目标项等较长业务名称的输入或选中标签显示。
- Preflight check: 先按 `label-width + grid-template-columns + gap` 核算真实输入区宽度；关键字段必须使用专用布局类和静态合同覆盖。`el-input-number` 默认宽度可能大于网格列，必须显式设置 `width: 100%` 收敛到所在列；文本输入列需要 `min-width: 0` 和 `width: 100%`。必要时在 `el-select` 控件作用域内覆盖 `.el-select__tags-text` 默认省略宽度。窄栏里的 Switch 主标签与状态提示不得全部挤在一个可收缩 flex 行内，状态提示较长时应独占行或使用明确 grid 布局，并对关键标签设置不换行；禁用提示不能只用过浅灰色小字，应有足够对比度或明确状态条承载。Playwright 操作 Element Plus Switch 时，不得点击隐藏的 `input[role="switch"]`；应点击可见 `.el-switch` 或 `.el-switch__core`，再读取隐藏 input 的 `aria-checked` 校验状态。
- Blocker: 若选中值、输入值或 Switch 状态提示在控件内仍显示为 `...`、换行后被裁切、文字对比度过低导致视觉上看不清、数字步进控件溢出挤压相邻输入框、只靠 tooltip 或下拉选项完整展示、或静态合同无法锁定该字段专用布局，必须停止并修复布局。
- Verification: 静态合同或真实 E2E 必须断言目标控件有专用布局类、关键列宽足够、数字步进控件收敛到当前列、文本输入框可完整占满分配列、选中标签未继续使用默认省略宽度，Switch 状态提示完整可见、不会被窄栏裁切，且颜色对比足够。
- Forbidden action: 禁止把 `collapse-tags-tooltip`、扩大整页/整弹窗、硬编码当前角色名/目标项名、只验证下拉选项文本、或只调宽一个控件但让相邻控件继续被挤压当成“显示完整”。
- Evidence: `doc/tasks/20260725-edhr-pressure-pump-v13-filler-role/verification-report.md`；`doc/tasks/20260726-codex-test-target-item-input-display/verification-report.md`；`doc/tasks/20260728-edhr-detail-assist-preview-switch/execution-log.md`。

## 表格行定位

- 当页面对列表进行本地排序、过滤或虚拟渲染时，Playwright 必须按页面可见的业务唯一文本定位目标行，再操作同一行的复选框或按钮。
- 不得直接用 API 返回数组下标映射前端表格行；接口排序和页面排序可能不同，会误选冻结行、错误行或无关业务数据。
- Element Plus `el-table` 存在 header/body/fixed 表格重复 DOM 时，选择行复选框必须限定在可见 `.el-table__body-wrapper tbody tr`，显式排除 `.el-table__header-wrapper` 和 `thead`；点击后必须立即断言已选业务唯一键集合，再进入“确认/应用”等写入动作。

### Element Plus 表格选择门禁

- Trigger: Playwright 需要在 Element Plus `el-table` 中勾选行复选框、批量操作、手动重排、确认应用或其他写入型流程。
- Preflight check: 在写入动作前读取可见 body 行文本，断言已选业务唯一键集合与目标集合完全一致。
- Blocker: 若选中集合缺失目标行、包含额外行，或点击坐标落在 header checkbox / indeterminate checkbox 上，必须停止并修复定位逻辑。
- Verification: 保留真实 E2E 命令、选中集合断言、写入请求参数、最终 UI/API 状态和截图/JSON 证据路径。
- Forbidden action: 禁止用表头全选、数组下标、API-only、直接 SQL 或坐标猜测绕过可见业务行定位。
- Evidence: `doc/tasks/verify-manual-reschedule-881mo-20260724/execution-log.md`，2026-07-24 手动重排修复验证。

### MES 手动重排全选应用完成门禁

- Trigger: Playwright 验证 `排产工单`、`手动重排`、`开始重排`、`确认应用重排`、全选排产工单、自动重排局部阻断、进度停在 `90%` 或“存在未参与排产的工单”。
- Preflight check: 写入型真实 E2E 必须使用真实前端路径逐行勾选可见 body 表格中的可选排产工单，记录已选业务行集合和开始重排日期；点击 `确认应用重排` 后必须同时等待并记录 `preflight`、`preview`、`apply` 三段目标请求，且 `apply` 必须返回 HTTP 2xx、业务 `code=0`。如果预览存在可归因到工单的阻断或未参与工单，页面只能给非阻塞提示；不得再打开会阻断 `apply` 的二次确认框。
- Blocker: 只完成排产前检查或重排预览、未观察到 `/auto-schedule/replan/apply`、进度停在 `90%`、开始日期弹窗或阻塞确认框未关闭、选中集合无法追溯、点击到禁用行/表头 checkbox、或目标请求/响应证据缺失时，必须判定真实 E2E 未通过。
- Verification: 证据必须包含选中行数/业务文本、开始日期、三段目标请求 URL 和 payload 摘要、三段响应 HTTP 状态和业务码、apply summary、进度最终状态、`confirmDialogVisible=false`、`dateDialogVisible=false`、`pageErrors=[]`、`consoleErrors=[]`、最终截图和 JSON 路径。
- Forbidden action: 禁止把夹具红行验证、只读红行验证、API-only apply、历史截图、预览 summary、进度中间值或 success toast 单独当作全选应用 E2E 通过；禁止为了继续排产而二次阻塞确认“未参与排产的工单”。
- Evidence: `doc/tasks/20260804-mes-partial-replan-blockers/verification-report.md`，2026-08-05 用户截图复验中，旧二次确认导致 `90%` 卡住风险，改为非阻塞通知后 `芋道源码/admin` 当前页 12 条可选排产工单全选应用真实 E2E PASS。

### Codex Runner 自动测试门禁

- Trigger: 新增、修改、运行或验收 `系统管理 > 测试管理`、Codex Runner、自然语言测试方法、检查点截图或由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前必须确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token 或经用户明确批准的本地 tokenless Runner 模式、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任；后端配置了 token 时必须用当前 token 完成注册探针，后端未配置 token 且任务明确采用 tokenless 本地模式时，Runner 请求不得发送伪 token 头，但仍必须完成后端注册、领取、心跳和结构化回写；Runner loop 必须在执行中和空闲轮询中持续 heartbeat；本机后端重启、换 jar 或切换运行态后必须重新确认 `yudao.codex-test.runner.token` 与当前模式一致，不能只检查当前 shell 环境变量或旧 Runner token 文件；不得把 `codex-test-runner.mjs --loop` 进程存在当作在线证明，必须核对后端 Runner 状态或数据库 `last_heartbeat_time` 未过期。测试管理执行入口若支持按需 Runner，前端不得因旧 Runner 离线/过期直接阻断执行，必须由后端受控启动脚本完成启动、注册、能力校验和失败原因返回；受控启动脚本不得把前端入口 HTTP 可达性作为启动前硬阻断，前端不可达应由具体真实页面任务在执行阶段暴露。Windows timeout/cancel 必须有独立的 child 收敛超时，不能把 `close` 事件必然触发作为前提。只读测试项必须默认使用短预算、中等推理、`--ignore-rules` 和最短 Playwright 路径 prompt，避免全局高推理配置或编码任务规则把页面冒烟核验拖到超时。
- Blocker: 任一 Runner 或租户前置条件缺失、Runner token 与后端运行态或 tokenless 模式不一致、Runner 进程存在但注册失败或 heartbeat 超过后端超时阈值、测试项会写入生产/非任务租户、失败检查点没有差异描述、截图路径不在受控临时目录、并行执行包含 `parallelSafe=false` 项、执行中 heartbeat 超过后端超时阈值、Windows `codex.cmd` 后代进程在超时/取消后仍持有 `codex-test-result-*` 输出文件、进程树已消失但当前 Runner 会话仍持续上报 `currentRunningCount > 0`、只读项仍按长运行写入型预算或继承项目编码规则执行时必须停止。
- Verification: 记录 Runner 注册/领取/执行期心跳/空闲心跳/回写命令、页面执行入口、租户/用户标签、检查点结果、失败截图 artifact、最终 UI 状态和必要的只读 API 核验；空闲场景至少等待一个 heartbeat 周期后复查 heartbeat age 仍小于超时阈值；Windows Runner 必须证明 timeout/cancel 后不存在本任务 `codex-test-result-*` 子进程，执行项不遗留 `CLAIMED/RUNNING`，并证明即使 child 未触发 `close`，有界等待结束后当前会话运行计数也回到 `0`；只读项还必须证明在只读预算内返回 JSON，且页面无写请求、无控制台错误。
- Forbidden action: 禁止把 API-only、静态合同测试、mock 截图、默认成功、Runner 离线跳过、前端硬拦截 `没有在线 Codex Runner`、绕过后端 Runner 会话和结构化回写直接裸调用 `codex` CLI、只杀 `cmd.exe` 而不处理 `node/codex.exe` 后代进程、无限等待 child `close`、把只读项放任为仓库级编码任务探索、或顺序执行降级当作真实 E2E 通过。
- Evidence: `doc/tasks/20260724-codex-test-management-delivery/verification-report.md`，2026-07-24 Codex 测试管理交付；`doc/tasks/20260725-codex-runner-void-test/verification-report.md`，2026-07-26 Runner 心跳、Windows 子进程树、取消处理修复；`doc/tasks/20260726-codex-runner-on-demand-wrapper/verification-report.md`，2026-07-26 按需 Runner 包装层；`doc/tasks/20260727-codex-runner-token-invalid/verification-report.md`，2026-07-28 只读 Runner 快速路径与真实测试管理自检 PASS。

### Codex Runner 运行态重启与 CLI 自检门禁

- Trigger: 测试管理批次执行期间本机后端重启、Runner 会话变为 `STALE`、批次长期停留 `RUNNING`，或 Runner 已 `ONLINE` 但测试项在启动 Codex 后立即 `exit 1` / 达到 `600000ms` 超时。
- Preflight check: 发起正式节点串前除注册、heartbeat 和能力字段外，还必须执行一个受控、短预算、无业务写入的 Codex CLI 自检，确认当前 provider、认证方式、插件目录同步和 feature 配置能够返回结构化结果；本机后端重启后必须先检查现有活动批次、执行项和旧 Runner session，任务自有悬挂批次应从真实 `测试记录` 页面取消并核对终态，再允许新建批次。
- Blocker: Runner 进程存在但 session `STALE`、heartbeat age 达到超时阈值、执行项仍遗留 `CLAIMED/RUNNING`、页面启动请求未返回 executionId、Codex CLI 自检 `exit 1`、远程插件认证方式不匹配、未知 feature 配置导致启动失败，或短预算自检超时时必须停止正式长链路。
- Verification: 记录后端重启前后 PID/运行 Jar 归属、旧新 Runner session、heartbeat age、悬挂批次页面取消结果、活动批次数量、Codex CLI 自检退出码与结构化输出；正式批次终态后还要在 `测试记录` 页面核对结果，并确认 Runner `currentRunningCount=0`。
- Forbidden action: 禁止在旧批次仍 `RUNNING` 时继续叠加新节点串，禁止用新 Runner 进程存在替代旧执行收敛，禁止把插件认证警告或未知 feature key 静默忽略后继续长链路，禁止 API-only 取消任务自有悬挂批次。
- Evidence: `doc/tasks/20260730-test-management-serial-routes-verification/verification-report.md`，3 条正式节点串验证中识别到后端重启后的悬挂批次、Runner session 切换、Codex CLI `exit 1` 和 `600000ms` 超时。

### Codex Runner 目标测试项存在性门禁

- Trigger: 用户指定运行测试管理中的某个测试项名称，例如“作废测试”，或要求 Runner 领取并执行单个自然语言测试项。
- Preflight check: 在点击执行前，先通过真实测试管理页面按可见业务名称搜索目标项；如页面未命中，再只读核对 `system_codex_test_case` 中目标名称、状态、租户和删除标记。
- Blocker: 目标测试项不存在、被删除、禁用、租户不匹配，或名称只存在于历史任务文档/截图而非当前系统数据时，必须停止；不得自动新建占位测试项、改跑其它测试项或把 Runner 空领取当作执行成功。
- Verification: 证据需包含页面搜索总数、只读 API 或 DB 名称列表、目标租户/用户标签，以及是否创建了 executionId。
- Forbidden action: 禁止用模糊关键词误选其它测试项；禁止用 API-only 启动替代页面行级“执行”点击；禁止在缺少测试方法和目标项的情况下临时造数。
- Evidence: `doc/tasks/20260725-codex-runner-void-test/verification-report.md`。

### 测试管理串行节点串门禁

- Trigger: 新增、修改或验收 `系统管理 > 测试管理` 的 `节点串名称`、`串内序号`、按节点串筛选、顺序执行创建或 Runner 领取逻辑。
- Preflight check: 先核对正式 schema 已包含节点串字段，页面可按节点串单独筛选，后端按 `node_chain_sort` 排序创建执行项；同一节点串必须从第 1 节点连续选择，且节点串执行不得依赖前端勾选顺序或 Runner 并发数刚好为 1。
- Blocker: 节点串可混入其它串或独立测试项、不完整节点串可启动、前置失败后后续节点仍可领取或遗留 `PENDING`、非节点串顺序执行被误阻断，或页面看不到不同节点串筛选项时必须停止。
- Verification: 证据需同时包含官方节点串筛选数量、乱序或不完整选择拒绝提示、前置失败后的后续节点 `BLOCKED` 且未领取、独立顺序执行后续项仍可继续，以及真实页面清理闭环。
- Forbidden action: 禁止把 Runner 单并发、人工只选择首节点、前端排序、API-only 执行结果、静态合同或后续手工取消当作正式串行能力。
- Evidence: `doc/tasks/20260727-codex-test-node-chain/verification-report.md`。

### 测试管理测试节点闭环门禁

- Trigger: 新增或修改 `系统管理 > 测试管理` 的自然语言测试项，尤其是按业务系统节点拆分、会新建/修改/删除/作废业务数据的测试项。
- Preflight check: 每个测试节点必须写清业务节点、固定样本或任务自有测试标识、前置复位、页面操作、页面可见验证、清理/恢复方式；测试方法和测试目标必须面向业务测试人员，避免只写接口、内部字段、状态码、hash、英文内部状态或代码视角。
- Blocker: 测试项只创建不清理、只删除不先准备样本、失败后下次运行会被残留数据阻塞、没有固定样本或任务自有标识、目标只能由程序员判断，或需要测试人员在测试说明之外手工猜测清理方式时必须停止。
- Verification: 证据需包含节点数量、每节点方法项数量、每节点目标项数量、固定样本/清理/恢复闭环核验、内部词扫描结果，以及写入租户和项目范围。
- Forbidden action: 禁止用 API-only 清理、生产或 admin 基线数据、隐藏脚本状态、程序员专用字段、一次性人工清库、或“执行失败后手工处理”替代测试节点自身闭环。
- Evidence: `doc/tasks/20260727-batch-record-test-node-items/verification-report.md`，2026-07-27 批记录 6 个节点闭环测试项。
## eDHR 本地状态样本操作审计追溯门禁

- Trigger: Playwright 验证本地状态样本、`LOCAL_STATE_SAMPLE_CREATE`、批次追溯操作审计、或只按 `batchExecutionId` 查询操作日志。
- Preflight check: 写入型 E2E 必须通过真实页面创建任务自有样本批次，并确认样本批次任务具备可用于批次追溯的对象级权限 scope（至少覆盖 `AUDIT_VIEW`）。
- Blocker: 如果操作审计行已创建，但追溯接口返回 `BATCH_EXECUTION:<id>` 对象级权限范围不存在或未启用，必须修复样本创建事务的权限 scope 绑定；不得用 SQL 补权限、API-only 或管理员绕过冒充通过。
- Verification: E2E 需断言 `/mes/pro/edhr-operation-audit/page` 请求包含 `batchExecutionId`，不包含 `objectType/objectId`，并在表格中看到目标 operationType、权限判定、结果状态和 audit hash。
- Forbidden action: 禁止只验证审计表落库而不验证批次追溯可见性；禁止把权限缺失解释为页面无数据；禁止记录登录密码。
- Evidence: `doc/tasks/20260724-batch-fda-audit-log-coverage/verification-report.md`。
