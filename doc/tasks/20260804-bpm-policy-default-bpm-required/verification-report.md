# Verification Report

## Summary

- Result: PASS for corrected approval-switch-scope implementation.
- Scope: Default business approval policy list now shows top-level switchable policy rows, including closed `DIRECT` policies, and excludes eDHR route form detail noise.
- Closeout status: implementation and targeted verification complete; commit/push remains blocked by pre-existing unrelated dirty workspace changes.

## Commands

- RED: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> FAIL, expected reason: page lacked `approvalSwitchScope: true` and still defaulted `policyMode=BPM_REQUIRED`.
- GREEN: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> PASS, 4 tests, 0 failures, 0 errors.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/frontend-feature-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/backend-api-evidence.md` -> PASS.
- GREEN: `git diff --check -- <task-owned paths>` -> PASS, with line-ending warnings only for task docs.
- GREEN: `python -X utf8 -c "...read_text(encoding='utf-8')..."` -> PASS, 5 task docs readable.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-bpm-policy-default-bpm-required --mode preview` -> PASS, keep includes all retained task evidence and delete/blocked/warnings are `<none>`.

## Changed Behavior

- `IntRuoyiFronted/src/views/bpm/businessApprovalPolicy/index.vue` initializes `queryParams.approvalSwitchScope` to `true` and leaves `queryParams.policyMode` unset.
- `IntRuoyiFronted/src/api/bpm/businessApprovalPolicy/index.ts` exposes `approvalSwitchScope?: boolean` on page requests.
- `BusinessApprovalPolicyPageReqVO` accepts `approvalSwitchScope`; `BusinessApprovalPolicyMapper` excludes `EDHR_ROUTE_FORM` / `MES_EDHR_ROUTE_FORM_FILL` only when that flag is true.
- Mapper regression confirms DCC, form template `DIRECT`, and batch-record policies remain visible while eDHR route form detail policies are excluded.

## Residual Risk

- Real browser E2E was not run because this change is a default query-state adjustment covered by the existing static contract.
- Commit/push closeout remains pending until unrelated workspace changes are reconciled or explicitly baselined.
