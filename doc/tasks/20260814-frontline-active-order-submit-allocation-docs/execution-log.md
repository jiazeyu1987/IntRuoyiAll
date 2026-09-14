# Execution Log

## User Intent

用户要求从现有文档和代码实际出发，判断当前流程是否符合：一线生产提交时选择活跃订单；提交后数量自动分配到该活跃订单；提交数量超过该活跃订单数量也允许提交；生产组长报工管理列表中该订单用红色标识；提交后生产组长仍可将数量重新分配给其它订单。随后先完成文档设计，写入 PRD、开发文档和测试文档。

## Skill Usage

- 使用 `product-requirements-docs`：输出 PRD 结构、业务规则、状态流转和验收标准。
- 使用 `system-design-docs`：输出后端、前端、数据、接口和错误状态设计。
- 使用 `bdd-tdd-acceptance-planner`：输出 BDD 场景、严格 TDD 顺序、E2E 路径和测试数据。

## BDD Scenarios

BDD: 一线选择活跃订单并自动分配 -> Given 一线生产已选择活跃订单 O1 且填写产出数量 Q When 员工电子签名并提交 Then 系统生成正式报工事件，并在同一正式链路中把 Q 初始分配到 O1。

BDD: 超过订单数量仍允许提交并红色标识 -> Given 活跃订单 O1 当前工序剩余 R 且 R 小于提交数量 Q When 一线生产提交 Q Then 提交成功，组长报工管理列表中该事件的 O1 显示红色超量标识。

BDD: 组长可重新分配到其它订单 -> Given 一线提交已把 Q 初始分配到 O1 When 生产组长打开报工管理并将部分或全部数量调整到 O2 Then 系统保存新的当前分配版本，保留调整审计，列表红色标识按新分配重新计算。

BDD: 非活跃订单不可作为分配目标 -> Given O9 未加入当前组长活跃订单池 When 一线提交或组长调整分配试图使用 O9 Then 系统拒绝并提示订单不在当前活跃订单池。

## Current Document Fit Analysis

- 不符合：旧 PRD 要求“手动分配和手动调整仍必须满足剩余数量校验”，并在边界场景中写明“活跃订单剩余数量不足以承接本次报工时阻塞确认”。这与新要求“超过活跃订单数量也允许提交并红色标识”冲突。
- 部分符合：旧 PRD 已明确活跃订单、FIFO、手动调整、组长报工确认和分配审计，但默认分配发生在组长确认阶段，不是一线提交后自动落初始分配。
- 不符合：旧测试计划把“FIFO 剩余不足阻塞”作为失败场景，新要求应改成“一线提交成功、初始超量分配红色标识、组长可调整”。
- 需要新增：选中活跃订单 ID 的正式持久化、初始分配模式、超量标识字段或可计算规则、初始分配与后续调整的审计区分。

## Current Code Fit Analysis

- 已具备：一线页面已经要求选择活跃订单后才能提交，并会用所选订单加载生产工序。
- 已具备：后端一线正式提交会校验所选活跃订单对应的工单、路线和工序，且不会按订单剩余量阻塞一线提交数量。
- 不符合：一线正式提交请求上下文没有 `activeOrderId`，只传 `workOrderId`，无法精确记录“用户本次选择的活跃订单”。
- 不符合：一线提交后没有在同一事务中创建初始报工分配记录，组长端只是打开分配弹窗时预填原订单。
- 不符合：组长分配保存逻辑会把超过订单工序剩余量的数量截断到剩余量，并留下未分配数量；新要求是允许初始分配超出并红色标识。
- 部分符合：组长报工管理列表已有“待调整”红色标签和分配弹窗，具备承接超量提示和后续调整的界面基础。

## TDD / Verification Evidence

- RED: 文档设计阶段不修改生产代码；将后续实现 RED 写入 `test-plan.md`，当前 RED 依据为文档和代码静态分析发现的缺口。
- STRUCTURE: 已按 PRD、开发文档、测试文档三类输出设计。

## Milestone Log

- in_progress: 2026-08-14 重启后确认 worktree `D:\IntRuoyiWorktree\20260814-frontline-active-order-submit-allocation` 和分支 `codex/20260814-frontline-active-order-submit-allocation` 存在，任务从文档设计进入实现验证阶段。
- in_progress: 开发计划交付技能初始化首次失败，原因是里程碑标题使用 `M1` 格式，未满足技能要求的 `里程碑 N：` 解析合同；已修正文档后重新初始化。
- completed: 读取项目任务与 UTF-8 规则、PRD/系统设计/BDD-TDD 技能规则。
- completed: 搜索旧任务文档，确认 20260731 生产组长工作台 PRD 与测试计划存在“剩余不足阻塞”冲突。
- completed: 阅读当前一线提交、组长分配和超量提示相关代码，确认现状部分具备入口但缺少正式初始分配与精确活跃订单 ID。
- completed: 写入 `prd.md`、`development-plan.md`、`test-plan.md`。
- completed: 完成 UTF-8 和结构验证，见 `verification-report.md`。

## Blockers

- 当前文档设计无阻塞。
- 后续代码实现阶段若缺任务自有测试账号、签名、活跃订单、组长权限或可清理测试数据，真实 E2E 必须 BLOCKED，不能用 mock 或 API-only 替代。

## P1 后端合同 RED（2026-08-14）

- RED-UNBLOCK: 按 `docs/worktree-memory.md` 的编译基线差异门禁，仅为验证临时同步主工作区 21 个缺失正式类和 `MesFrontlinePqcContextServiceImpl.java` 的 1 处构造参数修正；所有临时前置均经 SHA-256 或 Git blob 校验，未纳入本任务交付。
- RED: Java/Maven 定向测试已进入 Surefire；17 项测试中 5 项按预期业务原因失败、其余 12 项通过。失败精确对应缺 `activeOrderId`、授权未使用精确活跃订单、提交事务内未创建初始分配、超量仍被截断、分配快照缺超量状态。
- CLEANUP: RED 取证完成后已删除 21 个验证专用未跟踪前置文件，并恢复 1 处验证专用 tracked 基线差异；Git blob 复核与 `git status` 确认没有把其它任务基线留在当前功能分支。

- BDD: 精确选择活跃订单 -> Given 当前工单上下文中存在一线明确选择的活跃订单 When 一线提交报工 Then 后端必须接收必填 `activeOrderId`，并按 `activeOrderId + workOrderId + routeId + routeProcessId + processId` 精确校验，禁止仅按 `workOrderId` 推断。
- BDD: 提交事务内保存全量初始分配 -> Given 一线选择活跃订单 O1 且产出数量为 Q When 正式提交创建工序池事件 Then 同一提交事务在返回成功前必须把完整 Q 初始分配到 O1。
- BDD: 超过订单剩余量仍保留提交事实 -> Given O1 当前工序可承接数量小于 Q When 保存一线初始分配或组长显式调整分配 Then 系统不得用剩余量静默截断 Q，并在当前分配行暴露 `overageQuantity` 与 `needsAdjustment=true`。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\frontline-active-order-initial-allocation-static.spec.cjs` -> FAIL, 5/5 个合同断言按预期失败：缺必填 `activeOrderId`、仍按 `workOrderId` 授权、提交成功前未创建初始分配、分配服务仍用 `min` 截断超量、当前分配行未暴露订单级超量状态。
- BLOCKER: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesReportAllocationCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 在执行 MES 测试前被基线 DCC 编译失败阻断；直接运行 MES 模块也被多项基线缺类阻断。缺失类在 `E:\IntRuoyi` 是未跟踪文件，不属于本任务且未进入 `int_main` 提交，因此未复制或修改。此编译失败不作为 RED 证据，后续 GREEN/Maven 回归必须先取得这些正式基线文件。

## P2 后端实现 GREEN（2026-08-14）

- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\frontline-active-order-initial-allocation-static.spec.cjs` -> PASS，6/6；覆盖必填精确活跃订单、提交事务内初始分配、超量不截断、订单级超量快照和报工管理列表投影。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesFrontlineSubmitAuthorizationTest,MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProcessPoolSubmitEventServiceAdapterTest,MesReportAllocationCommandServiceTest,MesFrontlineInitialAllocationSchemaTest,ProcessPoolTimelineReportAllocationProjectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，50/50，0 failures，0 errors，0 skipped。
- IMPLEMENTED: 一线提交上下文新增必填 `activeOrderId`；授权按精确活跃订单和生产上下文校验；事件创建成功后、正式提交返回前，在同一事务内保存版本 1、`FRONTLINE_SELECTED`、完整提交数量的初始分配。
- IMPLEMENTED: 分配保存不再按订单工序容量静默截断；仍保留“分配总量不得超过本次报工池总量”的硬约束。当前分配快照与报工管理列表均返回 `overageQuantity/needsAdjustment`，缺正式订单工序计划快照时立即失败。
- IMPLEMENTED: 增加 `20260814_mes_frontline_selected_initial_allocation.sql`，正式允许初始分配没有组长复核记录，并声明分配模式合同。
- REVIEW: 首次独立复核的 47/47 未覆盖报工管理列表投影；主 Agent 发现后补 RED、实现列表正式投影并将验证扩展为 50/50。第二次独立复核确认 PASS，最终结论以 6/6 + 50/50 为准。
- CLEANUP: Maven 仅按编译基线差异门禁临时同步 21 个其它任务正式类并临时补齐 1 处并行构造参数；验证后 21 个文件全部删除、tracked 差异恢复，零残留。

