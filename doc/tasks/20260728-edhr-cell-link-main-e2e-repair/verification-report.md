# Verification Report

## Result

Frontend contract repair: PASS.

Real Playwright E2E: PASS on `int_main` main runtime and authorized `测试租户`.

## Static And Type Verification

- PASS: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- PASS: `node tests/e2e/edhr-pre-release-editable-submit-static.spec.js`
- PASS: `node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js`
- PASS: `node --check tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- PASS: `pnpm ts:check`
- BLOCKED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` currently fails on unrelated form-template API assertion `api misses templateId?: number`.

## Runtime E2E

- Runtime precheck: frontend `http://127.0.0.1:8081/` returned HTTP `200`; backend `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Runtime ownership: frontend PID listened from `E:\IntRuoyi\IntRuoyiFronted`; backend PID `50740` ran `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-110240.jar`.
- Command: `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081 EDHR_BATCH_E2E_TENANT_LABEL=测试租户 EDHR_BATCH_E2E_USERNAME=codexedhrcell01 EDHR_BATCH_E2E_REQUIRE_NEW_EXECUTION=0 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- Result: PASS. Playwright logged into the real frontend, opened the batch detail path, clicked `打开填写`, switched to `原表模式`, and asserted the persisted cell value on page.
- Evidence: `doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-evidence.md`.

## Fixture Diagnosis

- Authorized tenant/account: `122 / 测试租户`, `codexedhrcell01`.
- Task-owned batch execution: `900000000898 / BE-EDHR-CELL-20260728-104808`, batch code `EDHR-CELL-20260728-104808`.
- Batch task: `6955`, report `04fb8baed9d94f24bae6922f83e81e44 / 损耗单`, version `94`, execution `1579`.
- Fixture repair: `form_slot_type MAIN -> LOSS_REPORT`; `slot_config_snapshot_hash` filled with `0f84775df0c4a14feeedc6f606d4efc17434e2ce387ce93fb666ae91f26f8d52`.
- Link rule: `13`, source `PRODUCTION_WORK_ORDER.batchCode`, target cell `1:5`, scope `ROUTE_VERSION / 94`, enabled.
- Work task responsibility rollback: workTask `2243` was temporarily assigned to `codexedhrcell01` for the real path and rolled back to original `admin`.
- Persisted result: execution `1579` `cell_values_json` contains target cell `1:5` value `EDHR-CELL-20260728-104808`; `task/open` returned `cellLinkAutoPersist.status=NO_CHANGE_ALREADY_APPLIED`.

## Release Recommendation

The requested E2E can be claimed PASS for `int_main` main runtime: source batch number exists, the cell-link rule is active, and opening the execution record reads the persisted database value rather than a frontend fallback. One unrelated broad static contract remains blocked by a parallel form-template API assertion and is not part of this E2E sign-off.
