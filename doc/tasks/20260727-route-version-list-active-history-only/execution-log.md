# Execution Log

## User Intent

- 用户指出：版本列表里只显示已生效的历史版本，取消的不显示。
- 截图显示版本工作区列表中 `V18`、`V17`、`V16` 等 `已取消` 版本仍出现在列表里。

## Preflight

- 已读取 `bug-regression-fix-loop` 与 `frontend-feature-delivery` 技能说明及证据契约。
- 已读取 `docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/experience-index.md`。
- 继续使用已隔离 worktree `D:\IntRuoyiWorktree\20260727-route-history-cancelled-version-view`，当前分支 `codex/20260727-route-history-cancelled-version-view`，不触碰主工作区 `E:\IntRuoyi` 的并行脏改动。

## BDD

BDD: 版本列表仅显示已生效历史版本 -> Given 路线版本列表包含 DRAFT、ACTIVE、SUPERSEDED 和 CANCELLED / When 用户打开版本工作区 / Then 列表仅展示 ACTIVE、SUPERSEDED，隐藏 DRAFT 和 CANCELLED 等未生效候选版本

BDD: 深链只读能力保留 -> Given 用户通过已有只读版本上下文打开已取消版本 / When 前端加载关系图 / Then 仍按历史 `routeVersionId` 请求后端读取冻结快照，写控件保持禁用

## Current Status

## Evidence

- `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 的版本工作区表格原先直接绑定 `routeVersions`。
- `loadRouteVersions` 从后端拿到所有版本后未在列表展示层过滤，导致 `CANCELLED` 出现在版本列表。
- Completion audit later found the first implementation only used `version.lifecycleStatus !== 'CANCELLED'`, so `DRAFT` and other non-effective candidates could still appear; this did not satisfy “只显示已生效的历史版本”.

## RED

RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> FAIL, expected reason: table still binds raw `routeVersions` instead of filtered `visibleRouteVersions`.

RED: `node -e "const assert=require('node:assert/strict'); const {execFileSync}=require('node:child_process'); const src=execFileSync('git',['show','HEAD:IntRuoyiFronted/src/views/mes/pro/route/index.vue'],{encoding:'utf8'}); assert.match(src,/const EFFECTIVE_ROUTE_VERSION_STATUS_SET = new Set/,'pre-fix version list must define effective-only status set');"` -> FAIL, expected reason: previous HEAD filtered only `CANCELLED` and did not define an effective-only status set.

## GREEN

GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js` -> PASS, `PASS: mes route version list shows effective historical versions only`.

GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS.

GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js` -> PASS, `PASS: mes route cancelled version uses readonly historical viewer`.

GREEN: `pnpm install --frozen-lockfile --reporter append-only` -> PASS after the first two install attempts timed out before completing top-level dependency links.

GREEN: `pnpm ts:check` -> PASS.

GREEN: `git diff --check` -> PASS, with CRLF warnings only.

GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, worktree `int_main slot=8`, frontend `8089`, backend `48089`.

## Real E2E Continuation

GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS, generated `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.

GREEN: backend-runtime-start -> PASS, copied Jar to `output\runtime\route-version-list-e2e\yudao-server-exec-slot8.jar` and started Java PID `65060` on `48089` with `--spring.profiles.active=local`.

GREEN: `Invoke-RestMethod http://127.0.0.1:48089/actuator/health` -> PASS, `status=UP`.

GREEN: frontend-runtime-start -> PASS, started Vite PID `33848` on `8089`, proxying backend `48089`.

GREEN: `Invoke-WebRequest http://127.0.0.1:8089/` -> PASS, HTTP `200`.

GREEN: `node --check tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

GREEN: `node tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

- Real frontend entry: `http://127.0.0.1:8089/mes/pro/route?code=RT000028`.
- Backend entry: `http://127.0.0.1:48089`, health `UP`.
- Runtime PIDs: Vite PID `64380`, Java PID `52756`.
- Tenant/user label: `芋道源码/admin`，password not recorded.
- Read-only data source: logged-in session GET `/admin-api/mes/pro/route/page` and GET `/admin-api/mes/pro/route-version/list-by-route?routeId=922119`.
- Target route: `RT000028` / `球囊扩张压力泵` / route ID `922119`.
- Visible effective versions asserted in UI: `V15 ACTIVE`, `V14 SUPERSEDED`, `V13 SUPERSEDED`, `V4 SUPERSEDED`, `V3 ACTIVE`, `V2 ACTIVE`, `V1 ACTIVE`.
- Hidden cancelled versions asserted absent from UI: `V18`, `V17`, `V16`, `V12`, `V11`, `V10`, `V9`, `V8`, `V7`, `V6`, `V5`.
- MES write requests: none (`mesWriteRequests=[]`).
- Artifact JSON: `output\e2e\route-version-list-active-history-only\mes-route-version-list-20260727164419.json`.
- Artifact screenshot: `output\e2e\route-version-list-active-history-only\mes-route-version-list-20260727164419.png`.

