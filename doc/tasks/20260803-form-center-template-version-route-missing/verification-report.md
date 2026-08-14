# Verification Report

## Result

PASS for the current source checkout and live local `int_main` backend runtime. The reported URL no longer behaves as a missing route on `127.0.0.1:48081`; unauthenticated access reaches the security layer and returns `code=401,msg=账号未登录`.

## Evidence

- RED: pre-merge commit `03646727b` `ActionFormPanel.vue` still called `getTemplateVersion(templateId, versionNo)` inside the runtime action panel template load block, which would request `/form-center/templates/{id}/versions/{versionNo}`.
- GREEN: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> `edhr-switch-filler-formcenter-slot-static PASS`.
- GREEN: `node tests\e2e\edhr-dynamic-form-action-panel-prefill-static.spec.js` -> `PASS: eDHR dynamic form action panel renders persisted prefill data.`
- GREEN: `node tests\e2e\form-center-static.spec.js` -> `form-center static contract passed`.
- GREEN: `mvn.cmd -pl yudao-module-bpm -am "-Dtest=FormCenterRuntimeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> `BUILD SUCCESS`, 9 tests run, 0 failures, 0 errors.
- RUNTIME: `http://127.0.0.1:48081/actuator/health` -> `UP`; target URL `http://127.0.0.1:48081/admin-api/form-center/templates/28/versions/V3.0` -> wrapper `code=401,msg=账号未登录`, not `请求地址不存在`.

## Scope Notes

- No production source file was changed in this turn because the current checkout already includes the fix.
- No remote/test/prod deployment or restart was performed; server operations require explicit user authorization.
- Final commit/push is blocked by unrelated staged and modified files from concurrent DCC/header tasks in the same index.

