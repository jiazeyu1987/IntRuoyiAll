# Verification Report

## Summary

DCC-STATIC-020 is fixed in the clean worktree. Approval route selection now ignores active routes whose `effectiveTime` is in the future, and saving a future route no longer deactivates the currently effective route.

## Verification

- GREEN: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest test` -> PASS.
- Result: 17 tests run, 0 failures, 0 errors, 0 skipped.
- Compile coverage: Maven `compile` and `testCompile` completed as part of the targeted test command.
- MAIN GREEN: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest test` from `E:\IntRuoyi\IntRuoyiBackend` -> PASS, 17 tests, 0 failures, 0 errors, 0 skipped.
- MAIN PREFLIGHT: `E:\IntRuoyi\scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `int_main/int_main`, frontend 8081, backend 48081.
- Cleanup preview/apply: PASS; only temporary `bug-regression-evidence.md` was deleted.
- Bug regression evidence validator: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-020-route-effective-time\bug-regression-evidence.md` -> PASS.
- UTF-8 evidence read check: PASS.

## Files Verified

- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/route/DccCategoryApprovalRouteMapper.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccApprovalRouteAdminServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/route/DccApprovalRouteAdminServiceImplTest.java`
- `docs/worktree-memory.md`
- `docs/experience-index.md`

## Not Run

- E2E tests were not run by user instruction.
- Backend services were not started or restarted by user instruction.
- Databases, remote systems, and Git push were not touched by user instruction.

## Risk

- Runtime route resolution uses `DccCategoryApprovalRouteMapper.selectLatestActiveByCategoryId(categoryId)`, so the effective-time filter applies to new file submissions and readiness checks.
- Existing stored future routes with `active=true` remain configurable and become selectable only after their `effectiveTime` arrives.
- Existing routes with `effectiveTime = null` are treated as immediately effective to preserve current data semantics.

## Closeout Status

Task is `ready_for_closeout`. Local `int_main` integration is complete; remote push remains intentionally not performed under the current delegated scope.
