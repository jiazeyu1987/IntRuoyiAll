# Backend API Evidence

## Scope

- Endpoint scope: `GET /mes/pro/process-pool/team-leader/active-order/candidates` and `POST /mes/pro/process-pool/team-leader/active-order/add`.
- Service scope: `MesTeamLeaderActiveOrderServiceImpl` candidate search, workOrderId-only add, unique effective schedule resolution, active/reactivate/idempotent behavior, process snapshot creation, and PQC task creation.

## Contract

- Candidate API uses `mes:pro-process-pool-team-leader:maintain` and returns at most 20 confirmed production work orders as `workOrderId` and `workOrderCode`.
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

## RED

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL, initial active-order tab/static contract missing before implementation.
- RED: Backend active-order tests were updated to cover workOrderId-only add and missing effective schedule/route branches before the final GREEN run.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 25, Failures: 0, Errors: 0, Skipped: 0.

## Verification

- Contract verification: `MesProcessPoolTeamLeaderControllerTest` covers add request mapping and candidate endpoint permission.
- Service verification: `MesTeamLeaderActiveOrderServiceTest` covers candidate search, route derivation, idempotent add/reactivate, duplicate/concurrent behavior, and failure branches; add/reactivate no longer records transfer traces from request input.
- Integration boundary: frontend contracts now assert add payload keys are exactly `['workOrderId']`.

## Blockers

- Backend/API verification has no remaining code blocker.
- Full write-type real E2E remains blocked because required `TLW_*` test tenant/account/fixture variables are not injected in the current shell.
