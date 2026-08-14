# Verification Report

## Scope

- Verify `int_main` contains the replan fix from `origin/codex/replan-current-route-after-feedback`.
- Verify the behavior requested by the user: old feedback/finished records only determine completed quantity, while remaining work is scheduled from the current latest route and its available workstation/line/capacity.
- Verify conflict-adjacent frontend static contracts still pass after merging `origin/int_main`.

## Results

- PASS: `scripts\preflight\branch-runtime-port-guard.ps1`
  - `int_main` frontend `8081`, backend `48081`.
- PASS: `git diff --check -- IntRuoyiBackend IntRuoyiFronted`
  - Only CRLF warnings were emitted; no whitespace errors.
- PASS: `node IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js`
- PASS: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` from `IntRuoyiFronted`
- PASS: `node IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs`
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation+replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

## Commit Evidence

- Baseline before merge: `93ed7a841bc2e0f02965b88181045b79b7f4a1be`
- Baseline residual before merge: `0e33c7f4bd0a2d450e45ba66813b656694623469`
- `origin/int_main` merge: `4c865c4b1`
- Replan branch merge: `d310b19e3`
- Baseline residual after merge: `bdd31e608`
- Baseline residual closeout: `0f6ef01c3`
- Baseline active-order E2E evidence: `bdea0ba9c`
- Baseline frontline employee E2E evidence: `da25efec0`
- Baseline frontline employee experience log: `b943b2b85`
- Baseline submit round2 task records: `159a5ba95`
- Baseline concurrent ERP/PQC state: `a87234f9a`
- Baseline concurrent task record updates: `3c8e900aa`
- Baseline concurrent process/PQC state: `9f0a20319`

## Closeout

- PASS: `task-closeout-cleanup` preview/apply kept only `task.md`, `execution-log.md`, and `verification-report.md`; delete `<none>`, blocked `<none>`, warnings `<none>`.
- PASS: `git push origin int_main` updated `origin/int_main` from `3fd9a221e` to `b943b2b85`.
- PASS: additional remote sync reached `3c8e900aa` before this final record update.
- Pending at the time of this file edit: push `9f0a20319` and the final completion-record commit, then verify `git status --short --branch` reports no ahead state. Task-outside dirty files may remain if active parallel tasks continue writing.
