# Execution Log

## User Intent

- 用户指出：版本列表里只显示已生效的历史版本，取消的不显示。
- 截图显示版本工作区列表中 `V18`、`V17`、`V16` 等 `已取消` 版本仍出现在列表里。

## Preflight

- 已读取 `bug-regression-fix-loop` 与 `frontend-feature-delivery` 技能说明及证据契约。
- 已读取 `docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/experience-index.md`。
- 继续使用已隔离 worktree `D:\IntRuoyiWorktree\20260727-route-history-cancelled-version-view`，当前分支 `codex/20260727-route-history-cancelled-version-view`，不触碰主工作区 `E:\IntRuoyi` 的并行脏改动。

## BDD

BDD: 版本列表隐藏已取消候选版本 -> Given 路线版本列表包含 DRAFT、ACTIVE、SUPERSEDED 和 CANCELLED / When 用户打开版本工作区 / Then 列表展示 DRAFT、ACTIVE、SUPERSEDED，隐藏 CANCELLED

BDD: 深链只读能力保留 -> Given 用户通过已有只读版本上下文打开已取消版本 / When 前端加载关系图 / Then 仍按历史 `routeVersionId` 请求后端读取冻结快照，写控件保持禁用

## Current Status

## Evidence

- `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 的版本工作区表格原先直接绑定 `routeVersions`。
- `loadRouteVersions` 从后端拿到所有版本后未在列表展示层过滤，导致 `CANCELLED` 出现在版本列表。

## RED

RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> FAIL, expected reason: table still binds raw `routeVersions` instead of filtered `visibleRouteVersions`.

## GREEN

GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS, `PASS: mes route version list hides cancelled candidates only`.

GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS, `PASS: mes route cancelled version uses readonly historical viewer`.

GREEN: `pnpm install --frozen-lockfile --reporter append-only` -> PASS after the first two install attempts timed out before completing top-level dependency links.

GREEN: `pnpm ts:check` -> PASS.

GREEN: `git diff --check` -> PASS, with CRLF warnings only.

GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, worktree `int_main slot=8`, frontend `8089`, backend `48089`.

## Implementation

- Added `isVisibleRouteVersionInWorkspace(version)` to hide only `CANCELLED` versions in the workspace table.
- Added `visibleRouteVersions` computed rows and bound the version table to it.
- Kept `canViewRouteVersion` unchanged so direct readonly historical version context still works.

## Experience Consolidation

GREEN: experience-preflight -> PASS, no new long-term document needed.

- Existing `docs/frontend-development.md#前端静态契约隔离门禁` covers the focused static contract approach.
- Existing `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁` covers keeping the older deep-link contract green.
- Existing `docs/worktree-memory.md#worktree-前端依赖启动门禁` covers the missing `node_modules` dependency recovery.

## Current Status

- Implementation and verification complete.
- Current status: ready_for_closeout; commit and push pending.
