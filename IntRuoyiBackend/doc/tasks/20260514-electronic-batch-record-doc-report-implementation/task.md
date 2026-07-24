# Task: Electronic Batch Record DOC Report Backend

## Goal

Implement the backend slice for uploading the approved pilot `.doc`, parsing every discoverable Word table, generating or updating one JimuReport report per parsed table, persisting generated-report metadata, and exposing list/designer/delete APIs for the electronic batch-record page.

## Scope

- Add the generated-report metadata model and SQL migration.
- Add DOC parsing, report generation, generated-report listing, designer-path lookup, and delete APIs.
- Reuse JimuReport category/report persistence services where possible.
- Add targeted backend regression coverage and evidence.

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-dcc-intauth-position-source/task.md`
- Status before this task: blocked due to user reprioritization
- Impact: the unrelated DCC backend source-switch task remains paused while this batch-record report backend is implemented

## Milestones

- [x] M1: Review the latest backend task and explicitly block it before switching scope.
- [x] M2: Create this backend task document before production code changes.
- [x] M3: Record BDD scenarios and RED evidence for the missing DOC import and report-management backend.
- [x] M4: Implement the minimal backend parsing, persistence, and API behavior.
- [x] M5: Run targeted verification and update evidence.

## Expected Verification

- `POST /admin-api/mes/pro/batch-record-report/import` imports the pilot `.doc` and returns created or updated report summaries.
- `GET /admin-api/mes/pro/batch-record-report/page` returns only the generated electronic batch-record reports.
- `GET /admin-api/mes/pro/batch-record-report/designer-path` returns a usable designer path for a generated report.
- `DELETE /admin-api/mes/pro/batch-record-report/delete` removes both the JimuReport record and the local metadata binding.

## Current Status

Completed. The new backend package, parser, metadata persistence, APIs, and focused regression tests all pass on `int_main`.

## Blocker And Impact

- Blocker: none remaining for the focused backend compile and regression scope.
- Impact: the backend batch-record report slice is Maven-verified for the targeted compile and regression commands on `int_main`.
