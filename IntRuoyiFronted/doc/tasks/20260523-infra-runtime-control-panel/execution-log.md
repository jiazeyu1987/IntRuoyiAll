# Execution Log

BDD: Runtime matrix is visible -> Given an operator opens `基础设施 / 运行控制台`, When the page loads the overview API, Then Local/Test/Production columns and IntRuoyi frontend/backend/full plus Website frontend rows are visible.

BDD: Non-production restart confirms once -> Given an operator clicks restart for Local or Test, When they confirm the dialog, Then the page calls `/infra/runtime-control/restart` with environment, component, and reason.

BDD: Production restart requires explicit guard -> Given an operator clicks restart for Production, When the reason is blank or confirmation text is not exactly `PROD`, Then the page does not dispatch the restart request.

BDD: Polling reconnects after runtime interruption -> Given a restart interrupts local frontend or backend connectivity, When overview polling fails temporarily, Then the page keeps the last state and retries until the API is reachable.

RED: `node tests/e2e/runtime-control-static.spec.js` -> FAIL, expected missing `src/api/infra/runtimeControl/index.ts`.

GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS

REGRESSION: `pnpm ts:check` -> FAIL, Node default heap exhausted before type diagnostics.

REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL, existing unrelated showroom narration/payload type assertions in `src/views/showroom-admin/narration/NarrationWorkspace.vue` and `src/views/showroom-frontstage/shared/payload.ts`.

REGRESSION: Playwright `open http://localhost:8081`, login with visible test tenant defaults -> PASS, reached `/index`.

REGRESSION: Playwright `goto http://localhost:8081/infra/runtime-control` -> FAIL, route returned 404 because the local DB menu route has not been applied and current runtime has not been restarted with this task's backend/frontend code.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS after narrowing the existing showroom narration/payload type assertions.

GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS after adding the extended overview timeout and non-overlapping polling guard.

REGRESSION: Playwright `goto http://localhost:8081/infra/monitors/runtime-control` after menu API creation -> PASS, route renders `基础设施 / 监控中心 / 运行控制台`.

BLOCKED: Playwright populated matrix verification -> FAIL, local backend `48081` was replaced by `output/runtime/backend-ebr-visual-fidelity-*.jar`; page received `No static resource admin-api/infra/runtime-control/overview.` from the old backend.

BLOCKED: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component full` -> FAIL, current-source backend startup dies on missing table `ruoyi-vue-pro.dcc_controlled_file_nas_transfer_task`, so the latest dirty workspace cannot yet recover the local backend/full runtime after restart.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session runtime-control run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-infra-runtime-control-panel\scripts\verify-runtime-control-live.mjs` -> PASS, `http://127.0.0.1:8081/infra/monitors/runtime-control` rendered the Local/Test/Production matrix from `http://localhost:48081/admin-api/infra/runtime-control/overview`, `local intruoyi-backend` showed `运行中 / listening / HTTP 200`, `prod website-frontend` showed degraded status for `http://172.30.30.57:8083/`, and the production dialog blocked empty reason / missing `PROD` without sending any restart request.

GREEN: current-source local runtime rerun -> PASS, `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component full` recovered the backend and frontend from the current source tree.

GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS on the final closeout rerun.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS on the final closeout rerun.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session runtime-control run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-infra-runtime-control-panel\scripts\verify-runtime-control-live.mjs` -> PASS on the current-source local runtime; the page fetched `http://127.0.0.1:48081/admin-api/infra/runtime-control/overview`, and production restart remained blocked until `PROD` is entered.

GREEN: scoped frontend commit -> PASS, committed only runtime-control task files in `yudao-ui-admin-vue3` as `8e414b8c (任务: 新增运行控制台页面)`.
