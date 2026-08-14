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
- BDD: 候选下拉符合要求优先并绿色展示 -> Given 候选接口同时返回可加入和暂不可加入的已确认生产工单，When 生产组长输入订单号打开远程下拉，Then 可加入候选排在最前面并显示绿色“符合要求”标记，不可加入候选保留原因用于辨识。
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

## Focused Admin E2E 2026-08-06 17:18 +08:00

- User scope adjustment: 用户明确要求按实际页面路径验证：“登录admin账号，在生产组长页签里面找到一个生产订单，点击加入”。
- Preflight: 已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/database-rules.md`、`docs/powershell-encoding.md` 和 `docs/task-closeout-rules.md`；`npx` 可用，前端 8081 HTTP 200，后端 48081 health `UP`。
- Runtime refresh evidence: 48081 运行 Jar 已热替换到 `backend-runtime-frontline-employee-options-active-order-code-input-20260806-1638.jar`；聚焦 E2E 请求不再触发旧 `routeId/routeVersionId @NotNull` 参数错误。
- Command: `workdir=IntRuoyiFronted; ACTIVE_ORDER_E2E_BASE_URL=http://127.0.0.1:8081 ACTIVE_ORDER_E2E_WORK_ORDER_CODE=881MO093613 node tests/e2e/production-leader-active-order-focused.e2e.js` -> non-zero。
- REAL E2E BLOCKED: Playwright 使用 `芋道源码/admin` 登录，进入 `/mes/pro/process-pool/production-leader`，打开“活跃订单池”，点击“新增活跃订单”，远程下拉选择候选 `881MO093613`。
- Payload evidence: `IntRuoyiFronted/test-results/production-leader-active-order-focused/result.json` 记录候选 `{workOrderId: 925868, workOrderCode: "881MO093613"}`，新增请求体为 `{"workOrderId":925868}`，请求体字段集合仅包含 `workOrderId`。
- Backend response: `/mes/pro/process-pool/team-leader/active-order/add` 返回业务码 `1040506111`，消息为 `PQC 检验任务生成前置条件不满足：缺少已发布QA规程，activeOrderId=32，routeProcessId=926632，processId=922917`。
- Rollback verification: 只读 DB 核验 `mes_pro_process_pool_active_order`、`mes_pro_process_pool_active_order_process_snapshot`、`mes_pqc_inspection_task` 对 `activeOrderId IN (31,32)` 与 `workOrderId=925868` 的残留计数均为 0。
- Candidate inventory: 只读 DB 统计当前本机 `status=1` 已确认工单 4,338 条，其中唯一有效排产 55 条、存在启用排产工序 55 条、路线信息完整 55 条，但满足完整 QA 规程前置的可新增候选为 0 条。
- Existing-active check: 当前唯一活跃订单 `PQC-E2E-FS-20260804` 对应工单 `980019` 已确认，但没有有效排产记录；加入接口会先执行唯一有效排产校验，不能作为完整新增 PASS 候选复用。
- Impact: “请求参数不正确:不能为null”回归已通过真实页面路径排除；完整新增成功仍被本机正式 QA 规程/排产数据前置阻塞。未通过 SQL、隐藏字段、mock、自由输入或 API-only 写入补数据。

## Follow-up Admin Add Blocker 2026-08-06 PQC Regulation

- User report: 加入订单池时提示 `PQC 检验任务生成前置条件不满足：缺少已发布QA规程，activeOrderId=33，routeProcessId=926632，processId=922917`。
- Read-only DB check: 目标生产工单仍为 `workOrderId=925868` / `workOrderCode=881MO093613`；唯一排产工单为 `scheduleOrderId=131` / `scheduleOrderCode=SCH-881MO093613-20260707-0001`，路线为 `routeId=900026`、`routeVersionId=4`。
- Read-only DB check: 失败工序为 `routeProcessId=926632`、`processId=922917`、`processCode=Z2630`、`processName=吹球囊成型`，产品为 `productId=907176`。
- Read-only DB check: `mes_qa_inspection_regulation` 中不存在完全匹配 `productId=907176 + routeId=900026 + routeVersionId=4 + routeProcessId=926632 + processId=922917` 的 QA 规程；按 `processId=922917` 查询也无任何规程记录。
- Additional blocker: 同一排产工序 `plan_date` 为 `NULL`；即使补齐已发布 QA 规程，后续 PQC 任务生成仍会触发“排产工序缺少计划日期”门禁。
- Rollback verification: `activeOrderId=33` 在 `mes_pro_process_pool_active_order` 中残留行数为 0，说明当前失败仍在事务内回滚，未遗留活跃订单。
- Conclusion: 该报错是正式 PQC 前置数据缺失，不是空参数或接口地址问题；根据项目 PQC 门禁不能默认跳过 QA 规程或用 SQL/mock 补数据冒充新增成功。