## Effective-Only Real E2E Follow-Up

GREEN: `node --check tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

GREEN: `node tests\e2e\mes-route-version-list-active-history-only-real.e2e.js` -> PASS.

- Real frontend entry: `http://127.0.0.1:8089/mes/pro/route?code=RT000028`.
- Backend entry: `http://127.0.0.1:48089`, health `UP`.
- Tenant/user label: `芋道源码/admin`，password not recorded.
- Target route: `RT000028` / `球囊扩张压力泵` / route ID `922119`.
- Visible effective versions asserted in UI: `V15 ACTIVE`, `V14 SUPERSEDED`, `V13 SUPERSEDED`, `V4 SUPERSEDED`, `V3 ACTIVE`, `V2 ACTIVE`, `V1 ACTIVE`.
- Hidden non-effective versions asserted absent from UI: `V19 DRAFT`, `V18 CANCELLED`, `V17 CANCELLED`, `V16 CANCELLED`, `V12 CANCELLED`, `V11 CANCELLED`, `V10 CANCELLED`, `V9 CANCELLED`, `V8 CANCELLED`, `V7 CANCELLED`, `V6 CANCELLED`, `V5 CANCELLED`.
- MES write requests: none (`mesWriteRequests=[]`).
- Artifact JSON: `output\e2e\route-version-list-active-history-only\mes-route-version-list-20260727170445.json`.
- Artifact screenshot: `output\e2e\route-version-list-active-history-only\mes-route-version-list-20260727170445.png`.

## Commit And Closeout

GREEN: implementation-commit -> PASS, `d1f37893 fix: hide cancelled route versions from list`.

GREEN: git-push -> PASS, `git push origin codex/20260727-route-history-cancelled-version-view`, remote branch HEAD `d1f378930cc5d8608e8b0f973d0543930461a280`.

GREEN: real-e2e-evidence-commit -> PASS, `5efc7cd1 test: add route version list real e2e evidence`.

GREEN: closeout-doc-commit -> PASS, `679cde37 docs: record route version real e2e commit`.

GREEN: runtime-stop -> PASS, stopped task-owned Vite PID `33848` on `8089` and Java PID `65060` on `48089`; postcheck showed `RELEASED port=8089` and `RELEASED port=48089`.

GREEN: effective-only-runtime-stop -> PASS, stopped task-owned Vite PID `64380` on `8089` and Java PID `52756` on `48089`; postcheck showed `RELEASED port=8089` and `RELEASED port=48089`.

BLOCKER: task-closeout-cleanup preview -> current branch cannot be fast-forward merged into `int_main`, and main worktree is dirty: `E:\IntRuoyi`.

- Preview keep: `task.md`, `execution-log.md`, `verification-report.md`, `bug-regression-evidence.md`, `frontend-feature-evidence.md`.
- Preview delete: none.
- Impact: implementation is committed and pushed, but cleanup apply / ff-only merge / worktree removal cannot run safely until the target main worktree and branch merge relationship are resolved.

## Implementation

- Added `EFFECTIVE_ROUTE_VERSION_STATUS_SET` and `isVisibleRouteVersionInWorkspace(version)` to show only `ACTIVE` / `SUPERSEDED` effective historical versions in the workspace table.
- Added `visibleRouteVersions` computed rows and bound the version table to it.
- Updated version workspace hints so open candidates are handled through the pending-version/edit entry instead of hidden candidate rows.
- Kept `canViewRouteVersion` unchanged so direct readonly historical version context still works.

## Experience Consolidation

GREEN: experience-preflight -> PASS, no new long-term document needed.

- Existing `docs/frontend-development.md#前端静态契约隔离门禁` covers the focused static contract approach.
- Existing `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁` covers keeping the older deep-link contract green.
- Existing `docs/e2e-rules.md#Worktree 隔离运行态 URL 门禁` covers paired slot frontend/backend URL verification for real E2E.
- Existing `docs/local-runtime.md#2026-07-27 本地后端运行 Jar 不可变门禁` covers copying the backend Jar to `output\runtime\...` before starting the long-running E2E backend.
- Existing `docs/worktree-memory.md#worktree-前端依赖启动门禁` covers the missing `node_modules` dependency recovery.

## Current Status

- Implementation and static + real E2E verification complete.
- Current status: ready_for_closeout; real E2E evidence and runtime release are complete, while closeout apply / ff-only merge remains blocked.
