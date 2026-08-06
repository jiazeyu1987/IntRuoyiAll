# PQC 管理列表页面为空回归证据

## Bug Summary

- 用户截图显示 `PQC组长 > PQC管理` 表格为空，但本机 SQL 和登录态 API 已能查到测试事件 `160`。
- 期望：打开或切换到 `PQC管理` 时，列表应带可见的必填 `提交日期=今天` 条件并加载今天提交的 PQC 数据。

## Expected Behavior

- Given 今天存在 admin/PQC 可见的 PQC 提交事件 `160`
- When 用户打开 `PQC组长 > PQC管理`
- Then 页面列表显示该事件，而不是因为缺少提交日期停留在空表格。

## Reproduction

- Screenshot: `C:\Users\BJB110\AppData\Local\Temp\codex-clipboard-a2af6dfb-8dc3-4c64-aa41-b751b3f9430b.png`
- API with date: `GET /admin-api/mes/pro/process-pool/team-leader/submission/page?...&leaderType=PQC&submitDate=2026-08-06...` -> code `0`, total `1`, event `160`.
- API without date: `GET /admin-api/mes/pro/process-pool/team-leader/submission/page?...&leaderType=PQC...` -> code `500`.

## Root Cause

- `TeamLeaderWorkbenchPage.vue` initializes and resets `queryParams.submitDate` to blank even though the backend list API requires `submitDate`.
- `PqcLeaderWorkbenchPage.vue` starts on `人员管理`; switching to `PQC管理` only changes the module tab and previously did not call `getSubmissionList()`.
- Result: the visible page stayed in the empty table state even though SQL and authenticated API with `submitDate=2026-08-06` returned event `160`.

## RED Evidence

- RED: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> FAIL, expected reason `PQC submission list should use the shared date formatter to build an API-compatible YYYY-MM-DD default date.`
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> FAIL before fix.
- Expected failure: `PQC submission list should use the shared date formatter to build an API-compatible YYYY-MM-DD default date.`

## GREEN Evidence

- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS.
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS.
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS.
- `node doc/tasks/20260806-pqc-management-list-test-data/verify-pqc-management-list-real.e2e.cjs` -> PASS; browser request included `leaderType=PQC&submitDate=2026-08-06`, response total `1`, visible row `RRM-20260801-PP-MO-001` / `清洗工序`.
- `pnpm ts:check` from `IntRuoyiFronted` -> PASS.
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs doc/tasks/20260806-pqc-management-list-test-data` -> PASS.

## Verification

- Real browser path: local `芋道源码/admin` opened `/mes/pro/process-pool/pqc-leader`, clicked `PQC管理`, captured request `leaderType=PQC&submitDate=2026-08-06`, and saw `RRM-20260801-PP-MO-001`.
- Static and type verification passed for the bug-owned frontend change.

## Risk And Scope

- Scope is limited to the PQC/production leader shared submission list default required date and module-tab load behavior.
- No mock data, hidden fallback filter, backend downgrade, or hardcoded event ID is allowed.
- Residual adjacent contracts outside this minimal bug fix still need separate cleanup: `team-leader-production-report-payload-columns-static.spec.cjs` fails on pre-existing production column defaults, while older PQC standard-list contracts still assert an empty default condition that conflicts with the backend-required visible submit date fixed here.

## Blockers

- None for the PQC 管理 empty-list regression.
- Follow-up only: reconcile older list-structure static contracts with the current structured PQC list requirements.
