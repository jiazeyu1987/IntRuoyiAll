# Verification Report

## Verdict

PASS。当前源码、后端回归、新加载的本机 `48081` 运行 Jar 和登录态 `runtime-config` 接口均确认：一线生产运行态不再要求 `productionSubmitContext.activeOrder`，也不要求 `workOrderId`、`taskId`、`itemId`、`recordbookId`。

## Evidence

- Static source scan: `rg "productionSubmitContext\\.activeOrder|activeOrder routeId|requireSingleActiveOrder" IntRuoyiBackend\yudao-module-mes\src\main IntRuoyiBackend\yudao-module-mes\src\test` -> `NO_MATCH`。
- Source implementation: `MesFrontlineRuntimeConfigServiceImpl#resolveProductionSubmitContext` 仅要求 `approveUserId`，返回 `routeId/routeProcessId/processId/workstationId/approveUserId`，`workOrderId/workOrderCode/taskId/itemId/recordbookId` 均为 `null`。
- Test coverage: `MesFrontlineRuntimeConfigServiceTest` 断言 `productionSubmitContext.workOrderId/workOrderCode/taskId/itemId/recordbookId` 均为 `null`，同时保留 `routeId/routeProcessId/processId/workstationId/approveUserId`。
- Targeted JUnit: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineDeviceParameterValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, tests run 11, failures 0, errors 0, skipped 0。
- Runtime stale root cause found: old `48081` PID `66736` ran `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1524-pqc-snapshot-process-hotfix.jar`; Jar SHA256 `2C8BB890FE22A6020F89F86A7BA5BD4C663C3E0239F6CE060A51BDAFD20CD20F`; its nested `MesFrontlineRuntimeConfigServiceImpl.class` still contained `productionSubmitContext.activeOrder`, `activeOrder routeId`, and `requireSingleActiveOrder`。
- Isolated build: detached worktree `D:\IntRuoyiWorktree\20260808-frontline-active-order-runtime-verification-backend`, HEAD `68f90d0014238b611e904034bddf9fc9ebf78e72`; source scan -> `ISOLATED_NO_OLD_ACTIVE_ORDER_GUARD`; `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> BUILD SUCCESS。
- New runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1802-frontline-active-order.jar`; SHA256 `E03D5DDD801285F9A8E407BDFBE583FE0BE1E07C3FC9E022103BC8CCEA9644F5`; nested MES Jar contains target class and no old activeOrder strings。
- Runtime switch: stopped confirmed old int_main PID `66736`; started PID `62116` from the new runtime Jar; `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`。
- Login runtime check: local identity label `芋道源码/admin`; `/mes/pro/feedback/frontline/device-account/processes` -> code 0, 28 processes; target `routeId=922119`, `routeProcessId=980661`, `processId=922985`。
- Target runtime-config: `/mes/pro/feedback/frontline/device-account/runtime-config?routeId=922119&routeProcessId=980661&processId=922985` -> code 0; context `routeId=922119`, `routeProcessId=980661`, `processId=922985`, `workstationId=980010`, `approveUserId=1`; optional fields `workOrderId/taskId/itemId/recordbookId` all null; no activeOrder error。

## Notes

- First Maven `-am` run failed before Surefire because MES `target/classes` was stale: source contained `recordTeamLeaderReviewSignature` and `ACTION_TEAM_LEADER_REVIEW`, but javac saw the old symbol set. A current-module `clean test` restored the target output and passed 11 tests, then the expected `-am` command passed.
- No password or token was recorded in task evidence.
- Project experience consolidation result: no new long-term experience document needed; the existing `docs/local-runtime.md#2026-07-24 隔离构建 Jar 加载门禁` already covers the stale runtime Jar diagnosis and isolated build/load process used here.
- Cleanup result: cleanup preview/apply passed with no delete candidates or blocked paths; detached build worktree was removed and verified absent.
