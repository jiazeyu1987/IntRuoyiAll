# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 工艺路线版本弹窗表格只展示 `ACTIVE/SUPERSEDED`，导致正在进行的 `DRAFT` 草稿只能在候选版本工作区摘要中看到，不能在版本列表里看到和操作。
- Expected: 版本弹窗表格展示 `DRAFT` 草稿和已生效历史版本，`DRAFT` 行可通过“删除草稿”关闭；删除后再次点击编辑会基于当前 `ACTIVE` 版本创建新草稿，同时继续隐藏 `PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED` 等非允许状态。

## Reproduction

- Reproduction command: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js`。
- RED result: FAIL before fix，缺少包含 `DRAFT` 的 `ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET`，旧实现仍用 `ACTIVE/SUPERSEDED` effective-only 口径。

## Root Cause

- `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 中 `isVisibleRouteVersionInWorkspace` 只允许 `version.active` 或 `ACTIVE/SUPERSEDED` 状态，用户新要求中的 `DRAFT` 草稿被正向过滤排除。

## Regression Test

- Added: `IntRuoyiFronted/tests/e2e/mes-route-version-list-draft-visible-static.spec.js`。
- Updated/renamed: `IntRuoyiFronted/tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`，真实只读脚本断言草稿可见并保留无 MES 写请求检查。

## RED And GREEN

- RED: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> FAIL, expected reason: no draft-visible status set.
- GREEN: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Static regression verification passed.
- TypeScript verification passed.
- Real Playwright E2E is blocked by missing Chromium executable and is not recorded as PASS.

## Risk And Regression Scope

- Risk controlled by positive allow set: only `DRAFT` was added to visible table rows; closed/review states remain excluded by static contract.
- Existing read-only historical viewer remains covered by `mes-route-cancelled-version-view-static.spec.js`.

## Blockers And Follow-Up

- Real Playwright E2E is blocked by missing Chromium executable in the configured Playwright browser cache; no API-only or alternate-browser fallback was used.
- Commit/push blocked by unrelated dirty changes and branch divergence.
