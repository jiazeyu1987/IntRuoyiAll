# Backend API Evidence

## Scope

- Endpoint scope: `GET /mes/pro/process-pool/team-leader/active-order/candidates` and `POST /mes/pro/process-pool/team-leader/active-order/add`.
- Service scope: `MesTeamLeaderActiveOrderServiceImpl` candidate search, workOrderId-only add, unique effective schedule resolution, active/reactivate/idempotent behavior, process snapshot creation, and PQC task creation.

## Contract

- Candidate API uses `mes:pro-process-pool-team-leader:maintain` and returns at most 20 confirmed production work orders as `workOrderId`, `workOrderCode`, `eligible`, and `ineligibleReason`.
- Candidate API evaluates the same read-only add prerequisites used by the formal add flow, sorts eligible candidates first, and never creates active orders, process snapshots, PQC tasks, or audit rows during search.
- Add API accepts only required `workOrderId`; client-supplied `routeId`, `routeVersionId`, and `transferIds` are not part of the request VO/BO.
- Backend validates the confirmed production work order, resolves exactly one effective schedule order, and derives formal `routeId` / `routeVersionId` from that schedule.
- Validation errors fail fast with `PRO_PROCESS_POOL_ACTIVE_ORDER_EFFECTIVE_SCHEDULE_UNIQUE_REQUIRED` or `PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED`; failures do not create active orders, snapshots, or PQC tasks.

## Validation

- Auth: both active-order add and candidate search stay on maintain permission.
- Data: no migration required; transfer trace historical/read-only data remains on existing endpoints.
- Errors: no fallback, no default route, no client route comparison path, no swallowed missing schedule/route failures.

## BDD

- BDD: 按订单号加入活跃订单 -> Given 已确认生产工单存在且仅有一条有效排产 When 生产组长选择订单号候选并提交 Then 后端只接收 `workOrderId`、解析正式路线并加入活跃订单。
- BDD: 排产或路线缺失阻塞 -> Given 工单未确认、无有效排产、多条有效排产或排产缺路线/版本 When 调用新增接口 Then 返回明确业务错误且不写入业务数据。
- BDD: 候选符合要求优先 -> Given 候选搜索同时命中满足和不满足新增前置的工单 When 调用候选接口 Then `eligible=true` 的候选排在最前，`eligible=false` 的候选返回首个明确原因且无业务写入。
- BDD: 候选搜索及时结束 -> Given 订单号关键字可匹配多条已确认工单 When 调用候选接口 Then 后端批量读取排产、工序和 QA 前置数据并返回候选结果，不得逐候选逐工序串行查询导致远程下拉 loading 长时间不结束。

## RED

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL, initial active-order tab/static contract missing before implementation.
- RED: Backend active-order tests were updated to cover workOrderId-only add and missing effective schedule/route branches before the final GREEN run.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, candidate tests referenced missing `eligible` / `ineligibleReason` fields before implementation.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing batch QA item mapper and no proof that candidate eligibility avoids N+1 dependency loading.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldBatchLoadCandidateDependenciesSoRemoteDropdownDoesNotStayLoading" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 30, Failures: 0, Errors: 0, Skipped: 0.

## Verification

- Contract verification: `MesProcessPoolTeamLeaderControllerTest` covers add request mapping, candidate endpoint permission, and candidate eligibility fields.
- Service verification: `MesTeamLeaderActiveOrderServiceTest` covers candidate search, batch dependency loading for remote dropdown responsiveness, candidate eligibility sorting/reason, route derivation, idempotent add/reactivate, duplicate/concurrent behavior, and failure branches; add/reactivate no longer records transfer traces from request input.
- Integration boundary: frontend contracts now assert add payload keys are exactly `['workOrderId']`.
- Runtime verification: `backend-runtime-active-order-candidate-batch-20260806-213525.jar` is running on local `48081`; real page candidate search for `88` returned HTTP 200/business code 0 with 20 candidates, `eligible/ineligibleReason` fields, and `loadingCount=0`.

## Blockers

- Backend/API verification has no remaining code blocker.
- Full write-type real E2E remains blocked because required `TLW_*` test tenant/account/fixture variables are not injected in the current shell.
