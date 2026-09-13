# Execution Log

## BDD

BDD: future approval route does not replace current route early -> Given an old route is active and already effective, and a new route is saved with tomorrow's `effectiveTime` When a file is submitted or the route is previewed today Then the old route remains selected.

BDD: effective approval route replaces older route when due -> Given an old route is active and already effective, and a newer active route has `effectiveTime` at or before the selection time When a file is submitted or the route is previewed after that time Then the newer route is selected.

BDD: saving future route preserves current route -> Given a category has an active route whose `effectiveTime` is already due When an administrator saves a new route with a future `effectiveTime` Then the old route remains active and selectable until the new route is due.

## RED

RED: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest test` -> FAIL, expected reason: current save logic deactivates the already-effective old route and current route selection chooses the maximum active version without checking `effectiveTime`.

## Evidence

- Loaded bug regression skill and `references/bug-contract.md`.
- Read required project docs: `docs/task-closeout-rules.md`, `docs/backend-development.md`, `docs/database-rules.md`, and `docs/powershell-encoding.md`.
- Read DCC-STATIC-020 evidence from `E:\IntRuoyi\docs\bugs\20260912-dcc-90-step-static-audit.md`; the file is untracked in `E:\IntRuoyi` and absent from this clean worktree.
- RED: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest test` -> FAIL. New regression tests failed as expected:
  - `testSaveRoute_futureEffectiveRouteKeepsCurrentRouteSelectable`: expected old route `active=true`, actual `false`.
  - `testPreviewRoute_futureEffectiveRouteDoesNotReplaceCurrentRouteEarly`: expected stage 1 user `[200]`, actual future route user `[300]`.
- Root cause: `DccApprovalRouteAdminServiceImpl.saveRoute` deactivated every other active route immediately after saving a new route, regardless of the new route's future `effectiveTime`; `previewRoute` and runtime callers ultimately used `selectLatestActiveByCategoryId`, which selected the maximum active version without excluding routes whose `effectiveTime` had not arrived.
- Fix: `DccCategoryApprovalRouteMapper.selectLatestActiveByCategoryId` now filters active routes by `effectiveTime <= selection time`, with an overload for deterministic tests. `previewRoute` now uses the same mapper rule as runtime submission. `saveRoute` only deactivates other already-effective active routes when the newly saved route is already effective; saving a future route preserves the currently effective route.
- GREEN: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest test` -> PASS, 17 tests, 0 failures, 0 errors, 0 skipped.
- REGRESSION: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-020-route-effective-time\bug-regression-evidence.md` -> PASS, bug regression evidence is valid.
- REGRESSION: `python -X utf8 -c "<read task evidence files as UTF-8>"` -> PASS, UTF-8 read check passed.
- Project experience consolidation: appended `docs/worktree-memory.md#干净-worktree-与主工作区未跟踪证据分离门禁` and indexed it in `docs/experience-index.md`, because this task found the referenced bug file only as an untracked main-workspace evidence file while fixing in a clean worktree.
- 2026-09-13 local integration authorization: user requested fusion into `int_main`.
- Rechecked main worktree `E:\IntRuoyi`: target DCC-STATIC-020 source/test changes were already present in the `int_main` working tree and staged; non-task DCC/frontend/docs changes remained separate and were not modified.
- MAIN GREEN: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest test` from `E:\IntRuoyi\IntRuoyiBackend` -> PASS, 17 tests, 0 failures, 0 errors, 0 skipped.
- MAIN PREFLIGHT: `powershell -ExecutionPolicy Bypass -File E:\IntRuoyi\scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `int_main/int_main`, frontend 8081, backend 48081.
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-020-route-effective-time --mode preview` -> PASS; keep `task.md`, `execution-log.md`, `verification-report.md`; delete only `bug-regression-evidence.md`.
- CLEANUP APPLY: same script with `--mode apply` -> PASS; deleted only `doc\tasks\20260913-dcc-static-020-route-effective-time\bug-regression-evidence.md`.
- Project experience consolidation: reviewed under closeout baseline; no new durable lesson beyond the already-added clean-worktree/untracked-evidence memory entry.
- Not run: E2E, service start/restart, database writes, remote operations, Git push; all remain outside current delegated scope.
