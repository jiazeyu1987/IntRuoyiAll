# Verification Report

## Result

PASS for QMS backend/frontend local runtime.

## Evidence

- `python -m pytest IntRuoyiBackend\script\tests\test_runtime_source_tracking_guard.py`: PASS, 2 tests passed.
- `mvn.cmd -pl yudao-server -am -DskipTests package`: PASS in prior build verification, reactor build produced `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Backend runtime: `http://127.0.0.1:48061/actuator/health` returned `200 {"status":"UP"}`.
- Frontend runtime: `http://127.0.0.1:8061/` returned `200 OK`.
- Port ownership: `48061` owned by PID `7380`; `8061` owned by PID `32448`; both processes resolve to the QMS workspace path.

## Root Cause

The QMS checkout was not missing a Git pull. The comparable Shedule workspace had production Java source packages under directories named `runtime`, but the repository-level `.gitignore` rule `**/runtime/` ignored those package directories. QMS therefore lacked the local ignored source files until they were restored and `.gitignore` was corrected for backend Java runtime packages.

## Remaining Blockers

None for local runtime startup. Closeout/commit/push is still pending under the project task workflow.
