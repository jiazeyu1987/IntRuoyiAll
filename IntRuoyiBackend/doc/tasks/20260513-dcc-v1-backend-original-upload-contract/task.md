# Task: DCC v1 backend original upload contract

## Goal

Add a DCC-specific original-file upload endpoint that stores the uploaded PDF through the existing infra file service and returns the resulting `originalFileId`, so the DCC upload page can complete the happy path without relying on failing presigned PUT URLs.

## Scope

- Add one backend endpoint under the DCC module for multipart original-file upload.
- Persist the uploaded file through the existing `FileService`.
- Resolve and return the created `infra_file` id together with stable metadata needed by the upload page.
- Add or update only the minimal backend tests and task evidence required for this contract closure.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-browser-permission-contract/task.md`
- Status before this task: completed and committed.
- Impact: browser permission behavior is already closed, so this task can focus only on the original upload contract needed by the happy-path flow.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document, execution log, and backend API evidence created before production code changes.
- [x] M3: BDD scenario and RED verification captured for DCC original upload.
- [x] M4: DCC original upload endpoint and service logic implemented.
- [x] M5: Targeted tests and backend evidence validator completed.
- [x] M6: Task-only backend changes committed after verification passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260513-dcc-v1-backend-original-upload-contract/backend-api-evidence.md`

## Current Status

Completed. The DCC backend now exposes `/dcc/controlled-files/upload-original`, persists the uploaded PDF through the infra file service, and returns `{ fileId, fileName }` for the upload page. Verification required one isolated-runtime prerequisite update: the validation environment's master file storage had to be pointed at an accessible local MinIO bucket, because the previous master config targeted an external S3 endpoint that returned `403` and prevented any upload contract from completing.
