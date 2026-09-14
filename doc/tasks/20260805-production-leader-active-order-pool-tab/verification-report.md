# Verification Report

## Result

本轮已修复“加入活跃订单池提示 `请求参数不正确:不能为null`”回归，并补齐截图中的“只输入完整订单号但未点候选”路径：提交前会按 `workOrderCode` 精确解析候选，命中后才发送 `workOrderId`。活跃订单聚焦静态合同、`pnpm ts:check` 和目标 `git diff --check` 已通过；2026-08-06 17:18 使用 `芋道源码/admin` 真实页面复跑生产组长页签，新增请求体为 `{"workOrderId":925868}`，旧 null 参数错误未再出现。2026-08-06 19:04 又修复 `process-config/list` 地址不存在运行态回归：刷新后的本机 `48081` 新 Jar 已加载当前 MES 模块，只读 Playwright 登录并点击“工序配置”后目标接口 HTTP 200、业务码 0。2026-08-06 20:31 补齐候选下拉体验：后端候选接口只读评估新增前置、返回 `eligible/ineligibleReason` 并将可加入候选排在最前，前端用绿色“符合要求”标识可加入候选。2026-08-06 21:36 修复候选 eligibility N+1 查询风险并刷新本机运行态，真实页面输入 `88` 后候选接口 HTTP 200、业务码 0、约 3.2 秒返回 20 条且 `loadingCount=0`。本机仍无完整 QA 规程覆盖的可新增候选，真实新增成功被正式 PQC 前置条件阻塞，任务不得标记 completed。

2026-08-06 追加完成截图红框位置展示：生产组长各模块页签右侧显示当前组长负责的工艺路线名称，名称只来自正式 `processConfigRows.routeName` 并去重，不使用 `formBindings`、活跃订单、路线编码或路线 ID 推断。

## Acceptance

- AC1 PASS：新增活跃订单弹窗只保留“订单号”远程可搜索 `el-select`，候选展示生产工单编号并绑定 `workOrderId`。
- AC2 PASS：前端 `addTeamLeaderActiveOrder` 请求类型和提交 payload 只包含 `workOrderId`。
- AC3 PASS：后端 `POST /active-order/add` 只接收 `workOrderId`，并从唯一有效排产解析正式 `routeId` 和 `routeVersionId`。
- AC4 PASS：候选接口 `GET /active-order/candidates?keyword=...` 使用维护权限并返回已确认生产工单候选。
- AC4.1 PASS：候选接口返回 `eligible/ineligibleReason`，按完整新增前置只读评估可加入性，并将 `eligible=true` 候选稳定排在最前。
- AC5 PASS：无有效排产、多条有效排产或排产缺正式路线/版本时后端 fail fast，不创建活跃订单、工序快照或 PQC 任务。
- AC6 PASS：调拨关联输入已从新增动作拆除；既有调拨追溯仍为只读展示。
- AC7 PASS：未点击真实订单号候选、清空、搜索失败或候选刷新失配时，前端抛 `请选择订单号` 并阻止 `/active-order/add` 写请求。
- AC8 PASS：只输入完整订单号且精确命中候选时，前端提交前自动解析 `workOrderId`，避免后端收到 `null`。
- AC9 PASS：生产组长“工序配置”页签对应 `/process-config/list` 已在本机 `48081` 运行态加载，真实页面路径返回 HTTP 200、业务码 0，不再提示“请求地址不存在”。
- AC9.1 PASS：订单号下拉对 `eligible=true` 候选显示绿色“符合要求”标记，对 `eligible=false` 候选保留原因文本。
- AC9.2 PASS：生产组长页签栏右侧展示负责工艺路线名称，路线名称来自正式 `/process-config/list` 的 `routeName`，并按名称去重。
- AC10 BLOCKED：完整真实新增成功要求本机存在已确认工单、唯一有效排产、启用工序、计划日期和已发布 QA 规程完整覆盖；当前只读统计显示满足全部条件的候选数为 0。

