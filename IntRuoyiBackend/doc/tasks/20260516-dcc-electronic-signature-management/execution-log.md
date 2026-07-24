# Execution Log: DCC 电子签名管理与审批签名留痕

BDD: DCC document center exposes electronic signature management ->
Given a user opens the DCC document center page, When the feature is delivered,
Then the page must show an electronic signature management tab aligned with the
existing IntAuth management capability.

BDD: DCC approvals require electronic signature ->
Given a DCC approval action is triggered, When the operator has not completed
the required electronic signature confirmation, Then the approval must fail
fast and no approval result may be persisted.

BDD: every successful electronic signature is recorded ->
Given an operator completes an approval with a valid electronic signature, When
the approval is accepted, Then the system must persist a signature record that
can be queried from electronic signature management.

BDD: signature validation failures are observable ->
Given a signature payload is invalid, expired, or missing required signer
context, When DCC approval is submitted, Then the API must return an explicit
failure and must not silently downgrade to unsigned approval.

BDD: generic BPM approval cannot bypass DCC signature enforcement ->
Given a DCC controlled-file workflow task is opened from the shared BPM task
center, When the operator attempts to approve or reject it through the generic
BPM endpoint, Then the backend must fail fast and instruct the operator to
return to the DCC electronic-signature flow.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc,yudao-module-bpm -Dtest=DccControlledFileSignatureServiceTest,DccElectronicSignatureAuthorizationServiceTest,DccElectronicSignatureManagementServiceTest,BpmTaskExternalSignatureGuardTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the new RED suite references missing DCC signature authorization/management classes and the missing BPM generic-approval guard plus error code, so test compilation stops before execution.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc,yudao-module-bpm -Dtest=DccBaseSchemaTest,DccControlledFileSignatureServiceTest,DccElectronicSignatureAuthorizationServiceTest,DccElectronicSignatureManagementServiceTest,BpmTaskExternalSignatureGuardTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, backend DCC signature authorization/query services, BPM generic-approval guard, and DCC schema baseline all passed the targeted regression suite.

GREEN: `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 exec eslint src/api/dcc/controlledFile/signatures.ts src/views/dcc/controlled-file/signatures/index.vue src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue` -> PASS, the new DCC signature-management API/page and BPM redirect hint pass focused frontend linting.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-electronic-signature-management run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-electronic-signature-management\scripts\verify-dcc-electronic-signature-management.mjs` -> PASS, the real browser loaded `http://127.0.0.1:8081/dcc/controlled-file/signatures`, both `签名记录` and `签名授权` tabs rendered, backend APIs returned `recordsTotal=0` and `authorizationsTotal=14`, and screenshot `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-electronic-signature-management-20260516.png` was captured.

GREEN: repaired `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\types\auto-components.d.ts` and updated Vite component-dts generation to stop build output from corrupting the tracked declaration file.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check` -> PASS, after the declaration-file repair and targeted repo-wide type fixes in existing shared/frontend files, the relaxed Vue type baseline is green again.