## P3 前端合同 RED（2026-08-14）

- BDD: 一线精确提交所选活跃订单 -> Given 用户在一线生产页面明确选择活跃订单 O1 When 构造正式提交载荷 Then API 类型和 `processPoolContext` 必须携带 O1 的必填 `activeOrderId`。
- BDD: 报工管理按正式订单超量标红 -> Given 后端列表返回订单级 `overageQuantity/needsAdjustment` When 生产组长查看报工管理 Then 超量订单标签必须显示红色和超量数，页面不得用未分配量、订单总量或报工量猜测。
- BDD: 分配弹窗读取正式当前分配 -> Given 一线提交已保存版本 1 初始分配 When 生产组长打开分配弹窗 Then 页面直接展示后端当前分配，不得在后端无分配时由前端预填冒充已保存事实。
- RED: `node tests/e2e/frontline-production-active-order-submit-attribution-static.spec.cjs` -> FAIL，生产提交 API 和正式上下文缺少必填 `activeOrderId`。
- RED: `node tests/e2e/team-leader-report-overage-highlight-static.spec.cjs` -> FAIL，前端列表和当前分配类型未消费后端正式订单级超量字段，仍存在猜测和前端预填。

## P4 前端实现 GREEN（2026-08-14）

- IMPLEMENTED: 一线提交从当前选中的活跃订单读取并校验 `activeOrderId`，正式 `processPoolContext` 必填传入该 ID。
- IMPLEMENTED: 生产组长报工管理列表直接消费后端 `needsAdjustment/overageQuantity`；超量订单标签使用红色并显示超量数量；移除按未分配量或订单总量推断的旧逻辑。
- IMPLEMENTED: 分配弹窗仅展示后端当前快照，删除 `prefillSelectedOrderAllocation` 前端伪造初始分配；组长仍可切换到其它活跃订单或修改数量，保存后继续使用正式快照和列表刷新。
- GREEN: 7 个聚焦及相邻前端静态合同逐项执行并全部 PASS：活跃订单提交归属、超量标红、报工分配、清除分配、共享分配、零数量、正式提交。
- GREEN: `pnpm ts:check` -> PASS，退出码 0。worktree 初次执行因缺 `node_modules` fail fast；随后按锁文件执行 `pnpm install --frozen-lockfile`，锁文件和 package.json 无改动，再次类型检查通过。
- VERIFY: `git diff --check` -> PASS，仅有仓库既有 LF/CRLF 转换提示。

## P5 真实 E2E 前置核对（2026-08-14）

- RUNTIME: 已通过 `reserve-worktree-slot.ps1` 为当前 worktree 原子预留 `int_main` profile slot 18，前端 8099、后端 48099；两个端口均为空闲，未占用 `int_main` 的 8081/48081。
- PREREQ: 本机 Docker MySQL 23306 和 Redis 26379 正在监听。
- BLOCKER: 当前进程环境缺少写入型真实 E2E 所需的测试租户、测试账号密码、两张任务自有活跃订单、生产任务/路线/路线工序/工序、员工档案、设备、记录本、电子签名和生产组长身份等全部正式前置；现有 `team-leader-workbench-real-flow.e2e.js` 明确要求这些真实 ID，并禁止使用 `芋道源码/admin` 基线租户替代。
- IMPACT: 在取得上述已确认、可清理的本机测试数据前，不能执行“一线选 O1 超量提交 -> 组长列表 O1 红色 -> 组长改配 O2”的写入型 Playwright 路径，也不能把静态/API/单元测试冒充真实 E2E 成功。

## P4 独立复核修订（2026-08-14）

- REVIEW-FAIL: 首次独立复核发现列表 `reportAllocations`、当前快照 `snapshot.lines` 和 FIFO 预览 `preview.lines` 缺失时仍通过 `|| []` 静默降级为空分配，可能让正式投影错误时红色提示消失；P4 状态已回退为 `needs_revision`。
- RED: 扩展 `team-leader-report-overage-highlight-static.spec.cjs`，要求 `reportAllocations` 为必需字段并禁止四类空数组 fallback；执行后按预期失败在旧可选类型和 fallback。
- GREEN: 将报工列表分配类型改为必需字段，模板、列表汇总、当前快照和 FIFO 预览均直接消费正式数组；字段缺失时由类型或运行时错误进入现有可见错误处理，不再显示伪造的空分配。
- GREEN: 7 个聚焦及相邻静态合同重新逐项执行全部 PASS；`pnpm ts:check` 再次 PASS，退出码 0。

## P5 专用真实 E2E 执行合同（2026-08-14）

- REVIEW-GAP: 复核现有 `team-leader-workbench-real-flow.e2e.js` 后确认其仍是旧流程：一线提交阶段没有显式选择并断言 O1 的 `activeOrderId`，提交后由组长重新触发 FIFO 分配，也没有在组长改配前验证版本 1 初始分配和订单级红色状态；该脚本不能证明本任务业务闭环。
- BDD: 一线选单、自动初始分配、超量标红与组长改配 -> Given 任务自有 O1/O2、独立一线账号和生产组长账号，且 O1 正式计划数量小于提交数量 When 一线页面明确选择 O1 并正式提交，组长页面查看红色状态后将超量部分改配到 O2 Then 提交载荷必须携带 O1 `activeOrderId`，版本 1 为 `FRONTLINE_SELECTED` 全量初始分配，列表 O1 红色待调整，版本 2 为 O1/O2 手工分配且审计完整。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL，缺少本任务专用真实 E2E 脚本，旧脚本无法覆盖目标业务合同。
- IMPLEMENTED: 新增专用真实 Playwright 脚本，要求独立非 admin 一线/组长账号、任务自有 O1/O2、8099/48099 worktree 运行态和已确认清理方案；脚本通过真实登录页、一线活跃订单选择器、正式提交、组长报工列表、分配弹窗和最终只读审计完成验证。
- GREEN: `node --check tests/e2e/frontline-active-order-submit-allocation-real.e2e.js` -> PASS。
- GREEN: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS。
- BLOCKED-EVIDENCE: `node tests/e2e/frontline-active-order-submit-allocation-real.e2e.js` -> BLOCKED，专用脚本已生成脱敏前置清单；当前仍缺测试租户、两个独立账号、O1/O2 正式数据和清理确认，未启动服务、未写入数据库、未把前置不足记为 E2E PASS。

## P5 独立复核修订 BDD（2026-08-14）

- BDD: 精确访问令牌审计 -> Given 生产组长已通过真实登录页建立会话 When E2E 执行最终只读审计 Then 只能读取精确 `ACCESS_TOKEN` 存储键，禁止从任意名称含 token 的键猜测凭据。
- BDD: 异常路径仍完成任务数据清理 -> Given 外部编排已提供任务自有 fixture manifest 和清理执行器 When 真实路径成功或任一步骤抛错 Then `finally` 都必须调用外部清理，并且只有机器可读结果证明 `cleanupVerified=true`、`remainingTaskDataCount=0` 才能 PASS。
- BDD: 前置阻塞与业务失败分层 -> Given 服务、浏览器、登录、权限或任务数据任一前置缺失 When E2E 无法进入已确认业务断言 Then 结果必须为分类 `BLOCKED`；只有前置通过后的业务断言失败才为 `FAIL`。
- BDD: 当前 worktree 运行态和测试租户归属 -> Given 默认执行当前任务 worktree When E2E 校验 URL、端口监听、进程命令行、源码版本、工作树指纹、后端产物哈希和租户 Then 只允许 8099/48099 且归属当前 worktree，并要求租户 ID/名称同时命中显式测试白名单；8081/48081 只允许显式 `POST_MERGE_INT_MAIN` 模式。
- BDD: Java Long 身份全程精确 -> Given 活跃订单、工单、路线、工序、员工、事件和审计身份可能超过 `Number.MAX_SAFE_INTEGER` When E2E 读取环境、fixture、请求或响应 Then 所有 Long 类 ID 必须保持规范十进制字符串并以 BigInt-safe 方式比较，禁止 `Number` 转换。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL, 当前专用 E2E 首先缺少 `FAS_ALLOWED_TEST_TENANT_IDS` 显式测试租户 ID 白名单；扩展合同同时锁定精确 `ACCESS_TOKEN`、异常 `finally` 外部清理、BLOCKED 分类、运行态归属/版本证据和 Long ID 精度，失败来自当前实现缺失而非测试入口或脚本语法错误。

## P5 独立复核修订 RED 复现（2026-08-14）