## Verification

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，旧实现缺少候选 change/clear 事件和候选级提交校验。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，旧实现提交仍直接读取 `activeOrderForm.workOrderId`。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，第一轮修复仍缺少按完整订单号精确解析候选的提交前路径。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，第一轮修复仍要求点击候选，不能覆盖截图中的完整输入直接提交。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS，仅 CRLF working-copy 提示。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，候选下拉缺少 eligibility option 模板和绿色“符合要求”标识。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，页面缺少 `team-leader-workbench__active-order-candidate` 候选状态标记。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，候选 BO/VO 缺少 `eligible` / `ineligibleReason` 字段。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 29, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <本轮活跃订单候选相关文件>` -> PASS，仅 CRLF working-copy 提示。
- RUNTIME NOTE: 当前 `48081` 仍运行 `backend-runtime-process-config-list-autowired-20260806-183405.jar`；本轮后端代码尚未加载到本机运行 Jar，因停止/重启进程命令被执行策略拒绝，未继续强行刷新运行态。
- PRIOR PASS: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- PRIOR PASS: `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js` -> PASS。
- PRIOR PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 25, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，当前缺少 PQC 过程检验汇集稳定选择器 `data-pqc-process-inspection-aggregation`。
- BLOCKED: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，当前 PQC 组长切换后提交看板多维筛选重置链路合同失败。
- GREEN: `pnpm e2e:team-leader-workbench:real:check` -> PASS。
- BLOCKED: 注入 `TLW_FRONTEND_URL=http://127.0.0.1:8081` 与 `TLW_BACKEND_URL=http://127.0.0.1:48081` 后运行 `pnpm e2e:team-leader-workbench:real` -> non-zero，`IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json` 记录 `status=BLOCKED`，原因是缺少真实写入型 E2E 前置条件。
- REAL E2E BLOCKED: `ACTIVE_ORDER_E2E_BASE_URL=http://127.0.0.1:8081 ACTIVE_ORDER_E2E_WORK_ORDER_CODE=881MO093613 node tests/e2e/production-leader-active-order-focused.e2e.js` -> non-zero；Playwright 真实登录 `芋道源码/admin`，进入生产组长页签，远程下拉选择候选 `881MO093613` 后提交。
- PAYLOAD PASS: `IntRuoyiFronted/test-results/production-leader-active-order-focused/result.json` 记录候选 `{workOrderId: 925868, workOrderCode: "881MO093613"}`，新增请求体仅为 `{"workOrderId":925868}`，无 `routeId`、`routeVersionId`、`transferIds` 或 null `workOrderId`。
- BUSINESS BLOCKED: 聚焦 E2E 进入后端正式服务后返回业务码 `1040506111`，消息为 `PQC 检验任务生成前置条件不满足：缺少已发布QA规程，activeOrderId=32，routeProcessId=926632，processId=922917`。
- DB ROLLBACK PASS: 只读核验 `mes_pro_process_pool_active_order`、`mes_pro_process_pool_active_order_process_snapshot`、`mes_pqc_inspection_task` 对 `activeOrderId IN (31,32)` 与 `workOrderId=925868` 的残留计数均为 0。
- CANDIDATE INVENTORY BLOCKED: 只读 DB 统计当前本机已确认工单 4,338 条、唯一有效排产 55 条、路线信息完整 55 条，但完整 QA 规程覆盖的可新增候选 0 条。
- FOLLOW-UP BUSINESS BLOCKED: 用户再次复现 `activeOrderId=33` 同一 PQC 前置错误；只读 DB 确认 `activeOrderId=33` 未残留，目标候选仍是 `workOrderId=925868` / `workOrderCode=881MO093613`。
- FOLLOW-UP DATA CHECK: 该候选唯一排产为 `scheduleOrderId=131` / `SCH-881MO093613-20260707-0001`，路线 `routeId=900026`、`routeVersionId=4`；失败工序为 `routeProcessId=926632`、`processId=922917`、`processCode=Z2630`、`processName=吹球囊成型`。
- FOLLOW-UP DATA CHECK: 当前库没有匹配 `productId=907176 + routeId=900026 + routeVersionId=4 + routeProcessId=926632 + processId=922917` 的 QA 规程，且按 `processId=922917` 查询也没有任何 QA 规程记录；该排产工序 `plan_date` 也为 `NULL`。
- RUNTIME: `http://127.0.0.1:8081/` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`；端口 8081/48081 归属 `E:\IntRuoyi` 主工作区运行态。
- PROCESS-CONFIG RED: 只读检查旧运行 Jar `backend-runtime-frontline-employee-options-login-leader-20260806-171928.jar` 内嵌 MES 模块 -> FAIL，缺少 `/process-config/list`、`getProcessConfigList` 和 `MesTeamLeaderProcessConfigRowRespVO`。
- PROCESS-CONFIG RED: 首次刷新 Jar `backend-runtime-process-config-list-20260806-181206.jar` 启动 -> FAIL，`MesTeamLeaderProcessConfigServiceImpl` 多构造器缺正式 `@Autowired`，Spring 报 `No default constructor found`。
- PROCESS-CONFIG RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest#runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，正式运行构造器缺少 `@Autowired`。
- PROCESS-CONFIG FIX: 新运行 Jar `backend-runtime-process-config-list-autowired-20260806-183405.jar` SHA256 `A5D9E29678123C398D66E812F692B337804742CF9DA11523C7EF09837179EA91`；内嵌 MES jar `compress_type=0`，`javap` 可见 `/process-config/list` 与构造器 `Autowired` 注解。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest#runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- GREEN: `node test-results/process-config-route-focused/process-config-route-focused.e2e.cjs` -> PASS；结果 `IntRuoyiFronted/test-results/process-config-route-focused/result.json` 显示 `httpStatus=200`、`businessCode=0`、`pageErrors=[]`、`consoleErrors=[]`。
- RUNTIME GREEN: `http://127.0.0.1:48081/actuator/health` -> `UP`；PID `2548` 运行新 Jar `backend-runtime-process-config-list-autowired-20260806-183405.jar`。
- LOADING RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，候选搜索缺少批量 QA 项目 mapper，不能证明远程下拉不会因逐候选逐工序查询持续 loading。
- LOADING FIX: `searchActiveOrderCandidates` 改为批量读取候选有效排产、排产工序、产品级 QA 规程、发布版本和规程项目后在内存判定 `eligible/ineligibleReason`；新增 `MesQaInspectionRegulationItemMapper#selectListByVersionIds`。
- LOADING GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- LOADING GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 30, Failures: 0, Errors: 0, Skipped: 0。
- LOADING GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`、`node tests/e2e/team-leader-workbench-static.spec.cjs` 和 `pnpm ts:check` -> PASS。
- LOADING RUNTIME GREEN: 本机 `48081` 已刷新至 `backend-runtime-active-order-candidate-batch-20260806-213525.jar`；2026-08-06 21:48 复核 PID `47520` health `UP`，`http://127.0.0.1:8081/` HTTP 200。
- REAL UI GREEN: Playwright 登录 `芋道源码/admin`，进入生产组长页签，打开新增活跃订单并输入 `88`；候选接口 HTTP 200、业务码 0、约 3.2 秒返回 20 条，响应含 `eligible/ineligibleReason`，下拉 `loadingCount=0`。
- ROUTE HEADER RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，生产组长模块页签栏缺少 `data-production-leader-responsible-routes`。
- ROUTE HEADER RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，页面缺少负责路线名称 header 标记。
- ROUTE HEADER GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`、`node tests/e2e/team-leader-workbench-static.spec.cjs` 和 `pnpm ts:check` -> PASS。
- ROUTE HEADER GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs doc/tasks/20260805-production-leader-active-order-pool-tab/task.md doc/tasks/20260805-production-leader-active-order-pool-tab/execution-log.md` -> PASS，仅 CRLF working-copy 提示。

