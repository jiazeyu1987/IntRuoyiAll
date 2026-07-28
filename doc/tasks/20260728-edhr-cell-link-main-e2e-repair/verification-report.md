# Verification Report

## Result

Frontend contract repair: PASS.

Real Playwright E2E: PASS on `int_main` main runtime and authorized `测试租户`.

Screenshot regression root-cause verification: PASS on isolated slot 7 runtime (`8088/48088`) with the backend task-id fix loaded.

## Static And Type Verification

- PASS: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- PASS: `node tests/e2e/edhr-pre-release-editable-submit-static.spec.js`
- PASS: `node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js`
- PASS: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-cell-link-task-id-context-static.spec.cjs`
- PASS: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `node --check tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- PASS: `pnpm ts:check`
- BLOCKED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` currently fails on unrelated form-template API assertion `api misses templateId?: number`.

## Runtime E2E

- Runtime precheck: frontend `http://127.0.0.1:8081/` returned HTTP `200`; backend `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Runtime ownership: frontend PID listened from `E:\IntRuoyi\IntRuoyiFronted`; backend PID `50740` ran `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-110240.jar`.
- Command: `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081 EDHR_BATCH_E2E_TENANT_LABEL=测试租户 EDHR_BATCH_E2E_USERNAME=codexedhrcell01 EDHR_BATCH_E2E_REQUIRE_NEW_EXECUTION=0 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- Result: PASS. Playwright logged into the real frontend, opened the batch detail path, clicked `打开填写`, switched to `原表模式`, and asserted the persisted cell value on page.
- Evidence: `doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-evidence.md`.

## Slot 7 Screenshot Regression E2E

- Root cause: `MesProEdhrBatchExecutionServiceImpl.buildOpenOrCreateExecutionReq(...)` passed `.setTaskId(null)` for traditional batch-record opens, so execution-record lookup/creation could reuse the wrong coarse context instead of the current batch task.
- Fix loaded: `.setTaskId(task.getId())`, with JUnit assertions updated to require the current batch task ID.
- Runtime: isolated worktree `D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime`, frontend `http://127.0.0.1:8088`, backend `http://127.0.0.1:48088`, backend health `UP`.
- Command: `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8088 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48088 EDHR_BATCH_E2E_TENANT_LABEL=测试租户 EDHR_BATCH_E2E_USERNAME=codexedhrcell01 EDHR_BATCH_E2E_REQUIRE_NEW_EXECUTION=0 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- Result: PASS. `task/open` returned `cellLinkAutoPersist.status=NO_CHANGE_ALREADY_APPLIED`; execution detail and original-form page input both showed `EDHR-CELL-20260728-104808`.
- Evidence: `doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-slot7-evidence.md`.

## Fixture Diagnosis

- Authorized tenant/account: `122 / 测试租户`, `codexedhrcell01`.
- Task-owned batch execution: `900000000898 / BE-EDHR-CELL-20260728-104808`, batch code `EDHR-CELL-20260728-104808`.
- Batch task: `6955`, report `04fb8baed9d94f24bae6922f83e81e44 / 损耗单`, version `94`, execution `1579`.
- Fixture repair: `form_slot_type MAIN -> LOSS_REPORT`; `slot_config_snapshot_hash` filled with `0f84775df0c4a14feeedc6f606d4efc17434e2ce387ce93fb666ae91f26f8d52`.
- Link rule: `13`, source `PRODUCTION_WORK_ORDER.batchCode`, target cell `1:5`, scope `ROUTE_VERSION / 94`, enabled.
- Work task responsibility rollback: workTask `2243` was temporarily assigned to `codexedhrcell01` for the real path and rolled back to original `admin`.
- Persisted result: execution `1579` `cell_values_json` contains target cell `1:5` value `EDHR-CELL-20260728-104808`; `task/open` returned `cellLinkAutoPersist.status=NO_CHANGE_ALREADY_APPLIED`.

## Release Recommendation

The requested E2E can be claimed PASS for the tested authorized fixture: source batch number exists, the cell-link rule is active, opening the execution record reads the persisted database value rather than a frontend fallback, and the screenshot-consistent backend task-context bug is fixed and verified on an isolated runtime. The exact screenshot batch `881M009889` is not present in the current local database, so that specific batch could not be directly replayed locally. One unrelated broad static contract remains blocked by a parallel form-template API assertion and is not part of this E2E sign-off.

## Cleanup

- PASS: slot 7 frontend/backend processes stopped; ports `8088/48088` no longer listen.
- PASS: temporary worktree `D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime` removed; branch `codex/20260728-edhr-cell-link-taskid-runtime` deleted.
- PASS: port registry entry for slot 7 marked `active=false` with cleanup metadata.
- PASS: task-closeout-cleanup preview/apply completed with no delete, blocked, or warning entries.
- NOT RUN: commit/push, because the main workspace contains unrelated concurrent dirty changes and the current user scope was E2E verification only.
