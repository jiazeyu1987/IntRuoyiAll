# Verification Report

## Summary

新增活跃订单候选搜索源码和静态/后端聚焦验证已通过；真实页面 E2E 已验证前端弹窗、标签、占位文案和只读候选请求路径，但产品关键词搜索在当前本机 48081 运行态仍被阻塞，因为当前运行 Jar 未加载产品搜索 mapper 方法。已生成并校验可加载该实现的补丁 runtime Jar，但本地安全策略拦截直接停止/启动 48081，暂不能完成最终产品关键词 PASS 复验。

## Commands

- `mvn -pl yudao-module-mes clean test "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS, 43 tests run, 0 failures, 0 errors.
- `node tests\e2e\production-leader-active-order-pool-tab-static.spec.js` -> PASS.
- `node tests\e2e\team-leader-workbench-static.spec.cjs` -> PASS.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260808-active-order-product-search\backend-api-evidence.md` -> PASS before closeout cleanup.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260808-active-order-product-search\frontend-feature-evidence.md` -> PASS before closeout cleanup.
- Focused UTF-8 PowerShell contract check for `订单号/产品`, `请输入订单号、产品编码或产品名称`, and guarded `workOrderId` submit -> PASS.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-active-order-product-search --mode preview` -> PASS, no blocked paths.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-active-order-product-search --mode apply` -> PASS.
- `rg -n "Maven 同模块 target/classes 陈旧|刚新增 mapper 方法找不到|20260808-active-order-product-search" docs\experience-index.md docs\powershell-memory.md` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS.
- `node --check tests\e2e\team-leader-active-order-product-search-real.e2e.js` -> PASS.
- `node tests\e2e\team-leader-active-order-product-search-real.e2e.js` with default product name `球囊扩张压力泵` -> BLOCKED, no candidate data returned; no add request, no page error, no target network failure.
- `ACTIVE_ORDER_PRODUCT_E2E_KEYWORD=881MO093613 node tests\e2e\team-leader-active-order-product-search-real.e2e.js` -> PASS, real UI candidate search by order code returned 1 candidate and `activeOrderAddRequestCount=0`.
- Authenticated read-only detail for `workOrderId=925868` -> productCode `YXN.069.001.1013`, productName `冠状动脉棘突球囊扩张导管`.
- `ACTIVE_ORDER_PRODUCT_E2E_KEYWORD=YXN.069.001.1013 node tests\e2e\team-leader-active-order-product-search-real.e2e.js` -> BLOCKED, real UI path reached candidate API but product code keyword returned no candidates.
- Runtime Jar check -> BLOCKED, current 48081 process runs `backend-latest-20260808-1218-pqc-route-snapshot-hotfix.jar`; nested MES module lacks `selectListByCodeOrNameLike` and `selectConfirmedCandidatesByKeyword`, confirming stale backend runtime for this feature.
- Runtime refresh attempt: `mvn -pl yudao-module-mes clean test "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> BLOCKED before tests by Windows target lock while concurrent same-module Maven processes were active.
- Current rerun: `ACTIVE_ORDER_PRODUCT_E2E_KEYWORD=YXN.069.001.1013 node tests\e2e\team-leader-active-order-product-search-real.e2e.js` -> BLOCKED, real UI reached candidate API, no page errors, no target network failures, `activeOrderAddRequestCount=0`, but product code keyword returned no candidates.
- Current backend target verification: `mvn -pl yudao-module-mes clean test "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS, 43 tests run, 0 failures, 0 errors.
- Runtime refresh artifact: `E:\IntRuoyi\output\runtime\int_main\backend-latest-20260808-1243-active-order-product-search.jar` -> created from current 48081 runtime jar with only the task-owned MES classes replaced; SHA256 `2821D6AF9A7A6501BEAED757F710E33F4EA7DB1CEF7C427F23CACF9B54629B03`; nested MES jar stored uncompressed and `javap` shows both `selectListByCodeOrNameLike` and `selectConfirmedCandidatesByKeyword`.
- Runtime apply attempt -> BLOCKED before execution by local command policy rejecting direct 48081 stop/start; no force kill, random port, or fallback runtime was used.
- Current 48081 recheck -> PID 61424 runs `backend-latest-20260808-1244-pqc-active-order-latest-hotfix.jar`, health `UP`, SHA256 `2FF430472CFAE9B1C13C76CCDEA7EEDDC988E659F7D2139D2264C819CE66FC91`; nested MES jar is stored uncompressed but still lacks `selectListByCodeOrNameLike` and `selectConfirmedCandidatesByKeyword`.
- Current runtime E2E rerun: `ACTIVE_ORDER_PRODUCT_E2E_KEYWORD=YXN.069.001.1013 node tests\e2e\team-leader-active-order-product-search-real.e2e.js` -> BLOCKED, real UI reached candidate API, no page errors, no target network failures, `activeOrderAddRequestCount=0`, but product code keyword returned no candidates.

## Notes

- `node tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> FAIL before this task's active-order assertions on unrelated `data-pqc-process-inspection-aggregation`.
- No fallback, mock success, or degraded search path was introduced.
- Closeout cleanup removed validated task-local evidence files and kept the core task records.
- Real E2E artifact: `doc\tasks\20260808-active-order-product-search\evidence\active-order-product-search-real\result.json`.
- Temporary runtime inspection extracted a nested MES module Jar under `doc\tasks\20260808-active-order-product-search\evidence\runtime-jar-check\`; cleanup deletion was attempted but blocked by local safety policy, so it remains as task-owned diagnostic evidence and should not be committed.
- Current refresh evidence is under `doc\tasks\20260808-active-order-product-search\evidence\runtime-refresh-*`; it remains task-owned diagnostic evidence while runtime application is blocked.
