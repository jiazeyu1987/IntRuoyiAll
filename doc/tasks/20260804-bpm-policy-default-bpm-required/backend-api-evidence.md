# Backend API Evidence

## Scope

- Endpoint: `GET /business-approval/policies`.
- Owned backend files: `BusinessApprovalPolicyPageReqVO.java`, `BusinessApprovalPolicyMapper.java`, `BusinessApprovalPolicyMapperTest.java`, `BusinessApprovalPolicyControllerContractTest.java`.
- Behavior slice: add an explicit approval-switch listing scope for the policy page without changing policy execution, publish, disable, or switch-mode behavior.

## Contract

- Request VO adds `approvalSwitchScope`.
- When `approvalSwitchScope=true`, paging still respects tenant, status, policy mode, and latest-version filters.
- The scope excludes eDHR route form detail policies (`object_type=EDHR_ROUTE_FORM` or executor `MES_EDHR_ROUTE_FORM_FILL`).
- The scope does not force `policy_mode=BPM_REQUIRED`, so `DIRECT` policies for form templates and other top-level approval switches remain visible.
- No response fields, permissions, persistence schema, migrations, or executor contracts are changed.

## Validation

- Auth and permission contract remains `@ss.hasPermission('bpm:business-approval-policy:query')`.
- Validation behavior remains the existing `@Valid BusinessApprovalPolicyPageReqVO` path.
- Missing or invalid downstream services are not hidden; this change only adjusts mapper predicates.

## BDD

- BDD: 默认展示可开关审批策略 -> Given 文控、表单、批记录和 eDHR 路线表单明细策略同时存在 / When 管理员以 `approvalSwitchScope=true` 查询业务审批策略 / Then 列表包含文控、表单、批记录顶层策略，保留 `DIRECT` 关闭状态，并排除 eDHR 路线表单明细。

## RED

- RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> FAIL, expected reason before implementation: mapper test calls `setApprovalSwitchScope(true)` while `BusinessApprovalPolicyPageReqVO` has no `approvalSwitchScope` field and mapper does not exclude `EDHR_ROUTE_FORM`.

## GREEN

- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> PASS, 4 tests, 0 failures, 0 errors.

## Verification

- Mapper verification inserts DCC controlled-file publish, form-template upgrade in `DIRECT`, MES batch-record publish, and MES `EDHR_ROUTE_FORM` detail policies.
- Result asserts total `3`, includes `CONTROLLED_FILE`, `FORM_TEMPLATE`, `BATCH_RECORD_VERSION`, excludes `EDHR_ROUTE_FORM`, and confirms the form-template `DIRECT` policy remains visible.
- Controller contract verification confirms the page endpoint route, permission, and `approvalSwitchScope` request field.

## Blockers

- No backend code blocker remains.
- Full commit/push closeout is still blocked by pre-existing unrelated dirty workspace changes unless the user authorizes the required baseline/reconciliation flow.