- BDD: 当前 worktree 单一运行态 -> Given 本阶段验证发生在 `D:\IntRuoyiWorktree\20260814-frontline-active-order-submit-allocation` When 校验真实 E2E 运行态 Then 只接受前端 8099、后端 48099、固定本机测试租户 `122/测试租户` 及其机器可读归属证据；本阶段不接受 8081/48081 或 post-merge 模式。
- RED: `node IntRuoyiFronted\tests\e2e\frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL，退出码 1；首个预期失败为“最终只读审计必须精确读取 ACCESS_TOKEN”，当前实现仍通过遍历 localStorage 并模糊匹配任意 token 键取值。扩展静态合同后续断言同时覆盖实际 `finally` 清理、BLOCKED/FAIL 分层、8099/48099 与固定测试租户、Long ID 禁止转 `Number`，因此在修复前保持 RED。

## P5 独立复核修订 GREEN（2026-08-15）

- CONTRACT: 最终只读审计只读取精确 `localStorage.ACCESS_TOKEN`；已删除遍历并模糊匹配任意 token 键的逻辑，刷新令牌不能再被误用。
- CONTRACT: 外部 fixture/cleanup 编排成为必需执行合同。运行态、服务、fixture 或场景检查发生异常后仍由主执行路径 `finally` 调用清理；只有外部结果同时证明 `status=CLEAN`、`cleanupPerformed=true`、`cleanupVerified=true`、`remainingTaskDataCount=0` 且任务/运行/租户身份一致时才能写入 PASS。
- CONTRACT: 服务不可达、浏览器不可用、登录、权限、任务数据、运行态证据和清理前置均使用明确 `E2EBlockedError` 类别；业务路径中的正式载荷、响应、初始分配、红色状态、改配和审计断言继续抛普通断言错误并判为 FAIL，不再使用任意 `error.blocked` 属性降级。
- CONTRACT: 本阶段运行态固定为当前 worktree 的 8099/48099，并核验监听 PID、进程命令行、源码 revision/工作树指纹、后端运行产物路径和 SHA-256；租户必须同时精确匹配固定本机测试租户 `122/测试租户` 及显式 ID/名称白名单。本条取代上一段“允许 POST_MERGE_INT_MAIN”的旧描述，当前 P5 worktree 脚本不接受 8081/48081。
- CONTRACT: 活跃订单、工单、路线、路线工序、工序、员工、报工事件和审计中的 Long ID 全程按规范十进制字符串读取和 BigInt-safe 比较；提交/改配请求及 Playwright/fetch 响应使用保留 Long 原文的 JSON 解析，业务 ID 不再转 `Number`。
- GREEN: `node --check tests\e2e\frontline-active-order-submit-allocation-real.e2e.js` -> PASS，退出码 0。
- GREEN: `node tests\e2e\frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS，退出码 0；静态合同同时执行超出 `Number.MAX_SAFE_INTEGER` 的 Long ID 精度用例，以及 `E2EBlockedError -> BLOCKED`、普通业务断言 -> `FAIL` 的行为用例。
- REGRESSION: 8 个相关前端静态合同全部 PASS，`STATIC_TOTAL=8 / STATIC_FAILED=0`；覆盖活跃订单选择、提交归属、提交明细、组长分配/清除、超量标红、共享分配和本任务真实 E2E 合同。
- BLOCKED-PROBE: 在未提供任何 FAS 前置环境变量时执行专用真实 E2E，退出码 2，机器可读结果为 `BLOCKED/TASK_DATA_PREREQUISITE`，未启动前后端、未调用页面、未写入数据库；证明缺前置不会被记为 FAIL 或 PASS。
- VERIFY: 本次只修改专用真实 E2E、其静态合同和任务执行日志；未启动服务、未执行真实数据库写入、未修改产品代码、未修改 `task-state.json`、未提交或融合。

## P5 当前执行前置（2026-08-15）

- GREEN: experience-preflight -> PASS；已完整读取本 worktree 的任务、前端、E2E、数据库、登录、本机运行态、worktree、端口和 PowerShell 编码规则，并按 `docs/experience-index.md` 命中真实 E2E、Long ID、成对 worktree 运行态、异常清理和写入型任务自有数据门禁。
- TOOLING: `npx`、Node.js 与 pnpm 均存在；本轮只在 `IntRuoyiFronted/tests/e2e` 和当前任务目录内执行 P5，不修改 `task-state.json`，不提交、不融合、不推送。
- BDD: 配置失败后仍清理可识别数据 -> Given fixture manifest 与外部清理器已经提供任务、运行和租户身份 When 其它 FAS 配置缺失或 `collectConfig` 失败 Then 脚本仍必须在退出前调用 cleanup，并以机器可读结果证明 `CLEAN` 且残留为 0。
- BDD: E2E 证据递归脱敏 -> Given fixture、配置、编排结果或错误对象中出现嵌套敏感键/敏感值 When 写入 result/evidence Then manifest 必须拒绝敏感键，证据必须递归移除敏感字段和值，不能只删除顶层密码。
- BDD: 产品断言保持 FAIL -> Given 运行态、fixture、登录和权限前置均已通过 When 普通页面控件、提交载荷、红色状态、改配或审计断言失败 Then 结果必须是 FAIL，不得被前置包装器降级为 BLOCKED。
- BDD: Node 16+ Long ID 精确 -> Given Java Long ID 超过 `Number.MAX_SAFE_INTEGER` 且位于嵌套对象/数组 When 解析、比较和写证据 Then ID 保持规范十进制字符串，不依赖 Node 18 的全局 `fetch` 或 Node 16.6 才提供的 `Array.prototype.at`。
- BDD: PASS 与零写入阻塞证据 -> Given 无 FAS 前置或真实场景完成 When 写出机器可读结果 Then 无前置探针记录业务写请求 0、本轮任务数据残留 0；PASS 只能在 cleanup=`CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0` 时产生。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL，退出码 1；首个失败证明真实页面选单仍被不存在的 `blockedPhase` 前置包装器包裹，普通 UI 缺陷会被错误归类或直接触发引用错误。扩展合同还将继续覆盖配置失败清理、递归脱敏、PASS 清理硬门禁、零写请求/零残留和 Node 16+ Long ID 兼容。

## P5 任务自有真实数据补齐（2026-08-15）

- BDD: 任务自有账号与订单夹具 -> Given 固定本机测试租户 `122/测试租户` 中存在可复用的正式权限和工艺基线 When P5 编排器准备本轮数据 Then 必须新建带 `FAS-20260814-` 标识的一线账号、生产组长账号、权限角色、路线、人员范围、O1/O2 和签名授权，禁止修改或冒用其它任务账号和订单。
- BDD: O1 超量与 O2 承接能力 -> Given O1/O2 使用同一正式工序且独立 `activeOrderId` When 准备夹具 Then O1 计划数量必须小于提交数量，O2 计划数量必须足以承接调出数量，并按正式订单工序快照提供计划数量。
- BDD: 真实场景后零残留 -> Given 页面已产生报工、签名、工序池事件、数量片段、初始/手工分配、审计和登录令牌 When 外部编排执行 cleanup Then 必须按本轮 manifest 和事件身份精确删除任务数据，保留正式 schema 迁移，并返回 `CLEAN/cleanupVerified=true/remainingTaskDataCount=0`。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-fixture-static.spec.cjs` -> FAIL（预期），P5 仍缺少任务自有 fixture 外部编排器，无法从当前 BLOCKED 进入真实页面验证。

## P5 当前执行 GREEN 与真实前置结论（2026-08-15）

- IMPLEMENTED: 专用脚本在 `collectConfig` 前独立读取 fixture/cleanup 身份；配置解析失败或缺项时，只要清理身份可识别就执行外部 cleanup。行为合同以临时 fixture 和确定性清理器证明退出码 2、cleanup=`CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0`、业务写请求 0。
- IMPLEMENTED: 删除不存在的 `blockedPhase` 调用。一线页面选单、普通控件、正式载荷、列表红色、改配和审计断言均保持普通错误并判为 `FAIL`；只有服务、浏览器、登录重定向和明确权限重定向等正式前置使用 `E2EBlockedError`。
- IMPLEMENTED: fixture manifest 递归拒绝数组/对象内的敏感键；`result.json` 和 `evidence.md` 统一递归脱敏，同时移除敏感值在普通错误字符串中的副本。目标业务 POST 请求单独计数，PASS 前再次要求 cleanup=`CLEAN`、执行/核验为真且残留为 0。
- RED: 扩展递归脱敏行为用例后，敏感容器键下的嵌套值仍会在其它普通字符串中泄露，静态/行为合同退出码 1，失败原因与预期一致。
- GREEN: 递归敏感值收集增加敏感上下文传播，敏感容器的全部嵌套标量也进入副本脱敏；同一 P5 静态/行为合同复跑 PASS，退出码 0。
- IMPLEMENTED: Node `>=16` 路径移除 `Array.prototype.at(-1)` 和 Node 18 全局 `fetch` 依赖，改用核心 HTTP/HTTPS；超过 `Number.MAX_SAFE_INTEGER` 的嵌套 Java Long ID 行为合同继续保持精确十进制字符串。
- GREEN: `node --check tests\e2e\frontline-active-order-submit-allocation-real.e2e.js` -> PASS，退出码 0。
- GREEN: `node tests\e2e\frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS，退出码 0；包含配置失败仍清理、递归敏感键拒绝/脱敏、BLOCKED/FAIL 分层、Long ID 精度和 CLEAN 硬门禁行为测试。
- REGRESSION: 8 项相关静态合同逐项通过，`STATIC_TOTAL=8`、`STATIC_FAILED=0`、退出码 0。
- BLOCKED-PROBE: 清空全部 FAS 环境变量后执行 `node tests\e2e\frontline-active-order-submit-allocation-real.e2e.js` -> 退出码 2；结果为 `BLOCKED/TASK_DATA_PREREQUISITE`，精确列出 13 项必需 FAS 变量缺失及固定租户/manifest 校验缺项，业务写请求数 0，cleanup=`NOT_REQUIRED`、`cleanupVerified=true`、`remainingTaskDataCount=0`。含义仅为本轮未识别 fixture、未启动浏览器且未生成任务数据，不替代真实场景后的外部清理核验。
- PREREQ: 当前父进程中 13 项必需 `FAS_*` 全部 `MISSING`，未发现其它 `FAS_*`；没有可核验的 fixture manifest、运行态证据、外部清理器或测试凭据。
- RUNTIME: 端口注册表中当前 worktree 的 slot 18 为 active，配对端口为 8099/48099；实际探测两端口均 `NOT_LISTENING`。未启动服务，因为 fixture、测试账号和运行态证据前置均未齐备。
- P5-AC: AC1、AC2、AC4 至 AC8 均 `BLOCKED`；AC3 为 `PARTIAL`（8 项定向回归通过，阻塞探针写请求/残留均为 0，但真实场景清理与融合前证据未形成）。未用 mock、admin、API-only 或直接 SQL 替代真实 Playwright。
- SCOPE: 本轮未修改产品代码或 `task-state.json`，未启动服务，未提交、融合或推送，也未生成 `test-report.md`。
- PROCESS-CLEANUP: 一次静态行为复跑在 30 秒窗口内未返回，已仅停止该次命令的任务自有 Node/PowerShell 进程；随后同命令在 1 秒内退出 0，未遗留测试进程或服务进程。

