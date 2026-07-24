# Task: MES batch record template import phase 1

## Goal

Implement the backend Phase 1 paperless batch-processing slice in MES: upload one batch-processing `.doc/.docx` file, parse it into multiple batch-record form templates, preview the parsed tables, and support template save, base-information update, and delete.

## Scope

- Lock the backend API contract before code changes begin.
- Implement backend import-session persistence and template persistence.
- Implement a parser adapter that supports the pilot `.doc` sample through the system import flow.
- Verify the feature with BDD + strict TDD evidence and focused checks.

## Milestones

- [x] M1: Previous unfinished backend repo task explicitly blocked before starting this task.
- [x] M2: This task document, execution log, and API contract were created before production code changes.
- [x] M3: Record BDD scenarios and RED evidence against the locked Phase 1 contract.
- [x] M4: Implement backend parse/commit/template CRUD with focused tests.
- [x] M5: Run agreed verification commands and complete backend review.
- [x] M6: Update final status and commit only current task changes.

## Expected Verification

- The pilot sample `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` parses through the system import flow without manual pre-conversion.
- Parse preview returns multiple template candidates and does not create official template rows.
- Commit persists only selected template candidates.
- Template page/get/update/delete follow `api-contract.md` exactly.

## Current Status

Completed on 2026-05-14. The original backend Phase 1 slice in this worktree was resumed, repaired, Maven-verified, and prepared for a scoped backend commit before merge into `int_main`.

## Blocker And Impact

- Blocker: none remaining inside the scoped Phase 1 backend slice.
- Impact: the work is ready for scoped commit and merge handling.

## Resume Decision

- Decision: continue the original Phase 1 template-import backend path in `D:\wt\rbt-be`.
- Relationship to the newer report-based implementation: both may coexist temporarily in Git history, but this task now targets the original parse-preview, commit, and template CRUD contract locked in `api-contract.md`.

## Final Verification

- `mvn -pl yudao-module-mes -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=MesProBatchRecordWordParserTest,MesProBatchRecordTemplateServiceImplTest,MesProBatchRecordTemplateControllerTest" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\wt\rbt-be\doc\tasks\20260513-mes-batch-template-import-phase1\backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\wt\rbt-be\doc\tasks\20260513-mes-batch-template-import-phase1\database-schema-evidence.md` -> PASS
