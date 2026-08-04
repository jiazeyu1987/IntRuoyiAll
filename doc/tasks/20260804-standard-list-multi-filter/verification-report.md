# Verification Report: 标准列表模板支持多维度筛选

## Summary

- Implemented reusable condition Tab filtering for the standard list template and enabled it on the real MES 排产工单 page.
- 排产工单 pilot maps filled Tab conditions to formal existing query params only: `completionFilter`, `code`, `erpWorkOrderCode`, and `promiseDate`.
- Real Playwright E2E passed on `http://127.0.0.1:8081/mes/pro/schedule-order` against backend `http://127.0.0.1:48081`.
- The old fixed inline/more-filter design was replaced with add/remove condition Tabs; query submits the intersection of all filled Tabs.
- Cleanup preview/apply passed after the Tab redesign; only the old failed E2E `error.txt` artifact was deleted.
- Task remains `ready_for_closeout`; commit/push is not performed because the shared branch already has unrelated dirty/ahead concurrent changes.

## Passed

- `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> PASS.
- `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS.
- `node tests/e2e/unified-list-template-static.spec.js` -> PASS.
- `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS.
- `node tests/e2e/mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS.
- `pnpm ts:check:schedule` -> PASS.
- `node --check doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS, with Git LF/CRLF working-copy warnings only.
- Real E2E `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.

## E2E Evidence

- Tenant/user label: `芋道源码/admin`; password was read from local `.env` and not logged.
- Target sample: `SCH-CODEX-FACTOR-20260708093210-20260710-0001` / `CODEX-FACTOR-20260708093210`.
- Filtered request params: `pageNo=1`, `pageSize=20`, `code=<target>`, `erpWorkOrderCode=<target>`, `completionFilter=ALL`.
- Reset request params: `pageNo=1`, `pageSize=20`; `code`, `erpWorkOrderCode`, `completionFilter`, and `multiFilters` were absent.
- Result counts: initial `17`, filtered `1`, reset `47`; target write requests `0`, target HTTP errors `0`, runtime issues `0`.
- One initial GET was recorded as `net::ERR_ABORTED` after a subsequent page/filter request superseded it; it was recorded separately and did not affect target assertions.

## Design Verification

- No backend contract change, no mock data, no frontend-only filtering, no storage fallback, and no swallowed exception path was introduced.
- Condition Tabs are reusable in `TableMultiFilter`; 排产工单 no longer carries page-specific inline filter count handling.
- Existing quick filter and 排产工单 actions/table/pagination remain wired; the pilot adds multi-filter without removing the legacy quick-filter contract.
- Reusable experience was consolidated into `docs/frontend-development.md#统一列表复合工具栏布局门禁` and indexed for future standard-list multi-filter work.
- Same-file non-task diff exists around 同步工单 quick-filter handling; it was not authored by this task and was not included as task evidence beyond passing adjacent static/type checks.

## Blockers

- Latest `pnpm ts:check` is currently blocked by unrelated concurrent work in `src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue(3,26)`: `Type '"pqcLeader"' is not assignable to type 'EdhrBatchRecordTab'`.
- Full closeout commit/push remains blocked by shared branch `int_main...origin/int_main` being ahead with many unrelated dirty changes; this task did not use broad staging or push.