## P5 fixture 与正式 schema GREEN（2026-08-15）

- RED: `node tests\e2e\frontline-active-order-submit-allocation-fixture-static.spec.cjs` -> FAIL，退出码 1，预期原因是 `fas_fixture_orchestrator.py` 尚不存在。
- IMPLEMENTED: 新增任务自有外部编排器，支持 `--self-test` 与 `prepare/verify/cleanup`；固定租户 `122/测试租户`，只创建独立非 admin 一线/组长账号、正式菜单权限、电子签名授权、路线/路线版本/工序/人员绑定和任务自有 O1/O2；O1 计划 6、小于提交量 10，O2 计划 20、足以承接改配。
- IMPLEMENTED: manifest 仅保存任务/运行/租户身份、公开账号名和精确行 ID，递归拒绝敏感键；cleanup 使用 manifest 与 scenario event 的精确 ID，覆盖登录令牌、签名、反馈、工序池、数量片段、初始/手工分配、审计和全部 fixture 行，并返回机器可读残留数。
- GREEN: `python -X utf8 ...\fas_fixture_orchestrator.py --self-test` -> PASS，退出码 0。
- GREEN: `node tests\e2e\frontline-active-order-submit-allocation-fixture-static.spec.cjs` -> PASS，退出码 0。
- RED: 本地测试库迁移前查询 -> `review_id IS_NULLABLE=NO`，`allocation_mode` 注释仅为 `FIFO/MANUAL`，不满足一线初始分配正式合同。
- BACKUP: 迁移前 `SHOW CREATE TABLE` 已保存为 `e2e-artifacts/schema-before-migration.sql`；正式迁移宿主机/容器副本 SHA-256 一致，回滚 DDL 及先决清理条件记录于 `database-schema-evidence.md`，未执行回滚。
- GREEN: 官方 `20260814_mes_frontline_selected_initial_allocation.sql` 在本地测试库执行退出码 0；复核为 `review_id NULL`、分配方式注释含 `FRONTLINE_SELECTED/FIFO/MANUAL/SYSTEM`，临时迁移过程残留 0；正式 schema 保持安装。
- GREEN: 数据库 schema validator self-test 与证据校验均 PASS，退出码 0。
- SCOPE: 未修改产品代码或 `task-state.json`，未提交、融合或推送。

## P5 fixture 独立复核修订与运行阻塞（2026-08-15）

- BDD: 一线正式权限完整 -> Given 任务自有一线账号访问真实一线生产页面并提交报工 When 编排正式角色权限 Then 必须同时绑定页面菜单 `900437` 与正式报工父级/查询/创建 `900120/5550/5551/5552`，不得用页面 query 权限替代报工 create/query。
- BDD: 本机默认口令与任务账号一致 -> Given 本机 E2E 默认口令只在运行时注入 When 创建任务账号 Then 仅复制固定测试租户 `admin` 的现有哈希，不输出哈希或明文，也不使用哈希不同的其它账号作为来源。
- BDD: 全链路精确清理 -> Given 登录、签名授权、生产提交和组长确认可能生成登录/操作日志、授权审计、提交复核及复核签名 When cleanup 执行 Then 必须按任务用户或事件精确 ID 覆盖 `system_login_log`、`system_operate_log`、授权审计、`mes_pro_process_pool_submission_review` 和 `review_signature_id`，残留计数包含这些表。
- BDD: prepare 拒绝叠加残留 -> Given 任一旧 `FAS-20260814-` fixture 或输出仍存在 When 新一轮 prepare 开始 Then 在首次 INSERT 前阻塞并返回残留数；verify 同时要求正式迁移已使 `review_id` 可空。
- RED: 扩展 `frontline-active-order-submit-allocation-fixture-static.spec.cjs` 后执行退出码 1，首个预期失败为编排器尚未覆盖 `system_login_log`；合同同时锁定正式权限、admin 哈希来源、schema nullable、既有残留阻塞、复核/签名/日志清理。
- GREEN: 补齐上述合同后，fixture `--self-test`、Python 编译检查和静态合同均 PASS；一线正式角色包含 `5100/900120/5550/5551/5552/900220/900437`。
- RED: 第一次真实 `prepare` 退出码 1，MySQL 明确拒绝向生成列 `mes_pro_route_version.active_unique_flag` 写值；显式事务整体回滚，随后按任务 creator 核验残留 0。
- GREEN: 删除生成列显式写入后，`prepare -> verify -> cleanup` 全部退出码 0；cleanup=`CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0`。在 fixture 存在时第二次 `prepare` 按预期退出码 1 并报告残留 36，证明不会静默叠加新批次。
- GREEN: 官方 schema 迁移保持安装；verify 明确查询 `information_schema.columns` 并要求 `review_id IS_NULLABLE=YES`。
- BLOCKER: `mvn.cmd -pl yudao-server -am -DskipTests package` -> FAIL，退出码 1；当前 worktree 的 `yudao-module-dcc` 引用了但缺少 `DccProjectCodeAssignmentCandidatePageReqVO` 与 `DccProjectCodeAssignmentCandidateRespVO` 两个产品源码类，共 15 个编译错误，MES 和 `yudao-server` 被跳过，未生成 `yudao-server-exec.jar`。该缺陷位于 P5 owned paths 外，按范围约束未修改。
- IMPACT: 无当前 worktree 后端产物，不能启动 48099，也不能生成可信运行态证据；8099/48099 均未监听。因此真实浏览器业务路径未开始，状态为环境/源码前置 `BLOCKED`，不是业务断言 `FAIL`。
- BLOCKED-EVIDENCE: 注入固定租户、任务 manifest、任务账号名及仅存在于进程内的本机默认口令后执行专用 E2E -> 退出码 2，`BLOCKED/RUNTIME_EVIDENCE_PREREQUISITE`；实际目标业务写请求 0，finally 外部 cleanup=`CLEAN`、`cleanupPerformed=true`、`cleanupVerified=true`、`remainingTaskDataCount=0`。
- BLOCKED-PROBE: 无 `FAS_*` 环境探针再次退出码 2，`BLOCKED/TASK_DATA_PREREQUISITE`，写请求 0、cleanup=`NOT_REQUIRED`、残留 0；`FAS_ENV_COUNT=0`，未输出任何敏感值。
- REGRESSION: 8 项相关静态合同全部 PASS，`STATIC_TOTAL=8`、`STATIC_FAILED=0`；fixture 静态合同另行 PASS。最终再次执行 cleanup 返回 `CLEAN/0` 且删除行数 0，证明无任务数据残留。
- P5-AC: AC4=`PASS`（任务自有 O1/O2、权限、签名、路线与 O1 超量条件已独立 verify）；AC3=`PARTIAL`（定向回归和清理为真，缺真实场景/融合前证据）；AC1、AC2、AC5、AC6、AC7、AC8=`BLOCKED`（当前 worktree 后端无法构建，真实页面未执行）。
- SCOPE: 未修改 `task-state.json` 或 owned paths 外产品代码；未启动服务，未提交、融合或推送，未写 `test-report.md`。

## P5 真实 E2E、独立复核与融合前收口（2026-08-15）

