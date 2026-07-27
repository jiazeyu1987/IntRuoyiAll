# Verification Report

## Summary

- Status: ready_for_closeout
- Result: PASS for targeted frontend static contracts and TypeScript check.
- Scope: version workspace list hides cancelled route versions.

## Commands

- RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
  - Result: FAIL before fix.
  - Expected failure: table used raw `routeVersions`.

- GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
  - Result: PASS.

- GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route version list hides cancelled candidates only`.

- GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js`
  - Result: PASS.

- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route cancelled version uses readonly historical viewer`.

- GREEN: `pnpm install --frozen-lockfile --reporter append-only`
  - Result: PASS.
  - Note: first two plain install attempts timed out before linking top-level bins; final command completed dependency links.

- GREEN: `pnpm ts:check`
  - Result: PASS.

- GREEN: `git diff --check`
  - Result: PASS with CRLF warnings only.

- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1`
  - Result: PASS, frontend `8089`, backend `48089`.

- GREEN: `git push origin codex/20260727-route-history-cancelled-version-view`
  - Result: PASS.
  - Remote HEAD: `d1f378930cc5d8608e8b0f973d0543930461a280`.

- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-route-version-list-active-history-only --mode preview`
  - Result: blocked by non-ff-only merge relationship to `int_main` and dirty main worktree `E:\IntRuoyi`.
  - Cleanup plan: keep task records and evidence; delete none.

## Coverage

- Version table binds filtered rows.
- Filter hides only `CANCELLED`.
- `DRAFT`, active/effective historical versions, and non-cancelled candidate states remain available.
- Direct readonly historical version viewer contract remains green.

## Notes

- No backend production code changed.
- No runtime services or databases were started or modified.
