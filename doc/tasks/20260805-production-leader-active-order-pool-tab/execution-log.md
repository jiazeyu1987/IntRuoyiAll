# Execution Log

## User Intent

- 生产组长的活跃订单池作为独立 Tab。
- Tab 下使用标准列表模板展示全部活跃订单池。
- 提供新增按钮，点击后可以新增活跃订单。
- 2026-08-06 需求变更：新增活跃订单只输入“订单号”；订单号对应生产工单 `code`；输入控件为可搜索输入下拉；调拨关联从新增动作拆除，仅保留既有调拨追溯只读展示。

## BDD

- BDD: 生产组长查看活跃订单池 -> Given 用户进入生产组长页面，When 点击“活跃订单池”Tab，Then 页面使用统一标准列表模板展示正式接口返回的全部活跃订单。
- BDD: 生产组长新增活跃订单 -> Given 用户位于“活跃订单池”Tab，When 点击“新增活跃订单”并提交合法数据，Then 页面调用正式加入接口、关闭对话框并刷新列表。
- BDD: 生产组长移出活跃订单 -> Given 列表存在活跃订单，When 用户点击该行“移出活跃订单”，Then 页面调用正式移出接口并刷新列表。
- BDD: 其它生产组长模块保持不变 -> Given 用户切换人员管理、报工管理、看板、异常、损耗管理或班组配置，When 页面渲染对应模块，Then 原有模块内容和 PQC 组长行为不受影响。
- BDD: 按订单号加入活跃订单 -> Given 已确认生产工单存在且仅有一条有效排产，When 生产组长输入订单号并选择下拉候选后提交，Then 前端只提交 `workOrderId`，后端解析正式 `routeId/routeVersionId` 并加入活跃订单。
- BDD: 未选择真实候选阻塞 -> Given 生产组长只输入自由文本或清空下拉，When 点击加入活跃订单，Then 前端提示“请选择订单号”且不发起新增写请求。
- BDD: 后端前置数据失败不写入 -> Given 工单未确认、缺少有效排产、多条有效排产或排产缺正式路线/路线版本，When 调用新增接口，Then 后端返回明确业务错误，且不创建活跃订单、工序快照或 PQC 任务。
- BDD: 调拨追溯只读拆分 -> Given 已有正式调拨/发货/补料/退料关联数据，When 验证活跃订单调拨追溯，Then 只读读取正式追溯端点和页面表格，不通过新增弹窗、隐藏字段或 API 写入补数据。

## Initial Inspection

- 页面入口：`IntRuoyiFronted/src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue`。
- 共享实现：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 正式接口：`getTeamLeaderActiveOrderList`、`addTeamLeaderActiveOrder`、`removeTeamLeaderActiveOrder`。
- 当前活跃订单维护位于“班组配置”模块中的内嵌卡片，尚未作为独立 Tab，也未使用统一标准列表模板。
- `docs/experience-index.md` 已存在；命中统一列表、角色内容页签拆分和前端静态契约隔离门禁。

## Dirty Worktree Baseline

- 初始分支：`int_main`，跟踪 `origin/int_main`。
- 初始工作区存在当前任务开始前的并行改动，涉及后端 Team Leader 配置、系统用户 API、`UnifiedListTemplate`、PQC 规程页、`TeamLeaderWorkbenchPage.vue`、若干测试和既有任务文档。
- 基线提交：`633361dde chore: baseline pre-existing worktree changes`。
- 该共享基线由并行任务提交并推送，包含任务开始前既有改动，也包含本任务刚建立的三份任务文档；尚未包含本任务测试或生产代码。
- 基线后仍有其它并行任务文档改动；它们不属于本任务，后续只选择性暂存本任务文件。
- 2026-08-06 继续本需求前，当前工作区既有脏改动已按项目规则提交为基线 `a8f377ba0 chore: preserve preexisting workspace baseline`。
- 2026-08-06 基线后执行 `git fetch origin int_main` 与 `git merge --no-edit origin/int_main`，无冲突合并远端 16 个提交。

## Change Triage

- CHANGE: `docs/changes/20260806-active-order-code-input.md` -> Decision `Accept`。
- Impact: 新增候选接口、收缩新增请求、服务端解析排产路线、前端单字段远程下拉、真实 E2E 调拨追溯只读拆分。

