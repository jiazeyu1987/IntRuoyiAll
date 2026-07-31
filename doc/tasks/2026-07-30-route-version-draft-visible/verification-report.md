# Verification Report

## Summary

- Status: ready_for_closeout.
- Implementation result: PASS for targeted static contracts, TypeScript, and target backend lifecycle regression.
- Real E2E result: PASS through real Playwright page path on local `8081/48081` using system Chrome.
- Closeout result: BLOCKED by mixed baseline commit, unrelated dirty worktree changes, and branch push policy.

## Commands

- RED: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js`
  - Result: FAIL before fix.
  - Expected failure: missing `ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET` containing `DRAFT`.

- GREEN: `node --check tests/e2e/mes-route-version-list-draft-visible-static.spec.js`
  - Result: PASS.

- GREEN: `node tests/e2e/mes-route-version-list-draft-visible-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route version list shows active drafts and effective history`.

- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route cancelled version uses readonly historical viewer`.

- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js`
  - Result: PASS.
  - Output: `mes-pro-route-version-workspace-static PASS`.

- GREEN: `node tests/e2e/mes-route-list-edit-create-candidate-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route list edit uses single open candidate entry`.

- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: PASS.
  - Output: 17 tests, 0 failures, 0 errors, `BUILD SUCCESS`.

- GREEN: `mvn.cmd "-Dtest=MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" surefire:test`
  - Result: PASS.
  - Output: 17 tests, 0 failures, 0 errors, `BUILD SUCCESS`.
  - Note: this direct goal was used only as supplemental target verification while the standard lifecycle was temporarily blocked by an unrelated missing test class; the standard reactor command was rerun and passed after that concurrent blocker was removed.

- GREEN: `pnpm ts:check`
  - Result: PASS.

- GREEN: `node --check tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`
  - Result: PASS.

- BLOCKED: `node tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`
  - URLs: frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`.
  - Runtime precheck: frontend HTTP `200`, backend health `UP`.
  - Blocker: Playwright Chromium executable missing at `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe`.

- GREEN: `node --check doc/tasks/2026-07-30-route-version-draft-visible/route-draft-delete-recreate-real.e2e.cjs`
  - Result: PASS.

- GREEN: `node doc/tasks/2026-07-30-route-version-draft-visible/route-draft-delete-recreate-real.e2e.cjs`
  - Result: PASS.
  - Browser: `C:\Program Files\Google\Chrome\Application\chrome.exe`.
  - URLs: frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`.
  - Identity label: `芋道源码/admin`; password was not logged.
  - Output: `PASS: route draft delete and recreate E2E completed`.
  - Result JSON: `E:\IntRuoyi\output\playwright\route-draft-delete-recreate\20260730135955\route-draft-delete-recreate-result.json`.

- GREEN: `git diff --check -- <task-owned files>`
  - Result: PASS with CRLF warning only for `IntRuoyiFronted/src/views/mes/pro/route/index.vue`.

- GREEN: evidence validators
  - Result: PASS for bug regression evidence and frontend feature evidence.
  - UTF-8 readback: PASS for all task markdown files.
  - Task-doc `git diff --check`: PASS with line-ending conversion warnings only.

## Coverage

- `DRAFT` is included in the version workspace visible status set.
- `ACTIVE` and `SUPERSEDED` remain visible.
- `PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED` remain excluded from visible table rows.
- Cancelled/read-only historical route viewer static contract remains green.
- Route version workspace action and permission static contract remains green.
- DRAFT uses the explicit “删除草稿” action with confirmation; user cancellation does not call the backend.
- Confirmed deletion uses the existing cancel endpoint, shows “删除草稿成功”, then refreshes both version workspace and route list.
- `CANCELLED` is not an open candidate; a later edit creates a new DRAFT from current `activeRouteVersionId`.
- Backend regression proves the previous DRAFT becomes `CANCELLED`, the next version becomes a new `DRAFT`, and its `sourceRouteVersionId` is the current active version.
- Real E2E copied source route `RT000028/#922119` to task route `CODX-DRAFT-20260730135955/#922276`, created first draft `V2/#487` from active `#486`, deleted it to `CANCELLED`, then created new draft `V3/#488` from the same active version.
- Real E2E asserted the version dialog displayed “删除草稿” for the draft row, displayed the delete confirmation text, hid the cancelled draft after refresh, and created a new draft ID after the next list “编辑”.
- Real E2E cleanup cancelled final draft `#488` and deleted copied route `#922276` through the page; the final API read confirmed the copied route was absent.

## Remaining Blockers

- The configured Playwright Chromium browser cache is still missing; the completed real E2E used the explicitly recorded system Chrome path instead.
- Mixed baseline commit `67282a868c449ee0ea652491cfd45dc448b258e9` already combines task-owned files with non-task changes; independent task commit history cannot be reconstructed without an explicit shared-branch history decision.
- At the final verification checkpoint, branch state was `int_main...origin/int_main [ahead 7]`; push/closeout remains blocked by the project policy requiring clean branch push state.
- Concurrent dirty changes `doc/tasks/20260730-route-admin-list-layout-unification/*`、`doc/tasks/20260730-standard-template-list-search-alias/*`、`docs/e2e-rules.md`、`docs/experience-index.md` are outside this task and were not modified, staged, or committed by this task.
- The retained task E2E script is ignored by `.gitignore:99 doc/tasks/**/*.cjs`; if this task is later committed, it must be staged explicitly with `git add -f`.
