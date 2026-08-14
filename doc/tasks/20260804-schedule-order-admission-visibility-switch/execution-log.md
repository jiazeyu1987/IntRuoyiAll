# Execution Log: 同步工单已入池显示开关

- User intent: 在截图红框位置增加 switch 开关，可显示或隐藏已经加入排产池的订单。
- Scope: 仅修改排产工单页面“同步工单”页签筛选/操作区、同步工单查询状态及本任务静态契约。
- Non-goals: 不修改后端 API、不改变入池提交接口、不改变排产工单主列表、不引入兼容 fallback。
- Dirty workspace note: 任务开始前仓库已有大量与本需求无关的脏改动和本地 ahead 状态，本任务不回滚、不覆盖这些改动。
- `BDD: 同步工单默认隐藏已入池订单 -> Given 排产员打开排产工单页面并切换到同步工单页签 / When 页面首次加载同步工单列表 / Then 查询参数默认不包含已加入排产工单池的生产工单，列表聚焦可入池或需处理订单。`
- `BDD: 开关显示已入池订单 -> Given 排产员停留在同步工单页签 / When 打开“显示已入池订单”开关 / Then 页面重新查询第一页，并把已加入排产工单池的生产工单纳入列表展示。`
- `BDD: 重置恢复隐藏已入池订单 -> Given 排产员已打开显示已入池订单开关 / When 点击同步工单页签的重置按钮 / Then 开关恢复关闭状态并重新查询隐藏已入池订单的列表。`
- `BDD: 真实页面开关请求参数一致 -> Given 本机 int_main 前后端运行且用户登录排产工单页面 / When 用户切到同步工单页签并打开/关闭“显示已入池订单”开关 / Then 页面必须分别发出隐藏已入池、纳入已入池、再隐藏已入池的 admission-diff 请求，且不产生写请求。`
- User follow-up: `进行E2E验证`。
- E2E scope: 使用本机 `http://127.0.0.1:8081` / `http://127.0.0.1:48081` 只读验证，不新增/修改生产工单，不提交入池，不记录密码。

## Milestone Updates

- M1 completed: 已创建任务文档并记录 BDD/TDD 验收口径。
- Experience gate: 已读取 `docs/experience-index.md`，命中并采用 `docs/frontend-development.md#前端静态契约隔离门禁`、`docs/e2e-rules.md#Element Plus 选择框显示门禁`、`docs/e2e-rules.md#E2E 脚本入口存在性门禁`。
- M2 completed: 新增 `IntRuoyiFronted/tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js`，覆盖 Switch UI、默认关闭、查询参数切换、重置恢复和禁止本地过滤。
- M3 completed: `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue` 在同步工单 actions 区增加“显示已入池订单”开关；开关关闭时使用 `READY_TO_ADMIT`，打开时清空直接 `admissionStatus` 查询参数以纳入已入池状态；重置恢复关闭。
- M4 completed: 定向静态契约、相邻回归和排产定向类型检查已执行。
- M5 completed: 已使用真实 Playwright 登录本机 `芋道源码/admin`，进入 `/mes/pro/schedule-order` 的“同步工单”页签，验证显示已入池订单开关关闭、打开、再次关闭的请求参数和页面结果；全程目标写请求数为 0。

## Verification Evidence

- `RED: node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js -> FAIL, expected reason: 同步工单 actions 工具栏缺少显示已入池订单开关。`
- `RED: node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js -> FAIL, expected reason: 同步工单快速筛选仍直接调用 workOrderAdmissionQuickFilter.applyQuickFilter，工单编码筛选会删除显示已入池开关控制的 admissionStatus。`
- `GREEN: node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js -> PASS`
- `GREEN: node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js -> PASS`
- `GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS`
- `GREEN: node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js -> PASS`
- `GREEN: node tests/e2e/mes-schedule-order-admission-reason-options-static.spec.js -> PASS`
- `GREEN: pnpm ts:check:schedule -> PASS`
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-schedule-order-admission-visibility-switch/frontend-feature-evidence.md -> PASS`
- `E2E RED: node doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs -> FAIL, expected reason: 真实页面工单编码快速筛选请求缺少 admissionStatus=READY_TO_ADMIT，已入池样本会重新出现。`
- `E2E ENV: node doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs -> FAIL, environment reason: 48081 后端一度不可连接导致登录等待超时；确认 8081 前端 HTTP 200，恢复使用 PID 49968 的 int_main 后端后继续验证。`
- `E2E SCRIPT RED: node doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs -> FAIL, expected reason: Element Plus Switch 的 input[role="switch"] 为隐藏 input，不可直接点击；脚本改为点击可见 .el-switch 并读取 input aria-checked。`
- `GREEN: node --check doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs -> PASS`
- `GREEN: node doc/tasks/20260804-schedule-order-admission-visibility-switch/real-e2e-admission-switch.cjs -> PASS, tenant=芋道源码 username=admin sample=RRM-20260801-PP-MO-001 initial READY_TO_ADMIT total=84, hidden READY_TO_ADMIT+workOrderCode total=0, shown admissionStatus cleared total=1, hidden again READY_TO_ADMIT+workOrderCode total=0, targetWriteCount=0, targetBadResponseCount=0, pageErrorCount=0, consoleErrorCount=0.`
- `REGRESSION: pnpm ts:check -> FAIL, unrelated existing blocker: src/views/mes/qc/template/index.vue imports missing QaInspectionRegulationPublishedVersionVO / QaInspectionRuleVO and calls missing MesQcTemplateApi.getPublishedQaRegulationVersion.`
- `PROCESS CHECK: Get-CimInstance ... tsconfig.schedule-relaxed / tsconfig.relaxed -> PASS, no remaining vue-tsc validation process after checks completed.`
- `EXPERIENCE: project-experience-consolidation -> PASS, merged the reusable Element Plus Switch Playwright lesson into docs/e2e-rules.md; no new long-term experience document was created.`
- `CLEANUP PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-schedule-order-admission-visibility-switch --mode preview -> PASS, delete only doc/tasks/20260804-schedule-order-admission-visibility-switch/frontend-feature-evidence.md.`
- `CLEANUP APPLY: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-schedule-order-admission-visibility-switch --mode apply -> PASS, deleted frontend-feature-evidence.md and kept task.md/execution-log.md/verification-report.md.`
- `RUNTIME CLEANUP: stopped task-owned duplicate backend PID 38412 after it failed to bind 48081; active 48081 listener remains PID 49968 with command line rooted at E:\IntRuoyi output runtime.`
- `GIT STATE: git log -1 --stat -- target frontend files -> b59f5baf4 includes frontend source/test path changes in concurrent baseline commit; task closeout docs remain uncommitted because current int_main has unrelated dirty/ahead state.`

## Blockers

- 当前工作区已有大量非本任务改动；本任务验证会优先使用定向静态契约，提交/推送收尾需在不混入无关改动的前提下处理。
- 只读检索 `rg admission-diff... IntRuoyiBackend IntRuoyiFronted/...` 触发历史损坏目录 `IntRuoyiBackend/yudao-module-mes/target_corrupt_m4_20260802_1327/...` OS error 1392；已改用源码文件定向读取，不影响本任务前端实现。
- 任务最终仍处于 `ready_for_closeout`：全量 `pnpm ts:check` 有无关 QC 模板 blocker，且当前分支存在其它任务未提交/已暂存改动和 ahead 状态，无法安全独立提交/推送本任务收尾文档。