- BDD: 一线选择订单后全量初始分配 -> Given 一线账号选择计划量为 6 的活跃订单 O1 When 通过真实页面提交数量 10 Then 提交成功且版本 1 以 `FRONTLINE_SELECTED` 将 10 全量分配到 O1，允许超量 4。
- BDD: 生产组长识别并调整超量订单 -> Given O1 初始分配 10 且正式超量为 4 When 生产组长打开真实报工管理列表并把数量改配为 O1=6、O2=4 Then O1 初始标签为红色并显示“待调整 4”，保存后版本 2 为 `MANUAL`、总量仍为 10、两订单超量为 0 且红色消失。
- BDD: 状态变更后使用正式列表结果验收 -> Given 组长保存了新的订单分配 When 页面刷新报工列表 Then E2E 必须等待并核对本次保存后的 `/submission/page` 正式响应与 DOM，不能只以弹窗关闭或成功提示证明状态已刷新。
- BDD: 全链路真实清理 -> Given 页面已产生提交、初始分配、手工分配、复核、签名和审计 When E2E `finally` 与独立二次 cleanup 执行 Then 两次都必须返回 `CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0` 才能放行。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-fixture-static.spec.cjs` -> FAIL, 一线与组长任务角色缺少全局审批角标查询权限 `1221`，真实页面会产生非目标权限控制台错误。
- GREEN: `node tests/e2e/frontline-active-order-submit-allocation-fixture-static.spec.cjs` -> PASS；fixture 为两类任务角色补齐 `1221`，同时保留一线正式报工与页面权限。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL, 组长保存后只断言弹窗与旧列表状态，尚未等待正式 `/submission/page` 响应和红色标识消失。
- GREEN: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS；新增保存后列表响应、O1/O2 精确行、无超量和红色标签隐藏断言。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL, 网络失败证据没有 URL 和目标请求分层，无法证明异常是否属于本任务业务链路。
- GREEN: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS；证据记录失败 URL，目标 MES 请求失败单独归类，普通业务错误仍保持 FAIL。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL, 外部资源超时与控制台错误尚无精确关联分类合同，存在宽泛忽略或误报业务失败的风险。
- GREEN: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS；仅对与第三方请求超时精确关联的通用控制台超时分类，未关联超时和业务控制台错误继续失败。
- GREEN: `node --check tests/e2e/frontline-active-order-submit-allocation-real.e2e.js` -> PASS。
- GREEN: 8 项前端定向静态回归 -> PASS，`STATIC_TOTAL=8 / STATIC_FAILED=0`；fixture self-test、fixture 静态合同、harness 行为合同和证据脱敏扫描同时通过。
- GREEN: 主 Agent 专用真实 Playwright E2E -> PASS，事件 227；O1 计划 6、提交 10、初始超量 4 并红色，组长调整为 O1=6/O2=4 后总量 10、超量 0、红色消失，审计 3 条，目标写请求 2，目标页面/请求/HTTP/控制台错误 0。
- GREEN: 独立 tester 专用真实 Playwright E2E -> PASS，事件 228；重新准备任务自有 fixture 并复跑完整页面路径，P5-AC1 至 P5-AC8 按融合前口径全部通过。
- CLEANUP: 主 E2E `finally` 删除 67 行并返回 `CLEAN/0`；独立复核 E2E 同样返回 `CLEAN/0`，其后二次精确 cleanup 删除 0 行且残留仍为 0。
- RUNTIME: 后端 PID 36136、前端 PID 4880 及其任务自有包装/构建进程已停止；8099/48099 均无监听。
- OVERLAY-RESTORE: 临时运行覆盖清单共 36 项；13 个原有文件逐项恢复并匹配 `originalSha256`，23 个覆盖新增文件逐项删除并确认不存在，验证错误数 0。运行覆盖只用于证明当前 dirty 基线可构建和可执行，未纳入任务产品交付。
- STATUS: 实现、真实 E2E、独立测试和零残留清理均已通过，任务进入 `ready_for_closeout`；剩余步骤为任务临时产物清理、任务分支提交、融合 `int_main` 和融合后核验。
- EXPERIENCE: 按 `project-experience-consolidation` 将本任务可复用门禁合并到既有 `docs/e2e-rules.md`、`docs/worktree-memory.md` 和 `docs/experience-index.md`：写入型 fixture 需包含全局壳层只读权限；状态变更后必须等待正式列表响应；第三方超时仅可按精确关联分类；临时运行覆盖必须以原始/覆盖哈希清单恢复。未新建长期经验文档。

## P5 融合 int_main 与融合后验证（2026-08-15）

- CLEANUP: `task_closeout.py --mode preview --worktree-closeout off` 返回 `status=ready`、`blocked=[]`，保留 14 项正式任务文档/证据并识别 38 项临时产物；`--mode apply` 已删除全部候选文件，但脚本在清理空目录时触发 Windows `FileNotFoundError`。再次 preview 返回 `delete=[]`、`blocked=[]`，随后精确删除剩余 13 个临时覆盖备份文件；正式保留项完整，临时文件残留 0。该脚本空目录竞态不影响文件清理结论。
- GREEN: 清理后 9 项前端静态合同 -> PASS，`STATIC_TOTAL=9 / STATIC_FAILED=0`；后端 Node 静态合同 8/8、fixture self-test、E2E `node --check` 与 `git diff --check` 均通过。
- GREEN: 清理后 `pnpm ts:check` -> PASS。
- GIT: 任务实现与证据提交为 `dd446b06f`；为保留 `int_main` dirty 工作区的并发修改，先将本任务 3 处长期经验增量及任务目录精确提交为 `a740592fb`，再把当前 `int_main` 合入任务分支，产生融合提交 `25d1654e5`。
- GIT: 融合前通过 merge-base 增量门禁确认任务分支相对 `int_main` 的实际增量为 44 个产品/测试文件，与主工作区 11614 个 tracked/untracked dirty 路径交集为 0；`branch-runtime-port-guard.ps1` 在任务分支和主分支均通过。
- GREEN: `git merge --ff-only codex/20260814-frontline-active-order-submit-allocation` -> PASS；`int_main` 从 `a740592fb` 快进到 `25d1654e5`，未覆盖、暂存或提交任何无关 dirty 改动，未推送。
- GREEN: 融合后 `mvn.cmd -pl yudao-module-mes "-Dtest=MesFrontlineActiveOrderInitialAllocationContractTest,MesFrontlineSubmitAuthorizationTest,MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProcessPoolSubmitEventServiceAdapterTest,MesReportAllocationCommandServiceTest,MesFrontlineInitialAllocationSchemaTest,ProcessPoolTimelineReportAllocationProjectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，50/50，0 failures，0 errors，0 skipped。
- GREEN: 融合后 9 项前端静态合同、后端 Node 静态合同 8/8 与 `pnpm ts:check` -> PASS。
- GREEN: 融合后 `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS；30 个 reactor 模块全部 SUCCESS，生成 `IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`。
- GREEN: `check_plan_completion.py --apply` -> PASS，`complete=true`；P1-P5 和全部验收标准均为 `completed`，`test_status=passed`，无阻塞前置。
- FINAL: 本任务业务实现、真实页面验收、独立测试、数据清理、长期经验沉淀、融合与融合后回归全部完成。

## 工作树与端口资源收尾（2026-08-15）

- WORKTREE: `git worktree remove` 已先移除任务工作树的 Git 登记；Windows 首次返回“Directory not empty”，安全复核确认 `.git` 已不存在、任务进程为 0、8099/48099 无监听后，仅清理该任务路径内的残留文件和空目录。
- GREEN: 任务工作树物理路径不存在，`git worktree list --porcelain` 无该登记，8099/48099 无监听；端口注册表中 slot 18 已标记 `active=false` 并记录释放时间，全部 active slot/前后端端口唯一性校验通过。

## 融合后机器状态路径复核（2026-08-15）

- RED: `check_plan_completion.py --cwd E:\IntRuoyi --task-dir E:\IntRuoyi\doc\tasks\20260814-frontline-active-order-submit-allocation-docs` -> FAIL，机器状态中的 artifact 路径仍指向已删除的任务 worktree，因此误报 `Missing artifact: execution_log/test_report`。
- FIX: 将 `workspace_root`、`tasks_root`、`task_dir` 及全部 artifact 路径更新为融合后的 `E:\IntRuoyi` 正式位置；不改变业务实现、测试结果或原任务分支来源记录。
- GREEN: 同一 `check_plan_completion.py` 命令复跑 -> PASS，退出码 0，返回 `complete=true`；融合后机器状态和正式任务文档位置一致。

## P6 融合后 int_main 真实 E2E 纠偏启动（2026-08-15）

- AUDIT-FAIL: `check_plan_completion.py` 虽返回 `complete=true`，但完成审计发现现有事件 227/228 均来自已删除 worktree 的 8099/48099；融合后记录只包含后端/前端静态与构建回归，没有在 `E:\IntRuoyi` 的 8081/48081 重跑真实页面 E2E，原“融合后真实 E2E 已完成”结论证据不足。
- CORRECTIVE-PLAN: 按 `development-plan-supervisor` 完成审计规则新增 P6，只补齐融合后主线运行态真实页面验证，不重写已完成 P1-P5 历史。
- BDD: 融合后主线真实页面闭环 -> Given 当前 `int_main` 已含功能提交且运行态固定为 8081/48081 When 本轮 fixture 创建任务自有 O1/O2，一线真实选择 O1 提交 10，组长查看红色待调整 4 并改配 O1=6/O2=4 Then 必须形成新的融合后事件、两个正式分配版本、完整审计、零目标错误和 CLEAN/0 清理证据。
- BDD: 验证链路不复用旧工作树 -> Given 旧事件 227/228 来自 8099/48099 When 本轮验收 Then 旧端口、旧事件、旧截图和静态合同均不得替代 8081/48081 的真实页面证据。
- STATUS: 任务重新置为 `in_progress`；P6 完成并经独立测试前不得恢复 `completed`。

## P6 融合后 int_main 真实 E2E 执行（2026-08-15）

- GREEN: experience-preflight -> PASS；已完整读取 `AGENTS.md`、任务收尾、本机运行态、E2E、登录、数据库、后端、前端、PowerShell 编码/内存、分支端口、worktree 规则及 `docs/experience-index.md` 命中的主工作区端口归属、真实写入型 E2E、任务自有数据和异常清理门禁。
- GREEN: `git branch --show-current` 与 `git merge-base --is-ancestor <commit> HEAD` -> PASS；当前分支为 `int_main`，`dd446b06f`、`4ce3637a4`、`b35c4e29c`、`d67c40c26` 均为当前 `HEAD` 祖先。
- GREEN: 端口、进程与健康检查 -> PASS；8081 由 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite 进程监听，48081 由携带 `--yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend` 的 Java 进程监听，后端健康状态 `UP`、前端入口 HTTP 200。
- BDD: 融合后运行模式必须显式绑定主工作区 -> Given 当前真实 E2E 脚本仅允许 `WORKTREE` 及 8099/48099 When 以 P6 要求的 `POST_MERGE_INT_MAIN`、8081/48081 执行 Then 配置合同必须先 RED，随后最小修复应只允许该显式模式绑定 `E:\IntRuoyi`，并继续保留运行态归属、源码指纹、后端产物哈希、业务断言和清理门禁。
- RED: `node -e \"const fs=require('node:fs');const s=fs.readFileSync('IntRuoyiFronted/tests/e2e/frontline-active-order-submit-allocation-real.e2e.js','utf8');if(!s.includes('POST_MERGE_INT_MAIN')||!s.includes('8081')||!s.includes('48081'))process.exit(1)\"` -> FAIL，符合预期：脚本仍把 `FAS_RUNTIME_MODE` 限制为 `WORKTREE`，且只接受旧 worktree 的 8099/48099，不能产生 P6 融合后证据。
- FIX: 将真实 E2E 运行态改为显式 profile：保留 `WORKTREE` 的 8099/48099，同时新增 `POST_MERGE_INT_MAIN`，且只允许 `E:\IntRuoyi` 的 8081/48081；运行态证据 mode、工作区、监听 PID、源码指纹、后端产物路径和 SHA-256 继续按所选 profile 严格核验。融合后证据写入独立 `e2e-artifacts/post-merge-int-main/`，不覆盖旧事件 227/228 的历史文件。
- GREEN: `node IntRuoyiFronted/tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS；配套合同已覆盖融合后模式精确工作区/端口绑定、未知模式 fail fast，并保留原业务断言、Long ID、错误分类和清理门禁。
- GREEN: `node -e \"const fs=require('node:fs');const s=fs.readFileSync('IntRuoyiFronted/tests/e2e/frontline-active-order-submit-allocation-real.e2e.js','utf8');if(!s.includes('POST_MERGE_INT_MAIN')||!s.includes('8081')||!s.includes('48081'))process.exit(1)\"` -> PASS。
- RUNTIME-CHECK: 初检 48081 为 `E:\IntRuoyi` 的 PID 28436，但其运行 Jar 内嵌 MES 模块缺少 `createInitialAllocation`，不能作为本功能融合后证据；未把仅 health=`UP` 的旧运行态冒充验收通过。
- RUNTIME-CONCURRENCY: 准备 fixture 后，48081 被同一主工作区并行任务切换为 PID 20372。按主工作区并发重启所有权门禁，本任务未停止或重启该并行任务运行态；只读核验确认新 PID 的 repo-root 仍为 `E:\IntRuoyi\IntRuoyiBackend`，运行 Jar 内嵌 MES 服务与分配命令类均包含 `createInitialAllocation/getActiveOrderId`，后端 health=`UP`。8081 保持 PID 37616，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`，入口 HTTP 200。
- FIXTURE: `python -X utf8 fas_fixture_orchestrator.py prepare --manifest <post-merge-int-main/fixture-manifest.json> --result <post-merge-int-main/prepare-result.json>` -> PREPARED；runId=`FAS-20260814-20260815043616-3384`，tenant=`122/测试租户`，O1 计划量 6、O2 计划量 20、提交量 10，账号、角色、菜单权限、路线、人员绑定和电子签名均为本轮任务自有数据。
- RUNTIME-EVIDENCE: `POST_MERGE_INT_MAIN` 固定绑定 `E:\IntRuoyi`、8081/48081；E2E 写入前核验前端 PID 37616、后端 PID 20372、source revision=`90fb1af111e577431522a43f0d505ddfb7d8250d`、源码指纹 SHA-256=`39fedee445d1c8c77ab4d9df85dc59e1985f537e958f68c77e1232961aeb649c`、实际后端产物 SHA-256=`a9560d4a9b1bf109b7fe9e172f0a274055d71341c401a0928b1545a26cf7d702`。
- GREEN: 在敏感登录/签名值仅从本机受控配置读取且不输出、不写入证据的前提下，`FAS_RUNTIME_MODE=POST_MERGE_INT_MAIN; FAS_FRONTEND_URL=http://127.0.0.1:8081; FAS_BACKEND_URL=http://127.0.0.1:48081; node tests/e2e/frontline-active-order-submit-allocation-real.e2e.js` -> PASS，退出码 0，新事件 ID=`229`。
- E2E: 一线账号经真实登录页登录并在页面选择 O1；O1 计划 6、提交 10；提交成功后版本 1 为 `FRONTLINE_SELECTED`、O1=10、超量/待调整 4。生产组长经真实登录页进入报工管理，列表 O1 标签为红色并显示待调整 4；通过真实分配弹窗保存 O1=6、O2=4 后，正式列表刷新且红色消失。版本 2 为 `MANUAL`，池总量/已分配总量均为 10、未分配 0；审计 3 条完整覆盖版本 1 初始基线、版本 2 O1/O2 手工改配。
- E2E-WRITES: 目标业务写请求共 2 次，且仅为一线提交 `/admin-api/mes/pro/feedback/frontline/submit` 与组长确认 `/admin-api/mes/pro/process-pool/team-leader/submission/allocation/confirm`。
- E2E-ERRORS: page errors=`0`、target request failures=`0`、target HTTP errors=`0`、target console errors=`0`；非目标页面切换/关闭引发的普通 GET `ERR_ABORTED` 只保留在原始诊断中，不计入目标错误且没有被静默吞掉。
- CLEANUP: E2E `finally` cleanup -> `CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0`、删除 75 行本轮任务数据；独立命令 `python -X utf8 fas_fixture_orchestrator.py cleanup --manifest <fixture-manifest.json> --scenario-state <scenario-state.json> --result <secondary-cleanup-result.json>` -> 二次 `CLEAN/0`。
- GREEN: P6 独立证据合同 -> PASS；事件 229、8081/48081、O1=6/O2=4、版本 2、总量 10、未分配 0、审计 3、写请求 2、四类目标错误 0/0/0/0、finally CLEAN/0、二次 CLEAN/0 和两张非空页面截图全部通过机器断言。
- EVIDENCE: `e2e-artifacts/post-merge-int-main/result.json`、`evidence.md`、`runtime-evidence.json`、`fixture-manifest.json`、`prepare-result.json`、`scenario-state.json`、`fixture-verification.json`、`cleanup-result.json`、`secondary-cleanup-result.json`、`initial-overage-red.png`、`after-manual-reallocation.png`。旧根目录事件 227/228 证据未覆盖，仅保留为融合前历史。
- RUNTIME-KEEP: 8081/48081 均为本任务开始前或执行中由同一主工作区并行任务持有的共享运行态；按所有权规则保留，未由本任务停止。最终复核时仍需确认 PID/health 未变化。
- FINAL-CHECK: 8081 仍为 PID 37616、HTTP 200，48081 仍为 PID 20372、health=`UP`，两者归属 `E:\IntRuoyi`；P6 文档与 8 个 JSON 证据均通过 UTF-8/JSON 解析，页面截图为非空 PNG，任务相关差异 `git diff --check` 通过。
- STATUS: P6 executor 验证完成，融合后真实页面 E2E 为 PASS，任务数据 `CLEAN/0`，无业务阻塞；等待独立 tester 与主 Agent 更新机器状态，当前执行器未改写 `task-state.json` 或 `test-report.md`。

