# Backend API Evidence

## Scope

- Backend response contract involved in this task: `GET /admin-api/mes/pro/edhr-process-form-permission-rule/get-by-report`.
- Target behavior: when a form-level `FILL` rule uses `candidateSourceType=ROLE`, the response includes both resolved `candidateUsers` and role display names in `candidateSourceNames`.

## Contract

- Input: an authenticated “芋道源码” tenant request for a batch record report form permission rule.
- Output: `fillRule.candidateSourceType=ROLE`, `fillRule.candidateSourceIds=[roleId]`, `fillRule.candidateSourceNames=[roleName]`, and expanded enabled candidate users.
- No fallback, mock, default success, or silent downgrade is introduced.

## Validation

- BDD: 表单级角色填写人回显 -> Given 批记录表单默认填写人来源为角色 When 调用 `get-by-report` Then 响应必须返回角色来源名称与展开后的候选用户。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `fillRule.candidateSourceNames` was `null`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

## Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`.
- `python -X utf8 doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers.py --verify` -> PASS, `reports=15 roles=15 usersPerRole=3 apiVerified=15`.
- `node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\pressure_pump_role_filler_ui_readonly.e2e.js` -> PASS, real page row and dialog show role name.

## Blockers

- Product/runtime blocker: none for the verified behavior.
- Repository closeout blocker: shared `int_main` workspace is behind `origin/int_main` and contains unrelated dirty changes, so no commit/push was performed for this task.
