# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: show only effective historical route versions in the version workspace table.
- Non-goal: remove backend readonly snapshot access for cancelled versions or change write guards.

## Requirements And Acceptance IDs

- REQ-1: Version list renders only `ACTIVE` / `SUPERSEDED` effective historical rows.
- REQ-2: `CANCELLED`, `DRAFT`,审核中、待生效 and 已驳回 candidate versions are not visible in the version list.
- REQ-3: Candidate workflow still uses the existing pending-version/edit entry points and backend write guards.
- REQ-4: Existing readonly historical viewer handoff remains intact.

## UI Entry Points, Routes, Components, And Owned Files

- Entry point: MES route list -> row action `版本`.
- Route/component: `IntRuoyiFronted/src/views/mes/pro/route/index.vue`.
- Tests: `IntRuoyiFronted/tests/e2e/mes-route-version-list-active-history-only-static.spec.js`.
- Real E2E: `IntRuoyiFronted/tests/e2e/mes-route-version-list-active-history-only-real.e2e.js`.

## API Contracts And Data States

- API unchanged: `ProRouteApi.getRouteVersionList(routeId)` still returns the full version collection.
- Display states: only `ACTIVE` and `SUPERSEDED` are visible in the version list; non-effective candidate states remain in the API payload and state strip logic but are not rendered as list rows.

## BDD Scenarios

BDD: 版本列表仅显示已生效历史版本 -> Given 路线版本列表包含 DRAFT、ACTIVE、SUPERSEDED 和 CANCELLED / When 用户打开版本工作区 / Then 列表仅展示 ACTIVE、SUPERSEDED，隐藏 DRAFT 和 CANCELLED 等未生效候选版本

BDD: 深链只读能力保留 -> Given 用户通过已有只读版本上下文打开已取消版本 / When 前端加载关系图 / Then 仍按历史 `routeVersionId` 请求后端读取冻结快照，写控件保持禁用

## RED Command And Expected Failure

RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> FAIL, table still binds `routeVersions`.

RED: effective-only audit command against previous HEAD -> FAIL, previous implementation only filtered `CANCELLED`.

## GREEN Command And Passing Result

GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

GREEN: `node --check tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

GREEN: `node tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Layout unchanged; only table data source changed.
- Loading, empty text, error alert, permissions, and row actions remain unchanged.
- Existing action predicates still control edit/view/submit/cancel visibility for rows that remain displayed; hidden candidates are handled through existing pending-version/edit entry points.
- Real E2E used visible route row action `版本`, verified the dialog table, and asserted no MES write requests were sent.

## E2E Or Component Verification Path

- Static component contract verifies table data source and filter predicate.
- Existing static contract verifies readonly route-version context handoff remains intact.
- Real Playwright path: login `芋道源码/admin` -> `/mes/pro/route?code=RT000028` -> row `版本` -> dialog `工艺路线版本`.
- Real data assertion: visible `V15/V14/V13/V4/V3/V2/V1`; hidden `V19 DRAFT` and `V18/V17/V16/V12/V11/V10/V9/V8/V7/V6/V5 CANCELLED`; `mesWriteRequests=[]`.

## Blockers And Follow-Up Skills

- No implementation blocker.
- Worktree dependency recovery needed `pnpm install --frozen-lockfile --reporter append-only` because `node_modules` was initially missing.
