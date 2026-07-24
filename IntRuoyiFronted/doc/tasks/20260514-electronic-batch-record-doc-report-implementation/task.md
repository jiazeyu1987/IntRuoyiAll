# Task: Electronic Batch Record DOC Report Frontend

## Goal

Implement the frontend slice for the electronic batch-record tab so operators can upload the approved pilot `.doc`, view the generated report list, open a generated report in a JimuReport designer wrapper page, and delete generated reports.

## Scope

- Redesign the existing electronic batch-record tab into a toolbar + dense table management page.
- Add upload, refresh, search, list, designer-wrapper navigation, and delete interactions.
- Keep the current menu path `/mes/pro/batch-record-template`.
- Add targeted frontend regression coverage and evidence.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-dcc-intauth-position-source/task.md`
- Status before this task: blocked due to user reprioritization
- Impact: the unrelated DCC frontend source-switch task remains paused while this batch-record report frontend is implemented

## Milestones

- [x] M1: Review the latest frontend task and explicitly block it before switching scope.
- [x] M2: Create this frontend task document before production code changes.
- [ ] M3: Record BDD scenarios and RED evidence for the missing import/list/designer/delete flow.
- [ ] M4: Implement the minimal frontend management page and designer wrapper.
- [ ] M5: Run targeted verification, update evidence, and prepare a scoped frontend commit.

## Expected Verification

- The electronic batch-record page renders the upload toolbar and generated-report list instead of blocker-only text.
- Upload success refreshes the generated-report list and shows the import summary.
- Clicking `修改` opens the generated report in the wrapper page.
- Clicking `删除` removes the report row after backend confirmation.

## Current Status

Completed for the frontend slice. The electronic batch-record page now exposes DOC import, generated-report list management, and a designer-wrapper mode on the same entry path.