## Bug Regression 2026-08-06 Process Config Route Runtime

- User report: 生产组长页面提示 `请求地址不存在:admin-api/mes/pro/process-pool/team-leader/process-config/list`。
- BDD: 工序配置页签加载统一表 -> Given 本机 `int_main` 前后端运行态可用且用户以 `芋道源码/admin` 登录，When 打开生产组长页面并点击“工序配置”，Then `/mes/pro/process-pool/team-leader/process-config/list` 必须进入真实后端 Controller，返回 HTTP 200 和业务码 0，不得再返回地址不存在。
- RED: 只读检查旧运行 Jar `backend-runtime-frontline-employee-options-login-leader-20260806-171928.jar` 的内嵌 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` -> FAIL，`javap` 未发现 `/process-config/list`、`getProcessConfigList` 或 `MesTeamLeaderProcessConfigRowRespVO`，但源码和 target MES jar 已包含这些类/常量。
- RED: 第一次刷新运行 Jar `backend-runtime-process-config-list-20260806-181206.jar` 后启动 `48081` -> FAIL，Spring 启动失败：`MesTeamLeaderProcessConfigServiceImpl` 存在多个构造器但正式运行构造器未显式 `@Autowired`，Spring 查找默认构造器并报 `No default constructor found`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest#runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，断言正式运行构造器缺少 `@Autowired`。
- Fix: 锁定正式 5 参数运行构造器为 `@Autowired`，保留 6 参数 `Clock` 测试构造器；重新 `mvn -pl yudao-module-mes -am -DskipTests package` 构建 MES 模块，并基于旧稳定运行 Jar 只替换内嵌 `yudao-module-mes-2026.04-SNAPSHOT.jar`。
- Runtime: 新运行 Jar `backend-runtime-process-config-list-autowired-20260806-183405.jar` SHA256 `A5D9E29678123C398D66E812F692B337804742CF9DA11523C7EF09837179EA91`；内嵌 MES jar `compress_type=0`，`javap` 可见 `/process-config/list`、`getProcessConfigList` 和构造器 `RuntimeVisibleAnnotations: Autowired`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest#runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- Runtime GREEN: `http://127.0.0.1:48081/actuator/health` -> `UP`；监听 PID `2548` 命令行归属新运行 Jar。
- REAL E2E GREEN: `node test-results/process-config-route-focused/process-config-route-focused.e2e.cjs` -> PASS；Playwright 登录 `芋道源码/admin`，进入 `/mes/pro/process-pool/production-leader`，点击“工序配置”，目标接口 HTTP 200、业务码 `0`、`pageErrors=[]`、`consoleErrors=[]`，结果写入 `IntRuoyiFronted/test-results/process-config-route-focused/result.json`。
- Impact: 本次没有新增 fallback、mock endpoint、隐藏错误或切换端口；问题根因是本机 `48081` 运行 Jar 未加载当前 MES 模块，刷新后同页旧 `process-config/list` 地址不存在问题已消除。

## Candidate Eligibility Dropdown 2026-08-06

