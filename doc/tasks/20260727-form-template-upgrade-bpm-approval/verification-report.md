# Verification Report

## Summary

- Root cause fixed: `FORM_TEMPLATE_UPGRADE` can no longer be published or switched to DIRECT/SIGNATURE_REQUIRED.
- Stale seed behavior fixed: upgrade BPM seed now corrects published non-BPM policy rows before duplicate/conflict checks.
- Regression scope covered: policy administration, Form Template upgrade executor, BPM-required orchestrator path, upgrade seed, and release contract index.

## Commands

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test` -> PASS, 16 tests.
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> PASS, 3 tests.
- `mvn -pl yudao-module-bpm "-Dtest=FormTemplateUpgradeBusinessApprovalEffectExecutorTest,BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS, 10 tests.
- `python -X utf8 -m pytest script/tests/test_form_action_state_machine_release_contract.py script/tests/test_mes_route_version_publish_business_approval_policy_seed.py` -> PASS, 9 tests.

## Changed Areas

- `BusinessApprovalPolicyAdministrationService`: mandatory BPM executor guard.
- `BusinessApprovalPolicyAdministrationServiceTest`: RED/GREEN coverage for publish and switch downgrade attempts.
- `20260721_form_template_upgrade_bpm_seed.sql`: stale policy correction update.
- `test_form_template_upgrade_bpm_seed.py`: seed contract for policy correction.

## Remaining Risks

- The current task did not run a live browser import because local runtime/database state was not part of this scoped backend/seed fix.
- Current branch contains concurrent baseline commits; implementation files are already committed in `1a564046` and `e17cb4c7`.
