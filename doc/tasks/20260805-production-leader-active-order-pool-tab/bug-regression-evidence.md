# Active Order Null WorkOrder Regression Evidence

## Bug Summary

- 用户反馈：加入活跃订单池时页面提示 `请求参数不正确:不能为null`；截图复现为用户已在“订单号”框输入完整生产工单编号 `881MO093613`，但未点击下拉候选。
- 期望行为：新增活跃订单只允许提交正式生产工单候选对应的 `workOrderId`；若用户已输入完整订单号且精确命中候选，则提交前自动解析候选；未命中、清空或搜索失败时前端提示 `请选择订单号`，不得调用 `/active-order/add`，后端不得收到 `workOrderId=null`。

## Expected

- 只输入完整订单号且精确命中候选时，前端必须解析出候选 `workOrderId` 再发起新增写请求。
- 未命中真实候选时，前端必须 fail fast，提示 `请选择订单号` 并阻止新增写请求。
- 已选择真实候选时，新增请求体只能包含该候选对应的 `workOrderId`。

## Reproduction

- RED: `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，新增弹窗缺少 `@change="handleActiveOrderCandidateChange"` 与 `@clear="handleActiveOrderCandidateClear"`，不能证明提交前绑定了真实候选。
- RED: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，提交仍直接使用 `requirePositiveNumber(activeOrderForm.workOrderId, '请选择订单号')`，缺少候选级校验函数。
- RED: `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，旧修复仍缺少 `activeOrderCandidateKeyword` 与按订单号精确解析候选的提交前路径。
- RED: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，旧修复仍要求点击候选，不能覆盖截图中的“完整输入后直接提交”路径。

## Root Cause

- 前端只校验 `activeOrderForm.workOrderId` 是正数，没有保存并校验“当前值来自远程候选列表中的真实选项”。
- 第一轮修复要求点击候选，但 Element Plus 远程下拉在用户只输入完整订单号时不会更新 `v-model`；若运行态仍走旧提交链路，就会继续把空 `workOrderId` 发到后端。
- 静态合同只覆盖 payload 字段收缩，没有锁定完整订单号输入的精确候选解析行为，因此截图路径仍可能落到后端校验层。
- 真实运行态复验还确认过一次独立部署问题：48081 曾运行旧 Jar，旧 `MesTeamLeaderActiveOrderAddReqVO` 仍包含 `routeId/routeVersionId @NotNull`，导致前端正确提交 `workOrderId` 时仍返回 `请求参数不正确:不能为null`；热替换当前 Jar 后该旧参数校验不再出现。

## Fix

- 在 `TeamLeaderWorkbenchPage.vue` 增加 `activeOrderSelectedCandidate`，通过 `@change` 记录真实候选，通过 `@clear`、空搜索、搜索失败和候选刷新失配清除选择。
- 新增 `requireSelectedActiveOrderCandidateWorkOrderId()`；提交 `/active-order/add` 前必须同时满足表单 `workOrderId`、已选候选和当前候选列表一致，否则抛出 `请选择订单号` 并阻止 API 调用。
- 增加 `activeOrderCandidateKeyword` 与 `resolveActiveOrderCandidateByKeyword()`；用户只输入完整订单号时，提交前先用当前候选精确匹配 `workOrderCode`，未命中则即时调用候选搜索接口，精确命中后才提交对应 `workOrderId`。
- 未改后端 `@NotNull workOrderId`，没有引入兜底、默认值或兼容旧字段。

## Regression Test

- 更新 `production-leader-active-order-pool-tab-static.spec.js`，要求远程下拉绑定 change/clear 事件，并要求提交走候选校验函数。
- 更新 `team-leader-workbench-static.spec.cjs`，要求 `addTeamLeaderActiveOrder` 只接收 `requireSelectedActiveOrderCandidateWorkOrderId()` 返回值，并验证该函数会在未选真实候选时抛 `请选择订单号`。
- 更新同两个静态合同，要求保存输入的订单号关键字、按 `workOrderCode` 精确匹配候选，并在提交前 `await requireSelectedActiveOrderCandidateWorkOrderId()`。

## Verification

- GREEN: `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; pnpm ts:check` -> PASS。
- GREEN: `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS，仅 CRLF working-copy 提示。
- REAL E2E BLOCKED/PAYLOAD PASS: `workdir=IntRuoyiFronted; ACTIVE_ORDER_E2E_BASE_URL=http://127.0.0.1:8081 ACTIVE_ORDER_E2E_WORK_ORDER_CODE=881MO093613 node tests/e2e/production-leader-active-order-focused.e2e.js` -> non-zero；真实页面路径选择候选 `{workOrderId: 925868, workOrderCode: "881MO093613"}`，提交请求体为 `{"workOrderId":925868}`，旧 null 参数错误未出现。
- BUSINESS BLOCKED: 同一次真实 E2E 返回业务码 `1040506111`，消息为 `PQC 检验任务生成前置条件不满足：缺少已发布QA规程，activeOrderId=32，routeProcessId=926632，processId=922917`；这是正式 PQC 前置条件失败，不是请求参数 null。
- DB ROLLBACK PASS: 只读核验活跃订单、工序快照和 PQC 任务对 `activeOrderId IN (31,32)` 与 `workOrderId=925868` 的残留计数均为 0。
- FOLLOW-UP BLOCKED: 用户再次复现 `activeOrderId=33` 同一报错；只读 DB 证明 `activeOrderId=33` 未残留，`workOrderId=925868` / `881MO093613` 的失败工序为 `routeProcessId=926632`、`processId=922917`、`processName=吹球囊成型`，当前完全匹配的已发布 QA 规程记录数为 0。
- FOLLOW-UP BLOCKED: 同一排产工序 `plan_date` 为 `NULL`；即使先补齐 QA 规程，后续仍会因排产工序缺少计划日期阻塞 PQC 任务生成。

