# Verification Report

## Summary

- Status: ready_for_closeout
- Result: PASS for targeted frontend static contracts, TypeScript check, and real Playwright E2E.
- Scope: version workspace list shows only effective historical route versions.

## Commands

- RED: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
  - Result: FAIL before fix.
  - Expected failure: table used raw `routeVersions`.

- RED: effective-only audit command against previous HEAD
  - Result: FAIL before effective-only fix.
  - Expected failure: previous HEAD did not define `EFFECTIVE_ROUTE_VERSION_STATUS_SET` and filtered only `CANCELLED`.

- GREEN: `node --check tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
  - Result: PASS.

- GREEN: `node tests/e2e/mes-route-version-list-active-history-only-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route version list shows effective historical versions only`.

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

- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package`
  - Result: PASS.
  - Output: generated `yudao-server-exec.jar`.

- GREEN: backend runtime start
  - Result: PASS.
  - Runtime Jar: `output\runtime\route-version-list-e2e\yudao-server-exec-slot8.jar`.
  - PID/port: Java PID `65060`, backend `48089`.

- GREEN: `Invoke-RestMethod http://127.0.0.1:48089/actuator/health`
  - Result: PASS, `status=UP`.

- GREEN: frontend runtime start
  - Result: PASS.
  - PID/port: Vite PID `33848`, frontend `8089`, backend proxy `48089`.

- GREEN: `Invoke-WebRequest http://127.0.0.1:8089/`
  - Result: PASS, HTTP `200`.

- GREEN: `node --check tests\e2e\mes-route-version-list-active-history-only-real.e2e.js`
  - Result: PASS.

- GREEN: `node tests\e2e\mes-route-version-list-active-history-only-real.e2e.js`
  - Result: PASS.
  - Output: `PASS: route version workspace shows effective historical versions only; result=...\mes-route-version-list-20260727170445.json`.

- GREEN: `git push origin codex/20260727-route-history-cancelled-version-view`
  - Result: PASS before real E2E continuation.
  - Remote HEAD before real E2E continuation: `778fc54d`.

- GREEN: real E2E evidence commit
  - Result: PASS.
  - Commit: `5efc7cd1 test: add route version list real e2e evidence`.

- GREEN: effective-only implementation commit
  - Result: PASS.
  - Commit: `9808147d fix: show only effective route version history`.

- GREEN: runtime stop and port release
  - Result: PASS.
  - Stopped task-owned Vite PID `33848` and Java PID `65060`.
  - Ports released: `8089`, `48089`.

- GREEN: effective-only runtime stop and port release
  - Result: PASS.
  - Stopped task-owned Vite PID `64380` and Java PID `52756`.
  - Ports released: `8089`, `48089`.

- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-route-version-list-active-history-only --mode preview`
  - Result: blocked by non-ff-only merge relationship to `int_main` and dirty main worktree `E:\IntRuoyi`.
  - Cleanup plan: keep task records and evidence; delete none.

## Coverage

- Version table binds filtered rows.
- Filter shows only current/effective `ACTIVE` versions and `SUPERSEDED` historical versions.
- `DRAFT`,审核中、待生效、已驳回 and `CANCELLED` non-effective candidate versions are hidden from the version list.
- Direct readonly historical version viewer contract remains green.
- Real UI route `RT000028` / `球囊扩张压力泵` shows effective historical rows `V15`, `V14`, `V13`, `V4`, `V3`, `V2`, `V1`.
- Real UI version workspace hides non-effective rows including `V19 DRAFT` and cancelled rows `V18`, `V17`, `V16`, `V12`, `V11`, `V10`, `V9`, `V8`, `V7`, `V6`, `V5`.
- Real E2E recorded `mesWriteRequests=[]`, so verification stayed read-only.

## Notes

- No backend production code changed.
- Runtime services were started only in the isolated slot 8 worktree for E2E verification: frontend `8089`, backend `48089`.
- Task-owned runtime services were stopped after E2E and ports `8089/48089` were released.
- No database writes were performed by the E2E path.
