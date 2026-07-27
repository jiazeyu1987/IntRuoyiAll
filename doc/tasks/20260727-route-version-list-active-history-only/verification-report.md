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

## Coverage

- Version table binds filtered rows.
- Filter hides only `CANCELLED`.
- `DRAFT`, active/effective historical versions, and non-cancelled candidate states remain available.
- Direct readonly historical version viewer contract remains green.

## Notes

- No backend production code changed.
- No runtime services or databases were started or modified.