## P6 独立复核与收尾前门禁（2026-08-15）

- INDEPENDENT-TEST: 主线程未参与 E2E 脚本实现，在 executor 完成后独立复跑 `node --check`、真实 E2E 静态合同、fixture self-test、P6 结果机器断言、UTF-8/JSON 读取、分支/融合祖先关系和当前端口归属检查，全部 PASS；逐项 P6-AC1 至 P6-AC8 证据已写入 `test-report.md`。
- VISUAL: 独立查看两张页面截图；初始弹窗显示 O1 要生产 6、分配 10 和红色“待调整 4”，改配后页面显示“分配已保存”并返回报工管理列表。最终 O1/O2 数量、红色消失和版本审计同时由同轮 DOM 与结果 JSON 证明。
- BUG-VALIDATOR: `validate_bug_regression.py --self-test` 与 `--evidence ...\bug-regression-evidence.md` 均退出 0；修复证据包含 Bug、Expected、Reproduction、Root Cause、RED/GREEN、Verification、风险范围和 Blockers。核心结论已归档到本日志与 `verification-report.md`，临时 evidence 文件列入 cleanup candidates。
- EXPERIENCE: 按 `project-experience-consolidation` 将“融合后必须使用新事件、主端口、严格运行 profile 和独立证据目录，worktree 旧事件不能冒充融合后复跑；health UP 不能替代运行 Jar 关键能力核验”合并到既有 `docs/e2e-rules.md`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- STATE: `development-plan-supervisor` P6 已完成，P1-P6 全部为 `completed`、`test_status=passed`、无 blocking prerequisite；`task.md` 进入 `ready_for_closeout`，等待 task-closeout-cleanup preview/apply。

