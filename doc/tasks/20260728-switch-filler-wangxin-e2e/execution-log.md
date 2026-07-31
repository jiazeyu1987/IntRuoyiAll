# Execution Log

## User Intent

- 用户登录 wangxin 账号，业务要求可以切换到其他填写人；切换后表单也要跟着填写人的变化而变化。
- 当前症状：弹窗中其他填写人可见但不允许点击；此前重新创建批次执行时还出现“当前执行详情缺少填写人快照，不能切换填写人”。
- 2026-07-28 follow-up: 用户截图反馈切换到 `任丹` 后，顶部“填写人”仍显示 `王歆`，且“我的填写项”区域变为“未配置辅助模式”，填写内容丢失。

## BDD

- BDD: wangxin 可选择其他填写人 -> Given wangxin 进入 eDHR 辅助填写页且执行详情快照包含多个填写人候选 When 打开“切换填写人”弹窗 Then 非 wangxin 的可打开候选也应可点击，不得被当前登录用户 ID 禁用。
- BDD: 表单随选择的填写人变化 -> Given 用户在“切换填写人”弹窗选择另一个候选 When 前端调用正式 `openTask` 成功 Then 当前填写页保持辅助模式并使用后端返回的 execution/workTask/query 上下文刷新表单。
- BDD: 候选来自执行详情快照 -> Given 批次执行创建后填写人已固定 When 打开“切换填写人”弹窗 Then 候选来自 `execution.assistSwitchTasks`，不得重新调用全量批次详情接口。
- BDD: 后端仍做最终授权 -> Given 选择的候选后端不可打开 When `openTask` 返回业务错误 Then 前端在当前弹窗显示真实错误，不吞异常、不默认成功。
- BDD: 切换后顶部填写人同步 -> Given wangxin 在填写人弹窗选择任丹 When 后端 `openTask` 返回 `assistUserId=910181` Then 顶部“填写人”卡片必须显示任丹，不得继续显示当前登录人王歆。
- BDD: 切换后辅助内容不丢失 -> Given 所选填写人共享当前批记录填写任务 When route query 切换为所选 `assistUserId` Then “我的填写项”必须按所选填写人的可填行或共享批记录内容刷新，不得错误显示“未配置辅助模式”。

## RED/GREEN

