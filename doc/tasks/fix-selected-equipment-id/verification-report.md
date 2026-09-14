# Verification Report

## Summary

- Fixed one-line symptom: `itemResults.CODX-AO5-QA-FINAL.selectedEquipmentId` can no longer reach backend submit as a missing formal field from the one-line PQC frontend flow.
- Root cause: frontend PQC item equipment validation was conditional on `equipmentRequired`, while the backend formal `itemResults` contract requires equipment identity for every QA item result.
- Scope: frontend one-line PQC formal submit preflight and static contract only; no backend schema, API, SQL, tenant data, or runtime services changed.

## Verification

- RED: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> FAIL, missing `assertPqcSubmissionItemEquipmentSelections` before signature dialog.
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-item-equipment-standard-method-static.spec.js doc/tasks/fix-selected-equipment-id/task.md doc/tasks/fix-selected-equipment-id/execution-log.md` -> PASS, LF/CRLF warnings only.
- GREEN: `rg -n "selectedEquipmentId|CODX-AO5-QA-FINAL|assertPqcSubmissionItemEquipmentSelections" docs\experience-index.md docs\frontend-development.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-selected-equipment-id\bug-regression-evidence.md` -> PASS.

## Non-Target Regression Notes

- `node tests/e2e/mes-frontline-pqc-task-quantity-static.spec.js` -> FAIL on an existing static anchor mismatch: missing `<label for="frontlinePqcInspectionQuantity">检验数量</label>`.
- `node tests/e2e/pqc-submission-structured-columns-static.spec.js` -> FAIL on `TeamLeaderWorkbenchPage.vue` out-of-range display contract, which is outside the edited one-line PQC submit file.
- These failures are recorded as adjacent non-target failures, not used as GREEN evidence for this fix.

## Design Constraint Result

- Fallback introduced: no.
- Silent downgrade or swallowed exception introduced: no.
- Formal source preserved: yes, equipment identity must come from the selected formal QA equipment option.
- Temporary patch introduced: no.

## Cleanup

- Preview: `task_closeout.py --task-id fix-selected-equipment-id --mode preview` -> PASS, blocked `<none>`.
- Apply: `task_closeout.py --task-id fix-selected-equipment-id --mode apply` -> PASS, deleted temporary `bug-regression-evidence.md`.
- Remaining task records: `task.md`, `execution-log.md`, `verification-report.md`.
