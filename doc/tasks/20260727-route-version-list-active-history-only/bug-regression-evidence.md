# Bug Regression Evidence

## Bug Summary

工艺路线版本工作区列表展示了 `CANCELLED` 已取消候选版本，且第一轮修复仍会展示 `DRAFT` 等未生效候选版本；用户期望版本列表只保留当前/历史已生效版本，不展示取消版本或其它未生效候选版本。

## Expected Behavior

- 版本列表仅显示 `ACTIVE` 当前/历史生效版本和 `SUPERSEDED` 已替代历史版本。
- `DRAFT` 草稿、审核中、待生效、已驳回和 `CANCELLED` 已取消版本不在版本列表显示。
- 已取消版本的后端冻结快照只读读取能力保留，避免外部链接或已有上下文失效。

## Reproduction

RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> FAIL, expected reason: table still binds raw `routeVersions`, so `CANCELLED` rows are not filtered before rendering.

RED: effective-only audit command against previous HEAD -> FAIL, expected reason: previous implementation only filtered `CANCELLED` and had no `EFFECTIVE_ROUTE_VERSION_STATUS_SET`, so `DRAFT` candidates could still appear.

## Root Cause

- The route version workspace table directly bound `routeVersions`, which is populated from `ProRouteApi.getRouteVersionList(routeId)`.
- The first predicate only hid closed `CANCELLED` candidate versions and did not encode the user requirement that the version list is effective-history-only.

## Regression Tests

- `tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
- `tests/e2e/mes-route-version-list-active-history-only-real.e2e.js`
- Existing regression preserved: `tests/e2e/mes-route-cancelled-version-view-static.spec.js`

## Verification

GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS, `PASS: mes route version list shows effective historical versions only`.

GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

GREEN: `node --check tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

GREEN: `node tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS, route `RT000028` UI shows effective historical versions and hides `V19 DRAFT` plus cancelled versions with `mesWriteRequests=[]`.

## Risk And Scope

- Scope limited to frontend route-version list presentation and static tests.
- No backend API contract changed.
- No fallback, default-success value, or swallowed error added.
- Real E2E is read-only and records no MES write requests.

## Blockers And Follow-Up

- None currently.
