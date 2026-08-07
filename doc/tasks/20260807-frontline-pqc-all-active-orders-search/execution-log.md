# Execution Log

## User Intent

- 一线 PQC 可选择的订单应为所有生产组长 ACTIVE 订单的集合。
- 订单选择弹框应支持手动输入订单号来快速选择。

## BDD

- BDD: 展示所有生产组长活跃订单 -> Given 多个生产组长各自维护 ACTIVE 订单；When 一线 PQC 打开订单选择弹框；Then 页面从无当前组长过滤的 PQC 活跃订单接口加载并展示去重后的全部候选。
- BDD: 输入订单号过滤候选 -> Given 订单弹框已加载全部 ACTIVE 订单；When PQC 输入订单号的全部或部分字符；Then 页面仅显示订单号包含该输入的正式候选，清空输入后恢复全部候选。
- BDD: 回车快速选择 -> Given 输入订单号后存在订单号完全匹配或唯一过滤结果；When PQC 按回车；Then 页面选择该订单并继续既有工序加载链路；多条模糊结果或零结果不得猜测选择。
- BDD: 接口失败不得降级 -> Given 活跃订单接口失败或正式候选为空；When 页面初始化；Then 保留既有明确错误，不使用 mock、当前组长局部数据或默认订单冒充成功。

## Command Intent And Evidence

