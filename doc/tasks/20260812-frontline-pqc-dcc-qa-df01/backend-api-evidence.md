# Backend API Evidence - DF01 Active Order Selection Identity

## Scope

- Endpoint: GET /mes/pro/feedback/frontline/device-account/pqc/active-orders
- Service: MesFrontlinePqcContextService.listActiveOrders
- Response contract: MesFrontlineActiveOrderRespVO includes activeOrderId, workOrder, product, route, quantity, and latestSubmitTime.

## API And Data Contract

- Every effective active-order row returned by MesProcessPoolActiveOrderMapper.selectActiveList is represented once.
- activeOrderId is the stable page selection identity.
- workOrderId and routeId remain display/context fields, not the row identity.
- PQC task status is not read or used by the active-order list.

## Auth, Permissions, Validation, Error Behavior

- Existing permission remains mes:pro-feedback:query.
- Missing work order, route, product, or invalid active-order identity still fails fast through existing service exceptions.
- No fallback, mock success, or default empty success path was introduced.

## Required Config, Services, Fixtures, Migrations

- No new config or migration.
- Existing C00 schema is already merged into the DF01 worktree baseline.
- Unit fixtures cover three active-order rows sharing the same workOrderId and routeId.

## BDD Scenarios

- BDD: 三个活跃订单都可选 -> Given 三条有效 active order 中两条拥有相同 workOrderId 和 routeId, When 一线 PQC 打开订单选择列表, Then 响应保留三条 activeOrderId 行，不按 workOrderId + routeId 去重。
- BDD: PQC task 不过滤订单池 -> Given 有效 active order 没有 PENDING PQC task, When 一线 PQC 打开订单选择列表, Then 该 active order 仍返回，且服务不调用 task 状态筛选。
- BDD: activeOrderId 是页面身份 -> Given 列表返回订单卡片, When 前端选择一个订单, Then 选择值必须来自 activeOrderId，而不是 workOrderId 或 routeId 的组合。

## RED

- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, missing activeOrderId contract.
- Command: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- Result: FAIL.
- Expected reason: MesFrontlineActiveOrderCandidate did not expose activeOrderId(), proving the list contract could not select by activeOrderId.

## GREEN

- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.
- Command: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- Result: PASS.
- Evidence: Tests run 4, Failures 0, Errors 0, Skipped 0, BUILD SUCCESS at 2026-08-12T14:57:10+08:00.

## Contract Or Integration Verification

- Static check: rg did not find ActiveOrderKey, LatestActiveOrderContext current, or selectActiveOrderIdsByTaskStatus in MesFrontlinePqcContextServiceImpl.
- Static check: activeOrderId is present in MesFrontlineActiveOrderCandidate, MesFrontlineActiveOrderRespVO, controller mapping, and service tests.
- git diff --check: PASS; only Git line-ending warnings were reported.

## Observability Touchpoints

- No new logs or metrics were required.
- Existing fail-fast exception paths remain unchanged for invalid work order, route, product, and route-product data.

## Blockers And Downstream Skill Needs

- No DF01 implementation blocker remains.
- Downstream DF10 must use activeOrderId as the page selection identity and must not reintroduce workOrderId + routeId selection.
