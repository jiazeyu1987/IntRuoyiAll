# EDHR-STATIC-018 Verification Report

## Scope

EDHR-STATIC-018 only. The changed behavior is limited to formal material pick-list and batch source resolution for production orders.

## Bug

Ordinary work-order remarks could contain `[sourceActiveOrderId=...]`, and the shared formal pick-list resolver would parse that free text to read another active order's work-order code. This allowed order A's material and batch source to be resolved from order B.

## Expected

Material and batch sources must be decided by the current formal order, work-order, document, or an explicit controlled relation with authorization/audit evidence. A remark is only explanatory text and must not change the data source.

## Contract

`MesFormalProductionPickListSourceResolver.resolve(workOrderId)` reads the current work order, trims the current work-order code, and resolves ERP-synchronized pick-list items by that production order number. It must not inspect `workOrder.remark`, parse `sourceActiveOrderId`, or load another active order.

## Validation

No fallback path was added. Missing work-order code, missing formal pick-list source, invalid header/status, and invalid item identity continue to fail fast through existing resolver errors.

## BDD:

- Given ordinary order A has a free-text remark containing `[sourceActiveOrderId=<orderB>]`, When formal material pick-list and batch source resolution runs for order A, Then the resolver uses order A's own work-order / production-order identity and does not read order B's source.
- Given order A has no formal production pick-list source and its remark names order B, When source resolution runs, Then the operation fails or returns empty according to existing current-order rules and does not treat order B as a substitute source.
- Given a future cross-order simulation or copy flow needs another order's source, When source resolution is required, Then it must use an explicit controlled field or relation with authorization/audit evidence, not a remark parser.

## Reproduction

RED command:

```powershell
node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-018-remark-material-source-static.spec.cjs
```

RED result: FAIL, `formal source resolution must not inspect free-text work-order remarks`.

## Root Cause

`MesFormalProductionPickListSourceResolver` treated `[sourceActiveOrderId=...]` embedded in `MesProWorkOrderDO.remark` as a source-routing directive and used `MesProcessPoolActiveOrderMapper` plus a second work-order lookup to replace the current production order number.

## RED:

`node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-018-remark-material-source-static.spec.cjs` -> FAIL, expected reason: `formal source resolution must not inspect free-text work-order remarks`.

## GREEN:

- `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-018-remark-material-source-static.spec.cjs` -> PASS.
- `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-completion-all-pick-lists-static.spec.cjs` -> PASS.
- `node .\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-stage1-static.spec.cjs` -> PASS.
- `mvn -pl yudao-module-mes "-Dtest=MesProFeedbackMaterialBatchQueryServiceTest,MesTeamLeaderActiveOrderPickListCompletionSourceServiceTest" "-DfailIfNoTests=false" test` -> PASS, 14 tests, 0 failures, 0 errors, 0 skipped.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence .\doc\tasks\20260914-edhr-static-018-remark-material-source\verification-report.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence .\doc\tasks\20260914-edhr-static-018-remark-material-source\verification-report.md` -> PASS.
- `git diff --check -- <task-owned tracked paths>` -> PASS; only CRLF conversion warnings were reported.

## Verification

Verified the static source contract, adjacent multi-pick-list completion contract, adjacent Stage1 static contract, two targeted Java unit classes, and skill evidence contracts. No Playwright/E2E, database write, service start/stop/restart, remote operation, git commit, or git push was performed.

Cleanup preview:

```powershell
python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-018-remark-material-source --mode preview
```

Result: BLOCKED. The script kept `task.md`, `execution-log.md`, and `verification-report.md`, planned no deletes, and reported `Current worktree branch could not be resolved.`

## Blockers

- Product/code blocker: none after targeted verification.
- Historical closeout blocker: the previous turn forbade git commit/push while `docs/task-closeout-rules.md` requires commit and push before a task may be marked `completed`; cleanup preview also reported the linked worktree had no resolved current branch.
- Current closeout status: user has now authorized baseline commit plus fusion into `int_main`; final cleanup, push, and completion evidence is pending.

## Changed Paths

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFormalProductionPickListSourceResolver.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFeedbackMaterialBatchQueryServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderPickListCompletionSourceServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-static-018-remark-material-source-static.spec.cjs`
- `docs/experience-index.md`
- `doc/tasks/20260914-edhr-static-018-remark-material-source/task.md`
- `doc/tasks/20260914-edhr-static-018-remark-material-source/execution-log.md`
- `doc/tasks/20260914-edhr-static-018-remark-material-source/verification-report.md`
