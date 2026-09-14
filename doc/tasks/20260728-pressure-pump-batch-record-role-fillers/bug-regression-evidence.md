# Bug Regression Evidence

## Bug

批记录表单列表和填写人设置弹窗中，form-level `FILL` 规则已经配置为角色且候选用户能够展开，但 `get-by-report` 响应没有返回 `candidateSourceNames`，导致角色名在页面上表现为“填写人不见了”。

## Expected

当批记录表单默认填写人来源是 `ROLE` 时，后端响应必须同时返回角色来源名称和展开后的启用候选用户；前端列表与弹窗应显示业务可识别的角色名，最后由实际签字人负责。

## Reproduction

- RED: `python -X utf8 doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers.py --verify` -> FAIL, expected reason: 15 张目标表单的 DB 配置和角色成员均正确，但运行态 API 的 `fillRule.candidateSourceNames` 为空。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" test` -> FAIL, expected reason: `candidateSourceNames` expected `[粗洗工序填写者角色]` but was `null`。

## Root Cause

`MesProEdhrProcessFormPermissionRuleServiceImpl#toCandidateResp` 只把 `candidateSourceIds` 展开成 `candidateUsers`，没有像填写分配响应一样调用 `resolveCandidateSourceNames`。角色成员可以解析，但角色来源名称缺失。

## GREEN:

- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule" test` -> PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesProEdhrProcessFormPermissionRuleServiceImplTest" test` -> PASS, `Tests run: 33, Failures: 0, Errors: 0`。

## Verification

- `mvn -pl yudao-server -am "-DskipTests" package` -> PASS。
- `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS; 48081 health `UP`。
- `python -X utf8 doc\tasks\20260728-pressure-pump-batch-record-role-fillers\verify_pressure_pump_role_fillers.py --verify` -> PASS, `reports=15 roles=15 usersPerRole=3 apiVerified=15`。
- `node doc\tasks\20260728-pressure-pump-batch-record-role-fillers\pressure_pump_role_filler_ui_readonly.e2e.js` -> PASS，列表行和弹窗均显示 `粗洗工序填写者角色`。

## Blockers

- No product/runtime blocker remains for the verified behavior.
- Repository closeout is not completed because the shared `int_main` workspace is behind `origin/int_main` by 22 commits and contains unrelated dirty changes.
