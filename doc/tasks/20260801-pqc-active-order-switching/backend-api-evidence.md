# Backend API Evidence

## Scope

PQC frontline context source APIs under `/mes/pro/feedback/frontline/device-account/pqc/*` and the backend service `MesFrontlinePqcContextService`.

## Contract

- Active orders: `GET /pqc/active-orders` returns only `mes_pro_process_pool` rows whose `poolStatus=ACTIVE`, deduplicated by `workOrderId + routeId`.
- Active-order processes: `GET /pqc/active-order/processes?workOrderId=&routeId=` validates the selected active order and product-route binding, then returns route processes for that route.
- PQC personnel: `GET /pqc/personnel` returns enabled PQC leader users plus enabled employee-scope PQC users.
- PQC switch employee: `POST /pqc/switch-employee` validates active order, process, personnel, and template binding before returning the template descriptor.
- Production device-account endpoints remain unchanged and are verified by adjacent regression tests.

## Validation

Missing active order, missing product-route binding, empty route processes, empty PQC personnel, or unbound employee fail fast through MES error codes. No fallback to all orders, all processes, all users, or production device-account employee bindings is introduced.

BDD: PQC order selector uses active orders -> Given a PQC inspector opens the fixed template panel / When the order selector loads / Then only active orders are returned and all-order fallback is not allowed.

BDD: PQC process selector uses selected active order route -> Given a PQC inspector selected an active order with product route / When the process selector loads / Then processes come from that product route and missing route fails visibly.

BDD: PQC employee selector uses PQC personnel -> Given a PQC inspector opens the employee selector / When personnel options load / Then the options include all PQC employees and PQC leaders, not unrelated employees.

BDD: PQC leader review is consistent with inspector submissions -> Given PQC inspectors submitted inspection content / When a PQC leader opens the review list / Then list content matches submitted content and correction/submission logs are available.

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing `MesFrontlinePqcContextService`.

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, 0 failures, 0 errors.

## Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests, 0 failures, 0 errors.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolEventRevisionControllerContractTest,MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 26 tests, 0 failures, 0 errors.

## Blockers

No backend implementation blocker remains. Commit/push closeout is blocked by unrelated concurrent dirty worktree and existing branch ahead state.