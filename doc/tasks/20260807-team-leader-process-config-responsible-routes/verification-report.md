# Verification Report

## Scope

- 后端授权根因：生产组长工序配置列表与直接维护断言均只按 active `routeStartProductionLeaders` 正式负责路线命中范围授权；`mes:pro-process-pool-team-leader:maintain` 不再扩大路线工序范围。
- 前端/E2E：真实页面断言顶部负责路线和工序配置响应路线集合一致，且不再保留旧的 admin 全路线维护预期。
- 经验沉淀：更新 `docs/backend-development.md` 和 `docs/experience-index.md`，将旧“维护权限全路线”口径修正为“维护权限不扩路线范围”。

## RED Evidence

- `mvn -pl yudao-module-mes -am "-Dtest=MesRouteStartProductionLeaderAuthorizationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL。
- 失败原因：旧实现调用维护权限后把路线范围扩大到 `[101, 102]`，并允许 admin 直接维护未在正式负责路线中的 routeProcess。

## GREEN Evidence

- `mvn -pl yudao-module-mes -am "-Dtest=MesRouteStartProductionLeaderAuthorizationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesRouteStartProductionLeaderAuthorizationServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，26 tests。
- `node tests/e2e/team-leader-responsible-routes-static.spec.cjs` -> PASS。
- `node --check tests/e2e/team-leader-responsible-routes-real.e2e.js` -> PASS。
- `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` -> PASS。
- `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs` -> PASS。
- `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。

## Real E2E Evidence

- Command: `node tests/e2e/team-leader-responsible-routes-real.e2e.js`。
- Identity label: `芋道源码/admin`。
- Visible responsible routes: `["球囊扩张压力泵","按压式球囊扩充压力泵"]`。
- Process-config route names: `["球囊扩张压力泵","按压式球囊扩充压力泵"]`。
- Process-config row count: `28`。
- MES write requests: `0`。
- Page errors: `0`。
- Console errors: `0`。
- Raw browser screenshots and `result.json` were summarized here, then removed during task closeout cleanup as task-owned temporary evidence.

## Runtime Evidence

- Prior runtime: `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260807-2002-responsible-routes.jar`, PID `27904`。
- New runtime: `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260807-2158-process-config-responsible-routes.jar`, PID `53868`。
- New runtime SHA256: `99014581D86A569120C0754EAA4472B50BAF0E9BDF804E0A69EA4E99FB5E6D58`。
- Patched nested MES Jar SHA256: `7264D1C6EA09734DF5AA12194CB92C7D4C1872C15871A8226661E7B6BC9FD809`。
- Nested MES entry check: `compress_type=0 file_size=8743411 compress_size=8743411`。
- Health: `http://127.0.0.1:48081/actuator/health` -> `UP`。

## Closeout Cleanup

- `task-closeout-cleanup --mode preview` -> PASS，blocked `<none>`，warnings `<none>`。
- `task-closeout-cleanup --mode apply` -> PASS，已清理本任务截图、临时 `result.json` 和热补丁临时目录。
- Core records kept: `task.md`、`execution-log.md`、`verification-report.md`。

## Final Result

- PASS: 工序配置里的工序已按当前生产组长正式负责路线限定，其它工艺路线工序不再显示。
- PASS: 直接维护断言不再允许 `admin` 仅凭维护入口权限绕过正式负责路线范围。
- PASS: 无 fallback、无降级、无吞异常。
- PASS: 任务状态已标记为 `completed`；未执行 Git commit/push，因为当前项目规则要求用户明确请求后才进行 Git 操作。
