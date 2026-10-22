# Verification Report

## Result

The prior compile blocker is resolved; targeted verification, full server packaging, push, and runtime restart all pass.

## Evidence

- `scripts/preflight/branch-runtime-port-guard.ps1`: PASS for `int_main` (`8081` / `48081`).
- `mvn -pl yudao-module-mes '-Dtest=MesTeamLeaderActiveOrderServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`: FAIL during `testCompile` with 15 errors caused by missing BPM form-center types/methods.
- `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldExposeActiveOrderWhenFormalRouteIsMissing+shouldExposeInvalidActiveOrderWithoutBlockingValidRows+shouldRetainMissingOutputSnapshotAndHealthyOrder+shouldExposeActiveOrderWhenVersionDoesNotBelongToRoute' '-Dsurefire.failIfNoSpecifiedTests=false' test`: PASS, 4/4.
- `mvn -pl yudao-module-system -am '-Dtest=AdminAuthServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`: PASS, 20/20.
- `pnpm ts:check`: PASS, exit code 0.
- Five changed/new frontend static contracts: PASS.
- `mvn -pl yudao-server -am '-DskipTests' package`: PASS, full 31-module reactor and executable Jar generated.
- Standard full restart script: PASS, Maven `BUILD SUCCESS`, runtime Jar `output/runtime/int_main/backend-runtime-control-20260915-101359.jar`.
- Backend listener `48081` PID `15212`, health body `{"status":"UP"}`.
- Frontend listener `8081` PID `71464`, HTTP status `200`.

## Commit and Runtime Gate

Baseline commit `2aa120ebc` and task-record commit `4139270eb` completed after staged-file and port-guard checks; both are pushed to `origin/int_main`. Cleanup preview/apply both passed with no delete, blocked, or warning entries. Task status is `completed` pending the final closeout-record commit and push.
