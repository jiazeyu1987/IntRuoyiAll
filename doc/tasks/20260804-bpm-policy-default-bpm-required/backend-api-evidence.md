# Backend API Evidence

## Scope

- Endpoint: `GET /business-approval/policies`.
- Owned backend files: `BusinessApprovalPolicyPageReqVO.java`, `BusinessApprovalPolicyMapper.java`, `BusinessApprovalPolicyMapperTest.java`, `BusinessApprovalPolicyControllerContractTest.java`.
- Behavior slice: add an explicit approval-switch listing scope for the policy page without changing policy execution, publish, disable, or switch-mode behavior.

## Contract

- Request VO adds `approvalSwitchScope`.
- When `approvalSwitchScope=true`, paging still respects tenant, status, policy mode, and latest-version filters.
- The scope is a positive whitelist of top-level switchable business executor codes: DCC upload/publish/obsolete, form template upgrade/obsolete, MES route version publish, batch record version publish, eDHR batch execution submit review, and eDHR batch void.
- The scope excludes detail policies such as eDHR route form fill, form instance fill, route attachment upload, and other unrecognized detail executors.
- The scope does not force `policy_mode=BPM_REQUIRED`, so `DIRECT` policies for form templates and other top-level approval switches remain visible.
- No response fields, permissions, persistence schema, migrations, or executor contracts are changed.

## Validation

- Auth and permission contract remains `@ss.hasPermission('bpm:business-approval-policy:query')`.
- Validation behavior remains the existing `@Valid BusinessApprovalPolicyPageReqVO` path.
- Missing or invalid downstream services are not hidden; this change only adjusts mapper predicates.

## BDD

- BDD: 默认展示可开关审批策略 -> Given 文控、表单、工艺路线、批记录、批次执行和明细策略同时存在 / When 管理员以 `approvalSwitchScope=true` 查询业务审批策略 / Then 列表只包含顶层可开关执行器策略，保留 `DIRECT` 关闭状态，并排除表单实例、路线附件、路线表单填写等明细策略。

## RED

- RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> FAIL, expected reason before implementation: mapper test calls `setApprovalSwitchScope(true)` while `BusinessApprovalPolicyPageReqVO` has no `approvalSwitchScope` field and mapper does not exclude `EDHR_ROUTE_FORM`.
- RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest" test` -> FAIL, expected reason after the first scope implementation: exclusion-only filter returned 10 policies instead of the expected 7 top-level switchable policies.

## GREEN

- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> PASS, 4 tests, 0 failures, 0 errors.

## Verification

- Mapper verification inserts top-level DCC upload/publish, form-template upgrade in `DIRECT`, route version publish, batch-record version publish, eDHR submit review, eDHR batch void, and several detail/noise policies.
- Result asserts total `7`, includes the top-level executor codes, excludes `DCC_ARCHIVE_DETAIL`, `FORM_INSTANCE_FILL`, `MES_EDHR_ROUTE_FORM_FILL`, and `MES_EDHR_ROUTE_ATTACHMENT_UPLOAD`, and confirms the form-template `DIRECT` policy remains visible.
- Controller contract verification confirms the page endpoint route, permission, and `approvalSwitchScope` request field.

## Blockers

- No backend code blocker remains.
- Full commit/push closeout is still blocked by pre-existing unrelated dirty workspace changes unless the user authorizes the required baseline/reconciliation flow.
