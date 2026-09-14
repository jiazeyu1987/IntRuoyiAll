# EDHR-STATIC-018 Execution Log

## Rule And Source Review

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/bugs/20260913-edhr-additional-logic-audit.md` and located EDHR-STATIC-018.
- Read `docs/changes/20260831-frontline-material-progress-erp-sync-source.md`.
- Read `docs/changes/20260903-stage1-all-formal-pick-lists.md`.
- Read `docs/product/frontline-process-material-batch-record-mvp-prd.md`.
- Read `docs/product/frontline-process-material-batch-record-mvp-user-operation.md`.
- Read relevant `docs/backend-development.md` material/batch, route binding, and eDHR boundaries.
- Used `bug-regression-fix-loop` and `backend-api-delivery` skills; read their required contract references.

## BDD Scenarios

- BDD: ordinary remark text cannot redirect material source -> Given ordinary order A has a free-text remark containing `[sourceActiveOrderId=<orderB>]`, When formal material pick-list and batch source resolution runs for order A, Then the resolver uses order A's own work-order / production-order identity and does not read order B's source.
- BDD: missing formal source for the current order remains current-order scoped -> Given order A has no formal production pick-list source and its remark names order B, When source resolution runs, Then the operation fails or returns empty according to existing current-order rules and does not treat order B as a substitute source.
- BDD: controlled cross-order source is not represented by free text -> Given a future cross-order simulation or copy flow needs another order's source, When source resolution is required, Then it must use an explicit controlled field or relation with authorization/audit evidence, not a remark parser.

## TDD Evidence

- RED: `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-018-remark-material-source-static.spec.cjs` -> FAIL, expected reason: `formal source resolution must not inspect free-text work-order remarks`.
- GREEN: `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-018-remark-material-source-static.spec.cjs` -> PASS.
- GREEN: `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-completion-all-pick-lists-static.spec.cjs` -> PASS.
- GREEN: `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-stage1-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProFeedbackMaterialBatchQueryServiceTest,MesTeamLeaderActiveOrderPickListCompletionSourceServiceTest" "-DfailIfNoTests=false" test` -> PASS, 14 tests, 0 failures, 0 errors, 0 skipped.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence .\doc\tasks\20260914-edhr-static-018-remark-material-source\verification-report.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence .\doc\tasks\20260914-edhr-static-018-remark-material-source\verification-report.md` -> PASS.
- GREEN: `git diff --check -- <task-owned tracked paths>` -> PASS; only CRLF conversion warnings were reported.

## Implementation Notes

- `MesFormalProductionPickListSourceResolver` now derives `productionOrderNo` only from the current `MesProWorkOrderDO.code`.
- Removed resolver dependency on `MesProcessPoolActiveOrderMapper` and the free-text `[sourceActiveOrderId=...]` parser.
- Updated batch query and completion source unit tests to use the reduced resolver constructor.
- Replaced the old simulation-copy batch query test with a regression proving a remark marker does not redirect batch lookup away from the current work order.

## Verification Evidence

- Static contract and targeted Java unit verification passed. No E2E, database write, service start/stop/restart, remote operation, git commit, or git push was performed.
- `task_closeout.py --task-id 20260914-edhr-static-018-remark-material-source --mode preview` -> BLOCKED: current linked worktree branch could not be resolved. Keep list contains task.md, execution-log.md, and verification-report.md; delete list is empty.
- Project experience consolidation: updated existing `docs/experience-index.md` with an EDHR-STATIC-018 keyword entry pointing to the existing formal-source backend rule.

## Closeout Resume Evidence

- User authorization: `先把 int_main 现有全部脏改动做基线提交，再提交并合入 EDHR-STATIC-018`.
- Baseline commit on `int_main`: `61f313cc4` (`docs: record DCC static 021 cleanup note`) captured the pre-existing dirty `int_main` worktree before EDHR-STATIC-018 fusion.
- Current task branch was attached from detached HEAD for an EDHR-STATIC-018-only implementation commit; unrelated worktree changes remain unstaged and out of scope.

## Blockers

- Product/code blocker: none found after targeted verification.
- Closeout blocker: none after user authorized commit and fusion into `int_main`; final cleanup, push, and completion evidence pending.

## Suggested Shared Defect Table Update

- EDHR-STATIC-018: `FIXED_STATIC_VERIFIED` - formal pick-list resolver no longer parses work-order remarks or `[sourceActiveOrderId=...]`; targeted static and Java unit verification passed. Runtime/E2E/database verification was intentionally not run in this scope.
