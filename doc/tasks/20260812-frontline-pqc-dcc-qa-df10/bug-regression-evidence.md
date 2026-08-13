# DF10 Round-3 Bug Regression Evidence

## Bug Summary And Expected Behavior

The dedicated frontline PQC projection must compile with the canonical published-item fields and must read the order-locked QA aggregate only through `MesQaInspectionRegulationService#getLockedVersionForOrder`.

## Reproduction

RED: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before tests because the dedicated PQC converter calls removed compatibility setters. Independent source review also proves the projection duplicates regulation/version/process/item mapper reads instead of the frozen service boundary.

## Root Cause

DF07 implemented an incomplete process-only locked reader, so DF10 copied aggregate construction into a private resolver. Removing compatibility fields from the dedicated response then left two stale converter setter calls.

## Regression Tests

- QA regulation service tests must prove the full locked aggregate accepts PUBLISHED/RETIRED versions without current-version or enabled-DCC lookup.
- Context service tests must mock and verify the full locked aggregate call with the three active-order snapshot IDs.
- The module must compile after deleting only the dedicated PQC compatibility setter calls; the production-route response remains unchanged.

## GREEN:

GREEN: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest,MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2026-08-14 01:57:19，18 tests / 0 failures / 0 errors / 0 skipped。

## Verification

- `MesQaInspectionRegulationService#getLockedVersionForOrder` 返回 PUBLISHED/RETIRED 完整聚合，包含正式规则、QA 工序、检验项目和设备选项；不读取 currentVersionId，也不要求 DCC 当前启用。
- `MesFrontlinePqcContextServiceImpl#listProcessesByActiveOrder(Long)` 只通过上述服务边界读取锁定 QA 聚合；测试验证三个锁定 ID 原样传入，并验证不直接调用 regulation/version/process/item mapper。
- 专用 `MesFrontlinePqcProcessRespVO.PqcInspectionItem` 转换不再写入 acceptanceStandard/processInspectionMethod；生产路线 `MesFrontlineRouteProcessRespVO` 兼容字段未修改。
- `git diff --check` -> PASS，仅有 LF/CRLF 提示。
- backend-api evidence validator -> PASS。
- bug-regression evidence validator -> PASS（补齐本节后复跑）。

## Risk And Scope

Scope is limited to the full locked QA service contract, DF10 consumption, its tests, and the dedicated PQC converter compile fix. No schema, frontend, management current QA, mapper, or production-route contract change is allowed.

## Blockers And Follow-up

No external blocker. Independent re-verification remains a supervisor gate.
