# Verification Report: 标准列表模板支持多维度筛选

## Summary

- Implemented reusable condition Tab filtering for the standard list template and enabled it on the real MES 排产工单 page.
- 排产工单 pilot maps filled Tab conditions to formal existing query params only: `completionFilter`, `code`, `erpWorkOrderCode`, and `promiseDate`.
- Real Playwright E2E passed on `http://127.0.0.1:8081/mes/pro/schedule-order` against backend `http://127.0.0.1:48081`.
- 2026-08-04 user-requested E2E re-verification passed on the same real route and runtime.
- 2026-08-05 user-requested E2E re-verification passed on the same real route and runtime after one initial timeout waiting for the first schedule-order page response.
- The old fixed inline/more-filter design was replaced with add/remove condition Tabs; query submits the intersection of all filled Tabs.
- Fixed duplicate filtering UI reported from screenshot: 排产工单 now hides the left legacy quick filter when the right condition Tab filter is active.
- Fixed sync tab reuse gap: 同步工单 now uses the same standard condition Tab filter instead of the old quick filter and duplicate admitted-status controls.
- Fixed empty-condition prompt reported from screenshot: the second-line “点击右侧加号新增筛选条件。” hint is no longer rendered.
- Cleanup preview/apply passed after the 2026-08-05 E2E re-verification; the transient first-run `error.txt` artifact was deleted and the final `result.json` was kept.
- Task remains `ready_for_closeout`; commit/push is not performed because the shared working tree still has unrelated dirty/untracked concurrent changes.

## Passed

- `node tests/e2e/schedule-order-main-multi-filter-static.spec.js` -> PASS.
- `node tests/e2e/unified-list-template-multi-filter-static.spec.js` -> PASS.
- `node tests/e2e/unified-list-template-static.spec.js` -> PASS.
- `node tests/e2e/mes-schedule-order-sync-tab-static.spec.js` -> PASS.
- `node tests/e2e/mes-schedule-order-replan-visible-filter-static.spec.js` -> PASS.
- `pnpm ts:check:schedule` -> PASS.
- `pnpm ts:check` -> PASS.
- `node --check doc\tasks\20260804-standard-list-multi-filter\schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS, with Git LF/CRLF working-copy warnings only.
- Frontend feature evidence validator -> PASS.
- Bug regression evidence validator -> PASS.
- Cleanup preview/apply -> PASS, no deleted paths.
- Final closeout reverify -> PASS for multi-filter static contract, feature evidence validator, bug evidence validator, E2E script syntax, `git diff --check`, and cleanup preview/apply.
- Real E2E `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- Re-run Real E2E `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- Duplicate-filter regression E2E `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- Sync-tab regression E2E `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- Empty-condition prompt regression E2E `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.
- 2026-08-05 Real E2E re-run `node doc/tasks/20260804-standard-list-multi-filter/schedule-order-multi-filter-real.e2e.cjs` -> PASS.

## E2E Evidence

- Tenant/user label: `芋道源码/admin`; password was read from local `.env` and not logged.
- Target sample: `SCH-CODEX-FACTOR-20260708093210-20260710-0001` / `CODEX-FACTOR-20260708093210`.
- Filtered request params: `pageNo=1`, `pageSize=20`, `code=<target>`, `erpWorkOrderCode=<target>`, `completionFilter=ALL`.
- Reset request params: `pageNo=1`, `pageSize=20`; `code`, `erpWorkOrderCode`, `completionFilter`, and `multiFilters` were absent.
- Result counts: initial `17`, filtered `1`, reset `47`; legacy quick filter visible count `0`; target write requests `0`, target HTTP errors `0`, runtime issues `0`.
- Empty-condition prompt counts after reset: `conditionEmptyPromptVisibleCount=0`, `conditionEmptyPromptTextCount=0`; the Tab row still shows `暂无筛选条件`.
- Sync tab sample: `SMART-SCHED-20260630-RERUN5-MO` / `AW.106.03.08.1007` / `READY_TO_ADMIT`.
- Sync tab filtered request params: `pageNo=1`, `pageSize=20`, `admissionStatus=READY_TO_ADMIT`, `workOrderCode=<target>`, `productCode=<target>`; `quickFilter` and `multiFilters` were absent.
- Sync tab reset request params: `pageNo=1`, `pageSize=20`; `workOrderCode`, `productCode`, `admissionStatus`, `quickFilter`, and `multiFilters` were absent.
- Sync tab counts: initial `84`, filtered `1`, reset `1483`; legacy quick filter visible count `0`.
- One initial GET was recorded as `net::ERR_ABORTED` after a subsequent page/filter request superseded it; it was recorded separately and did not affect target assertions.
- 2026-08-05 reverify note: the first run timed out while waiting for the initial schedule-order page response; immediate rerun passed with the same formal parameter and no-write assertions.

## Design Verification

- No backend contract change, no mock data, no frontend-only filtering, no storage fallback, and no swallowed exception path was introduced.
- Condition Tabs are reusable in `TableMultiFilter`; 排产工单 no longer carries page-specific inline filter count handling.
- Existing quick filter contract remains reusable in `UnifiedListTemplate`; 排产工单 hides it only when its right-side condition Tab multi-filter is active.
- 同步工单 uses only backend-supported formal filters for the condition Tabs: `workOrderCode`, `productCode`, `admissionStatus`, and `requestDate`; old unmapped quick-filter-only fields are not exposed as multi-filter conditions.
- Reusable experience was consolidated into `docs/frontend-development.md#统一列表复合工具栏布局门禁` and indexed for future standard-list multi-filter work.

## Blockers

- Full closeout commit/push remains blocked by unrelated dirty/untracked concurrent changes in the shared working tree; this task did not use broad staging, broad baseline commit, or push.