## Evidence Validators

- `validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md` -> PASS。
- `validate_backend_api.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md` -> PASS。
- `validate_change_request.py --evidence docs/changes/20260806-active-order-code-input.md` -> PRIOR PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/bug-regression-evidence.md` -> PASS。

## Blockers

- Missing `TLW_TENANT`, `TLW_USERNAME`, `TLW_PASSWORD`, `TLW_WORK_ORDER_ID`, `TLW_WORK_ORDER_CODE`, `TLW_TASK_ID`, `TLW_ROUTE_ID`, `TLW_ROUTE_PROCESS_ID`, `TLW_PROCESS_ID`, `TLW_ITEM_ID`, `TLW_EMPLOYEE_PROFILE_ID`, `TLW_DEVICE_ID`, `TLW_RECORDBOOK_ID`, `TLW_SIGNATURE_ID`, `TLW_SIGNATURE_EMPLOYEE_ID`, `TLW_APPROVE_USER_ID`, `TLW_FEEDBACK_CODE`, and `TLW_FEEDBACK_TYPE`。
- 当前本机没有一条满足完整新增成功前置的正式候选；不得通过 SQL、隐藏字段、mock、自由输入或 API-only 写入补齐 QA 规程/排产数据来冒充 E2E PASS。
- 当前复现候选 `881MO093613` 的正式修复前置是：先为产品 `907176`、路线 `900026`、版本 `4`、路线工序 `926632`、工序 `922917/吹球囊成型` 配置并发布 QA 规程；随后补齐该排产工序计划日期，再重新走真实页面加入。
- Current frontend full-gate blockers: `role-requirement-matrix-preflight-static.spec.cjs`、`mes-process-pool-team-leader-static.spec.js` 均失败在并行 PQC 列表选择器/重置链路缺失，不属于本次活跃订单空值回归。
- Impact: 未执行写入型真实新增/移出/填报闭环 E2E；未使用 mock、自由输入、隐藏字段、API-only 或 admin 基线数据替代。
- Closeout: 因必需真实 E2E 和当前全量前端门禁阻塞，当前不运行 cleanup apply，不创建实现提交、收尾提交，也不推送 `int_main`。

## Press Balloon Route Copy

- GREEN: Formal route copy API returned `targetRouteId=980091` for `RT000028-IDI / 按压式球囊扩充压力泵`.
- GREEN: Product convergence transaction returned `target_version_id=622`, `target_item_count=3`, `final_target_product_bindings=3`, `final_old_product_bindings=0`.
- GREEN: Verification SQL confirmed target route `980091` has active version `622 / V1 / ACTIVE`, copied process count `14`, copied route flow configs `2`, active schedule configs `14`, exactly 3 active target-product bindings, 0 old-product bindings, 0 product BOMs, and snapshot item IDs `[907063, 913662, 924008]`.
- INFO: The 3 target products currently have 0 production work orders and 0 schedule orders; this route/product association does not create production or schedule orders.
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`

## Press Balloon Project Code And MDM Binding

- GREEN: `tmp-bind-press-balloon-project-mdm.sql` -> PASS，返回 `target_route_id=980091`、`target_route_version_id=622`、`project_code=IDI`、`mdm_product_id=14`、`target_route_product_id=923079`、`final_target_mdm_bindings=1`、`final_non_target_mdm_bindings=0`、`final_snapshot_contains_mdm=1`。
- GREEN: `tmp-press-balloon-project-mdm-verify.sql` -> PASS，`dcc_project_code.id=129 / IDI` 绑定启用 MDM `INT-15/id=14`；旧路线 `922119` 对 `item_id=14` 无活跃绑定；目标路线 `980091` 对 `item_id=14` 有且仅有 1 条活跃绑定；目标活跃版本 `622/V1` 快照包含该 MDM 产品，目标路线活跃产品总数为 4。
- SAFETY: 本次只操作本机 tenant 1 数据，未创建生产工单、排产工单、QA 规程、PQC 任务或 mock 数据。
