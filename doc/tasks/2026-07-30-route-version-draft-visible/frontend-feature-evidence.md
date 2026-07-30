# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 工艺路线版本弹窗表格可以看到正在进行的 `DRAFT` 草稿。
- Non-goal: 改后端接口、放开审核中/待发布/驳回/取消版本展示、修改提交发布或取消逻辑。

## Requirements And Acceptance IDs

- REQ-1: `DRAFT` 草稿版本在版本弹窗表格中可见。
- REQ-2: `ACTIVE` 当前生效版本与 `SUPERSEDED` 已替代历史版本继续可见。
- REQ-3: `PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED` 不因本次变更重新进入表格。
- REQ-4: 草稿仍走现有“编辑 / 提交发布 / 取消”动作；非草稿历史版本仍按现有只读查看入口处理。
- REQ-5: `DRAFT` 草稿行必须显示“删除草稿”，删除前弹出确认，成功后刷新版本弹窗并隐藏已取消草稿。
- REQ-6: 删除草稿后再次点击路线列表“编辑”，必须基于当前 `ACTIVE` 版本创建新的 `DRAFT` 草稿。

## UI Entry Points, Routes, Components, And Owned Files

- Entry point: MES 工艺路线列表 -> 行操作 `版本` -> `工艺路线版本` 弹窗。
- Component: `IntRuoyiFronted/src/views/mes/pro/route/index.vue`。
- Static test: `IntRuoyiFronted/tests/e2e/mes-route-version-list-draft-visible-static.spec.js`。
- Real E2E script: `IntRuoyiFronted/tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`。

## API Contracts And Data States

- API unchanged: `ProRouteApi.getRouteVersionList(routeId)` still returns the full route version list.
- Display state allow set: `DRAFT`、`ACTIVE`、`SUPERSEDED`。
- Hidden states: `PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED`。

## BDD Scenarios

BDD: 版本弹窗展示进行中草稿 -> Given 工艺路线版本列表包含 `DRAFT` 草稿和已生效历史版本 / When 用户打开工艺路线版本弹窗 / Then 表格可以看到正在进行的草稿版本，并仍能看到已生效历史版本

BDD: 非允许候选版本仍不展示 -> Given 工艺路线版本列表包含 `CANCELLED`、`REJECTED`、审核中和待生效等非允许状态 / When 用户打开工艺路线版本弹窗 / Then 表格不显示这些非允许状态，避免只删除旧过滤造成列表污染

BDD: 删除草稿后重新编辑新建草稿 -> Given 版本弹窗中存在当前 `DRAFT` 草稿 / When 用户点击“删除草稿”并确认 / Then 前端调用正式取消候选版本接口、刷新后隐藏已取消草稿；When 用户再次从路线列表点击“编辑” / Then 系统基于当前 `ACTIVE` 版本创建新的 `DRAFT` 草稿

## RED Command And Expected Failure

- RED: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> FAIL, expected reason: implementation does not define draft-visible `ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET`.

## GREEN Command And Passing Result

- GREEN: `node --check tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Layout unchanged; only table row allow set changed.
- Loading, empty text, error alert, status labels, permission guards and row actions remain unchanged.
- The existing `canEditRouteCandidateVersion` / `canSubmitRouteVersion` / `canCancelRouteVersion` guards continue to make visible `DRAFT` rows actionable without adding new fallback branches.

## E2E Or Component Verification Path

- Component/static path passed.
- Real path attempted on `http://127.0.0.1:8081` with backend `http://127.0.0.1:48081`; frontend PID `39032` belongs to `E:\IntRuoyi\IntRuoyiFronted` Vite and backend PID `50528` belongs to `E:\IntRuoyi` runtime jar.
- Real path blocked before browser launch because Playwright Chromium executable is missing in `E:\Int\DevCache\playwright-browsers`.

## Blockers And Follow-Up Skills

- Install or restore the configured Playwright Chromium browser cache, then rerun `node tests/e2e/mes-route-version-list-draft-visible-real.e2e.js` with the same local URLs.
- Resolve unrelated dirty worktree and branch divergence before task commit/push.