## P6 任务收尾（2026-08-15）

- CLOSEOUT-PREVIEW: `task_closeout.py --task-id 20260814-frontline-active-order-submit-allocation-docs --mode preview --workspace E:\IntRuoyi --worktree-closeout off --json` -> PASS，`status=ready`、`blocked=[]`、`warnings=[]`；25 项正式文档/证据保留，唯一删除项为本任务临时 `bug-regression-evidence.md`。
- CLOSEOUT-APPLY: 同一命令使用 `--mode apply` -> PASS，`status=applied`；仅删除预览确认的临时回归记录，融合后 `post-merge-int-main` 目录 11 个正式证据文件及全部核心任务文档完整保留。
- RUNTIME-KEEP: `int_main` 的 8081/48081 为共享主工作区运行态，按项目并发所有权规则保留；本任务未停止或重启，不使用旧 worktree 端口作为证据。
- FINAL: 融合后真实页面 E2E 事件 229、业务改配、四类目标错误 0、两次 `CLEAN/0`、独立复核及收尾清理均已通过；任务状态更新为 `completed`，无阻塞，未推送远端。
- FINAL-GATE: 完成态再次执行 closeout preview -> `delete=[]`、`blocked=[]`、`warnings=[]`；`check_plan_completion.py --apply` -> PASS，`complete=true`。E2E 语法/静态合同与 fixture self-test 复跑均 PASS。

## P7 芋道源码 admin 补充真实 E2E 启动（2026-08-15）

