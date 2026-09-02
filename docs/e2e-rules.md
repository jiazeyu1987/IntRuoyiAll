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
- 提交型后台任务的真实 E2E 必须区分“页面提交成功”和“业务执行完成”：前端 POST 返回成功只表示任务已受理，必须通过同一真实页面刷新或轮询确认成功/失败终态；若仍显示运行中，必须记录任务记录、访问日志和线程栈证据，禁止重复提交。

## 缺入口处理

- 发布、审计或独立验证任务发现前端无入口时，必须 fail fast，不得临时扩大范围新增入口。
- 功能或修复任务只有在入口属于用户批准范围，且已完成 BDD + TDD 时，才允许补入口。
- 计划或验收文档列出的 E2E 命令必须先核对 `package.json` 实际 script 和测试文件存在性；脚本或测试文件缺失时记录为 E2E 前置缺口，不得把命令解析失败、静态合同或 API wrapper 测试写成真实 E2E 通过。


## 静态合同与真实 E2E 同步门禁

### E2E 运行态代码来源门禁

- Trigger: 本地端口已有后端进程、运行 Jar 早于目标提交、目标修复依赖新增类或迁移，或完整构建被其它模块编译错误阻断但仍需做定向 E2E。
- Preflight check: 在浏览器验证前同时记录分支、提交、监听端口进程和运行 Jar；按目标修复列出关键类或资源，并直接核对运行 Jar 内嵌模块是否包含它们。需要组合定向测试 Jar 时，只能使用同一提交的干净 worktree 构建产物，校验内嵌 Jar 哈希和 Spring Boot nested Jar 压缩方式，并明确标为测试运行产物。
- Worktree port check: 可复用到多个 profile/worktree 的 E2E 脚本不得写死某次任务端口；必须按当前 repo root 读取正式 `worktree-ports.json` active 登记并校验前后端成对端口。主工作区只允许固定基准端口，附加 worktree 找不到唯一 active 登记时必须在浏览器启动前失败。
- Blocker: 运行 Jar 来源不明、关键类缺失、内嵌模块不是同一提交、哈希或压缩方式不符、替换后 health 未恢复，或定向组合产物被用作完整发布构建通过证据时必须停止。
- Verification: 证据包含提交、运行 Jar 路径与哈希、关键类清单、替换模块哈希、端口 PID 和 health；完整构建若失败，必须单独记录失败模块与错误数量，定向 E2E 只能证明目标模块运行行为，不能证明发布构建通过。
- Forbidden action: 禁止在未核对运行 Jar 内容时宣称当前代码 E2E 通过；禁止把旧 Jar、脏主工作区临时编译物、跨提交模块或静默拼装产物冒充当前运行态。
- Evidence: `doc/tasks/20260811-dcc-critical-remediation-int-main-admin-e2e/verification-report.md`。

### PDF 预览非空渲染门禁

- Trigger: 页面预览接口返回 PDF HTTP 200，但截图仍显示加载态、空白容器，或任务要求证明真实文件可以预览。
- Preflight check: Playwright 必须等待实际 PDF canvas 可见并完成渲染；在截图前同时检查 canvas 宽高大于 0，并对画布分布采样，确认存在足够的非白、非透明像素。二进制响应、元数据和 DOM 文案只能作为辅助证据，不能替代画布渲染。
- Blocker: 只有 HTTP 200、`application/pdf`、预览 token 或元数据成功，但 canvas 不存在、尺寸为 0、像素全空白、仍在加载或截图不可见时必须记录预览 E2E 失败。
- Verification: 证据包含预览 URL、元数据不可用原因、PDF 响应状态与内容类型、canvas 宽高、采样像素数、非白像素数和渲染完成后的页面截图。
- Forbidden action: 禁止在 canvas 渲染完成前截图，禁止只凭网络响应或 `canPreview=true` 宣称用户实际可以预览。
- Evidence: `doc/tasks/20260811-dcc-critical-remediation-int-main-admin-e2e/verification-report.md`。

### E2E 脚本入口存在性门禁

- Trigger: 任务验收文档指定 `pnpm test:e2e ...`、`pnpm test <target>`、Playwright spec 文件或新增真实用户路径 E2E。
- Preflight check: 运行前读取当前前端 `package.json` 的 scripts，确认命令名存在、命名 runner 能识别目标、spec 文件存在，并记录实际工作目录；PowerShell 下若 `pnpm --dir` 或 `pnpm -C` 解析异常，改用显式 `workdir` 复核，不把第一次命令解析失败当作业务 E2E 结果。
- Preflight check: 静态合同脚本可能按仓库根目录或前端根目录解析相对路径，运行前必须从脚本的 `process.cwd()` 和路径拼接方式确认期望工作目录；若只因工作目录错误导致文件不存在，应修正命令后重跑，不能把路径错误写成业务 FAIL。
- Preflight check: 验收文档包含写入型用户路径时，还必须同时确认真实页面入口、前端 route、权限 meta、页面主按钮和写 API wrapper 全链路存在；只有 API wrapper 或只读追溯页存在时，不得宣称写路径已实现。
- Preflight check: 复用历史真实脚本前，必须先按当前源码或真实 DOM 核对入口按钮文案、稳定锚点和可点击条件；按钮已 visible 但仍受 loading、navigationLoading、saving 或权限状态禁用时，脚本应等待正式可点击状态并记录禁用来源，不能把瞬时 disabled 或旧文案定位失败直接写成产品功能失败。
- Preflight check: Element Plus 弹窗标题可能使用业务文件名、编号或动态标题；脚本等待弹窗时应锚定真实 DOM 中稳定可见的业务文本或 `data-testid`，不得硬等旧固定标题。打开预览、抽屉或遮罩后继续操作底层页面按钮前，必须先关闭覆盖层并等待其隐藏，不能把遮罩拦截点击误判为业务按钮不可用或下载失败。
- Blocker: `ERR_PNPM_NO_SCRIPT`、named target unknown、spec 文件缺失、真实页面入口缺失、菜单权限或测试租户账号缺失，或当前源码/DOM 已证明入口文案和历史脚本定位不一致且未修正脚本时，必须停止并记录具体前置缺口。
- Verification: 证据必须区分静态合同 PASS、TypeScript PASS、Playwright 真实路径 PASS 和 E2E BLOCKED；真实 E2E 只有在 Playwright 操作真实页面并完成目标断言后才能记为 PASS。
- Forbidden action: 禁止新增虚假 script 包装静态测试冒充真实 E2E，禁止 API-only 替代页面路径，禁止把前端 API wrapper 存在宣称为页面入口已验收。
- Evidence: `doc/tasks/20260730-process-pool-f5-f6-implementation/execution-log.md`；`doc/tasks/20260828-batch-record-mappable-cells-int-main-e2e/verification-report.md`，融合后批记录可映射格子 E2E 先因旧按钮文案“规则”和按钮加载禁用态校准失败，最终按当前“填写配置”入口并等待“正式化可映射格子”按钮可点击后通过真实页面验证。

### 前端 API 路径同源门禁

- Trigger: Playwright 已通过真实页面完成提交或导入，但后续用 `fetch` 做最终状态核验时返回模块禁用兜底、请求地址不存在、401/403 或与页面网络请求不一致；尤其是 Form Center、DCC、MES 等前端 API wrapper 自带业务前缀时。
- Preflight check: E2E 脚本中的只读/API 核验路径必须从当前前端 API wrapper 或页面真实网络请求反推，不能凭模块名手工拼接额外前缀。若页面接口是 `/admin-api/form-center/...`，脚本不得改写成 `/admin-api/bpm/form-center/...`；发现模块禁用提示时先核对 pathname 是否与前端 wrapper 完全一致，再判断运行态模块是否缺失。
- Blocker: 页面动作已成功但核验接口命中错误模块前缀、脚本无法说明路径来源、或只因脚本路径错误就把产品导入/发布链路判为失败时，必须修正 E2E 脚本并重跑，不得改后端路由或开启无关模块来迎合脚本。
- Verification: 记录前端 API wrapper 路径、实际失败 pathname、修正后的真实 E2E 命令和最终业务码；若修正后仍失败，才进入对应模块运行态或业务逻辑排查。
- Forbidden action: 禁止把脚本手拼路径的 501/404 当成产品业务失败，禁止为了通过脚本新增兼容路由、模块前缀 fallback 或隐藏错误 toast。
- Evidence: `doc/tasks/20260828-form-template-import-auto-recognition-publish-flow/execution-log.md`，表单模板 Word 导入真实 E2E 已导入成功，但详情核验误用 `/admin-api/bpm/form-center/...` 命中 BPM 禁用兜底；修正为前端 wrapper 的 `/admin-api/form-center/...` 后通过并验证发布版本规则。

### 列表筛选输入与请求参数同步门禁

- Trigger: Playwright 在 Element Plus 表格、多条件筛选、quick filter、页签过滤或列表搜索中填入目标业务编号，但列表结果仍返回大批量数据、目标样本缺失、或失败信息疑似“数据不存在”。
- Preflight check: 点击查询前必须同时确认目标筛选容器内可见输入框的 DOM value 已等于目标值，并监听正式分页请求；请求返回后必须记录实际 request URL、业务码、total 和前几条业务 code，确认 URL 中包含目标筛选参数。对于需要页签或筛选状态同步的组件，必须等待组件内部状态更新后再点击查询。
- UnifiedList upgrade check: 复用历史脚本时先检查当前列表使用 `TableQuickFilter` 还是 `TableMultiFilter`。多条件筛选默认空状态必须按真实“新增筛选条件 -> 选择字段 -> 填值 -> 查询”操作；Element Plus 条件行可能同时含只读操作符 input 和业务文本框，必须按 placeholder/role 精确定位可填写控件，禁止继续使用泛化 `input`。
- Preflight check: 若目标列表在某租户下没有可见筛选框、筛选区被权限或页面布局隐藏，不能直接判定目标样本缺失；应先按真实分页控件逐页查找目标可见行，并记录命中的 pageNo、pageSize、目标行文本和目标写请求数。分页查找仍属于真实页面路径，但不得用 API-only 查询替代页面可见性断言。
- Blocker: 输入框可见值正确但正式分页请求 URL 未带目标参数、请求命中错误页签/错误接口、返回 list 前几条 code 明显未按目标过滤，或只能证明 DOM 值而不能证明请求参数时，必须停止并归因为脚本/组件同步问题，不得把结果写成目标业务数据缺失。
- Verification: 真实 E2E 证据需包含目标筛选值、最终请求 URL、业务码、total、前几条 code，以及修复后同一用户路径命中目标样本并完成后续断言。
- Forbidden action: 禁止只填 placeholder 输入框、只检查输入框 value、用 API-only 查询替代页面筛选、改成模糊全表扫描、或把未带筛选参数的大列表结果当作目标样本缺失证据。
- Evidence: `doc/tasks/20260813-scheduler-seven-issues-closure/execution-log.md`，排产目标 7 E2E 曾因生产工单分页 URL 未带 `code` 参数误报缺少 `CODexERP20260610E`，修复 quick filter 状态同步后真实页面闭环 PASS；`doc/tasks/20260830-bind-pressure-pump-idpr/verification-report.md`，表单中心测试租户无可见产品筛选框时，按真实分页到第 13 页找到目标产品行并验证项目代码显示。

### DCC 文控审批处理入口门禁

