# Task: Intern User Time Maintenance Module

## Goal
Build the "实习用户" time maintenance capability as a detachable module boundary. Loading the module exposes backend endpoints, permissions, and frontend entry points; not loading the module leaves no usable frontend entry or backend write endpoint.

## Milestones
1. Document module boundary and BDD scenarios.
2. Move file upload time maintenance out of generic infra file API into the intern-user time maintenance module.
3. Switch frontend entry and API wrapper to intern-user permissions and endpoint.
4. Provide module-scoped SQL seed for role/menu permissions.
5. Run focused RED/GREEN/REGRESSION validation or record blockers.

## BDD
BDD: module loaded exposes upload-time maintenance -> Given the intern-user module is enabled and a user has the intern-user time maintenance permission, When they open file management, Then the upload time edit entry is visible and saves through the intern-user module endpoint.

BDD: module not loaded leaves no write endpoint -> Given the intern-user module is disabled or not deployed, When a caller tries the old generic infra file endpoint, Then no upload-time write endpoint is available from generic file management.

BDD: detachable permission seed -> Given the customer no longer wants this feature, When the intern-user module SQL and frontend/backend module package are removed, Then the generic file module keeps normal upload/download/delete behavior without intern-user time maintenance permissions.

## Expected Verification
- Focused backend controller/service tests for intern-user upload-time endpoint.
- Focused frontend static contract proving the button permission and API URL use intern-user module names.
- SQL static contract or manual review proving role/menu permissions are module-scoped.

## Design Constraints Check
- Role display name must be `实习用户` and role code must be `intern_user` per customer requirement.
- Do not implement no-log writes; audit visibility can be added under the same business role later.
- The module boundary must use `intern-user` names for permission/API/menu assets so it can be cut out cleanly.
- Do not change unrelated DCC/BPM/MES worktree changes.

## Current Status
ready_for_closeout

Implementation, focused verification, and user-requested E2E verification are complete. Final `completed` status is intentionally not set because repository rules require Git commit/push for completion, while this turn has no explicit commit/push authorization.