- User request: 订单号下拉时，符合要求的候选列在最前面，并用绿色表明。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，候选下拉缺少自定义 option 模板、`team-leader-workbench__active-order-candidate` 和绿色“符合要求”标识。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，页面缺少候选 eligibility 状态标记。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，候选 BO/VO 缺少 `eligible` / `ineligibleReason` 字段。
- Fix: 后端候选搜索对最多 20 个已确认工单执行只读新增前置评估，返回 `eligible/ineligibleReason`，并用稳定排序将 `eligible=true` 候选排在前；前端 `el-option` 渲染候选编号、绿色“符合要求”徽标或不符合原因。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 29, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <本轮活跃订单候选相关文件>` -> PASS，仅 CRLF working-copy 提示。
- Impact: 候选搜索不创建活跃订单、工序快照、PQC 任务或审计记录；完整新增成功仍受当前本机缺少完整 QA 规程覆盖候选阻塞。
- RUNTIME NOTE: 当前 `48081` 仍运行 `backend-runtime-process-config-list-autowired-20260806-183405.jar`；本轮尝试生成热替换运行 Jar 时，停止/重启进程命令被执行策略拒绝，因此未刷新本机运行态。页面要看到 `eligible/ineligibleReason` 需要后续按本地运行态门禁授权重启后端。

## Bug Regression 2026-08-06 Active Order Candidate Loading

- User report: 新增活跃订单弹窗输入 `88` 后候选下拉一直显示 loading。
- BDD: 候选搜索 loading 必须结束 -> Given 生产组长打开“新增活跃订单”并输入订单号关键字 `88`，When 前端调用 `/active-order/candidates`，Then 后端必须批量只读评估最多 20 个候选并返回业务码 0，前端 loading 必须在响应后结束，不得因逐候选逐工序查询长时间挂起。
- Reproduction: 2026-08-06 21:10 真实页面诊断显示旧运行态候选接口约 3.5 秒返回且 `loadingCount=0`，但源码版 eligibility 评估仍存在逐候选查询排产、逐排产查询工序、逐工序查询 QA 规程/版本/项目的 N+1 风险；刷新运行态后先暴露漏热替换 BO/VO 的 `NoSuchMethodError`，候选接口业务码 500。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译失败，`MesQaInspectionRegulationItemMapper` 缺少批量 `selectListByVersionIds`，且候选服务仍不能证明批量加载依赖。
- Fix: 候选搜索先批量读取 20 个候选的有效排产、排产工序、产品级 QA 规程、规程发布版本和规程项目，再按内存 key 判定 `eligible/ineligibleReason`；新增 `selectListByVersionIds`，保留正式新增接口的 fail-fast 写入前置，不引入 fallback 或默认成功。
- Runtime fix: 第一轮热替换 `backend-runtime-active-order-candidate-batch-20260806-212939.jar` 漏掉候选 BO/VO/Controller class，运行态报 `NoSuchMethodError: MesTeamLeaderActiveOrderCandidateBOBuilder.eligible(boolean)`；第二轮补齐 `MesTeamLeaderActiveOrderCandidateBO*`、`MesTeamLeaderActiveOrderCandidateRespVO*` 和 `MesProcessPoolTeamLeaderController*` class 后启动 `backend-runtime-active-order-candidate-batch-20260806-213525.jar`，health `UP`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 30, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/qa/regulation/MesQaInspectionRegulationItemMapper.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceTest.java` -> PASS，仅 CRLF working-copy 提示。
- REAL UI GREEN: Playwright 登录 `芋道源码/admin`，进入 `/mes/pro/process-pool/production-leader`，打开“活跃订单池”新增弹窗并输入 `88`；候选接口 HTTP 200、业务码 0、约 3.2 秒返回 20 条，首条为 `881MO093613` 且包含 `eligible=false/ineligibleReason=缺少已发布QA规程`，下拉 `loadingCount=0`。
- Cleanup note: 任务自有热替换脚本已删除；两个运行目录下的解包临时目录因 `Remove-Item` 命令被桌面策略拦截，留待正式 task-closeout cleanup 处理。当前运行 Jar 保留。

## Validation Sweep 2026-08-06 21:48

- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/bug-regression-evidence.md` -> PASS，`Bug regression evidence is valid.`
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md` -> PASS，`Backend API evidence is valid.`
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md` -> PASS，`Frontend feature evidence is valid.`
- GREEN: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/qa/regulation/MesQaInspectionRegulationItemMapper.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceTest.java doc/tasks/20260805-production-leader-active-order-pool-tab/verification-report.md doc/tasks/20260805-production-leader-active-order-pool-tab/bug-regression-evidence.md doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md` -> PASS，仅 CRLF working-copy 提示。
- Runtime recheck: `http://127.0.0.1:8081/` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`；监听 PID `47520` 命令行归属 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-active-order-candidate-batch-20260806-213525.jar`。
- Impact: 下拉一直 loading 的源码和本机运行态均已闭环；完整新增成功仍因正式 QA 规程/排产计划日期数据前置缺失阻塞，不能用 mock、SQL 补数或 API-only 替代。

## Route Header 2026-08-06

- User request: 将生产组长负责的工艺路线的名称显示在截图红框位置，即生产组长页签栏右侧空白区域。
- BDD: 生产组长查看负责路线名称 -> Given 页面已通过正式 `/process-config/list` 加载当前生产组长负责的路线工序配置，When 任一生产组长模块页签栏渲染，Then 页签右侧显示去重后的 `routeName` 列表，不使用 `formBindings`、活跃订单、路线编码或路线 ID 推断路线名称。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，`data-production-leader-responsible-routes` 数量为 0，缺少页签右侧负责路线名称区域。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，页面缺少 `data-production-leader-responsible-routes` 标记。
- Fix: 在每个生产组长模块页签栏右侧增加 `data-production-leader-responsible-routes` 区域；新增 `productionResponsibleRouteNames` computed，仅从 `processConfigRows.value[].routeName` 读取、去重并显示为标签。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs doc/tasks/20260805-production-leader-active-order-pool-tab/task.md doc/tasks/20260805-production-leader-active-order-pool-tab/execution-log.md` -> PASS，仅 CRLF working-copy 提示。
- Experience consolidation: 已按 `project-experience-consolidation` 检索 `docs` 和 `AGENTS.md`；既有 `AGENTS.md#工艺路线三类配置术语契约` 与 `docs/backend-development.md#mes-生产人员档案正式工重复关联门禁` 已覆盖不得用 `formBindings`、活跃订单或替代来源推断生产组长负责路线，本次仅为一次性 UI 展示实现，无需新增长期经验文档。

## Data Change 2026-08-06 Press Balloon Route Copy

- User request: 复制 `球囊扩张压力泵`，将复制后的对象重命名为 `按压式球囊扩充压力泵`，并关联所有 `按压式球囊扩充压力泵` 产品。
- Interpretation: 按工艺路线复制处理；源路线为 tenant 1 正式路线 `routeId=922119` / `RT000028` / `球囊扩张压力泵`，目标路线编码采用未占用的 `RT000028-IDI`，目标路线名称为 `按压式球囊扩充压力泵`。
- BDD: 按压式球囊路线复制与产品关联 -> Given tenant 1 已存在源路线 `球囊扩张压力泵` 和 3 个同名目标产品 When 调用正式路线复制并收敛目标路线产品关联 Then 新路线保留源路线工序/流程/配置，且只关联 3 个 `按压式球囊扩充压力泵` 产品。
- Precheck: `mes_pro_route` 中不存在 `code='RT000028-IDI'`，不存在 `name='按压式球囊扩充压力泵'` 的路线。
- Precheck: 目标产品为 `907063/YXN.002.006.1003/INT-ID-233`、`913662/YXN.002.006.1001/INT-ID-243`、`924008/IDI`；当前 `mes_pro_route_product` 关联数均为 0。
- Gate: 本次只操作本机 tenant 1 数据，不修改远端；不创建 mock 订单、不默认补排产、不跳过 QA 规程或 PQC 前置。

## Blockers

- 当前本机无可用于完整新增 PASS 的正式候选：已确认 + 唯一有效排产 + 启用工序 + 计划日期 + 已发布 QA 规程 + 发布版本末检适用性 + 首检/巡检/末检规则同时满足的候选数为 0。
- 当前用户复现候选 `881MO093613` 的首个阻塞为 `吹球囊成型` 工序缺少匹配已发布 QA 规程；补齐 QA 后还需补齐该排产工序计划日期，否则仍无法生成 PQC 任务。
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

