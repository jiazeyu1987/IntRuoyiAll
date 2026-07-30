# Verification Report

## Summary

- Status: blocked.
- Implementation result: PASS for targeted static contracts and TypeScript.
- Real E2E result: BLOCKED before page interaction by missing Playwright Chromium executable.

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

- GREEN: `pnpm ts:check`
  - Result: PASS.

- GREEN: `node --check tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`
  - Result: PASS.

- BLOCKED: `node tests/e2e/mes-route-version-list-draft-visible-real.e2e.js`
  - URLs: frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`.
  - Runtime precheck: frontend HTTP `200`, backend health `UP`.
  - Blocker: Playwright Chromium executable missing at `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\chrome-headless-shell-win64\chrome-headless-shell.exe`.

- GREEN: `git diff --check -- <task-owned files>`
  - Result: PASS with CRLF warning only for `IntRuoyiFronted/src/views/mes/pro/route/index.vue`.

- GREEN: evidence validators
  - Result: PASS for bug regression evidence and frontend feature evidence.
  - UTF-8 readback: PASS for all task markdown files.

## Coverage

- `DRAFT` is included in the version workspace visible status set.
- `ACTIVE` and `SUPERSEDED` remain visible.
- `PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED` remain excluded from visible table rows.
- Cancelled/read-only historical route viewer static contract remains green.
- Route version workspace action and permission static contract remains green.

## Remaining Blockers

- Real Playwright page verification requires restoring the configured Chromium browser cache.
- Commit/push/closeout requires separating or resolving unrelated dirty worktree changes and the current branch divergence from `origin/int_main`.