- Trigger: 验证 DCC 文控上传、原版上传、上传审批、电子签名审批、升版发布、发布申请、文件作废/废止、旧版自动失效、`OBSOLETE`、`SUPERSEDED`、`DccControlledFileDetail`、`/approval-center?moduleCode=DCC`、`PROCESS_IN_MODULE`、`approve-task`、`DCC_PUBLISH` 或 `APPROVE_USER_SELECT` 链路。
- Preflight check: 浏览器审批前必须证明审批账号能从真实页面进入非只读处理态，并看到“审批阶段进度”、当前 `approvalTodoTask` 对应的签名按钮和目标写接口；真实 E2E 不能只断言处理区标题或“审批要求”文案，必须同时断言当前任务按钮可见，并排除“暂无待处理审批任务”“当前没有待处理审批任务”等空任务提示；同时核对 `DccControlledFileDetail.beforeEnter` 不会把非 viewer 处理态重定向到受控浏览。遇到 DCC “作废/废止”需求时，必须先确认用户要的是手动当前版本作废审批链路，还是升版发布后旧版本自动失效链路；若用户提到“升版本”“老版本自动作废/失效”“不走审批”，验收口径是旧 V1 `SUPERSEDED`、新 V2 `ACTIVE`、master 当前有效版本指向 V2，不要求创建 `OBSOLETE` 审批。发布申请前还必须核对发布申请人拥有 `form:instance:create`、`form:instance:submit`、`system:user:query` 和用户选择弹窗所需的用户查询权限；发布 BPM 审批如果后续节点是 `APPROVE_USER_SELECT`，必须在 BPM 流程详情页等待 `/bpm/process-instance/get-next-approval-nodes` 返回并选择下一节点审批人。受控浏览 viewer 模式的版本追溯入口是 `data-testid="dcc-controlled-preview-version-button"` 打开的版本信息弹窗，变更原因显示在详情基础信息的“提交备注”；受控浏览 traceability 模式是 `/dcc/controlled-file/detail/{id}?traceability=1&from=browser` 的追溯详情页，需验证内嵌“版本历史”表与升版原因；viewer 模式还必须渲染当前有效版的最终目录路径、`publishedFileId`、`stampedFileId` 或等价发布文件信息，不能只在非 viewer 详情路径展示该 linkage 卡片。脚本等待详情接口时必须精确匹配 `/admin-api/dcc/controlled-files/{id}` 的 pathname，避免误抓 `/preview`、`/preview-metadata`、`/access-explanation` 等同 ID 子接口。
- Blocker: DCC 审批中心行只能打开 `viewer=1` 只读预览、非 viewer 详情被路由守卫重定向、页面未渲染签名按钮、处理区只显示空任务提示但没有当前审批动作、只有 `approve-task` API wrapper 但无页面入口、BPM 原生行直接审批返回业务 `403`、发布申请弹窗提示缺审批人、用户选择弹窗因缺 `system:user:query` 报无权限、或 BPM 发布审批返回“下一个任务的审批人未配置”时必须停止并记录 E2E BLOCKED。若用户明确要求手动作废审批链路但运行态缺少已发布 `DCC / DCC / CONTROLLED_FILE / OBSOLETE` 业务审批策略，也必须记录 BLOCKED；若用户明确要求升版自动失效链路，则缺手动作废策略不能阻塞该链路验收。
- Verification: 证据需包含审批中心 DCC 行、跳转后的实际 URL、详情页处理态控件、当前任务按钮文本、签名弹窗、`/dcc/controlled-files/{id}/approve-task` 响应、Flowable 当前任务和 DCC 文件状态；原版上传链路还需包含同一 `file_number` 仅一条 V1.0 `NEW` 文件、状态 `ACTIVE`、master 当前生效版本指向该 V1.0、上传审批完成任务数不少于 4，且不存在升版/修订行；发布/升版自动失效链路还需包含 `bpm_form_action_instance.status=EFFECTIVE`、发布 BPM 完成任务数、旧版本 V1 `SUPERSEDED`、新版本 V2 `ACTIVE`、master 当前生效版本指向 V2、V1 successor 指向 V2；受控浏览链路还需包含 ACTIVE browser-page 只返回/默认打开 V2、V1 不作为当前有效行返回、viewer 版本信息弹窗或 traceability 详情内嵌版本历史可见 V1/V2、详情提交备注/升版原因可见、viewer 页面可见最终目录路径以及 published/stamped 文件 ID。若 blocked，记录路由守卫源码行、页面实际落点、viewer/traceability 模板缺口和任务自有残留数据。
- Forbidden action: 禁止用 BPM 原生审批行替代 DCC 上传审批、直接 API、SQL 改状态、移除断言、绕开路由守卫、只读 viewer 截图、跳过发布申请审批人选择、或把发布 BPM 审批人的 `APPROVE_USER_SELECT` 通过默认值/空值冒充配置完成。禁止把升版后的旧版 `SUPERSEDED` 误判为必须走 `OBSOLETE` 审批；禁止在用户明确要求升版自动作废/失效时，继续用缺手动作废审批策略作为当前链路失败结论。
- Evidence: `doc/tasks/20260802-dcc-upload-revision-e2e/verification-report.md`，DCC 上传升版 E2E 先暴露处理态、发布申请权限和 BPM 下一审批人选择缺口，补齐非 admin 角色权限并改为真实 DCC/BPM 页面路径后完成完整链路验证；`doc/tasks/20260802-dcc-upload-original-e2e/verification-report.md`，DCC 原版上传 E2E 验证 V1.0 `NEW` 文件审批后直接 `ACTIVE`，master 指向原版且无升版行；`doc/tasks/20260802-dcc-controlled-file-obsolete-e2e/verification-report.md`，DCC “作废/废止”需求经用户澄清后按升版自动失效链路验收，真实 Playwright 证明 V1 `SUPERSEDED`、V2 `ACTIVE`、master 指向 V2、受控浏览不再返回 V1 当前有效行，手动作废 OBSOLETE 策略缺失仅作为非当前链路 blocker 记录。

### DCC 受控浏览当前有效版与权限隔离门禁

- Trigger: 受控浏览已显示文控菜单/页签，但页面提示“无权限或无匹配当前有效文件”、指定目录为空、低权限账号看不到已发布文件，或需要判断 `ACTIVE`、`publishedFileId`、`stampedFileId`、`current_active_controlled_file_id` 与查看矩阵关系。
- Preflight check: 先记录真实请求的 `directoryId`、`includeDescendantDirectories`、`latestVersionOnly`、状态、类别和关键字；再按同一租户核对精确目录范围内的 `ACTIVE` 文件、`published_file_id`、master 当前有效指针及文件 `category_id`。文件级可见性必须分别核对申请人本人、目录管理权限和该类别启用查看矩阵解析用户；菜单角色只能证明页面入口，不能证明文件可见。
- Blocker: 类别没有启用查看矩阵规则、规则无法解析到实际用户、目录范围不含目标文件、文件不是已发布的当前有效版、申请人/目录管理员/查看矩阵均未命中，或只凭菜单权限/API/数据库结果宣称真实页面通过时必须停止。
- Verification: 证据至少包含租户与账号标签、目录 ID/路径、请求范围、ACTIVE 文件数量、发布文件 ID、master 当前有效文件 ID、类别 ID/名称、查看矩阵规则数及解析用户集合；真实页面验收还必须证明目标列表行可见，不能用扩大角色或下载权限代替。
- Verification: 复核受控浏览只读交互缺陷时，必须区分真实页面复现结论和源码风险；会话失效场景需同时记录失败业务码/弹窗、筛选标签、表格行是否来自失败前请求、URL/pageNo/jumper 值、预览 popup URL 或失败提示，以及目标 DCC 写请求数为 0。未复现时不得用静态合同或源码风险替代真实页面结论。
- Forbidden action: 禁止把 `wenkong`、下载角色或目录管理权限作为空列表的默认修复；禁止改前端空态文案掩盖查看矩阵缺口；禁止用 `formBindings`、旧版本、默认类别、全局 Redis 清理或 API-only 结果替代正式文件查看权限链。
- Evidence: `doc/tasks/20260807-test-wangsiyu-dcc-browser-empty-diagnosis/verification-report.md`。

### DCC 完整发布 E2E 分类与路线联合前置门禁

- Trigger: DCC 受控文件上传、完整审批矩阵、同名/同内容双提交、最终批准发布，或真实 E2E 同时依赖 `fileTypeTaxonomyId`、标准 `categoryId` 和审批路线。
- Preflight check: 在任何 upload-preview、ticket、submit 或 approve 写请求前，按目标标准类别读取其正式 `fileTypeTaxonomyId`，并确认同一类别存在当前有效审批路线及路线所需组织/岗位配置；Playwright 必须选择该正式绑定的 taxonomy 叶子并等待页面自动解析出精确类别，不能分别挑一个“有分类树的类别”和一个“有路线的类别”。预检同时记录目标写请求集合，阻断时应为 0。
- Blocker: 有路线的类别未绑定分类树、有分类树绑定的类别没有有效路线、类别与 taxonomy 跨绑定、提交人部门负责人或路线参与人岗位缺失时必须在写入前记录 E2E BLOCKED。该状态可以证明前置门禁生效，但不能写成完整发布成功路径 PASS。
- Verification: 证据至少包含目标类别、正式 taxonomy 绑定、有效路线解析结果、聚合 readiness blocker、页面自动绑定后的显示值和 `writeRequests=[]`；补齐正式测试数据后必须重新执行完整上传、四阶段审批、最终发布及访问验证，原阻断证据不能替代成功路径。
- Forbidden action: 禁止任意选择第一个 taxonomy 叶子、按文本猜测类别、直接更新数据库补绑定、跨类别复用路线、API-only 提交、mock success，或在预检 BLOCKED 后继续创建临时/正式文件。
- Evidence: `doc/tasks/20260810-dcc-critical-remediation/verification-report.md`，真实矩阵预检发现测试租户不存在同时具备正式分类树绑定和有效审批路线的标准类别，并在零业务写请求前稳定阻断。

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
- Preflight check: 采集失败请求的完整 URL、状态码和资源类型；按本机前端、当前后端、目标业务 API 与目标读写接口定义目标链路。多页签页面必须按当前验收页签精确限定目标接口，例如审批中心“已办”只把 `/approval-center/done` 和 `viewType=DONE` 列为目标链路，页签切换中被浏览器中止的 TODO 请求只能单独记录为非 DONE 审批中心请求，不得冒充 DONE 失败。只读页面若自动触发命令式 POST（如一线页面 `switch-employee` 用于授权校验、运行态读取和模板解析），必须先核对后端实现没有 insert/update/delete 或业务状态推进，再把它单独归类为“只读型 POST”；不得把它混入目标业务写请求，也不得在未核对实现时直接放行。只有在确认非目标 URL 未造成目标控件缺失且目标行为断言独立通过后，才允许将其单独记录为非目标链路异常；不得用域名白名单批量忽略错误。浏览器只给出通用 `Failed to load resource` 文本时，必须同时记录 response URL、HTTP status、status text，并按出现次数逐条绑定；只有非本机响应和 console 文本一一对应的条目可归类为外部资源错误。本机响应、数量不匹配或缺 URL 的通用错误继续作为目标失败。
- Preflight check: 同源但非当前目标的全局角标、通知、待办数量或布局附属请求返回“系统异常”时，必须单独记录接口归属和影响范围；只有目标列表接口、目标控件和目标行断言均通过，且该异常不阻断当前页面操作时，才可归类为非目标链路异常。
- Blocker: 任一本机目标业务请求失败、出现未解释的 `pageerror`、外部或非目标请求失败导致目标页面或控件不可用、无法确认目标写请求数量，或失败请求归属不明确时必须停止。
- Verification: 证据必须同时记录目标链路错误数、非目标同域请求或外部异常 URL 与状态码、`pageerror` 数量、目标 UI 断言、目标写请求数量，以及被单独归类的只读型 POST 数量和后端只读实现依据；只读/取消确认路径必须明确证明真正业务写请求为 0。若分类通用 console 错误，还必须保存与之关联的第三方失败响应 URL、状态、分类数量，并用负向合同证明未关联、本机或多余的同文错误不会被忽略。
- Forbidden action: 禁止全局关闭 console/network 断言、忽略全部第三方域名、仅按错误文案宽泛忽略超时、把页面 HTTP 200 当作目标功能通过，或省略外部异常证据。
- Evidence: `doc/tasks/20260801-dcc-list-auto-classify-local-e2e/verification-report.md`，DCC 列表只读 E2E 将外部头像 502 与本机/DCC 目标链路分开归因，并证明目标链路错误数和 DCC 写请求数均为 0；`doc/tasks/20260811-route-publish-chain-clarity/verification-report.md`，一线生产只读 E2E 将模板解析 `switch-employee` POST 与真正业务写请求分开统计，核对后端只读实现后证明业务写请求为 0；`doc/tasks/20260814-frontline-active-order-submit-allocation-docs/execution-log.md`，活跃订单提交 E2E 以精确关联合同分离第三方请求超时，目标页面、请求、HTTP 和控制台错误均保持为 0；`doc/tasks/20260830-bind-pressure-pump-idpr/verification-report.md`，表单中心目标行显示通过时，将审批待办角标“系统异常”单独归类为非目标链路异常。

### 多步骤 E2E 子步骤与脚本总状态判定门禁

- Trigger: 一个真实 E2E 同时执行多个业务步骤，某个目标步骤已收到明确成功响应并形成状态证据，但脚本在后续无关断言、证据分类或清理后检查中最终退出失败。
- Preflight check: 必须逐步记录目标请求路径、HTTP/业务码、关键返回字段、页面触发动作、数据影响和后续失败断言；只有能够证明后续失败不可能改变目标步骤结果时，才允许把该子步骤单独记为 PASS，同时保留脚本整体 FAIL。
- Blocker: 目标步骤只看到页面文案、没有响应或状态证据，后续失败可能发生在同一事务/同一状态提交、脚本吞掉目标异常、或无法证明失败断言与目标步骤独立时，子步骤不得判 PASS。
- Verification: 报告必须同时写明“脚本整体状态”和“目标子步骤状态”，列出成功响应的业务码/关键字段、整体失败断言及二者独立性依据；应修正过期脚本分类规则并补跑，不能长期只依赖部分通过结论。
- Forbidden action: 禁止把脚本整体 FAIL 改写成整条链路 PASS；禁止因最后一个通用断言失败而否认已经有精确动态证据的独立步骤；禁止删除失败断言或忽略真实目标网络异常来制造绿色结果。

### Playwright 登录重定向与目标接口监听门禁

- Trigger: 真实 E2E 登录 URL 的 `redirect` 已指向目标页面，但脚本登录后才注册 `waitForResponse`、目标列表/详情接口监听、toast observer 或 console 断言。
- Preflight check: 目标接口监听必须先于触发该接口的导航、点击、输入、`blur` 或其它会发起请求的页面动作注册；若登录只用于拿到会话，登录 `redirect` 使用 `/` 等中性落点，然后在目标监听和页面 observer 安装完成后再进入目标页面。若必须登录后直达目标页，必须在登录前安装覆盖目标接口的监听并把该响应作为目标证据。登录页本身也要按本机冷启动处理，当前页面可能同时挂载多套隐藏 `.login-form`，脚本应等待有实际宽高且包含账号登录关键文本的可见表单，再填租户、用户名和密码；不得只用短超时等待第一条 `form.login-form`。登录后的动态权限和路由初始化必须等待 `/system/auth/get-permission-info` 返回业务成功后再进入目标页面；该接口在本机并发验证或后端冷启动时可能超过 60 秒，脚本不得用短超时 `catch(() => null)` 后继续导航并把应用加载页误判为目标业务失败。
- Blocker: `waitForResponse` 超时、目标页面已经由登录重定向加载、Vue Router 缓存未再次请求目标接口、observer 安装晚于目标 toast/console、登录截图已经显示账号登录面板但脚本选择器仍未命中可见表单，或权限路由初始化尚未完成时页面停留在应用加载页，必须先归因为脚本监听/登录前置顺序问题，不得把超时记录为业务页面失败。
- Verification: 修正顺序后重跑真实页面路径，证据需包含登录落点、权限信息接口业务码、目标导航 URL、目标接口响应或明确的无目标写请求计数、console/pageerror/toast 采集结果和截图/result JSON。
- Forbidden action: 禁止通过加长超时、读取旧 `result.json`、API-only 查询、重复刷新碰运气或忽略缺失目标响应来宣称 E2E 通过或未复现。
- Evidence: `doc/tasks/20260808-process-route-editor-stack-overflow-repro/verification-report.md`，工艺路线编辑器复现脚本先把登录 redirect 从目标页改为 `/`，再显式进入工艺路线列表，避免列表接口响应在监听前被登录重定向提前消费；`doc/tasks/20260808-dcc-upload-optimization-fixes/verification-report.md`，DCC 上传 current-version 真实 E2E 将 `waitForResponse` 提前到文件编号 `fill/blur` 前，避免请求过快导致脚本漏听；`doc/tasks/20260829-stage1-simulation-fix/verification-report.md`，Stage1 真实 E2E 曾因权限信息接口耗时超过脚本 60 秒等待而停留应用加载页，也曾因登录页多套表单和短选择器等待导致业务接口未调用；修正为等待可见账号登录表单与权限路由完成后再进入生产组长页面。

