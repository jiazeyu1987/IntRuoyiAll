# Verification Report

## Summary

Implemented release-owner parsing from route-level `RELEASE_APPROVE` configuration and removed release-stage fallback to `stageOwnerRole`.

## Automated Verification

- RED backend: target Maven command -> FAIL, missing release owner getters.
- RED frontend: `node tests\e2e\edhr-release-owner-label-static.spec.js` -> FAIL, missing `releaseOwnerConfigured` API type.
- GREEN backend: target Maven command with USER, ROLE_GROUP, close-owner-only, password and authorization cases -> PASS, 9 tests.
- GREEN frontend static: `node tests\e2e\edhr-release-owner-label-static.spec.js; node tests\e2e\edhr-release-screenshot-action-buttons-static.spec.js` -> PASS.
- GREEN frontend typecheck: `pnpm ts:check` -> PASS.
- GREEN route configuration contracts: `mes-route-flow-end-release-owner-static` and `mes-route-flow-release-owner-candidate-static` -> PASS.
- Final static rerun confirms missing release-owner data displays `放行责任人未配置`.
- Isolated `javac` compilation of the final backend workbench service and the new release-service regression test -> PASS.
- Blank user/role labels now fail fast; the implementation no longer falls back to numeric IDs.
- Final isolated Maven target rerun -> PASS, 10 tests, 0 failures, 0 errors, including the empty `ROLE_GROUP` candidate-pool case.
- Isolated `yudao-server` package from latest `origin/int_main` -> PASS.

## Verification Retry Status

- Added `MesProEdhrReleaseServiceImplTest#submitRejectsWhenRouteReleaseRoleHasNoEnabledMembers`.
- The earlier main-workspace Maven timeout was resolved by running the exact target suite in a detached build worktree; the final result is 10 tests passed.
- Final `pnpm ts:check` retry after the label-only refinement timed out while unrelated `vue-tsc` processes were active; the focused static contracts passed.

## Real E2E Status

- Frontend `http://127.0.0.1:8081` responded `200`.
- Backend `http://127.0.0.1:48081/actuator/health` responded `200` with `{"status":"UP"}`.
- `8081` is Vite from `E:\IntRuoyi\IntRuoyiFronted`.
- `48081` is PID `61040`, running `java -jar E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Database read-only verification confirms `922119 / RT000028 / 球囊扩张压力泵` has enabled `RELEASE_APPROVE` rule `9000253153`, configured to `USER 1 / admin / 瑛泰管理员`; latest batch `900000000881` uses that route.
- The loaded Jar was built from `a9dfaf9e7f9338dce43ca6955d79f1a6e6c291e2`; SHA256 is `7A3F2A015A0816D9F6876DBAAE4D99DB1619F7C5011E79E0EF7D72AE43A7DA0C`.
- Old Jar SHA256 `6B86DCDAC11258E897F2F168C3F43E0E425D7F4D8CF8B93BCCC1C2169BD8921C` is preserved at `E:\IntRuoyi\output\runtime\int_main\backend-release-owner-before-20260727-195336.jar`.
- Official login preflight passed with the authorized `芋道源码/admin` identity after one cold-start permission-cache warm-up retry.
- Playwright opened batch `900000000881`, selected the visible `99 放行` node, and confirmed `当前放行负责人：瑛泰管理员`.
- The page did not contain `放行责任人未配置` or generic owner fallback `执行人`.
- The authenticated workbench response returned HTTP `200`, business code `0`, `releaseOwnerConfigured=true`, `releaseOwnerSourceType=USER`, and `releaseOwnerLabel=瑛泰管理员`.
- The real-browser verification was read-only and sent no MES write action.

## Design Constraint Check

- Fallback/degradation/exception swallowing introduced: No.
- Root-cause and long-term maintainability: Yes; display and authorization now use the same `RELEASE_APPROVE` source.
- Temporary patch or bypass: No.

## Git Status

- Concurrent baseline commit `f18927b9` was created and pushed to `origin/int_main` at `2026-07-27 18:41:23 +08:00`.
- That baseline contains the core release-owner backend, frontend, tests, and task artifacts.
- Latest `origin/int_main` commit `a9dfaf9e` contains the final release-owner implementation used for the verified Jar.
- Current evidence updates remain uncommitted because the main workspace contains unrelated concurrent task changes that must not be included in this task's closeout commit.

## Current Result

- Implementation is complete.
- Automated regression, isolated package, safe local restart, authenticated API verification, and real-browser verification pass.
- Task is `ready_for_closeout`; only safe Git evidence commit/push remains pending due unrelated concurrent main-workspace changes.
