# EDHR-STATIC-012 Verification Report

## Scope

- Fixed only EDHR-STATIC-012: production-release eDHR batch creation and archive manifest route identity after route master rename or republish.
- No Playwright/E2E, database writes, service start/stop/restart, remote operation, or git commit/push.

## Implementation Evidence

- `MesProEdhrBatchExecutionServiceImpl.openOrCreateFromProductionRelease` now requires `MesProRouteVersionDO.routeSnapshotJson` and derives persisted `routeCode`/`routeName` from the frozen route snapshot instead of current route master data.
- `buildArchiveManifest` now writes batch-saved `routeId`, `routeVersionId`, `routeVersionNo`, `routeCode`, and `routeName`.
- `requireArchiveFrozenRouteIdentity` now requires persisted route version identity before archive generation and validates saved batch code/name against `routeSnapshotJson`.
- Static and Java regression coverage now lock the route rename-before-batch-creation path and existing archive route identity guard.

## Verification

- RED: `node -e <HEAD baseline EDHR-STATIC-012 route identity assertion>` -> FAIL; baseline does not derive batch route code/name from frozen route snapshot.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation test` -> FAIL; partial implementation missed the static import for `PRO_EDHR_BATCH_EXECUTION_ROUTE_SNAPSHOT_REQUIRED`.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-012-archive-route-contract.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation test` -> PASS; Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation+generateArchive_usesFrozenBatchRouteIdentityAfterCurrentRouteRenameAndDelete test` -> PASS; Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#generateArchive_requiresFrozenRouteIdentityAndSnapshot test` -> PASS; Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- COMMIT: `git commit -m "fix: freeze eDHR archive route identity"` -> PASS; implementation commit `8e94cdb83` on branch `codex/20260914-edhr-static-012-route-rename-archive`.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-012-route-rename-archive --mode preview` -> BLOCKED; keep set was limited to `task.md`, `execution-log.md`, and `verification-report.md`, delete set was empty, blockers included non-fast-forward merge into `int_main`, dirty main worktree `E:\IntRuoyi`, and unrelated pending changes in the current worktree.

## Changed Paths

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-static-012-archive-route-contract.spec.cjs`
- `docs/backend-development.md`
- `docs/experience-index.md`
- `doc/tasks/20260914-edhr-static-012-route-rename-archive/task.md`
- `doc/tasks/20260914-edhr-static-012-route-rename-archive/execution-log.md`
- `doc/tasks/20260914-edhr-static-012-route-rename-archive/verification-report.md`

## Risks And Blockers

- Existing unrelated dirty workspace changes remain outside this task and were not modified for EDHR-STATIC-012.
- Merge into `int_main` cannot proceed safely until the branch is rebased/cherry-picked onto current `int_main` and the `E:\IntRuoyi` main worktree is clean.
- Cleanup apply was not run because preview reported blockers.
