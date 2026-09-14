# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 工艺路线版本弹窗表格原先只展示 `ACTIVE/SUPERSEDED`；草稿可见后，DRAFT 行仍沿用泛化“取消”语义，用户无法明确知道可以删除当前草稿并重新从已发布版本开始编辑。
- Expected: 版本弹窗表格展示 `DRAFT` 草稿和已生效历史版本，`DRAFT` 行可通过“删除草稿”关闭；删除后再次点击编辑会基于当前 `ACTIVE` 版本创建新草稿，同时继续隐藏 `PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED` 等非允许状态。

## Reproduction

- Reproduction command: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js`。
- RED result: FAIL before fix，缺少包含 `DRAFT` 的 `ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET`，旧实现仍用 `ACTIVE/SUPERSEDED` effective-only 口径。

## Root Cause

- `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 中 `isVisibleRouteVersionInWorkspace` 原先只允许 `version.active` 或 `ACTIVE/SUPERSEDED` 状态，用户新要求中的 `DRAFT` 草稿被正向过滤排除。
- 草稿关闭动作复用了泛化“取消”文案，没有向用户说明逻辑删除后的状态及再次编辑会基于当前 `ACTIVE` 新建草稿。
- 重新编辑链路本身已有正式同源草稿检查和 active 来源创建逻辑，本次无需新增 API 或后端生产分支，只需锁定 `CANCELLED` 不属于打开候选并补齐回归。

## Regression Test

- Added: `IntRuoyiFronted/tests/e2e/mes-route-version-list-draft-visible-static.spec.js`。
- Updated: `IntRuoyiFronted/tests/e2e/mes-pro-route-version-workspace-static.spec.js`，覆盖删除按钮、确认、用户取消、API 和刷新。
- Updated: `IntRuoyiFronted/tests/e2e/mes-route-list-edit-create-candidate-static.spec.js`，覆盖取消草稿不复用及新草稿 active 来源。
- Updated: `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionWorkflowServiceTest.java`，覆盖取消后重新创建草稿。
- Updated/renamed: `IntRuoyiFronted/tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`，真实只读脚本断言草稿可见并保留无 MES 写请求检查。

## RED And GREEN

- RED: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> FAIL, expected reason: no draft-visible status set.
- RED: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> FAIL before delete-draft implementation, expected reason: no DRAFT delete action, confirmation, cancellation guard, API and refresh contract.
- GREEN: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-route-list-edit-create-candidate-static.spec.js` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Static regression verification passed for draft visibility, delete confirmation/action, refresh, cancelled-candidate exclusion, and active-source recreation.
- Target backend lifecycle regression passed with 17 tests.
- TypeScript verification passed.
- Real Playwright E2E is blocked by missing Chromium executable and is not recorded as PASS.

## Risk And Regression Scope

- Risk controlled by positive allow set: only `DRAFT` was added to visible table rows; closed/review states remain excluded by static contract.
- Existing read-only historical viewer remains covered by `mes-route-cancelled-version-view-static.spec.js`.

## Blockers And Follow-Up

- Real Playwright E2E is blocked by missing Chromium executable in the configured Playwright browser cache; no API-only or alternate-browser fallback was used.
- Independent commit/push is blocked because mixed baseline commit `67282a86` already contains task and non-task files, the branch has an actively changing ahead/behind divergence, and concurrent dirty changes include shared task files.