- USER-INTENT: 用户明确要求使用“芋道源码/admin”在当前芋道源码租户执行真实页面 E2E；本轮使用 `int_main` 主运行态 8081/48081，不访问远端环境。
- PREFLIGHT: 当前分支为 `int_main`；8081 HTTP 200、48081 health=`UP`；`npx` 与 Node 可用。只读数据库核验确认租户 `1/芋道源码` 启用、admin 用户 ID 1 启用，且已有签名授权；凭据和密码指纹未输出。
- BDD: admin 补充真实页面闭环 -> Given `int_main` 主运行态与“芋道源码/admin”真实登录 When admin 在任务自有 O1/O2 上完成一线超量提交和组长改配 Then 必须得到 O1 计划 6、提交 10、红色待调整 4、O1=6/O2=4、版本 2、未分配 0 和完整审计。
- BDD: admin 基线不可修改 -> Given admin 是受保护基线账号 When 准备和清理任务 fixture Then admin 用户、密码指纹、角色集合和既有签名授权必须前后一致，不得新增、删除或修改基线配置。
- BDD: admin 模式不得替代独立账号验收 -> Given P5/P6 已由独立非 admin 一线/组长账号通过 When 新增 admin 模式 Then 原固定租户、账号隔离、端口、业务断言和清理合同必须保持 GREEN，admin 结果仅作为补充运行证据。
- BDD: admin 补充链路零目标错误和零业务残留 -> Given 页面只允许一线提交与组长确认两次目标业务写入 When 场景结束或失败 Then 四类目标错误为 0、finally 与二次 cleanup 均为 `CLEAN/0`、任务自有业务数据残留 0。
- RED-EXPECTED: 现有真实 E2E 固定拒绝 admin、固定租户 `122/测试租户` 且要求一线/组长账号不同；现有 fixture 也固定创建并删除两个临时用户，不能安全用于租户 1 的受保护 admin。本轮必须先以可执行合同取得预期 RED，再实现显式、隔离且不触碰 admin 基线的补充模式。
- STATUS: 任务重开为 `in_progress`；P7 完成真实 E2E、独立 tester、双重清理和基线不变核验前不得恢复 `completed`。
- BLOCKER: `docs/e2e-rules.md:293` 明确规定仅授权“芋道源码/admin”时写入型、签名及需清理的 E2E 必须 BLOCKED，且不得在 admin 基线租户创造测试写入数据；第 302、304 行进一步禁止只能使用默认 admin 时把写入型 E2E 判为通过。当前用户要求包含一线提交与组长确认两次正式写入，因此需要用户明确覆盖此安全门禁后才能继续。
- SAFE-STOP: 在识别门禁后已停止 P7 executor；未执行 fixture prepare，未产生一线提交或组长确认请求，未修改 admin 用户/密码/角色/签名授权，任务数据残留 0。
- REQUIRED-AUTHORITY: 若继续，授权必须明确限定为本机租户 `1/芋道源码`，允许通过 admin 真实页面创建并清理带本任务标识的 O1/O2、工序、路线和人员绑定以及两次目标业务写入；仍禁止修改 admin 用户、密码、角色、既有签名授权和任何无关正式业务数据。
- AUTHORIZATION: 用户已明确回复授权覆盖 admin-only 写入门禁，允许在本机租户 1 创建并清理本任务 O1/O2 等测试数据及两次业务写入；明确禁止修改 admin 用户、密码、角色、既有签名和无关数据。P7 仅按该最小边界解除阻塞。
- RESUME: fixture prepare 前必须记录 admin 用户、密码指纹、角色集合和既有签名授权指纹；任何目标写入前若保护指纹或页面前置不满足则继续 fail fast。任务状态恢复为 `in_progress`。
- RED: `node IntRuoyiFronted\\tests\\e2e\\frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL，第一个预期断言明确报告尚无 `ADMIN_TENANT1_INT_MAIN` 独立运行模式；后续合同同时锁定租户 `1/芋道源码`、`ADMIN_TENANT1` fixture 声明、admin 受保护基线指纹前后一致，并保留 P6 租户 122 合同。
- GREEN: 新增的 admin 模式静态合同、原 P6 合同、E2E Node 语法检查与 fixture `--self-test` 全部 PASS；admin 模式只允许 tenant `1/芋道源码`、同一 `admin` 账号和 8081/48081，原模式仍要求 tenant 122 及独立非 admin 账号。
- RED: 首次 `ADMIN_TENANT1` prepare -> FAIL，MySQL `BIT(1)` 的 `deleted` 字段返回 `b'\\x00'`，基线指纹代码不能直接 `int()`；准备事务已回滚，未留下任务数据。
- GREEN: 增加显式 BIT 值解析后 prepare/verify PASS；本轮 runId=`FAS-20260814-20260815122606-41624`，O1=6、O2=20、提交量=10，admin 用户/密码/角色/既有签名授权仅生成脱敏 SHA-256 基线指纹，fixture 所有的用户、角色、权限、签名授权 ID 均为空。
- SAFE-BLOCKED: 首次运行态验证因并行源码指纹变化在启动浏览器和目标业务写入前 BLOCKED，`writeRequestCount=0`；finally cleanup 删除 14 行准备数据，独立二次 cleanup 再次证明 `CLEAN/0`，admin 受保护基线前后指纹一致。随后共享 8081/48081 被并行任务停止，本执行器未擅自启动或占用主运行态。
- GREEN: `node --check tests/e2e/frontline-active-order-submit-allocation-real.e2e.js`、`node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> PASS；新增显式 `ADMIN_TENANT1_INT_MAIN` 运行模式，精确绑定 `E:\IntRuoyi` 的 8081/48081 与租户 `1/芋道源码`，未知模式 fail fast，原 `POST_MERGE_INT_MAIN` 仍精确绑定租户 `122/测试租户`。
- GREEN: `python -X utf8 -m py_compile doc\\tasks\\20260814-frontline-active-order-submit-allocation-docs\\fas_fixture_orchestrator.py`、`python -X utf8 ...\\fas_fixture_orchestrator.py --self-test` -> PASS；admin fixture 只复用现有 admin 身份，manifest 的 owned user/role/signature 列表为空，不创建或删除 admin 用户、角色与签名授权，并以用户/密码摘要输入、角色集合和既有签名授权集合生成不可逆保护指纹。
- PREFLIGHT-RECHECK: 8081 仍由 PID 41700 的 `E:\IntRuoyi\IntRuoyiFronted` Vite 监听且 HTTP 200；48081 仍由 PID 46644 运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260815-122829-word-route-e2e-0e653c709.jar` 且 health=`UP`。共享运行态未停止、未重启。
- FIXTURE-PREPARE: admin 补充 fixture 在租户 `1/芋道源码` 创建成功，runId=`FAS-20260814-20260815123450-30656`；O1 计划量 6、O2 计划量 20、提交量 10。`verify` 返回 `READY`、`protectedBaselineVerified=true`，manifest 中 owned user/role/signature 数均为 0。
- E2E: 使用显式 `ADMIN_TENANT1_INT_MAIN` 和受控本机默认登录配置启动真实 Playwright -> `BLOCKED`，阻塞类别为 `RUNTIME_EVIDENCE_PREREQUISITE`；在浏览器和目标业务写入前核验 8081 监听时发现端口已消失，随后复核 8081、48081 均无监听且 HTTP/health 均不可达。此前核验通过的共享 PID 41700/46644 在本任务未停止、未重启的情况下被外部并发操作终止。
- CLEANUP: 本轮目标业务写请求为 0、未生成事件；E2E finally cleanup -> `CLEAN`、`remainingTaskDataCount=0`、`deletedRowCount=14`、`protectedBaselineVerified=true`。独立二次 cleanup -> `CLEAN`、`remainingTaskDataCount=0`、`deletedRowCount=0`、`protectedBaselineVerified=true`。
- BLOCKER: P7 真实业务闭环尚未执行，需由共享运行态所有者恢复并确认 `E:\IntRuoyi` 的 8081/48081 后才能重新 prepare 新 fixture 和复跑；按用户边界，本 executor 未自行启动或重启共享运行态，亦未复用本轮已清理 fixture 冒充成功。

## P7 并发隔离修订与 admin 补充真实 E2E 完成（2026-08-15）

- ROOT-CAUSE: P7 初次多轮 `prepare/verify` 的任务数据消失并非 fixture 业务逻辑错误，而是同一固定 admin artifact/manifest 被并发验证流程覆盖，且另一流程的 cleanup 精确删除了当轮 14 行 fixture，导致运行批次和数据库身份失配。所有受影响轮次均在业务写入前停止或由各自 `finally` 清理，最终残留 0、admin 受保护基线不变。
- BDD: admin 每轮证据与 fixture 隔离 -> Given 同一任务可能存在并发验证流程 When 启动 `ADMIN_TENANT1_INT_MAIN` Then 必须要求合法 `FAS_EVIDENCE_RUN_ID`，并把 manifest、运行态、场景状态、截图、结果和清理证据写入该轮独立子目录；路径穿越和共享目录标识必须 fail fast。
- RED: `node tests/e2e/frontline-active-order-submit-allocation-real-static.spec.cjs` -> FAIL，预期失败为脚本缺少 `FAS_EVIDENCE_RUN_ID` 和 `artifactDirFor`，admin 多轮仍共用固定证据目录。
- GREEN: 新增 admin 运行证据 ID 校验和独立子目录；非法路径标识拒绝，WORKTREE/P6 原目录合同保持不变。`node --check` 与真实 E2E 静态合同 -> PASS。
- E2E-DIAGNOSTIC: 独立目录 `p7-main-20260815-2115` 完成事件 `232` 的全部业务步骤和两次目标写入，但因两条通用 `502 Bad Gateway` console 文本缺少响应 URL 归因而按业务 FAIL；`finally` 为 `CLEAN/0`、admin 基线不变，没有把无法证明来源的错误静默忽略。
- BDD: 外部 HTTP 错误必须逐条可归因 -> Given 浏览器 console 只提供通用 502 文本 When 非本机资源返回 HTTP 错误 Then 只有已记录的非本机响应 URL、状态码和状态文本能与 console 文本一一对应时才归类为外部资源错误；本机或数量不匹配的错误必须继续阻断。
- RED: 外部响应错误分类合同 -> FAIL，原诊断只记录 request failure，不能把 HTTP 502 response 与通用 console 文本绑定。
- GREEN: 页面诊断新增全部 HTTP 错误响应的 method/URL/status/statusText；使用计数映射逐条消费外部证据，本机 MES 响应仍进入目标错误。静态行为合同 -> PASS。
- E2E-BLOCKED: 独立目录 `p7-main-20260815-2122` 在登录页等待 `networkidle` 超时，分类为 `LOGIN_PREREQUISITE`；浏览器和页面后台持续请求未静默放宽，目标业务写入 0，`finally CLEAN/0`、admin 基线不变。
- BDD: 登录等待真实 DOM 和正式登录响应 -> Given 登录页存在持续轮询或外部资源 When 进入登录页 Then 只等待 `domcontentloaded` 和真实表单可见；登录成功仍必须核验正式 `/auth/login` HTTP/业务响应及离开登录路由，不能用 `networkidle` 作为登录页可用性的代理。
- RED: 登录等待合同 -> FAIL，原脚本固定 `page.goto(... waitUntil: 'networkidle')`。
- GREEN: 登录页改为 `domcontentloaded`，后续租户、账号、密码控件和正式登录响应断言全部保留；`node --check` 与静态合同 -> PASS。
- FIXTURE: 最终独立目录 `e2e-artifacts/admin-tenant1-int-main/p7-main-20260815-2132/`；租户 `1/芋道源码`，复用受保护 admin 身份，O1 计划 6、O2 计划 20、提交量 10；fixture verify 为 `READY` 且 `protectedBaselineVerified=true`，owned user/role/signature 集合为空。
- GREEN: `FAS_RUNTIME_MODE=ADMIN_TENANT1_INT_MAIN` 真实 Playwright E2E -> PASS，退出码 0，新事件 ID=`233`。一线通过真实页面选择 O1 并提交 10；版本 1 为 `FRONTLINE_SELECTED`、O1=10、红色待调整 4；组长通过真实页面改配 O1=6/O2=4，版本 2 为 `MANUAL`、池总量 10、未分配 0、红色消失，审计记录完整。
- E2E-WRITES: 目标业务写请求恰好 2 次，只包含一线提交和组长确认；page errors、目标 request failures、目标 HTTP errors、目标 console errors 均为 0。两条外部头像资源 502 均有非本机响应 URL/status/statusText 一一对应，完整保留为 `externalResourceConsoleErrors`，未当作业务成功或静默丢弃。
- CLEANUP: E2E `finally` cleanup -> `CLEAN`、`cleanupVerified=true`、`remainingTaskDataCount=0`、`protectedBaselineVerified=true`；独立二次 cleanup -> `CLEAN/0` 且基线再次一致。
- VISUAL: `initial-overage-red.png` 为真实组长分配弹窗，显示 O1 要生产 6、分配 10 和红色“待调整 4”；`after-manual-reallocation.png` 显示“分配已保存”并返回报工管理列表。两张截图均为 1280x720 非空 PNG，最终 O1/O2 数量、红色消失和审计由同轮 DOM/JSON 共同证明。
- GREEN: 最终回归 -> `node --check` PASS、真实 E2E 静态合同 PASS、fixture `py_compile`/`--self-test` PASS、12 项正式证据文件（9 JSON、1 Markdown、2 PNG）机器校验 PASS、任务差异 `git diff --check` 无错误。
- STATUS: P7 admin 补充真实 E2E 已完成；它只作为补充证据，不替代 P5/P6 的租户 122 独立账号真实验收。未修改生产业务代码，未推送远端。

## P7 独立复核与任务收尾（2026-08-15）

- INDEPENDENT-TEST: 主线程未参与 P7 实现，独立复跑 E2E `node --check`、静态合同、fixture `py_compile`/`--self-test` 及结果机器断言，全部 PASS；逐项证据已写入 `test-report.md`。
- INDEPENDENT-EVIDENCE: 事件 233、目标写请求 2、四类目标错误 0、版本 2 O1/O2=`6/4`、总量 10、未分配 0、审计 3、finally 与二次 `CLEAN/0`、admin 保护指纹一致及两张页面截图均通过独立检查。
- CLOSEOUT-PRECHECK: 清理前枚举 6 份 admin fixture manifest，每个 runId 均存在 `CLEAN/0` 且 `protectedBaselineVerified=true` 的清理证据，未带残留风险进入产物删除。
- CLOSEOUT-PREVIEW: `task_closeout.py --mode preview --worktree-closeout off` -> `status=ready`、`blocked=[]`、`warnings=[]`；保留核心文档、P5/P6 历史证据和 P7 事件 233 的 12 项正式证据，只删除本任务失败/阻塞轮次、无效运行标识、孤儿证据与 `__pycache__`。
- CLOSEOUT-APPLY: 同一命令 `--mode apply` -> `status=applied`；清理范围与 preview 一致，未操作生产代码、admin 基线、无关并行任务文件或共享运行态。
- EXPERIENCE: 按 `project-experience-consolidation` 复核后，P7 的通用经验已合并到既有 `docs/e2e-rules.md` 与 `docs/experience-index.md`：写入型 fixture 全量按 run ID 隔离、通用 console 错误须用外部 response 逐条归因、持续请求页面不得用 `networkidle` 代替正式登录/控件判据。未新建长期经验文档。
- RUNTIME-KEEP: 最终 8081 PID 35448 HTTP 200、48081 PID 21556 health=`UP`，均归属 `E:\IntRuoyi`；按项目规则保留共享 `int_main` 运行态。
- FINAL-GATE: 完成态 closeout preview -> `delete=[]`、`blocked=[]`、`warnings=[]`；`check_plan_completion.py --apply` -> `complete=true`；P7 正式目录仅保留事件 233 的 12 项证据，临时产物 0。
- FINAL: P7 状态 `completed`，任务业务数据残留 0、admin 基线不变、无阻塞，未提交或推送远端。