## Process Config Route Runtime Regression

- Bug Summary: 用户反馈生产组长页面提示 `请求地址不存在:admin-api/mes/pro/process-pool/team-leader/process-config/list`。
- Expected: 登录后真实页面点击“工序配置”时，`/mes/pro/process-pool/team-leader/process-config/list` 必须由运行态 Controller 接收，返回 HTTP 200 和业务码 0，不得返回地址不存在。
- Reproduction: 旧 `48081` 运行 Jar `backend-runtime-frontline-employee-options-login-leader-20260806-171928.jar` 的内嵌 `yudao-module-mes-2026.04-SNAPSHOT.jar` 经 `javap` 检查缺少 `/process-config/list`、`getProcessConfigList` 和 `MesTeamLeaderProcessConfigRowRespVO`，而源码/target MES jar 已包含这些符号。
- Root Cause: 本机 `48081` 仍运行旧 runtime Jar，未加载当前 MES 模块；首次刷新后又暴露 `MesTeamLeaderProcessConfigServiceImpl` 多构造器缺正式 `@Autowired` 的 Spring Bean 构造器选择问题。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest#runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，正式运行构造器缺少 `@Autowired`。
- Fix: 正式 5 参数运行构造器标注 `@Autowired`，保留 6 参数 `Clock` 测试构造器；重新打包 MES 模块，并基于旧稳定运行 Jar 只替换内嵌 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar`。
- Verification: 新运行 Jar `backend-runtime-process-config-list-autowired-20260806-183405.jar` 内嵌 MES jar `compress_type=0`，`javap` 可见 `/process-config/list` 和构造器 `Autowired` 注解；`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest#runtimeConstructor_hasAutowiredAnnotationSoSpringDoesNotRequireDefaultConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `workdir=IntRuoyiFronted; node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; node test-results/process-config-route-focused/process-config-route-focused.e2e.cjs` -> PASS；真实登录 `芋道源码/admin` 后进入生产组长页面并点击“工序配置”，目标接口 HTTP 200、业务码 0、`pageErrors=[]`、`consoleErrors=[]`。

## Active Order Candidate Loading Regression

- Bug Summary: 用户反馈新增活跃订单弹窗输入订单号关键字后下拉一直显示 loading。
- Expected: 候选搜索必须在一次正式请求后结束 loading；后端不得逐候选逐工序串行查询导致远程下拉长时间挂起，搜索失败也必须明确失败而不是默认成功。
- Reproduction: 真实页面诊断复现输入 `88` 的候选请求；刷新源码版运行态后候选接口先因热替换漏 BO/VO 暴露 `NoSuchMethodError`，业务码 500，证明运行态类集合必须成组更新。
- Root Cause: eligibility 搜索路径把完整新增前置评估直接放进 20 个候选循环中，逐候选查询有效排产、逐排产查询工序、逐工序查询 QA 规程/版本/项目，刷新到运行态后容易拖住 Element Plus remote select 的 loading；热替换时只替换 service/mapper 还会让旧候选 BO builder 缺 `eligible(boolean)`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译失败，缺少批量 `MesQaInspectionRegulationItemMapper#selectListByVersionIds`，且不能证明候选搜索批量加载依赖。
- Fix: `searchActiveOrderCandidates` 先批量读取候选有效排产、排产工序、产品级 QA 规程、发布版本和规程项目，再在内存中按候选 key 判定 `eligible/ineligibleReason`；运行态第二轮热替换同时补齐 service、candidate BO、controller、resp VO 和 mapper class。
- Regression Test: 新增 `MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading`，断言候选搜索只调用批量排产、批量工序、产品级 QA 规程、批量版本和批量项目查询，并禁止回退到逐候选/逐版本查询。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 30, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- Runtime Verification: `48081` 已运行 `backend-runtime-active-order-candidate-batch-20260806-213525.jar`，health `UP`；真实页面输入 `88` 后候选接口 HTTP 200、业务码 0、约 3.2 秒返回 20 条，响应含 `eligible/ineligibleReason`，下拉 `loadingCount=0`。
- Risk: 当前库没有 `eligible=true` 的完整候选，真实页面只能证明 loading 结束和不符合原因展示；完整新增成功仍被正式 QA 规程/排产计划日期前置阻塞。

## Blockers And Follow-Up

- BLOCKED: 当前本机可新增候选库存不足；只读 DB 统计已确认工单 4,338 条、唯一有效排产 55 条、完整 QA 规程覆盖可新增候选 0 条。
- BLOCKED: 当前用户复现候选 `881MO093613` 缺少匹配 `productId=907176 + routeId=900026 + routeVersionId=4 + routeProcessId=926632 + processId=922917` 的已发布 QA 规程，且该排产工序计划日期为空。
- BLOCKED: `workdir=IntRuoyiFronted; node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，当前缺少 PQC 过程检验汇集稳定选择器 `data-pqc-process-inspection-aggregation`，不属于本次活跃订单空值修复。
- BLOCKED: `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，当前 PQC 组长切换后提交看板多维筛选重置链路合同失败，不属于本次活跃订单空值修复。
- BLOCKED: 写入型真实 Playwright E2E 仍缺少任务自有 `TLW_*` 测试租户、账号、工单、工序、设备和签名夹具；未使用 mock、自由输入、隐藏字段或 API-only 替代。
