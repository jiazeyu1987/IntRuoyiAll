# Bug Regression Evidence

## Bug

点击“导入表单模板”选择已有模板升版时，后端返回 `Form template upgrade approval cannot be started: Form template upgrade requires BPM approval`。该错误来自升版 executor 的 `executeDirect` 分支，说明策略解析为 DIRECT，但升版业务要求必须进入 BPM。

## Expected

已有模板导入升版必须启动 `form-template-upgrade-v1` BPM 审批，返回 `approvalRequestId` 与 `approvalProcessInstanceId`；管理端和迁移均不得把 `FORM_TEMPLATE_UPGRADE` 降级为 DIRECT 或 SIGNATURE_REQUIRED。

## Reproduction

- UI path: 表单中心 -> 表单模板 -> 导入 -> 输入/选择已有模板“过程检验记录” -> 上传 doc/docx -> 点击导入。
- Deterministic RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test` failed because DIRECT publish/switch for `FORM_TEMPLATE_UPGRADE` did not throw.
- Deterministic RED: `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` failed because the seed did not assert/update stale non-BPM policy rows.

## Root Cause

`BusinessApprovalPolicyAdministrationService` allowed any registered effect executor to be published or switched to DIRECT. When `FORM_TEMPLATE_UPGRADE` resolved to DIRECT, `BusinessApprovalOrchestrator` called `FormTemplateUpgradeBusinessApprovalEffectExecutor#executeDirect`, which intentionally rejects direct execution with “requires BPM approval”. The upgrade seed also inserted BPM policy when missing but did not correct an already-published wrong policy.

## RED:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test` -> FAIL, 2 failures: no exception was thrown for publishing/switching form template upgrade to DIRECT.
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> FAIL, 1 failure: missing `UPDATE bpm_business_approval_policy` contract for stale policy correction.

## GREEN:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test` -> PASS, 16 tests.
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> PASS, 3 tests.

## Verification

- `mvn -pl yudao-module-bpm "-Dtest=FormTemplateUpgradeBusinessApprovalEffectExecutorTest,BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS, 10 tests.
- `python -X utf8 -m pytest script/tests/test_form_action_state_machine_release_contract.py script/tests/test_mes_route_version_publish_business_approval_policy_seed.py` -> PASS, 9 tests.

## Blockers

- No functional blockers remain.
- Commit boundary note: current task code/test changes were captured by concurrent baseline commits `1a564046` and `e17cb4c7`; this task records that evidence instead of rewriting shared history.
