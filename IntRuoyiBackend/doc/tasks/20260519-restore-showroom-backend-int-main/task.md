# Task: Restore Showroom Backend Into int_main

## Goal

Restore the Showroom backend controllers and module into backend `int_main` so the already-merged frontend Showroom pages can load `admin-api/showroom/*` and `admin-api/showroom/display/*` from the mainline runtime.

## Scope

- Confirm the current backend `int_main` does not contain the Showroom module.
- Merge the completed backend branch `codex/showroom-t6-integration-hardening` into `int_main`.
- Preserve unrelated in-progress local backend modifications.
- Run focused backend verification for the Showroom module and HTTP contract tests.

## Non-Scope

- Do not continue the paused AI TTS token-save task in this change.
- Do not fake or bypass missing backend endpoints.
- Do not revert unrelated DCC, AI, or MES local work already present in `int_main`.

## Milestones

- [x] M1: Confirm the live defect and the missing Showroom backend code in `int_main`.
- [x] M2: Mark the previous unfinished backend task as blocked by this higher-priority regression.
- [x] M3: Merge `codex/showroom-t6-integration-hardening` into `int_main`.
- [x] M4: Run focused Showroom backend verification.
- [x] M5: Record results and commit only this task record.

## Expected Verification

- `mvn -pl yudao-module-showroom test`
- `rg -n "ShowroomAdminController|ShowroomDisplayController|/showroom/company/current|/showroom/display/home|yudao-module-showroom" . -g "*.java" -g "pom.xml"`
- A direct local backend request to `/admin-api/showroom/company/current` should resolve to the Showroom controller path instead of `No static resource`.

## Current Status

Completed. Backend `int_main` now contains the Showroom module and the Showroom admin endpoint resolves through Spring security/controller mapping instead of static-resource 404 handling.

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom test` -> 28 tests passed.
- PASS: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`.
- PASS: authenticated `GET /admin-api/showroom/company/current` now returns `{"code":0,"msg":"","data":{"companyId":0,"revisionId":null,"revisionNo":0,"status":"DRAFT","fields":{}}}`.
- PASS: unauthenticated `GET /admin-api/showroom/company/current` returns `401` instead of static-resource 404.

## Cleanup Keep

- `doc/tasks/20260519-restore-showroom-backend-int-main/task.md`
- `doc/tasks/20260519-restore-showroom-backend-int-main/execution-log.md`
