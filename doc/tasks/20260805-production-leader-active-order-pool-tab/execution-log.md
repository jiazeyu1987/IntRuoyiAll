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
- `docs/experience-index.md` 已存在；命中统一列表、角色内容页签拆分、前端静态契约隔离、Element Plus 下拉选择和 PowerShell/Git 门禁。

## Dirty Worktree Baseline

- 初始分支：`int_main`，跟踪 `origin/int_main`。
- 历史基线提交：`633361dde chore: baseline pre-existing worktree changes`。
- 2026-08-06 继续本需求前，当前工作区既有脏改动已按项目规则提交为基线 `a8f377ba0 chore: preserve preexisting workspace baseline`。
- 2026-08-06 基线后执行 `git fetch origin int_main` 与 `git merge --no-edit origin/int_main`，无冲突合并远端 16 个提交。
- 本轮接手时工作区仍存在大量并行任务改动；仅修改本任务目标文件和同页静态合同所需的最小修正。

## Git Lock Recovery

- 2026-08-06 12:23:35 检查 `.git/index.lock`：精确路径 `E:\IntRuoyi\.git\index.lock`，长度 `0`，最后写入时间 `2026-08-06 11:35:38`。
- 只读枚举 `git` / `git-lfs` 进程：无活动进程。
- 按 `docs/powershell-memory.md#Git index.lock 陈旧锁恢复门禁` 删除 0 字节且超过 60 秒的陈旧锁；随后 `git status --short --branch` 可读取。
- 2026-08-06 13:33 复查 `.git/index.lock`：长度 `0`，最后写入时间 `2026-08-06 12:29:49`，但存在由 `ChatGPT.exe` 触发的后台 `git status --no-renames --porcelain=v1 -z --untracked-files=normal` 进程；按门禁未删除锁文件、未停止外部进程。

## Change Triage

- CHANGE: `docs/changes/20260806-active-order-code-input.md` -> Decision `Accept`。
- Impact: 新增候选接口、收缩新增请求、服务端解析排产路线、前端单字段远程下拉、真实 E2E 调拨追溯只读拆分。

## Implementation Summary

- 后端 `MesTeamLeaderActiveOrderAddReqVO` 和 `MesTeamLeaderActiveOrderAddReqBO` 只保留 `workOrderId`。
- 后端候选接口 `GET /active-order/candidates` 返回 `workOrderId`、`workOrderCode`，并使用维护权限。
- 后端新增接口从唯一有效排产工单解析 `routeId` 和 `routeVersionId`，缺少唯一有效排产或路线/版本时使用专用业务错误 fail fast。
- 前端新增弹窗只保留“订单号”远程可搜索 `el-select`，选项 label 为 `workOrderCode`，value 为 `workOrderId`。
- 前端提交只调用 `addTeamLeaderActiveOrder({ workOrderId })`；旧路线 ID、路线版本 ID、调拨单 ID 输入和解析函数已移除。
- RRM 真实脚本将加入动作改为选择订单号候选，失败分支改为未选择候选不发写请求；调拨追溯只使用已有 `RRM_TRANSFER_TRACE_ACTIVE_ORDER_ID` 做只读验证。
- 同页相邻静态合同要求 `resetSubmissionMultiFilter()` 异步刷新提交看板；已补齐 `await getSubmissionList()` 和 `await resetSubmissionMultiFilter()`。