- RED: `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> FAIL，旧前端用 `currentAssistUserId() === item.userId` 硬禁用非当前登录人的填写人候选。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，旧切换填写人弹窗仍调用全量 `getEdhrBatchExecution`，未消费执行详情 `assistSwitchTasks` 快照。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，当前源码仍在 `openOrCreateByContext` 的 active 查询、active key 和创建记录中使用 `taskId(null)`，未保存请求中的批次任务 ID。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-switch-filler-selectability-static.spec.js` -> PASS，候选可选态不再按当前登录人禁用，active 使用 route-id 语义比较，点击传入所选 `assistUserId`。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS，执行详情快照、`assistUserId` 请求/响应和批次任务隔离合同通过。
- GREEN: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue src/api/mes/pro/feedback/index.ts src/api/mes/pro/edhr/batchExecution.ts tests/e2e/edhr-switch-filler-selectability-static.spec.js --format stylish` -> PASS。
- BLOCKER: `pnpm ts:check` -> FAIL，失败在无关既有文件 `BatchRecordCellRulesConfirmDialog.vue(117,21)/(121,36)` 缺少 `assistPreviewRows`，以及 `BatchExecutionDetailPage.vue(387,25)/(396,59)/(399,37)/(429,27)/(527,25)/(533,31)/(538,26)` 缺少 `effectiveDetailPreviewAssistMode` / `selectedPreviewAssistFields` / `selectedPreviewAssistRowsConfigured`；本任务 `ExecutionPage.vue` 无新增 tscheck 错误。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_exposesOnlyCurrentUsersAssistRowsFromFrozenResponsibilityScope+openTask_exposesAssistRowsWhenAllRangeScopeCoversSnapshotSourceTable" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0。
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS，生成 `yudao-server-exec.jar` 并加载到 int_main 本地后端。
- GREEN: `node doc\tasks\20260728-switch-filler-wangxin-e2e\e2e-artifacts\switch-filler-wangxin-real.e2e.cjs` -> PASS，真实前端登录 `芋道源码/wangxin`，打开待办任务 `2244`，弹窗展示 `王歆` 和 `任丹`，任丹 enabled；点击任丹后 `task/open` payload 带 `assistUserId=910181`，URL `assistUserId=910181`，顶部填写人显示 `任丹`，辅助填写行 `assistRows=87`，重开弹窗高亮任丹，全量批次详情重载 `0`，API error `0`。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260728-switch-filler-wangxin-e2e\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260728-switch-filler-wangxin-e2e\frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260728-switch-filler-wangxin-e2e\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-switch-filler-wangxin-e2e --mode preview` -> ready，keep 9 files，delete/blocked/warnings none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-switch-filler-wangxin-e2e --mode apply` -> applied，deleted_paths none。

## Milestone Updates

- M1 completed: 已建立任务目录，读取并记录任务、前端、后端、E2E、登录、数据库、本地运行态、PowerShell 编码和相关经验门禁。
- M2 completed: 已通过聚焦静态合同复现“非当前登录人不可点”和“弹窗重新拉全量批次详情”两个 RED。
- M3 completed: 已修复 `ExecutionPage.vue` 从执行详情 `assistSwitchTasks` 读取候选；可选态仅按任务可打开判断；点击时向 `openTask` 传入所选 `assistUserId`；active 改为 `sameRouteQueryId(currentAssistSwitchUserId(), item.userId)`，避免 route 字符串和数字严格等于导致切换后不高亮。
- M3 completed: 已补齐 `openTask` 前后端 `assistUserId` 合同，后端按所选候选校验并返回已确认 `assistUserId`，传统批记录执行记录按 `batchExecutionId + taskId` 隔离。
- M4 completed with blocker: 静态合同、ESLint、后端编译、定向 JUnit、后端 package 和 wangxin 真实 Playwright E2E 均通过；全量 `pnpm ts:check` 仍被无关既有页面类型错误阻塞，已记录失败文件和行号。
- M5 partial: 已运行三类证据校验脚本并通过。
- M5 partial: 已按 `project-experience-consolidation` 将 route query ID 字符串/数字比较经验沉淀到 `docs/frontend-development.md#前端 Route Query ID 比较门禁`，并更新 `docs/experience-index.md`。
- M5 partial: cleanup preview/apply 均通过，无删除项。
- M5 blocked for repository completion: 当前状态保持 `blocked_for_closeout`；未执行 commit/push，因为根仓存在无关 DCC 任务改动、本分支 ahead 1，且全量 `pnpm ts:check` 仍有无关阻塞，本任务不提交无关改动。
- M6 completed: 已处理用户截图反馈的二次回归：切换后顶部填写人不变、辅助内容为空。真实 E2E 确认切到任丹后 `switchedFillerLabel=任丹` 且 `assistRowCountAfterSwitch=87`。

## Verification Evidence

- `real-e2e-evidence.md` 与 `e2e-artifacts/switch-filler-wangxin-real-result.json` 记录真实 E2E 脱敏证据。
- 本地后端已重启到当前任务构建 Jar：`output\runtime\int_main\backend-runtime-control-20260728-134227.jar`，PID `3672`，SHA256 `6F8E17BB3DE9CABD384BD428AD9EAEB3FAE04388C063E838F108DA308ECB5096`，`/actuator/health` 为 `UP`。
- 长期经验已记录：`docs/frontend-development.md`、`docs/backend-development.md` 与 `docs/experience-index.md`。

## Blockers

- 根仓当前存在无关 DCC 任务文档改动且分支 `int_main` ahead 1；本任务不回滚、不覆盖、不提交无关文件。
- `pnpm ts:check` 当前被无关既有类型错误阻塞；本任务已用聚焦静态合同、ESLint、后端编译/JUnit 和真实 E2E 覆盖本缺陷。
- 定向 JUnit 曾在与后端编译并行执行时超时；已按门禁只停止本任务 Maven PID，并单独复跑得到明确 PASS。其他并行任务进程未动。
- Git closeout blocker: 未做提交/推送；若后续需要提交，需先按项目规则处理现有无关脏改动、本分支 ahead 状态与本任务变更的提交边界。
