# Bug Regression Evidence

## Bug Summary

工艺路线版本工作区列表展示了 `CANCELLED` 已取消候选版本，用户期望版本列表只保留草稿、当前已生效版本和已生效过的历史版本，不展示取消版本。

## Expected Behavior

- 版本列表隐藏 `CANCELLED` 版本。
- `DRAFT` 草稿、`ACTIVE` 当前生效版本、`SUPERSEDED` 已替代历史版本仍显示。
- 已取消版本的后端冻结快照只读读取能力保留，避免外部链接或已有上下文失效。

## Reproduction

RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> FAIL, expected reason: table still binds raw `routeVersions`, so `CANCELLED` rows are not filtered before rendering.

## Root Cause

- The route version workspace table directly bound `routeVersions`, which is populated from `ProRouteApi.getRouteVersionList(routeId)`.
- There was no presentation-layer predicate to hide closed `CANCELLED` candidate versions from the version list.

## Regression Tests

- `tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
- `tests/e2e/mes-route-version-list-active-history-only-real.e2e.js`
- Existing regression preserved: `tests/e2e/mes-route-cancelled-version-view-static.spec.js`

## Verification

GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

GREEN: `node --check tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

GREEN: `node tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS, route `RT000028` UI shows effective historical versions and hides cancelled versions with `mesWriteRequests=[]`.

## Risk And Scope

- Scope limited to frontend route-version list presentation and static tests.
- No backend API contract changed.
- No fallback, default-success value, or swallowed error added.
- Real E2E is read-only and records no MES write requests.

## Blockers And Follow-Up

- None currently.