## Verification Evidence

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，首个失败为生产功能模块状态缺少 `activeOrder` Tab，符合预期。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，旧相邻合同仍要求新增活跃订单 payload 暴露 `routeId/routeVersionId/transferIds`。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 25, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md` -> PASS。
- GREEN: `validate_backend_api.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md` -> PASS。
- GREEN: `validate_change_request.py --evidence docs/changes/20260806-active-order-code-input.md` -> PASS。
- GREEN: `git diff --check` -> PASS（仅 CRLF working-copy 提示）。
- REAL E2E BLOCKED: `node tests/e2e/team-leader-workbench-real-flow.e2e.js` -> exit code 1/blocked result，当前环境无 `TLW_*` 变量；阻塞详情写入 `IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json`。

## Bug Regression 2026-08-06 Null WorkOrder

- User report: 加入活跃订单池时提示 `请求参数不正确:不能为null`。
- BDD: 未选择真实订单号候选不发写请求 -> Given 生产组长只输入自由文本、清空下拉或候选刷新后没有真实选中项，When 点击“加入活跃订单”，Then 前端提示 `请选择订单号`，不得调用 `/active-order/add`，后端不得收到 `workOrderId=null`。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，新增弹窗缺少 `@change="handleActiveOrderCandidateChange"` / `@clear="handleActiveOrderCandidateClear"`，不能证明绑定真实候选。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，提交仍直接使用 `requirePositiveNumber(activeOrderForm.workOrderId, '请选择订单号')`，缺少候选级提交门禁。
- Fix: 前端新增 `activeOrderSelectedCandidate`、`handleActiveOrderCandidateChange`、`handleActiveOrderCandidateClear` 和 `requireSelectedActiveOrderCandidateWorkOrderId()`；搜索为空、搜索失败或候选刷新不包含当前值时清除旧选择；提交前必须确认表单值、已选候选和当前候选列表一致。
- Follow-up report: 截图显示用户已输入完整订单号 `881MO093613`，但未点击候选时仍提示后端 `请求参数不正确:不能为null`。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，缺少 `activeOrderCandidateKeyword` 和按 `workOrderCode` 精确解析候选的提交前路径。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，提交路径未 `await requireSelectedActiveOrderCandidateWorkOrderId()`，不能覆盖完整输入后直接提交。
- Fix: 增加 `activeOrderCandidateKeyword` 与 `resolveActiveOrderCandidateByKeyword()`；提交前优先复用已选候选，否则按完整输入的订单号精确匹配当前候选，未命中时即时请求候选接口并再次精确匹配，命中后才提交对应 `workOrderId`。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS，仅 CRLF working-copy 提示。
- BLOCKED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，当前 PQC 过程检验汇集选择器缺失，不属于本次活跃订单空值修复。
- BLOCKED: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，当前 PQC 组长切换后提交看板多维筛选重置链路合同失败，不属于本次活跃订单空值修复。

## E2E Verification 2026-08-06 14:58 +08:00

- Preflight: 已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md` 和 `docs/task-closeout-rules.md`。
- Runtime: `http://127.0.0.1:8081/` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`；端口 8081/48081 归属 `E:\IntRuoyi` 主工作区运行态。
- GREEN: `pnpm e2e:team-leader-workbench:real:check` -> PASS。
- BLOCKED: 注入 `TLW_FRONTEND_URL=http://127.0.0.1:8081` 与 `TLW_BACKEND_URL=http://127.0.0.1:48081` 后运行 `pnpm e2e:team-leader-workbench:real` -> non-zero，真实脚本写入 `IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json`，状态 `BLOCKED`，原因是缺少真实写入型 E2E 前置条件。
- Missing: `TLW_TENANT`、`TLW_USERNAME`、`TLW_PASSWORD`、`TLW_WORK_ORDER_ID`、`TLW_WORK_ORDER_CODE`、`TLW_TASK_ID`、`TLW_ROUTE_ID`、`TLW_ROUTE_PROCESS_ID`、`TLW_PROCESS_ID`、`TLW_ITEM_ID`、`TLW_EMPLOYEE_PROFILE_ID`、`TLW_DEVICE_ID`、`TLW_RECORDBOOK_ID`、`TLW_SIGNATURE_ID`、`TLW_SIGNATURE_EMPLOYEE_ID`、`TLW_APPROVE_USER_ID`、`TLW_FEEDBACK_CODE`、`TLW_FEEDBACK_TYPE`。
- Impact: 本次没有进入新增活跃订单写入路径，未使用 mock、自由输入、隐藏字段、API-only 或 admin 基线数据替代。

## Blockers

- 缺少写入型真实 E2E 前置：`TLW_TENANT`、`TLW_USERNAME`、`TLW_PASSWORD`、`TLW_WORK_ORDER_ID`、`TLW_WORK_ORDER_CODE`、`TLW_TASK_ID`、`TLW_ROUTE_ID`、`TLW_ROUTE_PROCESS_ID`、`TLW_PROCESS_ID`、`TLW_ITEM_ID`、`TLW_EMPLOYEE_PROFILE_ID`、`TLW_DEVICE_ID`、`TLW_RECORDBOOK_ID`、`TLW_SIGNATURE_ID`、`TLW_SIGNATURE_EMPLOYEE_ID`、`TLW_APPROVE_USER_ID`、`TLW_FEEDBACK_CODE`、`TLW_FEEDBACK_TYPE`。
- 当前全量前端门禁还受并行 PQC 列表改动阻塞：`role-requirement-matrix-preflight-static.spec.cjs` 和 `mes-process-pool-team-leader-static.spec.js` 均失败在 PQC 选择器/重置链路缺失。
- Impact: 未执行写入型真实新增/移出/填报闭环 E2E；未使用 mock、自由输入、隐藏字段、API-only 或 admin 基线数据替代。
- Because required real E2E is blocked, task status is `blocked`; no cleanup apply, implementation commit, closeout commit, or push is performed.

## Closeout

- Current status: `blocked`。
- `task-closeout-cleanup` preview/apply not run in this continuation because required write-type real E2E is blocked.
- Evidence validators and `git diff --check` have passed; cleanup/apply/commit/push remain blocked until the required write-type real E2E fixture is injected and passes.
- Current Git closeout note: `.git/index.lock` remains present as a 0-byte lock because an external Codex desktop background Git status process is active; future index-write work must rerun the stale-lock gate before add/commit/merge.
- Experience consolidation: merged the reusable static-contract lesson into `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁` and updated `docs/experience-index.md`; no new long-term document was created.
