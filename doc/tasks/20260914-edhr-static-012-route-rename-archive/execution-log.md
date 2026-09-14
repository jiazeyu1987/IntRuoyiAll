# Execution Log

## BDD Scenarios

BDD: EDHR-STATIC-012 route rename before batch creation -> Given an active order has frozen route V1 with historical route code, name, and version, and the same route is renamed or republished to V2 before eDHR batch creation; When the batch is created and later archived; Then the batch persisted route identity and archive manifest use the frozen V1 route code/name/version consistently, and no current V2 route name is mixed into the historical batch.

BDD: EDHR-STATIC-012 missing frozen route identity fails fast -> Given an active order or batch lacks the frozen route ID/code/name/version needed for archival identity; When batch creation or archive manifest generation requires route identity; Then the service blocks with an explicit route identity error instead of reading current route master data as a fallback.

## Evidence Log

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/backend-development.md` route identity, active-order snapshot, eDHR batch task source, historical batch read-only, and route version snapshot sections.
- Read `docs/product/active-order-latest-version-upgrade-restart-prd.md`.
- Read `docs/product/frontline-process-material-batch-record-mvp-prd.md`.
- Read `docs/changes/20260907-backup-minimal-closure.md`.
- Read `docs/bugs/20260912-edhr-90-step-static-audit.md`.
- Read `docs/bugs/20260913-edhr-additional-logic-audit.md`.
- Read `docs/acceptance/production-execution-main-loop/traceability-matrix.md`.
- Implemented frozen route identity extraction for production-release batch creation in `MesProEdhrBatchExecutionServiceImpl`.
- Extended archive manifest route identity output to include `routeVersionId` and `routeVersionNo`.
- Added Java regression for route rename between active-order freeze and batch creation.
- Extended static contract `mes-edhr-static-012-archive-route-contract.spec.cjs`.
- Consolidated reusable route identity lesson into `docs/backend-development.md` and `docs/experience-index.md`.

## RED / GREEN

- RED: `node -e <HEAD baseline EDHR-STATIC-012 route identity assertion>` -> FAIL, expected reason: baseline production-release batch creation did not derive batch route code/name from frozen route snapshot.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation test` -> FAIL, expected reason: partial implementation referenced `PRO_EDHR_BATCH_EXECUTION_ROUTE_SNAPSHOT_REQUIRED` without an import.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-012-archive-route-contract.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreateFromProductionRelease_usesFrozenRouteSnapshotIdentityAfterRouteRenameBeforeBatchCreation+generateArchive_usesFrozenBatchRouteIdentityAfterCurrentRouteRenameAndDelete test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#generateArchive_requiresFrozenRouteIdentityAndSnapshot test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-012-route-rename-archive --mode preview` -> BLOCKED, expected closeout blocker: current linked worktree branch could not be resolved because repository is on detached HEAD.
- COMMIT AUTHORIZATION: user requested `提交并融合进int_main`; created branch `codex/20260914-edhr-static-012-route-rename-archive`.
- RUNTIME PROFILE: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\reserve-worktree-slot.ps1 -Name IntRuoyi -Path C:\Users\BJB110\.codex\worktrees\330f\IntRuoyi -Branch codex/20260914-edhr-static-012-route-rename-archive -Profile int_main -AsJson` -> PASS, slot 20, frontend 8154, backend 48154.
- COMMIT: `git commit -m "fix: freeze eDHR archive route identity"` -> PASS, implementation commit `8e94cdb83`.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-012-route-rename-archive --mode preview` -> BLOCKED, expected closeout blockers: task branch cannot be fast-forward merged into `int_main`, main worktree `E:\IntRuoyi` is dirty, and current worktree still contains unrelated pending changes.

## Blockers

- Merge into `int_main` is blocked because branch `codex/20260914-edhr-static-012-route-rename-archive` was created from older HEAD `b4303b4` and cannot be fast-forward merged into current `int_main` `43071eb7e` as of 2026-09-14T09:17:44+08:00.
- Merge into `int_main` is also blocked because main worktree `E:\IntRuoyi` has unrelated dirty changes.
- Cleanup apply was not run because preview reported blockers.