## Verification Evidence

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，首个失败为生产功能模块状态缺少 `activeOrder` Tab，符合预期。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js` -> PASS。
- REGRESSION: `production-leader-function-tabs-static.spec.js`、`production-leader-tabs-flat-style-static.spec.js`、`production-leader-remove-header-content-static.spec.js`、`team-leader-workbench-static.spec.cjs`、`role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- REGRESSION: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，失败点是既有合同要求 `resetSubmissionMultiFilter()` 异步调用 `getSubmissionList()`；当前基线实现为同步清空，失败与本任务新增 Tab、列表、新增弹窗和活跃订单接口无关，按前端静态契约隔离门禁保留。
- REAL E2E: 官方登录前置在 `http://127.0.0.1:8081/mes/pro/process-pool/production-leader` 使用 `芋道源码/admin` -> PASS。
- REAL E2E: 只读 Playwright 打开“活跃订单池”Tab，确认标准列表、新增按钮和新增弹窗可见，关闭弹窗且未提交；运行态活跃订单数量为 `0`，`activeOrderWriteRequestCount=0`、`targetFailureCount=0`、`pageErrorCount=0`、`consoleErrorCount=0` -> PASS。
- RUNTIME: 前端 `8081` HTTP `200`，进程归属 `E:\IntRuoyi\IntRuoyiFronted`；后端 `48081` health `UP`，运行参数 `repo-root` 归属 `E:\IntRuoyi\IntRuoyiBackend`。
- DIFF: 当前任务源码与测试 `git diff --check` -> PASS。
- EVIDENCE: `validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md` -> PASS。
- EXPERIENCE: 已执行 `project-experience-consolidation` 检索；本次“只读打开新增弹窗并断言目标写请求为 0”已由 `docs/e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁` 覆盖，“既有大合同无关失败使用任务专用最小合同隔离”已由 `docs/frontend-development.md#前端静态契约隔离门禁` 覆盖，无需新增或修改长期经验文档。

## Implementation Summary

- 生产组长七组功能 Tab 均加入独立“活跃订单池”入口。
- 活跃订单池使用 `UnifiedListTemplate` 和客户端分页展示正式列表接口返回的全部活跃记录。
- 新增按钮打开独立对话框，并通过正式加入接口提交生产订单、路线、路线版本和调拨单 ID。
- 每行保留正式移出能力；班组配置不再重复承载活跃订单维护。
- 调拨库存追溯随活跃订单维护移动到新 Tab，加载和错误状态继续显式暴露。

## Blockers

- 完整写入型真实 E2E 需要已确认的测试租户、生产组长账号和 `TLW_*` 任务夹具变量；当前仅有 `芋道源码/admin` 只读身份，因此未执行新增/移出写入。当前任务按聚焦 RED/GREEN、相邻回归、类型检查和只读真实页面路径验收，未使用 admin 基线数据写入。
- 若并行任务继续修改本任务目标文件并产生同一区域冲突，将停止并报告。
- Git 提交/推送阻塞：选择性暂存时检测到并发 `git commit -m "fix: isolate QA inspection rules by product"`，`.git/index.lock` 为非空文件（`1441792` 字节）。等待并发提交退出后锁文件仍保持非空；按 `docs/powershell-memory.md#Git index.lock 陈旧锁恢复门禁`，非空锁禁止删除，因此本任务不能安全执行 `git add`、实现提交、收尾提交或 push，状态保持 `ready_for_closeout`。

## Closeout

- 当前状态已切换为 `ready_for_closeout`。
- cleanup 默认保留 `task.md`、`execution-log.md`、`verification-report.md`，删除已完成归档的 `frontend-feature-evidence.md`。
- `task_closeout.py --mode preview` -> keep 3、delete 1、blocked 0、warnings 0。
- `task_closeout.py --mode apply` -> PASS，已删除 `frontend-feature-evidence.md`，当前为主工作区，无 worktree 合并或删除动作。
- 实现提交、收尾提交和 `git push origin int_main` 因非空 `.git/index.lock` 阻塞，尚未完成。