### 动态菜单真实入口与直达地址假阳性门禁

- Trigger: Playwright 通过 `page.goto()` 直达动态路由后只看到页面标题、面包屑或布局框架，目标列表接口未发出，或需要判断当前账号是否拥有某个菜单入口。
- Preflight check: 先枚举当前账号真实可见的左侧菜单项，并从可点击菜单进入目标页面；同时监听目标列表接口，确认页面主体控件或表格已渲染。地址栏、面包屑、标签页标题和菜单搜索结果只能证明路由文字存在，不能证明当前账号拥有入口或目标组件已加载。
- Blocker: 真实菜单中没有目标项、点击菜单后目标组件未渲染、目标列表接口未发出，或只能通过手工拼接 URL 显示面包屑时，必须按账号权限或页面入口缺失记录阻塞；不得把空框架误判为业务数据为空。
- Verification: 证据需包含账号标签、可见菜单项、点击后的实际 URL、页面主体关键控件、目标列表请求及业务结果；若阻塞，还需记录登录后写请求为 0，并区分无关静态资源失败与目标接口失败。
- Forbidden action: 禁止用 `page.goto()` 直达地址、面包屑文本、页面标题、API-only 列表或 SQL 查询替代真实菜单入口；禁止因为头像、图标或统计资源失败就把未加载的业务页面归因为目标接口失败。
- Evidence: `doc/tasks/20260814-production-release-flow-implementation/execution-log.md#pass-51-p11-second-tenant-real-menu-diagnosis`，生产放行 P11 第二租户盘点中，直达 `/system/tenant` 只渲染布局和面包屑，当前管理员真实菜单没有“租户管理”，目标列表请求为 0。

### Vite 动态导入 500 与冲突标记门禁

- Trigger: 真实 E2E 出现 `Failed to fetch dynamically imported module`、Vite 返回 500、Vue 编译器报 `Attribute name cannot contain`，或目标页面根节点等待超时但模块 URL 可直接返回 Vite overlay。
- Preflight check: 先直接请求报错模块 URL 或读取 Vite overlay，定位 `loc.file/line/frame`；随后对目标文件运行锚定扫描 `rg -n "^(<<<<<<<|=======|>>>>>>>)" <file>`。若命中冲突标记，必须先归因为前端源码未解决冲突，而不是业务页面缺控件或接口失败。
- Blocker: 目标页面依赖的 `.vue/.ts` 文件存在 Git 冲突标记、Vite 编译错误或动态导入 500 时，真实 E2E 必须记录 BLOCKED；冲突取舍不明确或文件包含并行任务脏改动时，不得擅自解析并继续验收。
- Verification: 冲突处理获批并完成后，先用锚定 `rg` 证明目标文件无冲突标记，再直接请求动态模块 HTTP 200，最后重跑原真实 E2E；不得用 API-only、旧截图、清浏览器缓存或关闭 Vite overlay 冒充通过。
- Forbidden action: 禁止把动态导入 500 写成业务功能 FAIL，禁止忽略 `pageerror` 继续断言，禁止全仓未锚定扫描 `=======` 造成误报，也禁止在未获授权时改写并行任务冲突内容。
- Evidence: `doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/verification-report.md`，一线生产员工弹窗真实 E2E 在后端运行 Jar 刷新后被 `TeamLeaderWorkbenchPage.vue` 未解决冲突标记阻塞，页面模块 Vite 500，需先解析前端冲突再复验。

### 共享 Vite 遮罩与目标链路验收边界门禁

- Trigger: `int_main` 共享 Vite 运行态因无关并发脏文件触发 `vite-error-overlay`、PostCSS/编译错误或全屏开发遮罩，但本轮目标页面模块、目标接口和目标控件仍可独立访问。
- Preflight check: 读取遮罩中的文件、行号和错误类型，核对报错文件是否属于当前任务及是否位于目标页面依赖链；同时记录目标页面 URL、目标 API 响应和目标控件状态。报错文件属于并发任务时不得修改或回退；只有明确记录遮罩来源后，才可临时关闭遮罩收集窄范围目标链路证据。
- Blocker: 遮罩来自目标页面依赖、目标模块或目标接口，关闭遮罩后目标控件不可用，或无法证明错误与当前任务独立时，真实 E2E 必须记录 BLOCKED。
- Verification: 窄范围结果必须分别记录目标链路 PASS 与共享前端运行态未全局通过；证据至少包含遮罩来源、目标接口 HTTP/业务码、目标错误文案是否出现和目标 UI 交互。关闭遮罩只允许用于继续取证，不得作为错误已修复或全局 console 健康的证据。
- Forbidden action: 禁止回退或修改无关并发文件，禁止隐藏遮罩后宣称共享前端无错误，禁止省略仍存在的 console/request 异常，也禁止用窄范围 PASS 覆盖共享运行态问题。
- Evidence: `doc/tasks/20260807-fix-zhaohaichen-upload-category-permission/verification-report.md`，本机文件上传页受无关 `TeamLeaderWorkbenchPage.vue` PostCSS 遮罩影响；记录归属并关闭遮罩后，仍以真实 `zhaohaichen` 账号完成上传 taxonomy 目标接口和三级分类选择验证，同时明确不宣称共享前端全局健康。

### Playwright 快照与 daemon 收尾门禁

