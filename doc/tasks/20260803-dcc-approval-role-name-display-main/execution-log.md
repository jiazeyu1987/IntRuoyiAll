# Execution Log

## User Intent

用户反馈页面仍和之前一样；排查确认当前可见主工作区 `E:\IntRuoyi` 尚未包含隔离 worktree 中的审批角色名称显示修复。

## Preflight

- Read `bug-regression-fix-loop` and bug evidence contract.
- Read frontend, task closeout, and PowerShell encoding rules.
- `BDD: DCC approval route role-name display main sync -> Given POSITION nodes contain candidateSourceIds and stale subjectLabel permission codes, When the route list renders node columns in E:\IntRuoyi, Then it displays approval role names and filters permission-code labels.`

## RED

- `RED: node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> FAIL, 固定审批角色 900332 缺少 文控 映射。`

## GREEN

- Added `900332 -> 文控` to the fixed DCC approval role map.
- Kept full `positions.value` for historical display while passing only `activePositions.value` into route edit forms.
- Updated `formatRouteNodeSubject` so POSITION nodes resolve from `candidateSourceIds` before stale `subjectLabel` / `subjectName`.
- Filtered technical labels such as `doc-control-review`, `matrix-review`, and `审批角色#900332`.
- `GREEN: node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> PASS`

## Regression

- `GREEN: node tests/e2e/dcc-controlled-file-routes-node-columns-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-route-summary-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-controlled-file-routes-standard-list-template-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Follow-up: Node 2 Blank Regression

- User reported `节点2是空的，以前好像不是空的`.
- Read-only DB check showed active route stage 2 nodes have real POSITION IDs such as `900834/900847/900859/900860/900844`, whose official names are `编制人直接主管/QA/QMS/注册/文档管理员`.
- Root cause: route list first load did not call `loadRouteSubjectLookups()` when no category filter was selected, so only fixed hardcoded roles could display; unknown POSITION IDs became `审批角色#ID` and were filtered as technical labels into `-`.
- `BDD: DCC approval route node2 first-load role names -> Given active approval route node2 contains POSITION candidateSourceIds and no subjectName, When the route list first loads without category filter, Then it loads approval role/user dictionaries before assigning routes and displays official approval role names instead of -。`
- `RED: node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> FAIL, handleQuery did not load approval role/user dictionaries before route page data.`
- Updated `handleQuery` to call `await loadRouteSubjectLookups()` before `getApprovalRoutePage(queryParams)`.
- `GREEN: node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-controlled-file-routes-node-columns-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-route-summary-static.spec.js -> PASS`
- `GREEN: node tests/e2e/dcc-controlled-file-routes-standard-list-template-static.spec.js -> PASS`
- `GREEN: pnpm ts:check -> PASS`

## Runtime Check

- 8081 frontend process PID `28264` belongs to `E:\IntRuoyi\IntRuoyiFronted`.
- 48081 backend process PID `43876` belongs to `E:\IntRuoyi\output\runtime`.
- `Invoke-WebRequest http://127.0.0.1:8081/src/views/dcc/controlled-file/shared/utils.ts` returned source containing `[900332, "文控"]`.
- `Invoke-WebRequest http://127.0.0.1:8081/src/views/dcc/controlled-file/routes/index.vue` returned source containing `activePositions`, `TECHNICAL_ROUTE_NODE_LABEL_PATTERN`, `resolveRouteNodePositionNames`, and `positions.value = positionList`.
- Follow-up runtime source check returned `handleQuery` with `await loadRouteSubjectLookups()` before `getApprovalRoutePage(queryParams)`.

## Real Page Verification

- 2026-08-03 local runtime precheck: `http://127.0.0.1:8081/` returned `200`; `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Playwright bundled Chromium cache was missing at `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\...`; system Chrome was confirmed at `C:\Program Files\Google\Chrome\Application\chrome.exe` and used as the real browser executable.
- First node2 assertion attempt matched rows by category name and failed because multiple visible rows share category `其他`; the validation script was corrected to compare API rows and DOM rows by list order.
- `GREEN: inline Playwright readonly check for /dcc/controlled-file/routes -> PASS`
- Evidence: login used local `.env` default identity label `芋道源码/admin`; target path `/dcc/controlled-file/routes`; route total `64`; visible rows `20`; DCC write requests `0`; page errors `0`.
- Evidence: node2 row checks matched approval position names, including `编制人直接主管、QA、QMS、注册、文档管理员` and `编制人直接主管、QA、QMS`; no `审批角色#ID`, `doc-control`, or `matrix-review` technical labels were visible in node2.

## Blockers

- No implementation or verification blocker remains for the local main workspace patch.
- Formal commit/push remains blocked by pre-existing unrelated dirty changes in `E:\IntRuoyi` and unavailable GitHub HTTPS 443 connectivity. The task-owned code changes remain uncommitted in the main workspace to avoid mixing with concurrent work.
