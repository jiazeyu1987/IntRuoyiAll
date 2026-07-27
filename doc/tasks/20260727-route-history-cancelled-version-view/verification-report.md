# Verification Report

## Summary

- Status: ready_for_closeout
- Result: PASS for targeted backend regression and frontend static contract.
- Scope: readonly viewing of cancelled or otherwise closed historical route versions.

## Commands

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: FAIL before production fix.
  - Expected failure: `CANCELLED` / `REJECTED` / `SUPERSEDED` read paths blocked by `PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE`; readonly candidate schedule rows empty.

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteProcessFlowServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Result: BUILD SUCCESS.
  - Tests: 96 run, 0 failures, 0 errors, 0 skipped.

- GREEN: `node --check tests/e2e/mes-route-cancelled-version-view-static.spec.js`
  - Result: PASS.

- GREEN: `node tests/e2e/mes-route-cancelled-version-view-static.spec.js`
  - Result: PASS.
  - Output: `PASS: mes route cancelled version uses readonly historical viewer`.

## Coverage

- `CANCELLED` route graph readonly snapshot read.
- `REJECTED` route graph readonly snapshot read.
- `SUPERSEDED` route graph readonly snapshot read.
- `CANCELLED` / `REJECTED` flow config readonly snapshot read.
- Candidate schedule snapshot read for `PENDING_APPROVAL`, `READY_TO_PUBLISH`, `REJECTED`, and `CANCELLED`.
- `CANCELLED` write rejection for graph save, flow config save, and schedule config save.
- Frontend viewer handoff keeps route version ID, version number, and lifecycle status; frozen versions keep write controls disabled.

## Notes

- No frontend production TypeScript was changed.
- No runtime services or databases were started or modified.
- Main workspace `E:\IntRuoyi` contains unrelated dirty changes; this task was implemented in isolated worktree `D:\IntRuoyiWorktree\20260727-route-history-cancelled-version-view`.