- Trigger: 使用 Playwright CLI / headed browser 验证登录页、发布控制台、版本变更说明或任何可能包含输入框内容的真实页面。
- Preflight check: 运行前把输出目录限定到当前任务或 releaseTag；Windows 命名会话必须使用 CLI 实际支持的 `-s=<session>` 语法，`open` 后立即用同一会话 `snapshot/list` 验证会话仍存在。读取快照前先假定登录页预填值可能包含真实密码，不得把原始 YAML 回显到任务日志；验收后扫描 `.playwright-cli\page-*.yml`、trace、截图、视频和 CLI daemon 进程，判断是否包含登录预填字段、账号、密码、token 或任务敏感数据。
- Blocker: CLI 在 Windows 出现 `UV_HANDLE_CLOSING` 断言、`open` 后同名会话不存在、登录快照含未脱敏预填凭据、任务输出目录存在未脱敏 `page-*.yml`/trace/视频/截图，或存在命令行可证明属于当前任务的 `cliDaemon.js <task-or-release>` 进程仍占用输出目录时，CLI 验证不得记 PASS，任务也不得 closeout。
- Verification: CLI 会话异常必须先记录为工具链失败，再使用项目既有 Playwright 脚本承载同一真实页面路径，不能降级为 API-only；删除或脱敏任务自有 Playwright artifact；若目录被锁，只停止命令行明确属于当前任务的 daemon 和子进程；最终记录任务输出目录 `Test-Path=False` 或 artifact 清单为空。
- Forbidden action: 禁止提交或回显原始 Playwright 登录快照；禁止把 CLI 会话丢失或运行时断言归因成产品失败；禁止为了清理目录误停其他并发 E2E/Playwright 任务；禁止用旧页面快照代替本轮真实页面验证。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260730-head-test-only-release\execution-log.md`，发布验收后清理当前任务 `.playwright-cli` 快照，并只停止 `cliDaemon.js r260731c-r2 --headed` 任务归属进程树；`doc/tasks/20260807-frontline-pqc-all-active-orders-search/verification-report.md`，Windows CLI 会话未保持并出现运行时断言后，改用任务自有 Playwright 脚本完成同一真实页面任务边界验证。

### Worktree / int_main 运行态 URL 门禁

- Trigger: 主工作区默认端口被并行任务占用、旧 jar 未加载当前接口、真实 E2E 需要使用已登记 worktree slot 端口运行，或 worktree 融合后需要在 `E:\IntRuoyi` 的 `int_main` 主端口复验。
- Preflight check: 同时显式传入前端和后端 URL；附加 worktree 必须来自同一 runtime slot，融合后主运行态只允许 `8081/48081` 且端口命令行归属 `E:\IntRuoyi`。脚本应只允许这两种合法模式：`int_main 8081/48081` 或按集中端口契约成对分配的 `int_main slot 1..100`。当验收要求“融合后复跑”时，必须使用新的 run/event、独立证据目录和当前主运行态证据；worktree 上已经 PASS 的事件、截图和结果只能作为融合前历史。
- Blocker: 只传一个 URL、端口既不是 `8081/48081` 又不属于同一 slot、未确认端口监听命令行归属目标 worktree/主工作区、后端业务接口返回配置缺失/404、运行 Jar 缺少本次关键方法/字段、或真实 E2E 仍只接受 worktree 模式时必须停止并记录真实原因，不得静默切换端口或 API-only。
- Verification: 记录运行模式、base URL、backend URL、端口归属、前端 HTTP 200、后端 health UP、源码 revision/工作树指纹/运行产物哈希、关键目标接口业务响应、新的真实页面事件与断言，以及任务结束后的任务自有数据清理结果。融合后证据必须与融合前证据分目录保存，防止覆盖后无法证明各自运行态。
- Forbidden action: 禁止强停并行 48081、随机换端口、只看 health 就宣称目标 Controller 已加载、用未配对的 frontend/backend URL 造成前端访问旧后端、让融合后 E2E 脚本拒绝合法 `int_main 8081/48081` 主运行态，或把 worktree 旧事件复制/改名后冒充融合后复跑。
- Evidence: `doc/tasks/20260726-edhr-release-dossier-requirement-switches/execution-log.md`，48081 旧 jar 返回新增接口 404 后，使用 slot 5 的 8086/48086 成对 URL 完成真实 E2E；`doc/tasks/20260727-edhr-visual-fill-config-implementation/execution-log.md`，融合后先在 slot 2 通过，再修正脚本允许 `int_main 8081/48081` 并完成主端口真实 E2E；`doc/tasks/20260814-frontline-active-order-submit-allocation-docs/verification-report.md`，旧 8099/48099 事件仅保留为融合前证据，新增显式主线 profile 后在 8081/48081 生成新事件并独立保存融合后证据。

### Playwright 全新上下文登录导航竞争门禁

- Trigger: 真实 E2E 登录阶段出现 `Execution context was destroyed, most likely because of a navigation`，脚本在首次打开登录页后执行 `page.evaluate(() => localStorage.clear())`，或登录页存在自动重定向。
- Preflight check: `browser.newContext()` 创建的非持久化上下文默认没有上一轮 cookie、localStorage 或 sessionStorage；登录脚本应在首次导航前按需调用 `context.clearCookies()`，然后只导航一次登录页。若任务确需清理持久化 storage，必须使用受控持久化上下文并在应用加载前完成，不得在 Vue 路由已启动后清理。
- Blocker: 首次 `page.goto()` 后页面正在自动跳转、`page.evaluate` 因执行上下文销毁失败、登录请求尚未发出，或通过捕获该异常继续执行时必须停止并修正登录前置顺序。
- Verification: `node --check <real-e2e-script>` 通过后，使用官方登录身份和真实前后端 URL 重跑完整 Playwright 路径，必须得到登录接口成功、目标页面断言 PASS、任务自有 fixture 清理为 0。
- Forbidden action: 禁止吞掉导航异常、循环重试登录掩盖脚本竞争、复用带未知登录态的持久化 profile、或把已生成的旧截图/旧 `result.json` 当成本轮 E2E PASS。
- Evidence: `doc/tasks/20260730-edhr-frontline-fill-tabs/execution-log.md`，一线填写真实 E2E 在全新 context 的首次登录页导航后清 storage 触发执行上下文销毁，改为导航前清 cookie 且单次打开登录页后通过。

### Windows 换行与脚本行为同步

- Trigger: 修改 `tests/e2e/*static.spec.js`、真实 `*.e2e.js` 脚本、Windows worktree 融合后出现静态合同在目标 worktree 自身失败、CRLF/LF 差异或废弃弹窗流程断言。
- Preflight check: 先在目标 worktree 和当前工作区分别运行同一静态合同；读取源码时对只检查模板片段的静态合同统一归一化 CRLF 为 LF；定位 Vue/SFC 弹框、函数或组件块时优先按稳定 class、data 属性、组件名或下一个函数/组件声明回找边界，不用缩进数量精确匹配；负向断言必须先收窄到目标函数/模板块，避免把同文件无关编辑表单、弹窗或其它能力中的合法字段误判为失败；确认真实 E2E 脚本与当前页面真实用户路径一致。接口或页面契约收缩、删除旧输入字段、拆分写入/只读链路时，必须同步搜索相邻宽静态合同和真实脚本，删除对旧字段的正向断言并补充旧字段禁止断言；不得只因目标聚焦合同通过就忽略相邻合同仍要求旧行为。
- Blocker: 若静态合同在目标 worktree 自身也失败，必须先判断是合同过期、换行误判、正则范围过宽还是产品实现失败；不得把目标 worktree 自身失败直接当作融合漏项。
- Narrow fix: 若当前任务只修一个窄范围页面缺陷，而同一个宽静态合同存在无关既存失败，先保留失败证据，再新增或运行聚焦本缺陷的独立静态合同；不得为了通过宽合同顺手改无关产品逻辑或断言。
- Verification: 更新静态合同后必须重跑目标 worktree 涉及的全部静态合同；涉及真实 E2E 脚本行为变更时，至少用静态合同断言真实脚本等待的 API、点击的按钮和禁止的旧弹窗步骤。
- Experience: 前端静态合同若使用相对源码路径，必须从脚本约定的工作目录执行；从仓库根目录直接执行导致 `ENOENT` 时，应先修正命令工作目录再判断产品实现，不能把测试前置错误记为功能回归。
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
- Preflight check: 运行前必须明确本轮用户要求的主链路范围与可选扩展断言边界；主链路结果文件、扩展诊断结果文件和固定最终证据文件必须使用不同路径，或在脚本启动前确认无同任务目录写入进程会覆盖默认结果。写入型 E2E 的 manifest、scenario state、截图、结果和 cleanup 也必须绑定同一合法 run ID 并进入独立子目录；只隔离截图/结果而仍共享 fixture manifest，会让另一轮 cleanup 删除当前 fixture，仍不算隔离。若完成门禁同时读取 Markdown evidence 和 Playwright `result.json`，二者必须来自同一个 task root 和同一轮 run，不能回退读取主工作区、其它 worktree 或历史 run 的同名结果；若 evidence 记录目标请求或响应身份，`result.json.targetRequestEvidenceFlushed` 必须为 `true`，`result.json.targetRequests` 每一项必须是 JSON object，`result.json.targetRequests` 与 Markdown 中的 URL、Method、HTTP Status、Business Code 等关键请求字段必须逐项一致，且每个 `targetRequests[*].label`、`targetRequests[*].url` 与 `targetRequests[*].method` 必须存在并非空，`targetRequests[*].httpStatus` 与 `targetRequests[*].businessCode` 都必须存在并可解析为数字；`result.json.targetResponseIdentities.<LABEL>` 每一项必须是 JSON object，`result.json.targetResponseIdentities.<LABEL>.field` 必须存在且非空，`result.json.targetResponseIdentities.<LABEL>.value` 必须存在且可解析为正整数，`result.json.targetResponseIdentities.<LABEL>.sourceRequestLabel` 必须存在且非空，并绑定回同一个 canonical `<LABEL>`，且 `targetResponseIdentities` key 集合必须等于同一 artifact 内 `targetRequests[].label` 观测集合。扩展断言必须通过显式 opt-in 环境变量开启，默认不得影响主链路验收结论。
- Blocker: 若扩展诊断脚本仍在运行、默认结果文件或 fixture manifest 被其它进程改写、当前 run 的数据库对象被其它 cleanup 删除、可选断言失败覆盖主链路 PASS、报告中的文件号/状态与最新主链路结果不一致、Markdown evidence 与 `result.json` 的 status/root ID/关键闭环字段不一致，Markdown 目标请求成功但 `result.json.targetRequestEvidenceFlushed` 不是 true、`result.json.targetRequests` 缺失、`targetRequests[*]` 非 JSON object、指向其它后端、方法/HTTP 状态/业务码不一致、`label` 缺失/为空、`url` 缺失/为空、`method` 缺失/为空、`httpStatus` 缺失/非数字、`businessCode` 缺失/非数字，或 `result.json.targetResponseIdentities.<LABEL>` 非 JSON object、缺少 `field`、缺少可解析正整数 `value`、缺少对应 `sourceRequestLabel`、`sourceRequestLabel` 为空 / 串用其它 label / 与 `targetRequests[].label` 观测集合不一致，必须停止收尾并恢复到清晰的主链路证据；不得把被扩展诊断覆盖的 `BLOCKED`、旧文件号或旧 `result.json` 当作当前验收结论。
- Verification: 收尾前延迟复查一次结果文件和任务文档，记录无当前任务脚本进程、默认结果和固定最终结果均为预期状态，且 `verification-report.md`、`task.md`、`execution-log.md` 的文件编号、文件 ID、master ID、状态、浏览路径、目标请求以及 Markdown evidence 与 `result.json` 的核心身份字段一致。
- Forbidden action: 禁止多个并行 Playwright 脚本共享同一个最终结果路径；禁止扩展诊断失败后直接改口主场景 BLOCKED 或 PASS；禁止用旧 resume 结果覆盖新建任务文件；禁止让 completion gate 为了“找得到结果”跨 task root 读取旧 run 或其它 worktree 的 `result.json`；禁止 Markdown 手工写目标请求成功而 `result.json.targetRequests` 指向旧后端、缺少真实请求、缺少实际 label、缺少实际 URL、缺少实际方法、缺少可解析 HTTP 状态或缺少可解析业务码；禁止只用响应身份 key 替代 `field` / `value` 结构化采集，禁止只用响应身份 key、field、value 三元组替代来源请求 label 绑定或跳过请求/响应身份集合一致性；禁止把可选 viewer linkage、签核追溯、权限负向验证混入用户明确限定的主验收范围。
- Evidence: `doc/tasks/20260802-dcc-original-release-e2e-current/execution-log.md`，DCC 原版发布主链路 PASS 后，可选 viewer linkage / traceability 诊断多次覆盖默认结果和报告，最终通过显式关闭扩展断言、固定主链路结果文件并延迟复查结果稳定性收口。
- Evidence: `doc/tasks/20260802-dcc-traceability-ux-fixes/verification-report.md`，签核追溯 UX 复验先识别默认 ACTIVE 源缺待签名按钮导致错误密码诊断不适用，再显式绑定任务自有 wrong-password 结果文件，最终同时证明页面 UX、只读一致性和 `dccWriteRequests=[]`。

### 真实 E2E 页面加载判据门禁

- Trigger: 真实 Playwright 验证只读详情页、批次执行详情、当前工序高亮、页面顶部批号/执行号/标题文案可能与接口字段不一致。
- Preflight check: 脚本必须先等待目标业务接口命中目标对象 ID，再等待本次需求真正依赖的页面控件或状态渲染；只读页面可用任务组、状态 class、颜色、按钮可见性等目标控件作为页面加载判据。登录页、首页和带轮询/统计角标/外部资源的 SPA 不得把 `networkidle` 当作可用性前置，应使用 `domcontentloaded` 或 `commit` 后继续等待目标表单/控件，并核验正式登录或业务响应。页面初始化若按订单、工序、人员等链式异步选择并会关闭弹窗，自动化在重新打开选择器前必须等待整条初始化链的末端状态稳定；只等待第一个字段有值仍可能与后续回写竞争。PQC 订单选择后即使订单号已经显示，也必须继续等待正式工序、任务和检验面板就绪，或明确出现 `data-frontline-error-message`；订单摘要文本不能替代工序/任务加载完成判据。
- Blocker: 页面不稳定展示内部执行号、生产批号或标题文本时，不得让这类文本等待替代目标行为断言；因持续后台请求永远达不到 `networkidle` 时应修正等待判据，不能把它误报为登录或业务接口失败；若目标业务控件未渲染或接口未命中目标 ID，必须失败并截图记录。
- Verification: 证据需包含目标接口 ID、目标页面控件状态、截图路径、关键样式/交互断言和 MES 写请求数；PQC 还必须记录订单摘要、工序/任务选择和检验面板（或正式错误状态）三段链路；修正等待条件后必须重跑真实 Playwright。
- Forbidden action: 禁止为了通过 E2E 删除目标页面断言、改成 API-only、等待无关菜单/标题文本、或把页面未渲染解释成接口已通过。
- Evidence: `doc/tasks/20260729-edhr-parallel-start-process-highlight/verification-report.md`，真实脚本改为接口命中目标批次后等待工序组渲染，并断言三 个当前工序黄底；`doc/tasks/20260820-frontline-production-pqc-success/verification-report.md`，PQC 真实脚本修正为订单摘要后继续等待工序/任务检验面板或正式错误，再执行提交。

### 真实 E2E 用户列配置与列表可见性门禁

- Trigger: 真实 Playwright 验证报工列表、排产工单、统一列表模板或任何支持用户自定义显示字段的表格，尤其默认用户列配置可能隐藏单据编号、报工单号、内部 ID 或状态列。
- Preflight check: 列表断言前先确认当前用户实际可见列；若目标编号列被隐藏，必须改用页面可见的业务唯一组合（如工序编码、人员工号、数量、来源生产工单号、进度文本）证明列表更新，并用 DB 或只读 API 复核隐藏编号与正式记录绑定。
- Blocker: 页面已显示目标业务行但脚本只因隐藏编号列缺失而失败时，不得判定产品失败；若可见业务组合也不足以唯一证明目标行，必须记录当前可见列并补充只读后置核验。
- Verification: 证据需同时包含真实页面可见字段断言、隐藏编号的只读 DB/API 绑定证据、目标页面路由和当前用户列配置影响说明。
- Forbidden action: 禁止把用户列配置隐藏导致的编号不可见写成业务未更新；禁止为通过 E2E 强行重置用户列配置、改用 API-only 替代页面列表、或断言不可见列文本。
- Evidence: `doc/tasks/20260802-test-server-feedback-import-not-working/verification-report.md`，报工列表当前列配置未显示报工单号，但页面显示 5 条导入明细，DB 复核 `FB-000157` 至 `FB-000161` 与导入记录绑定，排产工单页面 `/mes/pro/schedule-order` 显示目标工单进度。

### 真实 E2E 动态事件查询与确认响应门禁

- Trigger: 真实 E2E 在页面写入后需要只读发现新生成事件、提交记录、分配记录、审计记录或其它运行态 ID，或确认按钮依赖后端写接口完成后继续断言。
- Preflight check: 只读发现接口必须携带后端分页接口要求的完整查询条件，例如日期、租户、业务对象、提交编码或任务自有前缀；确认类动作必须等待对应写接口响应，断言 HTTP 成功且业务 `code=0`，再进入后续 UI 或只读核验。若确认动作会刷新列表或状态投影，还必须在触发保存前注册保存后列表响应等待，并按本次事件/业务身份核对正式响应中的目标行。
- Blocker: 只按列表默认条件查询导致接口 500、跨日/跨页误选、用外部预填 eventId 替代页面提交后动态发现、等待瞬时 toast 而未等待写接口响应、保存后只读取旧 DOM/旧响应、或确认接口业务码非 0 时必须停止。
- Verification: 静态合同应锁定真实 E2E 对必填查询条件、动态 ID 占位符、确认接口响应和保存后正式列表响应断言的使用；真实 E2E 证据需记录动态发现的 ID、确认接口路径、业务响应校验、保存后列表目标行和后置只读核验结果。
- Forbidden action: 禁止把 toast 文案、弹窗关闭、列表第一行、保存前缓存响应、硬编码事件 ID、API-only 写入、或忽略业务 `code` 的 HTTP 200 当作确认完成。
- Evidence: `doc/tasks/20260731-team-leader-workbench-prd-plan/execution-log.md`，生产组长真实 E2E 事件发现补齐 `submitDate`，确认报工改为等待 allocation confirm 响应并断言业务码；`doc/tasks/20260814-frontline-active-order-submit-allocation-docs/execution-log.md`，组长改配保存后等待 `/submission/page` 正式响应，核对 O1/O2 精确行、超量清零与红色状态消失。

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
- Preflight check: 切换前读取并记录原始状态；脚本必须有 `finally` 恢复逻辑，恢复后再用独立 API 或页面断言确认状态回到原始值。共享路线、租户开关等状态如果目标值已满足，脚本必须记录“未改变”并跳过恢复，禁止用固定布尔值把原本启用的共享状态在异常分支改回停用。
- Blocker: 关闭/开启断言通过但恢复失败、恢复后接口值不一致、或页面仍显示变更后的状态时，必须立即执行受控恢复并记录失败位置；不得把产品断言 PASS 当作完整 E2E PASS。
- Verification: 证据必须同时包含变更态断言、恢复动作结果、恢复后页面或接口复验；恢复使用 API 时必须说明它是 cleanup，不得替代真实页面变更路径。
- Forbidden action: 禁止留下全局开关关闭、禁止记录密码/token、禁止用未复验的 `finally` 假设恢复成功，禁止在未确认本脚本实际改变共享状态时执行反向恢复。
- Evidence: `doc/tasks/20260725-edhr-global-recordbook-switch/verification-report.md`。

### 当前共享数据写入 E2E 派生状态恢复门禁

- Trigger: 用户明确授权 Playwright 使用当前共享数据、既有管理员数据或非任务新建数据执行写入验证，且页面结果包含由分配、配置、明细或关系实时计算的进度、汇总、状态或统计值。
- Preflight check: 默认禁用当前共享数据写入；只有用户对租户、账号和“当前数据”作出明确授权后，才能把写入范围收敛到一个稳定业务对象。写入前必须同时保存正式源事实快照和所有受影响派生值；每个成功写响应后立即记录当前版本，异常恢复必须优先复用同一真实页面。若相同源事实重新保存后派生值与测试前不同，先核对历史记录身份、聚合分组和正式计算来源，再把首次正式重算后的值作为规范基线，不能把陈旧派生值当作恢复目标。
- Concurrent identity recheck: 共享 `int_main` 数据可能在代码构建、运行态重启或只读诊断期间被其它正式页面任务修改。首次目标写入前必须从真实页面重新核对稳定业务 ID、编码、删除状态、启用状态、ACTIVE 版本和未结束候选；若目标被删除、替换、改绑或版本链漂移，立即停止。不得把预检返回的“新建”当作原目标恢复，不得创建新业务对象冒充原对象继续验收。
- Blocker: 无明确当前数据授权、无法唯一定位写入对象、原始源事实快照不完整、写入后无法通过页面恢复、恢复后的源事实不一致、派生值差异无法由正式计算来源解释、目标页面未刷新受影响派生接口、当前数据缺少能触发目标计算分支的正式样本，或异常分支不能确认服务端是否已写入时必须停止，并记录精确对象和残留影响。
- Verification: 证据必须包含租户/账号标签、稳定业务对象身份、写前源事实、每轮页面写响应、目标页面刷新接口响应、变更态派生值、最终源事实数量/模式/只读状态等值比较、全部受影响派生值与规范基线比较、`pageErrors` 和目标接口错误为空。写入生成的正式审计版本可以保留，但不得把审计存在误判为业务数据未恢复。若配置变更理论上只影响特定计算分支，必须证明当前页面数据实际命中该分支；否则记录为数据前置 BLOCKED，不得用未命中的手动覆盖、固定产能或 API-only 结果冒充正向通过。
- Forbidden action: 禁止把直接 API/SQL 写入当作页面 E2E，禁止只恢复输入数量而不复核派生进度，禁止强行把正式重算结果改回陈旧页面值，禁止在恢复失败后继续其它写入场景，禁止用成功路径末尾的假设代替 `finally` 异常恢复。
- Evidence: `doc/tasks/20260810-active-order-progress-allocation/verification-report.md`；`doc/tasks/20260811-scheduler-night-shift-human-efficiency-admin-e2e/verification-report.md`。



## 官方登录前置与 admin-only 全量验证门禁

- Trigger: E2E 脚本调用 `scripts/preflight/login-preflight.mjs`、执行 `芋道源码/admin` 只读全量验证、或工作区融合后发现真实 E2E 登录前置脚本缺失/目标文案过期。
- Preflight check: `scripts/preflight/login-preflight.mjs` 必须存在于工作区根目录并通过真实前端登录；目标文本必须使用当前页面真实可见文案，不得沿用历史菜单标题。从 `.env` 读取 `VITE_APP_DEFAULT_LOGIN_*` 时，解析器必须允许键、等号和值之间存在空白（例如 `\s*=\s*`），调用登录脚本前先确认租户、用户名和密码均为非空；不得让缺失值造成后续 `--tenant/--username/--password` 参数错位并误报登录响应超时。Windows/PowerShell 下传递中文租户名、目标文案或其它中文参数给 Node/Playwright 时，优先使用 Node `spawn` 参数数组、临时环境变量或已验证的 UTF-8 包装脚本；若 `waitForResponse` 超时但诊断脚本能看到真实登录请求成功，应先排查参数编码和脚本锚点，不得直接判定登录接口或业务页面失败。密码只能通过临时环境变量或命令参数传入，任务日志和证据必须脱敏。若 Playwright 默认浏览器缓存缺失，先检查本机稳定 Chrome/Edge 可执行文件并通过 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 显式传入；只有缺少可用浏览器时才记录 E2E BLOCKED，不得临时下载浏览器或把浏览器缺失写成业务页面失败。
- Blocker: 若只授权 `芋道源码/admin`，写入型、多用户、签名、放行、发布或需测试租户数据清理的 E2E 必须记录 BLOCKED；不得在 admin 基线租户上创造测试写入数据，也不得用 API-only、直连历史 execution 填写页或 mock 代替。
- Verification: 管理员只读验证应优先覆盖登录前置、目标页面文案、关键目标接口业务码、批次详情、只读预览、伴随单据、表单日志、权限可见性和无 MES 写请求；当前活动填写必须走正式页面按钮或 `openTask` 返回上下文，历史只读必须走 tracking 模式。若先因浏览器缓存或运行库迁移缺失失败，必须记录 RED 原因、解除动作和复跑 GREEN 证据。
- Forbidden action: 禁止删除或跳过官方登录 preflight；禁止把缺失 preflight 脚本当成 E2E 通过；禁止在真实脚本中保留历史默认密码；禁止把过期固定批次/任务 ID 当作长期前置。
- Evidence: `doc/tasks/20260725-full-e2e-admin-validation/verification-report.md`；`doc/tasks/20260730-banzuzhang/verification-report.md`；`doc/tasks/20260807-remove-team-configuration-tab/verification-report.md`。

### 写入型 E2E 任务自有模拟环境门禁

- Trigger: 写入型、多账号、权限范围、共享数据或跨角色可见性的真实 E2E 初始判断为缺少测试账号、租户、路线、工序、报工样本或可清理数据前置。
- Preflight check: 先判断缺口是否可在本机测试租户内用任务自有前缀、安全 fixture 脚本和正式 schema/API 补齐；fixture 必须创建真实账号、角色、权限、业务对象和清理范围，密码只能由环境变量注入，不得写入日志或证据。角色权限除目标菜单外，还必须盘点页面全局壳层会自动请求的角标、待办或导航查询权限；缺少这些只读权限会制造与目标业务无关的控制台错误，必须在 fixture 合同中显式列出，不能靠 E2E 忽略。涉及正式生产提交、报工、批记录或生产组长可见性闭环时，fixture 还必须逐项断言已确认工单、非空记录本字段 schema、生产组长 `PRODUCTION + EMPLOYEE` 正式人员范围，以及每轮运行新业务签名，不能复用旧提交痕迹。验收要求覆盖不同员工或不同工序时，任何写请求前必须从页面运行态盘点去重后的正式员工、路线工序和 MES 工序数量；不足目标组合时先 BLOCKED，再用固定任务身份补齐正式员工档案、人员范围、工序配置、签名授权及授权审计，不能把选择入口可用当作已覆盖组合。涉及同一目标工序的多类传统报表资料时，启动任何写路径前必须只读统计每类正式 `batchRecordReportId` 的非空数，并证明同一任务自有路线工序具备完整组合；`formSlotType + formTemplateId` 只能证明动态表单槽位，不能替代传统报表绑定。涉及过程检验、损耗单等动态 FormCenter 槽位时，E2E 前置脚本也必须按当前路线工序正式绑定的 `formTemplateId + lastPublishedTemplateVersionId` 校验，不得把截图中的模板 ID 或历史测试模板 ID 写死为通过条件。跨角色签名链路还必须先证明每个业务账号能登录且签名口令已通过安全环境变量注入，数据库中仅存在账号或签名授权行不算凭据可用。
- Preflight check: 涉及由按钮或二级页面打开的跳转页时，真实 E2E 的 URL 断言必须跟当前源码和实际跳转结果一致，优先校验页面是否打开、关键 DOM 是否可见和返回路径是否正确；不要把已经不在当前页面契约里的旧 query 参数或历史 helper 参数名当成必需条件。
- 如果上一轮真实 E2E 已执行 cleanup 并清空任务数据，后续重跑前必须重新 prepare fixture；若 verify 提示 `Task-owned users or roles are missing`，说明当前 manifest 对应的任务用户或角色已被清掉，不能复用旧 manifest 或旧 runId，必须先重建任务自有账号/角色再继续 Playwright。
- Blocker: 无测试租户授权、缺正式 schema、缺目标工序要求的任一传统报表绑定、只有动态表单槽位、跨角色登录/签名凭据未证明、缺必要菜单/角色权限、无法清理任务自有数据、只能使用 `芋道源码/admin` 或需要生产/无关真实业务数据时，必须继续记录 BLOCKED。
- Verification: 模拟环境完成后必须分别记录 fixture 输出、运行态 API 只读核验、真实 Playwright 页面路径、跨账号可见性、目标写接口业务 `code=0`、目标 HTTP/page errors 为空，以及删除/禁用/清理后的状态。正式提交链路必须额外记录生成的报工、记录本、工序池事件 ID；多员工、多工序验收还必须逐轮记录页面所选员工、路线工序、MES 工序和签名主体，并以正式数据库事实证明匹配，不能只统计入口数量。人员范围验收还需用对应组长可见、非对应组长不可见证明范围生效。若前置阻塞，证据必须记录各正式来源总数/非空数、完整组合查询结果、缺失的凭据类别、实际业务写请求数和任务残留数，不能只写“缺 fixture”。
- Forbidden action: 禁止把 API-only、静态合同、默认 admin、mock 数据或前端直塞 localStorage 当作写入型 E2E 通过；禁止因首次缺账号就跳过可安全构造的任务自有模拟环境。
- Evidence: `doc/tasks/20260805-process-loss-reasons/verification-report.md`，AC-D04 先从缺生产组长/员工前置转为任务自有模拟环境，再用两个生产组长真实页面验证授权工序、共享新增、共享修改和删除停用；`doc/tasks/20260807-formal-frontline-production-submit/verification-report.md`，一线正式提交 fixture 补齐已确认工单、记录本 schema、`PRODUCTION + EMPLOYEE` scope 和新签名后，真实提交事件只对对应生产组长可见；`doc/tasks/20260814-frontline-active-order-submit-allocation-docs/execution-log.md`，任务角色补齐全局审批角标查询权限 `1221` 后，真实页面目标控制台错误归零。

### 工艺路线过程检验映射正式来源门禁

- Trigger: 从工艺路线“流转关系图/表单槽位”进入批记录单元格链接，或验证一线 PQC 数据映射到共用过程检验记录表单；尤其涉及 `PQC_AGGREGATE_DETAIL`、`PROCESS_INSPECTION`、`formTemplateId`、`lastPublishedTemplateVersionId`、`routeProcessId` 或“粗洗/精洗”等工序名称。
- Preflight check: 先按当前路线的 `routeId + routeProcessId` 解析工序正式身份，再按该工序的 `PROCESS_INSPECTION formBinding` 取得 `formTemplateId + lastPublishedTemplateVersionId`；PQC 字段必须来自当前路线绑定的 DCC 项目及其当前发布 QA 版本，并按 QA 工序正式 code/ID 或已确认的 routeProcessId→qaProcessId 关系匹配。正常入口可以省略模板 query 参数，但后端必须从正式 binding 自动解析；前端入口必须携带当前 routeProcessId 和 `sourceReportId=PQC_AGGREGATE_DETAIL`。
- Blocker: 当前发布 QA 版本没有目标工序、正式 QA 项目为空、模板版本未发布、来源字段属于其它 routeProcessId、或只能按名称把相邻工序（例如“清洗”）猜成目标工序时，必须在业务写入前停止并记录 BLOCKED。
- Verification: 真实 Playwright 必须从工艺路线页面点击过程检验链接进入 `/mes/pro/batch-record-cell-link`，并断言当前 routeProcessId、PQC 来源、过程检验目标模板和当前工序字段数量；API 只能做最终只读复核，证据还需记录 QA/DCC 正式来源及 MES 写请求数为 0（只读场景）。
- Forbidden action: 禁止把截图中的模板 ID、历史模板版本、产品 ID、processId、表单名称或相邻 QA 工序作为默认来源；禁止把动态 `formBindings` 当传统批记录报表绑定；禁止用 API/SQL 直接补映射后冒充真实页面验证。
- Evidence: `doc/tasks/20260820-pqc-shared-process-inspection-mapping/verification-report.md`，精洗/清洗正常入口与 58 个 PQC 字段真实通过；粗洗因 DCC 项目 147 当前发布 QA 版本缺正式工序而按门禁阻塞。

### 写入型 E2E 响应不确定断点恢复门禁

- Trigger: 写入请求已发出，但浏览器等待业务响应、列表刷新或页面确认时超时；批量页面维护只完成部分记录；重新执行可能重复提交、覆盖并发数据或误判失败。
- Preflight check: 写入型测试脚本必须在每次目标写响应明确成功后立即持久化回执身份和当前进度，再执行页面复位、可操作性或最终网络诊断断言。恢复前必须启动新的只读会话，按稳定记录 ID、业务编码或唯一业务键把每个目标分类为“仍为原值”“已经是目标值”“出现其它值”；同时重新核对所属业务对象、启用状态和全部非目标记录。只有仍为原值的目标才能进入待处理集合；已是目标值的记录必须跳过；并发新增的非目标记录若不违反业务唯一性，纳入保持快照，不得删除或覆盖。
- 页面请求监听必须先于触发动作建立，尤其是 Element Plus 远程下拉的 `fill`、输入或点击动作；这类动作可能立即发出请求，先操作后等待会漏掉响应并制造假超时。超时后仍须按稳定业务键只读核验，不能仅凭等待器超时判定写入失败。
- Blocker: 缺少稳定记录身份、当前值既不是原值也不是目标值、目标记录被删除或移动、内部编码/启用状态漂移、同一精确范围仍有活动写入任务、或无法区分超时请求是否落库时必须停止；不得继续批量重放。
- Verification: 记录恢复前 `completed/pending/diverged` 数量；如果失败发生在成功回执后的页面断言或诊断阶段，先用已持久化回执和全新只读会话确认该轮正式事实，再决定剩余轮次，不能把 harness FAIL 等同于业务写入失败。当前会话实际写请求数必须与 pending 数完全一致、每个写响应业务码成功；最终用全新只读会话断言目标值全部完成、ID/业务编码/所属对象/启用状态保持、非目标记录保持、占位值为零且 MES 写请求为零。
- Forbidden action: 禁止把客户端超时直接当作服务端失败并盲目重试；禁止为恢复方便删除并发新增数据、重建整批记录、改用直接 SQL/API-only 写入、扩大范围或把其它当前值强制覆盖成目标值。
- Evidence: `doc/tasks/20260807-loss-reason-human-readable-names/verification-report.md`。

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


## eDHR 任务自有资料模拟夹具详情读取边界门禁

- Trigger: 真实 E2E 为 eDHR 批次资料上传、特殊节点完成或分段模拟创建“仅包含资料节点”的任务自有批次，并在写入后打开真实批次执行详情页。
- Preflight check: 模拟夹具必须在 `remark` 与 `activeContextKey` 同时保存完整阶段标记和 `simulationRunId`；详情读取流程只能对两字段完全匹配的任务自有夹具跳过通用工艺路线/批记录任务补建，真实批次、标记不完整或字段不一致的批次继续走正式路由存在性、批记录配置和权限校验。
- Blocker: 目标上传接口返回成功但详情页出现“工艺路线不存在”“缺少工艺流程批记录配置”或同类通用配置错误时，必须判定详情页 E2E 未通过；不得只凭 POST 业务码和输出快照宣称页面链路通过。
- Verification: Playwright 必须从真实登录页进入列表和详情页触发按钮，断言目标响应、页面无上述配置错误、`pageErrors=[]`，并在第二轮运行断言上一轮 `simulationRunId` 被清理；API 只能做最终只读对象核验。
- Forbidden action: 禁止用有效共享工艺路线、修改真实路线配置、隐藏错误提示、放宽所有活跃批次的路由补建条件或 API-only 写入来掩盖模拟夹具与详情读取边界不一致。
- Evidence: `doc/tasks/20260821-simulation-stage4-dossier-upload-design/verification-report.md`。

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

- Trigger: Playwright 在 Element Plus `el-select` 中选择租户、工单、工艺路线、角色、用户、弹框表单项或其他写入型业务对象。
- Preflight check: 优先按页面可见业务唯一文本定位选项，例如租户名称、工单编码、路线编码/名称/ID；填入搜索词后必须等待目标 `.el-select-dropdown__item:visible` 出现并点击该选项。Element Plus 的 placeholder 可能由外层组件展示而不写入真实 `input[placeholder]`，真实 E2E 定位搜索型 `el-select` 时应先用 DOM 快照确认可见 `input.el-select__input[role="combobox"]` 或控件作用域内稳定选择器，再填值触发远程搜索。若同一弹框内存在多个下拉或校验消息会产生重复可见文本，必须先按精确可见 `.el-form-item__label` 定位当前表单项，再在该 `.el-form-item` 内读取当前 combobox 的 `aria-controls`，只从对应下拉面板选择选项。若上一步操作的是 `multiple` 多选下拉，Element Plus 选择后可能继续保持浮层展开，切换到另一个下拉前必须显式关闭旧浮层并等待目标表单项自己的下拉面板可见；读取选项时优先限定当前表单项或最新可见下拉，避免把旧多选面板当成新字段选项。若标签存在包含关系，例如“类别”和“类别否变更”，不得用 `hasText('类别').first()` 定位目标下拉，应给目标控件增加稳定 `data-testid` 或使用精确标签边界定位。若 `el-select` 位于 `el-popover`、抽屉内局部弹层或 click-outside 容器中，必须确认下拉面板归属不会触发外层误关闭，必要时使用受控可见状态和 `:teleported="false"` 静态合同锁定。
- Blocker: 如果只按 `input[placeholder=...]` 找不到控件、只填输入框后按 Enter 未触发真实选项选择、目标选项未出现、页面显示文本与脚本断言字段不一致、同名校验消息或重复下拉项导致 locator 命中多个字段、多选旧浮层未关闭导致读取到错误选项、或选择项点击导致外层 Popover 在确认动作前误关闭，必须停止并记录输入框 DOM 快照、下拉可见文本、弹层状态和相关接口响应，不得继续提交写入。
- Verification: 对写入结果使用 UI 响应和最终只读 API/DB 核验；涉及发布版/草稿版差异时，必须核验落库版本 ID、版本号、快照 JSON 和当前草稿仍存在。Popover 内下拉还必须验证“选择后保持打开、确认成功后显式关闭”。
- Forbidden action: 禁止把接口数组下标、隐藏 value、输入框残留文本、API-only 选中或坐标点击当作真实页面选择。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/execution-log.md`；`doc/tasks/20260726-route-flow-copy-popover-stability/execution-log.md`；`doc/tasks/20260730-standard-template-list-search-alias/`，顶部菜单搜索框视觉上显示 placeholder，但真实 DOM 只有 `input.el-select__input[role="combobox"]`，最终真实 E2E 改用 combobox 后通过；`doc/tasks/20260829-registration-certificate-upload-production-fields/bug-regression-evidence.md`；`doc/tasks/20260901-registration-change-mvp-e2e/execution-log.md`。

### Element Plus 权限树与多选提交门禁

- Trigger: Playwright 在权限角色菜单树、用户角色多选或其它树/多选弹窗中选择并提交授权。
- Preflight check: 权限标识、角色码和菜单 ID 必须先从完整页面数据按唯一键核对，再用弹窗实时可见的节点名称选择；树节点重绘后不得复用旧下标。多选下拉选择后必须显式收起下拉层并确认其不可见，再点击“确定”；父子联动开关必须读取实际 checkbox 状态后明确关闭或开启。
- Blocker: 选择后下拉层仍拦截确认按钮、树节点名称重复且无法映射到唯一 ID、父子联动状态未知、提交响应返回的 ID 集合与预期不一致，或只通过页面标签而没有最终 ID 回读时必须停止并纠正，不得继续下一个角色或账号。
- Verification: 每次授权提交都记录允许的写请求路径和次数，随后重新打开同一弹窗按 ID 集合回读；幂等续跑只能复用已逐项核验的对象，不能盲目重放或删除重建。
- Forbidden action: 禁止按静态树下标、名称首个匹配、接口数组顺序或隐藏 value 选中权限；禁止把下拉层未收起、toast 成功或页面标签可见单独当作角色权限提交成功。

### Element Plus 页签点击门禁

- Trigger: Playwright 点击 Element Plus `el-tabs/el-tab-pane`，页面给 `el-tab-pane` 配置了 `data-*` 测试属性，点击后需要等待列表接口或页签内容。
- Preflight check: 先检查真实 DOM 和可访问树；`el-tab-pane` 上的 `data-*` 通常落在隐藏内容 pane，而可点击标签是独立的 `role="tab"` 元素。应按 `getByRole('tab', { name, exact: true })` 点击可见标签，点击后断言 `aria-selected="true"` 和目标内容可见，再使用 API/DB 做最终只读核验。多模块工作台（例如生产组长“人员管理/报工管理/报工历史”等）不得假设默认页签就是目标业务页签；定位表格筛选框、业务行或操作按钮前，必须先切到目标模块页签并等待该模块内容渲染。
- Blocker: `data-*` 定位器存在但不可见、点击长期超时、页签未变为选中、目标列表未渲染，或导航/点击异常被提前创建的未处理 `waitForResponse` Promise 覆盖时必须停止，记录页面文本、可访问角色、选中状态和目标网络请求。
- Verification: 真实 E2E 同时证明页签可点击、选中状态生效、目标业务行在页面可见；需要监听响应时应在触发动作前即时注册并确保监听异常不会覆盖导航或点击的原始错误。
- Forbidden action: 禁止对隐藏 pane 使用强制点击、坐标点击或仅修改 `data-*` 让脚本通过；禁止只用 API 响应代替页签切换和业务行可见性。
- Evidence: `doc/tasks/20260807-pqc-leader-management-five-records/verification-report.md`；`doc/tasks/20260807-formal-frontline-production-submit/execution-log.md`。

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
- Preflight check: 先按 `label-width + grid-template-columns + gap` 核算真实输入区宽度；关键字段必须使用专用布局类和静态合同覆盖。`el-input-number` 默认宽度可能大于网格列，必须显式设置 `width: 100%` 收敛到所在列；文本输入列需要 `min-width: 0` 和 `width: 100%`。必要时在 `el-select` 控件作用域内覆盖 `.el-select__tags-text` 默认省略宽度。窄栏里的 Switch 主标签与状态提示不得全部挤在一个可收缩 flex 行内，状态提示较长时应独占行或使用明确 grid 布局，并对关键标签设置不换行；禁用提示不能只用过浅灰色小字，应有足够对比度或明确状态条承载。Playwright 操作 Element Plus Switch 时，不得点击隐藏的 `input[role="switch"]`；应先等待可见 `.el-switch` 不含 `is-disabled`，再点击可见 `.el-switch` 或 `.el-switch__core`，最后读取隐藏 input 的 `aria-checked` 轮询校验状态，避免页面初始加载禁用态点击被吞掉。
- Preflight check: 多控件使用 CSS Grid 保持同一主行时，`grid-column: 1 / -1` 的条件提示、校验标签或状态条必须放在全部主行控件之后；若插在输入框与按钮之间，自动布局会让提示占满当前行并把后续按钮推到下一行。Playwright 判断是否同一行应比较数字框/选择框等可见控件外壳与按钮的中心线或整体边界，不得用原生 `input` 内部元素的 `top` 直接比较，因为控件内部垂直留白不同会造成误判。
- Blocker: 若选中值、输入值或 Switch 状态提示在控件内仍显示为 `...`、换行后被裁切、文字对比度过低导致视觉上看不清、数字步进控件溢出挤压相邻输入框、只靠 tooltip 或下拉选项完整展示、或静态合同无法锁定该字段专用布局，必须停止并修复布局。
- Verification: 静态合同或真实 E2E 必须断言目标控件有专用布局类、关键列宽足够、数字步进控件收敛到当前列、文本输入框可完整占满分配列、选中标签未继续使用默认省略宽度，Switch 状态提示完整可见、不会被窄栏裁切，且颜色对比足够；涉及 Switch 写入的真实 E2E 还必须记录点击前后 `aria-checked` 与保存接口 payload 一致。
- Verification: 网格主控件完整显示场景还必须锁定跨整行提示位于主控件之后，并在真实页面记录各可见控件外壳的边界或中心线，证明输入框和按钮没有被条件提示拆成两行。
- Forbidden action: 禁止把 `collapse-tags-tooltip`、扩大整页/整弹窗、硬编码当前角色名/目标项名、只验证下拉选项文本、或只调宽一个控件但让相邻控件继续被挤压当成“显示完整”。
- Evidence: `doc/tasks/20260725-edhr-pressure-pump-v13-filler-role/verification-report.md`；`doc/tasks/20260726-codex-test-target-item-input-display/verification-report.md`；`doc/tasks/20260728-edhr-detail-assist-preview-switch/execution-log.md`；`doc/tasks/20260805-profile-nas-table-auto-sync/execution-log.md`；`doc/tasks/20260813-allocation-dialog-remove-fifo-full-display/verification-report.md`。

### Element Plus 表格长文本换行与固定列边界门禁

- Trigger: 修改 `el-table` 的描述、备注、原因或其它长文本列，要求超宽自动换行、完整显示，且表格启用了 `show-overflow-tooltip`、用户列配置或固定操作列。
- Preflight check: 表级 `show-overflow-tooltip` 会让普通列进入单行省略逻辑，目标长文本列必须显式关闭该行为并使用专用 class 设置 `white-space: normal`、`overflow-wrap: anywhere` 和适用的 `word-break`。页面使用 `useUserTableColumns` 或同类列配置时，模板 fallback 与正式 default columns 的 `minWidth` 必须同步调整，不能只改其中一处。Element Plus 的 `class-name` 可能同时出现在表头、正文及固定列副本；采集正文文本或 scroll/client 尺寸时必须把 locator 限定到可见 `.el-table__body-wrapper td.<class-name> .cell`，并先断言命中的文本不是列标题。
- Blocker: 长文本仍只能依赖 tooltip 查看、computed `white-space` 仍为 `nowrap`、单元格 `scrollWidth > clientWidth`、`scrollHeight > clientHeight`、较窄桌面下描述列右边界越过固定操作列左边界，或正式用户列默认值仍保留旧宽度时必须停止。
- Verification: 聚焦静态合同同时锁定目标列关闭 tooltip、专用换行样式、模板 fallback 和正式 default columns；Playwright 至少覆盖常用桌面与较窄桌面，使用正文限定 locator 记录实际业务文本、computed style、行高、单元格 scroll/client 尺寸，并断言 `descriptionRight <= actionLeft`、console/page error 为空。
- Forbidden action: 禁止只加 CSS 但继续继承表级 tooltip 单行逻辑；禁止只改模板宽度而遗漏用户列默认定义；禁止用扩大浏览器、截图裁切、移除固定操作列或 tooltip 冒充完整显示。
- Evidence: `doc/tasks/20260809-edhr-batch-record-description-wrap/verification-report.md`；`doc/tasks/20260809-batch-record-test-mismatch-description-wrap/verification-report.md`。

### 写入型远程下拉候选新鲜度门禁

- Trigger: Playwright 写入型 E2E 通过远程搜索下拉选择正式用户、员工、角色、设备、路线、表单或其它会被新增/绑定/消费的候选对象，尤其脚本可重复运行。
- Preflight check: 每次写入型 E2E 前必须确认候选对象未被本业务关系消费；如关系具有唯一键或禁用后仍占用业务身份，应创建任务自有新鲜 fixture 或显式走“启用既有记录”路径。
- Blocker: 搜索候选已被上一轮 E2E 绑定、已禁用但仍占用唯一键、下拉仍可选但提交返回重复业务错误或 DB 500、或脚本把重复失败当成可接受成功时必须停止并刷新 fixture 或修业务校验。
- Verification: 证据记录候选来源、任务自有标识、页面真实选择动作、写接口响应和最终 UI/API/DB 状态；重复运行时不得复用已消费候选冒充新增路径通过。
- Forbidden action: 禁止用固定历史候选长期重复写入、禁止前端数组下标选中隐藏值、禁止 API-only 造绑定替代页面下拉、禁止记录密码/token 或把 DB 500 当作预期重复提示。
- Evidence: `doc/tasks/20260805-production-personnel-management/verification-report.md`，生产人员档案真实 E2E 使用新正式工 fixture `ppmformal151308` 完成正式工远程搜索关联。

## 表格行定位

- 当页面对列表进行本地排序、过滤或虚拟渲染时，Playwright 必须按页面可见的业务唯一文本定位目标行，再操作同一行的复选框或按钮。
- 行状态文案存在包含关系时必须使用精确状态或显式反向排除，例如“不可重排”包含“可重排”；脚本不得用 `hasText('可重排')`、`includes('可重排')` 直接定位可重排行，必须同时排除“不可重排”或读取专用状态/aria。
- 不得直接用 API 返回数组下标映射前端表格行；接口排序和页面排序可能不同，会误选冻结行、错误行或无关业务数据。
- Element Plus `el-table` 存在 header/body/fixed 表格重复 DOM 时，选择行复选框必须限定在可见 `.el-table__body-wrapper tbody tr`，显式排除 `.el-table__header-wrapper` 和 `thead`；点击后必须立即断言已选业务唯一键集合，再进入“确认/应用”等写入动作。
- 行内编辑会把原显示文本替换为输入框、开关或其它编辑控件时，只能用原文本定位并点击进入编辑；进入编辑态后必须改用当前弹框或表格作用域内唯一可见编辑器继续填写，保存刷新后再用目标文本重新定位。若同时出现多个可见编辑器或无法按稳定记录 ID 证明编辑对象，必须停止，不得继续复用依赖旧文本的动态行 locator。

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
- Blocker: 只完成排产前检查或重排预览、未观察到 `/auto-schedule/replan/apply`、进度停在 `90%`、开始日期弹窗或阻塞确认框未关闭、选中集合无法追溯、点击到禁用行/表头 checkbox、或目标请求/响应证据缺失时，必须判定真实 E2E 未通过；如果 `apply` 返回“排产完成创建 eDHR 批次缺少前置条件：首任务责任来源/候选池”，也必须判定为后端回归，不得把它解释成页面选择问题。
- Verification: 证据必须包含选中行数/业务文本、开始日期、三段目标请求 URL 和 payload 摘要、三段响应 HTTP 状态和业务码、apply summary、进度最终状态、`confirmDialogVisible=false`、`dateDialogVisible=false`、`pageErrors=[]`、`consoleErrors=[]`、最终截图和 JSON 路径；若后端已修复，`apply` 不能再返回 eDHR 批次前置条件错误。
- Forbidden action: 禁止把夹具红行验证、只读红行验证、API-only apply、历史截图、预览 summary、进度中间值或 success toast 单独当作全选应用 E2E 通过；禁止为了继续排产而二次阻塞确认“未参与排产的工单”。
- Evidence: `doc/tasks/20260804-mes-partial-replan-blockers/verification-report.md`，2026-08-05 用户截图复验中，旧二次确认导致 `90%` 卡住风险，改为非阻塞通知后 `芋道源码/admin` 当前页 12 条可选排产工单全选应用真实 E2E PASS；`doc/tasks/20260828-schedule-replan-all-worktree-e2e/verification-report.md`，手动重排真实 E2E 复验 PASS，且后端不再返回 eDHR 批次前置条件错误。

### Codex Runner 自动测试门禁

- Trigger: 新增、修改、运行或验收 `系统管理 > 测试管理`、Codex Runner、自然语言测试方法、检查点截图或由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前必须确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token 或经用户明确批准的本地 tokenless Runner 模式、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任；后端配置了 token 时必须用当前 token 完成注册探针，后端未配置 token 且任务明确采用 tokenless 本地模式时，Runner 请求不得发送伪 token 头，但仍必须完成后端注册、领取、心跳和结构化回写；Runner loop 必须在执行中和空闲轮询中持续 heartbeat；本机后端重启、换 jar 或切换运行态后必须重新确认 `yudao.codex-test.runner.token` 与当前模式一致，不能只检查当前 shell 环境变量或旧 Runner token 文件；不得把 `codex-test-runner.mjs --loop` 进程存在当作在线证明，必须核对后端 Runner 状态或数据库 `last_heartbeat_time` 未过期。测试管理执行入口若支持按需 Runner，前端不得因旧 Runner 离线/过期直接阻断执行，必须由后端受控启动脚本完成启动、注册、能力校验和失败原因返回；受控启动脚本不得把前端入口 HTTP 可达性作为启动前硬阻断，前端不可达应由具体真实页面任务在执行阶段暴露。Windows timeout/cancel 必须有独立的 child 收敛超时，不能把 `close` 事件必然触发作为前提。普通只读页面冒烟测试项必须默认使用短预算、中等推理、`--ignore-rules` 和最短 Playwright 路径 prompt，避免全局高推理配置或编码任务规则把页面冒烟核验拖到超时；`analysisMode=CODE_READONLY` 的代码分析测试项必须在测试项、执行快照、Runner claim 和 prompt 中显式透传，只允许只读扫描代码、路由、API、测试等证据，不得以浏览器作为优先路径。
- Blocker: 任一 Runner 或租户前置条件缺失、Runner token 与后端运行态或 tokenless 模式不一致、Runner 进程存在但注册失败或 heartbeat 超过后端超时阈值、测试项会写入生产/非任务租户、失败检查点没有差异描述、截图路径不在受控临时目录、并行执行包含 `parallelSafe=false` 项、执行中 heartbeat 超过后端超时阈值、Windows `codex.cmd` 后代进程在超时/取消后仍持有 `codex-test-result-*` 输出文件、进程树已消失但当前 Runner 会话仍持续上报 `currentRunningCount > 0`、只读项仍按长运行写入型预算或继承项目编码规则执行、`CODE_READONLY` 未透传到执行快照/Runner claim 或仍使用 Playwright 优先 prompt 时必须停止。
- Verification: 记录 Runner 注册/领取/执行期心跳/空闲心跳/回写命令、页面执行入口、租户/用户标签、检查点结果、失败截图 artifact、最终 UI 状态和必要的只读 API 核验；空闲场景至少等待一个 heartbeat 周期后复查 heartbeat age 仍小于超时阈值；Windows Runner 必须证明 timeout/cancel 后不存在本任务 `codex-test-result-*` 子进程，执行项不遗留 `CLAIMED/RUNNING`，并证明即使 child 未触发 `close`，有界等待结束后当前会话运行计数也回到 `0`；普通只读页面项还必须证明在只读预算内返回 JSON，且页面无写请求、无控制台错误；`CODE_READONLY` 项必须证明 `analysisMode` 保存、默认值、非法值拒绝、执行快照、Runner claim 和只读代码分析 prompt 均有静态或单元测试覆盖。
- Forbidden action: 禁止把 API-only、静态合同测试、mock 截图、默认成功、Runner 离线跳过、前端硬拦截 `没有在线 Codex Runner`、绕过后端 Runner 会话和结构化回写直接裸调用 `codex` CLI、只杀 `cmd.exe` 而不处理 `node/codex.exe` 后代进程、无限等待 child `close`、把普通只读页面项放任为仓库级编码任务探索、把 `CODE_READONLY` 代码分析伪装成 Playwright E2E 冒烟测试，或顺序执行降级当作真实 E2E 通过。
- Evidence: `doc/tasks/20260724-codex-test-management-delivery/verification-report.md`，2026-07-24 Codex 测试管理交付；`doc/tasks/20260725-codex-runner-void-test/verification-report.md`，2026-07-26 Runner 心跳、Windows 子进程树、取消处理修复；`doc/tasks/20260726-codex-runner-on-demand-wrapper/verification-report.md`，2026-07-26 按需 Runner 包装层；`doc/tasks/20260727-codex-runner-token-invalid/verification-report.md`，2026-07-28 只读 Runner 快速路径与真实测试管理自检 PASS；`doc/tasks/20260808-edhr-batch-record-test-tab/verification-report.md`，2026-08-08 `CODE_READONLY` 代码只读分析模式。

### Codex Runner CODE_READONLY 长任务与实时代码证据门禁

- Trigger: 测试管理行级“测试”显示 `timeout of 30000ms exceeded`、按需 Runner 已注册但启动接口不返回 executionId、长时间 Codex 任务因迟到 heartbeat 中断，或 Windows `read-only` sandbox 报 `apply deny-read ACLs` / 无法读取正式项目代码。
- Preflight check: 按需 Runner 可用性探测必须在调用方 `REPEATABLE_READ` 业务事务之外读取最新注册会话；heartbeat 入口必须将“已注册且未显式下线”的身份状态校验与 claim/status 使用的心跳新鲜度校验分离，允许迟到的有效 heartbeat 续租但不得放宽任务领取新鲜度；前端执行结果查询必须使用独立于普通 API 的长请求预算，并在非终态继续轮询。`CODE_READONLY` 必须使用低推理、原生 `read-only` sandbox、严格输出 Schema 和正式项目根；Runner 构造提示词所需的业务描述必须从 claim Task 的正式字段读取（当前为 `testDataText`），不得从 Checkpoint 猜测 `remark` 等未声明字段，修改提示词前必须同时核对响应 VO、任务快照映射和 Runner 读取点。若 Windows sandbox 无法执行 shell，只允许 Runner 从明确白名单的前端 `src`/E2E、后端模块 `src/main`/`src/test` 与受控 SQL 目录实时收集有界证据，按 View/API/Router/Controller/Service/DAL/测试/SQL 分类配额和业务行为别名截取片段，再交给 Codex 只做结构化判断。职责描述类 CODE_READONLY 提示词必须先拆解参与角色、页面入口/按钮、输入来源、关键对象、路由、API、权限、状态链路、结果去向和测试证据；PASS 只能用于每个关键义务都有实时证据支持，核心入口/API/状态链路/权限/测试证据缺失应返回 FAIL，只有无相关源码证据或描述无法拆解时才返回 BLOCKED。用户可见的 `summary`、`actualText`、`mismatchDescription` 必须先给一句业务结论，再用通俗话说明冲突在哪里、是否需要修改和应核对哪条业务流程；若证据属于相邻角色或另一条业务链，必须明确说明“测试拿错了链路”，不得用 API、Controller、Service、Mapper、SQL、字段名、枚举名、文件名或类名堆叠成技术审计报告。
- Blocker: Runner 注册提交后启动事务仍读取旧快照、heartbeat 自身因超过 freshness 阈值被拒绝、显式 `OFFLINE` 会话可被续租、结果查询仍继承 30 秒全局预算、Runner 读取 claim 协议未声明字段或正式 `testDataText` 未进入提示词、证据扫描进入整个后端根/`target`/依赖/任务记录、通用 `API` 等结构词耗尽证据配额、缺少匹配源码仍给 PASS、职责描述提示词允许相邻业务词/局部页面/部分链路冒充完整职责 PASS、用户可见回复只罗列技术对象而没有说明业务冲突和是否需要修改、Codex CLI 未返回严格结构化结果，或页面没有 executionId/真实回复时必须停止通过结论。
- Verification: 后端必须覆盖“调用方重复读事务可见新注册 Runner”“迟到 ONLINE heartbeat 可续租”“显式 OFFLINE heartbeat 被拒绝”；Runner 静态测试必须覆盖只读 sandbox、Schema、正式项目根、claim Task/Checkpoint 字段归属、证据白名单/数量/字节上限、分类配额、业务别名、职责描述拆解、PASS/FAIL/BLOCKED 门槛、局部证据不得通过，以及用户可见回复包含简明结论、业务冲突、是否需要修改并避免技术词堆叠；真实 Playwright 路径必须从页面点击行级“测试”，记录 executionId、最终状态、Codex CLI 实际回复和截图，并确认页面未出现通用 30 秒超时或 claim 字段缺失错误。任务结束前还要确认 Runner `currentRunningCount=0` 或已继续空闲 heartbeat。
- Forbidden action: 禁止把 `read-only` 失败静默降级为 `workspace-write`、bypass 或 API-only；禁止把任务文档、历史截图、生成目录、依赖或构建输出当作当前代码证据；禁止仅靠 prompt 要求 Codex 自行无界搜索；禁止为接受迟到 heartbeat 而放宽 claim/status 的新鲜度校验；禁止用 mock、默认成功或 Runner 进程存在冒充 Codex CLI 完成。
- Evidence: `doc/tasks/20260809-batch-record-test-codex-cli-response/verification-report.md`，2026-08-09 批记录测试启动事务、心跳续租、独立结果预算和 Windows 只读实时代码证据修复；`doc/tasks/20260811-batch-record-test-short-prompt/verification-report.md`，2026-08-11 claim 正式描述字段与短提示词真实页面回归。

1. 阶段 1：启动与事务。必查项：启动请求是否返回 executionId、Runner 注册提交时间与调用事务隔离；Fail Fast：注册成功但启动仍等待到全局超时；必须记录：请求耗时、Runner session 和事务回归测试。
2. 阶段 2：执行与心跳。必查项：领取、执行期 heartbeat、会话状态和 `currentRunningCount`；Fail Fast：迟到 heartbeat 中断任务或显式离线会话被恢复；必须记录：heartbeat age、终止原因和会话终态。
3. 阶段 3：只读证据。必查项：白名单根、分类配额、业务别名、文件/字节上限和严格 Schema；Fail Fast：进入无关大目录、证据缺层或 sandbox 权限被降级；必须记录：证据文件类别、CLI 退出状态和结构化回复。
4. 阶段 4：真实页面验收。必查项：行级点击、结果轮询、终态 UI 与回复文本；Fail Fast：API-only、通用 30 秒超时、没有 executionId 或只展示占位回复；必须记录：executionId、截图和页面可见终态。

### Codex Runner 运行态重启与 CLI 自检门禁

- Trigger: 测试管理批次执行期间本机后端重启、Runner 会话变为 `STALE`、批次长期停留 `RUNNING`，或 Runner 已 `ONLINE` 但测试项在启动 Codex 后立即 `exit 1` / 达到 `600000ms` 超时。
- Preflight check: 发起正式节点串前除注册、heartbeat 和能力字段外，还必须执行一个受控、短预算、无业务写入的 Codex CLI 自检，确认当前 provider、认证方式、插件目录同步和 feature 配置能够返回结构化结果；本机后端重启后必须先检查现有活动批次、执行项和旧 Runner session，任务自有悬挂批次应从真实 `测试记录` 页面取消并核对终态，再允许新建批次。
- Blocker: Runner 进程存在但 session `STALE`、heartbeat age 达到超时阈值、执行项仍遗留 `CLAIMED/RUNNING`、页面启动请求未返回 executionId、Codex CLI 自检 `exit 1`、远程插件认证方式不匹配、未知 feature 配置导致启动失败，或短预算自检超时时必须停止正式长链路。
- Verification: 记录后端重启前后 PID/运行 Jar 归属、旧新 Runner session、heartbeat age、悬挂批次页面取消结果、活动批次数量、Codex CLI 自检退出码与结构化输出；正式批次终态后还要在 `测试记录` 页面核对结果，并确认 Runner `currentRunningCount=0`。
- Forbidden action: 禁止在旧批次仍 `RUNNING` 时继续叠加新节点串，禁止用新 Runner 进程存在替代旧执行收敛，禁止把插件认证警告或未知 feature key 静默忽略后继续长链路，禁止 API-only 取消任务自有悬挂批次。
- Evidence: `doc/tasks/20260730-test-management-serial-routes-verification/verification-report.md`，3 条正式节点串验证中识别到后端重启后的悬挂批次、Runner session 切换、Codex CLI `exit 1` 和 `600000ms` 超时。

### Codex CLI 上游错误与长提示词门禁

- Trigger: 后端业务接口调用 Codex CLI 返回超时、候选输出文件已生成但子进程未退出，或 CLI 日志出现 `502 Bad Gateway`、`Upstream request failed`。
- Preflight check: 真实业务 E2E 前先在同一运行环境执行短预算、只读、严格 JSON 输出的 Codex CLI 自检；再用目标模板的实际输入文件复验，并记录 CLI 入口、模型配置、输出文件状态和 provider 响应。Windows 下优先直接启动 PATH 中的 `node.exe` 与 Codex `codex.js`，避免 `cmd.exe` 包装进程影响收敛。
- Blocker: 短自检或目标输入返回 provider 5xx、长时间无结构化输出、超时后子进程仍持有候选文件，或无法区分 CLI 未启动与 provider 失败时必须停止真实业务 E2E，不能把页面 HTTP 200 或候选文件存在当作通过。
- Verification: 记录后端接口耗时和业务错误码、CLI stderr 的 provider 状态、子进程退出码/信号、候选文件字节数、运行 Jar 与 PID；provider 恢复后使用同一真实 Playwright 路径复跑并核对返回的业务结果。
- Forbidden action: 禁止仅延长请求超时、吞掉 provider 5xx、把已有候选文件直接视为成功、切换到 mock/API-only 数据，或在上游未恢复时伪造草稿版本成功。
- Evidence: `doc/tasks/20260825-ai-autodetect-auto-draft-version/verification-report.md`，AI 填写规则真实页面登录与入口通过，但 Codex 直接诊断在目标模板输入下返回上游 `502 Bad Gateway`，后端按超时失败。

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

- Trigger: 新增或修改 `系统管理 > 测试管理` 的自然语言测试项，或修改“批记录测试”等业务页面中的测试任务页签、固定测试项、标题和说明，尤其是按业务系统节点拆分、会新建/修改/删除/作废业务数据的测试项。
- Preflight check: 每个测试节点必须写清业务节点、固定样本或任务自有测试标识、前置复位、页面操作、页面可见验证、清理/恢复方式；测试方法、测试目标、业务页签列名、固定项标题、说明和测试项名称必须面向业务测试人员，避免只写接口、内部字段、状态码、hash、程序组件、测试工具、英文内部状态或代码视角。页面固定项会按测试项名称加载持久化说明时，业务化改名必须同步测试项名称或执行正式内容迁移，防止旧技术说明重新覆盖新默认文案。
- Blocker: 测试项只创建不清理、只删除不先准备样本、失败后下次运行会被残留数据阻塞、没有固定样本或任务自有标识、目标只能由程序员判断、业务页签仍出现程序术语、旧测试项名称仍会加载技术化持久说明，或需要测试人员在测试说明之外手工猜测清理方式时必须停止。
- Verification: 证据需包含节点数量、每节点方法项数量、每节点目标项数量、固定样本/清理/恢复闭环核验、内部词扫描结果，以及写入租户和项目范围；业务页签固定列表还必须用明确数据块边界正向锁定业务列名和关键业务口径，负向扫描标题、说明、测试范围和测试项名称中的程序术语，并运行相邻列表合同。
- Verification: 测试历史入口、历史页签或结果按钮的颜色必须读取对应测试项的正式执行状态；`PASS` 使用成功色，`FAIL/BLOCKED/TIMEOUT` 使用失败色，无终态结果保持中性并禁用。不得只按“已有历史”或 `ready=true` 把所有终态结果统一显示为绿色。
- Forbidden action: 禁止用 API-only 清理、生产或 admin 基线数据、隐藏脚本状态、程序员专用字段、CSS 隐藏、只改默认说明但保留会回载旧说明的测试项名称、一次性人工清库、或“执行失败后手工处理”替代测试节点自身闭环。
- Evidence: `doc/tasks/20260727-batch-record-test-node-items/verification-report.md`，2026-07-27 批记录 6 个节点闭环测试项；`doc/tasks/20260809-batch-record-mapping-business-copy/verification-report.md`，批记录映射页签将列名、固定项标题、说明、测试范围和测试项名称统一改为业务语言，并用数据块程序术语负向扫描锁定；`doc/tasks/20260809-batch-record-test-history-result-color/verification-report.md`，逐行历史按钮按正式结果状态显示成功绿色、失败红色。
### 写入型 E2E 异常路径任务数据清理门禁

- Trigger: Playwright 通过真实页面新增、绑定、启用或提交任务自有数据，后续修改、断言、截图、网络检查或删除步骤仍可能失败。
- Preflight check: 脚本必须在每个写响应 `code=0` 后立即更新机器可读数据状态，不得等列表刷新或后续断言完成；异常分支应通过同一真实页面路径停用、删除或恢复任务数据，并记录清理是否尝试、是否完成和是否仍有启用残留。
- Blocker: 写请求成功后脚本失败却只关闭浏览器、依赖成功路径末尾才清理、列表刷新失败导致脚本不知道写入已发生、或异常分支找不到任务数据时必须将精确残留标识和影响写入结果，不得宣称任务已清理。
- Verification: 真实通过路径最终状态必须是已停用、已删除或已恢复；脚本静态合同锁定写成功状态更新和异常清理调用，失败结果包含 `cleanup`、`cleanupError` 或等价字段及残留状态。
- Forbidden action: 禁止用 API-only、SQL、管理员或生产基线数据完成清理；禁止根据刷新前的陈旧 DOM 推断后端已停用；禁止吞掉清理失败或覆盖原始失败原因。
- Evidence: `doc/tasks/20260807-team-leader-loss-maintenance-dialog/`，损耗真实 E2E 在 POST/PUT/DELETE `code=0` 后单独记录任务数据状态，并在后续失败时通过同一维护弹框尝试停用。

### 真实页面配置行缺失的 E2E 阻断门禁

- 触发场景：真实页面已经打开新增配置入口并显示字段，但当前租户没有可编辑的正式配置行，无法证明行级输入、保存和边界行为。
- 经验规则：只读 E2E 可以证明菜单、页签、字段和新增入口可达；没有任务自有配置行时必须记录 `BLOCKED`，不得通过 API、SQL、mock 或共享管理员数据补行来升级结论。
- 验证方式：结果中记录租户/账号标签、配置列表真实返回的业务码和行数、字段/入口可见性、所有写请求数量；只有确认的可丢弃测试租户、账号和配置行完成真实页面创建后，才运行写入型边界 E2E。
- 禁止做法：禁止把“页面有字段”写成“配置保存已通过”，禁止把无数据的空表当作业务失败，禁止用 API-only 代替真实生产组长页签路径。

### 顶部固定信息栏真实视口边界门禁

- Trigger: 修改顶部订单摘要、工序、员工、全屏切换等横向固定信息栏的列宽、字号、换行或响应式布局。
- Preflight check: 目标视口宽度必须按真实页面内容区计算，包含左侧导航、页面内边距和滚动条占用；不能只根据组件自身设计宽度相加。至少选择一个带长订单号或长业务名称的正式样本，分别覆盖常用桌面宽度、较窄桌面宽度和业务全屏状态。
- Blocker: 顶部栏任一外边界越过 `viewportWidth`、相邻卡片边界相交、值节点越过父卡片、关键业务值使用省略号或 `nowrap` 隐藏，或只凭截图肉眼判断而没有 DOM 边界证据时必须停止并修复。
- Verification: Playwright 结果必须记录 `viewportWidth`、顶部栏和每张卡片的 `getBoundingClientRect()`、关键值与父卡片边界、`white-space`、`text-overflow` 和字号；同时保留每个目标视口截图。全屏状态必须重新采集边界，不能复用普通页面结果。
- Forbidden action: 禁止用扩大浏览器宽度、隐藏左侧导航、只验证组件内部无重叠、tooltip 或截图裁切来替代真实页面视口内完整可见。
- Evidence: `doc/tasks/20260807-frontline-pqc-order-product-summary/verification-report.md`，1440x900 首轮发现顶部栏右边界超过视口，收紧响应式最小列宽后以 DOM 边界和截图完成普通页面及 PQC 全屏复验。

### 一线 PQC 活跃订单路线产品项目上下文门禁

- Trigger: 一线 PQC 活跃订单列表可见目标产品订单，但选择订单后检验项目区为空、检验方法按钮不渲染，或页面提示 `routeProjectItems`、`missingItemIds`、PQC 任务身份不一致、设备账号上下文不完整。
- Preflight check: 活跃订单存在只证明候选入口，不证明该订单可执行 PQC。必须通过真实页面选择精确目标产品订单，等待 `/mes/pro/feedback/frontline/device-account/pqc/active-order/processes` 完成，核对业务码，并确认返回任务的 `activeOrderId`、`regulationVersionId`、`qaProcessId` 和规则身份完整；需要路线产品项目的场景还必须核对正式 `routeProjectItems` 已绑定到该 `routeId`。同产品存在多条任务自有订单时至少复验两条，区分单订单异常、共享路线绑定缺口和历史任务回填缺口。
- Blocker: 页面返回 `routeProjectItems routeId=<id>，missingItemIds=[...]`、PQC 任务身份不一致且 `regulationVersionId`/`qaProcessId` 为空、目标订单和路线不一致、工序接口 HTTP 200 但业务码非 0、页面业务上下文拒绝、或检验方法入口未渲染时必须记录 BLOCKED；HTTP 200、活跃订单数量、其它产品订单可用或静态弹窗合同均不能替代目标订单真实路径。
- Verification: 证据必须包含租户/账号标签、目标订单编码与产品名称、`activeOrderId`、`routeId`、`pqcTaskId`、`regulationVersionId`、`qaProcessId`、缺失项目 ID、工序请求 HTTP/业务码、页面错误文案、检验方法按钮/弹窗是否可见、目标业务写请求、`consoleErrors` 和 `pageErrors`。页面自动调用 `/pqc/switch-employee` 时，只有源码确认该服务不执行 Mapper/DAO 写入或事务持久化后，才可将其单独记录为上下文解析 POST；不得省略该请求或把它计成 PQC 正式提交。
- Forbidden action: 禁止用 API/SQL 临时补 `routeProjectItems`、前端直塞工序/检验项目、跨产品或跨路线借用其它订单、忽略页面业务错误、只看 HTTP 200，或把上下文解析 POST 冒充正式提交成功。
- Evidence: `doc/tasks/20260809-frontline-qa-inspection-detail-fields/verification-report.md`，QA 来源页真实通过后，一线 PQC 三个同产品订单均因路线 `980091` 缺产品项目 `14` 而被正式上下文拒绝；`doc/tasks/20260817-frontline-pqc-yudao-source-validation/verification-report.md`，订单可见但三条正式 PQC 任务因 `qaProcessId` 为空被工序读取 fail-fast 拒绝；`doc/tasks/20260817-frontline-pqc-historical-task-repair/verification-report.md`，历史缺 QA 工序身份任务在无提交事件、无 PQC 记录、无聚合明细且实际检验数量为 0 时，按受控取消与软删除恢复工序列表；若要恢复可提交任务，必须先修正锁定 QA 规程数量/比例规则，再按锁定版本生成任务，不得运行时推算。

## eDHR 本地状态样本操作审计追溯门禁

- Trigger: Playwright 验证本地状态样本、`LOCAL_STATE_SAMPLE_CREATE`、批次追溯操作审计、或只按 `batchExecutionId` 查询操作日志。
- Preflight check: 写入型 E2E 必须通过真实页面创建任务自有样本批次，并确认样本批次任务具备可用于批次追溯的对象级权限 scope（至少覆盖 `AUDIT_VIEW`）。
- Blocker: 如果操作审计行已创建，但追溯接口返回 `BATCH_EXECUTION:<id>` 对象级权限范围不存在或未启用，必须修复样本创建事务的权限 scope 绑定；不得用 SQL 补权限、API-only 或管理员绕过冒充通过。
- Verification: E2E 需断言 `/mes/pro/edhr-operation-audit/page` 请求包含 `batchExecutionId`，不包含 `objectType/objectId`，并在表格中看到目标 operationType、权限判定、结果状态和 audit hash。
- Forbidden action: 禁止只验证审计表落库而不验证批次追溯可见性；禁止把权限缺失解释为页面无数据；禁止记录登录密码。
- Evidence: `doc/tasks/20260724-batch-fda-audit-log-coverage/verification-report.md`。

## 逻辑删除表真实 E2E 断言门禁

- Trigger: 真实页面 E2E 验证删除、移除、撤销同步、解绑附件、作废文件、清理任务数据，且相关表使用 `deleted`、状态字段或类似软删除机制。
- Preflight check: 先确认被验证对象的正式删除语义是物理删除还是逻辑删除；逻辑删除表必须区分“有效记录不存在”和“历史行仍保留且 `deleted=1`/删除状态正确”。
- Blocker: E2E 只断言页面文案已删除但没有查有效记录；或者按物理行不存在断言逻辑删除表，导致脚本与正式数据保留策略冲突时必须修正测试，不得改业务代码去迎合错误断言。
- Verification: 成功路径至少证明页面状态、有效记录计数、逻辑删除标记或删除状态三者一致；任务自有数据清理后要只读确认无有效残留。
- Forbidden action: 禁止把逻辑删除历史行当成未清理残留，禁止为了通过 E2E 对正式审计/文件表做物理删除，禁止吞掉异常清理失败。
- Evidence: `doc/tasks/20260830-nas-original-path-sync/verification-report.md`。
