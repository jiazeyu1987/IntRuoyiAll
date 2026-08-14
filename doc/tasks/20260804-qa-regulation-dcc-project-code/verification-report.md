# Verification Report

## Result

- Status: PASS for implementation/static/unit verification; BLOCKED for the new real status-split E2E because formal local DCC data is missing the `IDI -> productMasterId` binding.
- Scope: `QaRegulationPage.vue` DCC project selector, formal QA configuration status split, pressure-pump `IDI` template retention, backend `project-statuses` API, static contracts, TypeScript check, targeted JUnit, and read-only real QA page checks.

## Evidence

- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest" test` -> PASS; `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/frontend-feature-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/backend-api-evidence.md` -> PASS.
- Runtime Jar refresh for E2E -> PASS; `48081` is running `output/runtime/int_main/backend-runtime-control-20260805-qa-regulation-dcc-status-20260805-003532.jar`, health `UP`, nested MES jar stored, SHA256 `DEFA78D20752D4A35348A4C37C45216823627D3F5848D858A2915F00BDB86ACB`.
- `node scripts\preflight\login-preflight.mjs` with env-sourced local default login and target `/mes/pro/process-pool/qa-regulation` -> PASS.
- `node tests\e2e\mes-edhr-qa-menu-real.e2e.js` -> PASS; screenshot `output/playwright/20260804-qa-regulation-tab/edhr-qa-menu-real-e2e.png`.
- `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS after selecting DCC project `IDI`; screenshot `output/playwright/20260804-qa-regulation-tab/qa-regulation-original-excerpt-real-e2e.png`.
- `pnpm e2e:qa-regulation:dcc-status:real` -> BLOCKED; `IDI` DCC project code id `129` returned `productMasterId=null`, so the real page cannot verify the backend product-level `project-statuses` split without a formal DCC-to-product binding.
- Scoped `git diff --check` -> PASS.

## Notes

- The page now separates loaded DCC projects into `已配置 QA 规程` and `待配置 QA 规程` using backend QA regulation records keyed by DCC `productMasterId`.
- The status summary is hidden while DCC projects or QA status data are loading or when loading fails, so QA is not shown a false `0 / 0` result.
- `IDI` and the pressure-pump regulation template remain as the selected project's draft initializer; they are no longer the page-level source of configured/unconfigured status.
- The page keeps the visible warning that formal save/publish persistence is not connected, so no unconfigured project is silently treated as configured.
- Real E2E probe evidence: `/dcc/project-codes/page?pageNo=1&pageSize=50&status=ENABLE` returned `count=50`, `productBoundCount=0`; `keyword=IDI` returned one row with `projectCode=IDI` and `productMasterId=null`.
- No fallback was used for the blocked status E2E: the test did not infer product identity from the pressure-pump name, hardcoded IDI, or the frontend draft template.