## Data Change 2026-08-06 Press Balloon Route Copy Completed

- Runtime: `48081` was initially down, so the first Node API attempt failed at network layer with `ECONNREFUSED` and performed no write. A later existing `int_main` backend PID `23164` under `E:\IntRuoyi\output\runtime\int_main\backend-runtime-latest-process-config-admin-auth-20260806-224302.jar` was confirmed `UP` before retry.
- RED: Initial precheck SQL without explicit collation -> FAIL with `ERROR 1270 Illegal mix of collations`; fixed by comparing hex Chinese constants with `COLLATE utf8mb4_unicode_ci`.
- GREEN: Formal API `POST /admin-api/mes/pro/route/copy` copied source route `922119 / RT000028 / 球囊扩张压力泵` to target route `980091 / RT000028-IDI / 按压式球囊扩充压力泵`.
- GREEN: Guarded DB transaction converged target route products: target active version `622`, target item count `3`, final target bindings `3`, final old-product bindings `0`.
- Verification: New route `980091` has active version `622 / V1 / ACTIVE`, copied process count `14`, copied route flow config count `2`, active schedule config count `14`, active product bindings `923072/907063`, `923073/913662`, and `923074/924008`, snapshot product item IDs `[907063, 913662, 924008]`, and active product BOM count `0`.
- Read-only order check: target products `907063`, `913662`, and `924008` currently have `0` production work orders and `0` schedule orders, so no active-order candidate appears solely from this route/product association.
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`

## Data Change 2026-08-07 Press Balloon Project Code And MDM Binding

- User request: 将 `按压式球囊扩充压力泵` 工艺路线绑定对应的项目代码和 MDM。
- BDD: 按压式球囊项目代码与 MDM 绑定到新路线 -> Given tenant 1 已存在目标路线 `980091 / RT000028-IDI`、DCC 项目代码 `IDI` 和启用 MDM 产品 `INT-15/id=14`，When 收敛路线产品绑定，Then `item_id=14` 只在目标路线活跃、DCC 项目代码继续指向 MDM 产品，目标活跃版本快照包含该 MDM 产品。
- Precheck: `dcc_project_code` 字段包含 `product_master_id`，`mdm_product` 包含 `id/product_code/name_cn/status`，`mes_pro_route_product.item_id` 为工艺路线产品绑定字段。
- Precheck: `dcc_project_code.id=129` / `project_code=IDI` 已绑定 `product_master_id=14`，对应 `mdm_product.id=14` / `product_code=INT-15` / `name_cn=按压式球囊扩充压力泵` / `status=ENABLE`。
- Precheck: 目标路线 `980091` 已活跃绑定 3 个同名 MES 物料产品；但 MDM 产品 `item_id=14` 活跃绑定仍在旧路线 `922119`，目标路线同一绑定 `923071` 为软删除状态。
- RED: `tmp-bind-press-balloon-project-mdm.sql` first run -> FAIL，`ERROR 1267 Illegal mix of collations`，事务未提交；随后按目标列 `utf8mb4_unicode_ci` 显式声明过程变量排序规则。
- GREEN: `tmp-bind-press-balloon-project-mdm.sql` -> PASS，返回 `target_route_id=980091`、`target_route_version_id=622`、`project_code=IDI`、`mdm_product_id=14`、`target_route_product_id=923079`、`final_target_mdm_bindings=1`、`final_non_target_mdm_bindings=0`、`final_snapshot_contains_mdm=1`。
- Verification: `tmp-press-balloon-project-mdm-verify.sql` -> PASS，DCC 项目代码 `IDI` 绑定 MDM `INT-15/id=14`；旧路线 `922119` 对 `item_id=14` 无活跃绑定；目标路线 `980091` 对 `item_id=14` 有且仅有 1 条活跃绑定；目标活跃版本 `622/V1` 快照包含 MDM 产品，目标路线活跃产品总数为 4。
- Safety: 本次只操作本机 tenant 1；未创建生产工单、排产工单、QA 规程、PQC 任务或 mock 数据；未使用空 `productMasterId`、默认项目代码、前端 payload 或隐藏字段推断绑定。
