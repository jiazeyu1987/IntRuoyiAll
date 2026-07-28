# Backend API Evidence

## Scope

Backend service and data seed behavior for platform business approval policy administration and Form Center template upgrade approval.

## Contract

- `FORM_TEMPLATE_UPGRADE` and `FORM_TEMPLATE_OBSOLETE` are mandatory-BPM effect executors.
- Published policies for mandatory-BPM executors must use `BPM_REQUIRED` and a valid process definition key.
- Existing template import upgrade must resolve to BPM-required business approval and start `form-template-upgrade-v1`, not execute direct.
- The upgrade seed must correct stale published policy rows to `BPM_REQUIRED/form-template-upgrade-v1/FORM_TEMPLATE_UPGRADE` before duplicate/conflict checks.

## Validation

- Publishing a DIRECT policy for `FORM_TEMPLATE_UPGRADE` fails fast with `BUSINESS_APPROVAL_MODE_INVALID`.
- Switching an existing published `FORM_TEMPLATE_UPGRADE` BPM policy to DIRECT fails fast before password validation, signature recording, or disabling the old policy.
- SQL seed remains tenant-scoped, idempotent, and non-destructive while correcting stale published policy mode/process/executor fields.

## BDD:

- Existing template import starts BPM approval -> Given an existing template receives a new doc/docx version When the import action submits `FORM_TEMPLATE/UPGRADE/DRAFT` Then BPM process `form-template-upgrade-v1` starts and the response includes approval request/process ids.
- Mandatory BPM executor cannot be downgraded -> Given a published `FORM_TEMPLATE_UPGRADE` policy When an administrator switches it to DIRECT Then the service fails fast and keeps the old BPM policy published.
- Migration preserves BPM-required upgrade policy -> Given a stale published DIRECT upgrade policy When the seed runs Then it corrects the row to BPM_REQUIRED before conflict gates.

## RED:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test` -> FAIL because mandatory-BPM downgrade was allowed.
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> FAIL because stale policy correction was not asserted.

## GREEN:

- `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest" test` -> PASS.
- `python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py` -> PASS.

## Verification

- `mvn -pl yudao-module-bpm "-Dtest=FormTemplateUpgradeBusinessApprovalEffectExecutorTest,BusinessApprovalOrchestratorBpmRequiredTest" test` -> PASS.
- `python -X utf8 -m pytest script/tests/test_form_action_state_machine_release_contract.py script/tests/test_mes_route_version_publish_business_approval_policy_seed.py` -> PASS.

## Blockers

- No backend/API blockers remain.
- Real UI E2E was not run because the defect is isolated to backend policy mode and seed contracts; no frontend behavior changed.
