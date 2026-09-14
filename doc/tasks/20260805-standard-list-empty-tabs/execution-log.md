# Execution Log: 标准列表模板统一为空条件 Tab 筛选模式

## User Intent

- 用户要求把当前所有标准列表模板统一改成截图红框里的模式：默认没有筛选条件，点击加号新增筛选条件；先梳理系统里有多少标准列表模板，再逐个优化。

## BDD Scenarios

- BDD: 标准列表默认空条件 -> Given 页面使用标准列表模板 / When 列表首次渲染 / Then 筛选区域显示“暂无筛选条件”和左右加减按钮，不预置业务筛选条件。
- BDD: 加号新增筛选条件 -> Given 标准列表模板筛选区域为空 / When 用户点击右侧加号 / Then 新增一个条件 Tab，用户可选择字段和值。
- BDD: 多条件交集查询 -> Given 用户新增多个条件 Tab 并填写不同字段 / When 用户点击查询 / Then 列表请求只提交正式 query 参数交集，不发送 `quickFilter` 或 `multiFilters` 临时参数。
- BDD: 旧 quick filter 迁移 -> Given 页面此前使用标准列表模板 quick filter / When 完成本次迁移 / Then 页面不再显示旧 quick filter 区域，筛选能力通过同一条件 Tab 机制承载。
- BDD: 无筛选重置 -> Given 当前没有任何筛选条件 / When 用户点击重置或查询 / Then 不产生默认业务筛选条件，不改变后端接口契约。

## Command And Evidence Log

- Loaded `frontend-feature-delivery` skill and required project rules before implementation.
- Initial git status shows unrelated concurrent dirty/untracked task files; this task will avoid broad staging, commit, push, or unrelated cleanup.
- Experience gate: matched `docs/frontend-development.md#统一列表复合工具栏布局门禁` through `docs/experience-index.md`; applied the template-level Tab mechanism requirement and real E2E query-param verification requirement.
- RED: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> FAIL before implementation because `UnifiedListTemplate` still rendered old `TableQuickFilter`, the 84-template empty Tab inventory contract did not exist, and schedule/admission lists still had seeded conditions.
- RED: `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> FAIL before update because the contract still expected direct multi-filter-only event passthrough instead of the unified standard condition Tab bridge.
- RED: `pnpm ts:check:schedule` -> FAIL before fix because `handleStandardFilterQuery` event dispatch typing did not match the SFC emit union after introducing the shared bridge.
- M1 inventory complete: generated `doc/tasks/20260805-standard-list-empty-tabs/artifacts/standard-list-template-inventory.json`; summary `totalBlocks=84`, `totalFiles=67`, `showQuickFilterFalse=10`, `explicitOrDynamicMultiFilter=2`, `defaultConditionTabByTemplateBridge=73`.
- M3 implementation complete: `UnifiedListTemplate` now renders standard filters through `TableMultiFilter`, converts quick-filter definitions/state into condition Tab definitions/state, and routes query/reset/remove through mode-aware handlers.
- M3 implementation complete: `useTableQuickFilter` now stores condition Tab state, validates duplicate formal query params, applies all active filled conditions as formal query params, and clears both `quickFilter` and `multiFilters` on reset.
- M3 implementation complete: MES 排产工单 and 同步工单 no longer pre-seed `completionFilter='INCOMPLETE'`, `admissionStatus='READY_TO_ADMIT'`, or `.setCondition(...)`.
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\unified-list-template-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\unified-list-template-filter-query-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\table-quick-filter-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\unified-list-template-reset-column-default-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check:schedule` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe' node doc\tasks\20260805-standard-list-empty-tabs\schedule-order-empty-tabs-real.e2e.cjs` -> PASS, real page path `http://127.0.0.1:8081/mes/pro/schedule-order`, backend `http://127.0.0.1:48081`.
- E2E evidence: `doc/tasks/20260805-standard-list-empty-tabs/artifacts/schedule-order-empty-tabs-real/result.json` status `PASS`; 排产工单 initial params `{ pageNo, pageSize }`, filtered params include `code + erpWorkOrderCode`, reset params return `{ pageNo, pageSize }`.
- E2E evidence: 同步工单 initial params `{ pageNo, pageSize }`, filtered params include `workOrderCode + productCode + admissionStatus`, reset params return `{ pageNo, pageSize }`.
- E2E evidence: target write requests `0`, target bad responses `0`, runtime issues `0`; the stale `error.txt` records an earlier login timeout attempt and is not the final result source.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-standard-list-empty-tabs/frontend-feature-evidence.md` -> PASS before cleanup.
- Project experience consolidation: updated existing `docs/frontend-development.md#统一列表复合工具栏布局门禁` and `docs/experience-index.md` with default-empty condition Tab and no page-level `.setCondition(...)` hidden filter preflight; no new long-term document created.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-standard-list-empty-tabs --mode preview` -> PASS, blocked `<none>`, warnings `<none>`.
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-standard-list-empty-tabs --mode apply` -> PASS, deleted task-local stale `error.txt` and temporary `frontend-feature-evidence.md`; kept task records, E2E script, inventory JSON and final result JSON.
- Final E2E rerun: `Invoke-WebRequest http://127.0.0.1:8081/` -> `200`; `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`.
- Final E2E rerun: `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe' node doc\tasks\20260805-standard-list-empty-tabs\schedule-order-empty-tabs-real.e2e.cjs` -> PASS; result again showed both initial requests only carry pagination params, filtered requests carry formal query params, reset clears params, target write requests `0`, target bad responses `0`, runtime issues `0`.
- Final static rerun: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js`, `node tests\e2e\unified-list-template-multi-filter-static.spec.js`, `node tests\e2e\unified-list-template-static.spec.js`, `node tests\e2e\unified-list-template-filter-query-static.spec.js`, `node tests\e2e\table-quick-filter-static.spec.js`, and `node tests\e2e\unified-list-template-reset-column-default-static.spec.js` -> all PASS.
- User-requested E2E rerun 2026-08-05: prerequisite check `npx --version` -> `11.6.2`; frontend `http://127.0.0.1:8081/` -> HTTP `200`; backend `http://127.0.0.1:48081/actuator/health` -> `UP`; Chrome executable path exists at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- User-requested E2E rerun 2026-08-05: `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe' node doc\tasks\20260805-standard-list-empty-tabs\schedule-order-empty-tabs-real.e2e.cjs` -> PASS; 排产工单 and 同步工单 initial requests only had pagination params, filtered requests used formal query params, reset cleared params, target write requests `0`, target bad responses `0`, runtime issues `0`.