- 2026-08-07：读取项目触发规则、技能契约、经验索引、现有一线 PQC 组件、API 和后端服务。
- 2026-08-07：确认 `MesFrontlinePqcContextServiceImpl.listActiveOrders()` 调用 `MesProcessPoolActiveOrderMapper.selectActiveList()`；该 mapper 仅按 `activeStatus=ACTIVE` 查询，没有 `leaderUserId` 或登录人过滤，并在服务层按 `workOrderId + routeId` 去重，已是所有生产组长 ACTIVE 订单的统一集合。
- 2026-08-07：确认现有 `MesFrontlinePqcContextServiceTest.shouldListActiveOrdersFromUnifiedActiveOrderAuthority` 锁定使用全局 active-order mapper，且禁止回退工序池活跃列表。
- 2026-08-07：确认相邻在途任务 `20260807-frontline-pqc-latest-active-version` 会修改 PQC 路线版本链路；本任务不修改其后端服务或测试。
- RED: `node tests\\e2e\\mes-frontline-pqc-all-active-orders-search-static.spec.cjs` -> FAIL, 一线 PQC 订单弹框缺少订单号搜索输入、正式候选过滤和确定性回车选择逻辑。
- 2026-08-07：Playwright CLI 已按技能要求完成 npx 前置和真实登录页快照，但 Windows 会话在登录后未保持并出现 CLI 运行时断言；未将该结果记为 E2E 通过，改用项目既有 Playwright 库运行任务自有只读脚本，且脚本只从本机环境读取凭据、不输出或保存凭据。
- E2E first run: `node doc\\tasks\\20260807-frontline-pqc-all-active-orders-search\\frontline-pqc-all-active-orders-search-real.e2e.cjs` -> FAIL, 全量候选、输入过滤和零结果均已通过，但脚本把下游工序/登录 PQC 员工完成后的弹框关闭误作为订单号搜索本身门禁，等待 90 秒超时；调整为断言回车触发目标订单正式工序请求和已选状态，并单独记录下游关闭状态。
- E2E second run: 同一命令 -> FAIL, 登录接口完成后立刻直达动态路由时权限信息尚未完成，页面未发出活跃订单请求；按官方登录前置补充等待 `get-permission-info` 成功，不降低为 API-only。
- E2E third run: 同一命令 -> FAIL, 订单号搜索回车已触发正式工序接口，但默认首个候选返回“当前工序缺少已发布 QA 检验规程，activeOrderId=39”；该失败属于现有正式数据/相邻路线版本任务 blocker，不由本任务吞掉。后续固定使用用户截图中的 `PQC-E2E-FS-20260804`，缺该 ACTIVE 样本或仍缺规程即阻塞，不改选其它订单。
- E2E fourth run: 同一命令 -> TASK-SCOPE PASS / DOWNSTREAM BLOCKED, 正式 ACTIVE 集合返回 1 条，弹框显示数量与接口一致；输入框自动聚焦，大小写不敏感订单号过滤、清空恢复、零结果提示均通过；回车命中 `PQC-E2E-FS-20260804` 并发出对应 `workOrderId + routeId` 的正式工序请求。下游返回“当前工序缺少已发布 QA 检验规程，activeOrderId=30，routeProcessId=980645，processId=922985”，因此未把弹框最终关闭写成通过，也未增加 fallback。
- GREEN: `node tests\\e2e\\mes-frontline-pqc-all-active-orders-search-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\\e2e\\mes-frontline-pqc-order-picker-production-layout-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\\e2e\\mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS。
- REGRESSION: `node tests\\e2e\\mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- BACKEND TARGETED TEST: `mvn.cmd -pl yudao-module-mes -Dtest=MesFrontlinePqcContextServiceTest#shouldListActiveOrdersFromUnifiedActiveOrderAuthority -DfailIfNoTests=false test` -> NOT COMPLETED, Maven 重新编译 2531 个源文件，运行约 21 分钟仍停留 javac 且未进入 Surefire；已终止本任务启动的 Maven 进程，不记录为 PASS。全局 ACTIVE 来源由本任务静态合同和既有测试源码共同锁定。
- EVIDENCE VALIDATION first run: frontend/backend validators -> FAIL, 证据内容存在但缺校验器要求的字面 `BDD:/RED:/GREEN:` 和 `Verification` 标记；已补机器可读标记后复跑。
- EVIDENCE VALIDATION: frontend/backend evidence validators -> PASS。
- EXPERIENCE CONSOLIDATION: 按 `project-experience-consolidation` 将 Windows Playwright CLI 命名会话、登录快照凭据和工具链失败归因规则并入既有 `docs/e2e-rules.md#Playwright 快照与 daemon 收尾门禁`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260807-frontline-pqc-all-active-orders-search --mode preview` -> READY，无 blocked/warnings；保留三份核心任务记录，删除两份已归档技能 evidence、临时真实 E2E 脚本和任务 Playwright 目录。
- CLEANUP PROCESS: 停止命令行明确属于本任务的 Playwright CLI daemon `pqcactiveorders`；未停止其它浏览器、前后端或并发任务进程。
- CLEANUP APPLY: `task_closeout.py --task-id 20260807-frontline-pqc-all-active-orders-search --mode apply` -> PASS；两份技能 evidence、临时真实 E2E 脚本和任务 Playwright 目录已删除，三份核心任务记录保留。
- FINAL REGRESSION: 任务静态合同、PQC 订单弹框布局、活跃订单切换、PQC 登录员工锁定四个测试再次运行 -> PASS。
- FINAL CHECK: task-owned `git diff --check` -> PASS，仅有仓库 CRLF 工作区提示。
- GIT OWNERSHIP: 本任务未执行 stage/commit/push。共享工作区的并发 baseline 流程在任务期间已把组件和正式静态回归测试纳入当前 HEAD；本任务未触碰同时出现的后端或其它任务改动，收尾仅保留本任务核心记录和按规则删除技能 evidence。

## Milestone Status

- M1：completed。
- M2：completed。
- M3：completed。
- M4：completed，真实页面下游规程 blocker 已单列。
- M5：completed。

## Blockers

- 截图订单 `PQC-E2E-FS-20260804` 的现有后端运行态缺已发布 QA 检验规程，阻塞选择订单后的工序链路完成；相邻任务 `20260807-frontline-pqc-latest-active-version` 已记录同一根因。本任务不修改其后端文件，不做数据或旧规程 fallback。
- 后端目标 Maven 未进入单测阶段；不影响本任务前端生产代码、静态合同或类型检查结论，但不能声称该 JUnit 本轮已通过。
