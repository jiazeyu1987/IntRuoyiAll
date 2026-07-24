# eDHR Role/Tenant Matrix E2E Gate

## Goal

Close the eDHR production-readiness gap for runtime role and tenant evidence by adding a real Playwright E2E gate and supporting contract checks for the role/tenant matrix described in `doc/edhr-production-implementation/05-frontend-api-e2e-contract.md`.

## Scope

- Add a real UI E2E test that verifies eDHR permission boundaries for test-tenant executor, approver, QA/archive, readonly, and no-permission users.
- Keep formal `芋道源码/admin` coverage readonly only.
- Add a test-tenant fixture preparation script only for role/user/menu setup in tenant `122`; it must fail fast unless explicitly run with `--apply`.
- Add a static contract test that proves the matrix E2E and package scripts are wired.
- Do not add frontend debug controls, mock APIs, fallback branches, or silent skip behavior.

## Non-Goals

- Do not replace the existing full mutating eDHR approval/archive E2E.
- Do not write to the formal `芋道源码` tenant.
- Do not claim full production Go/No-Go readiness; CSV, DR, alerting, and storage retention remain separate gates.

## Milestones

| Milestone | Status | Output |
| --- | --- | --- |
| M1 Task docs and BDD/TDD plan | Completed | Task package and acceptance criteria |
| M2 RED contract | Completed | Failing contract check for missing permission matrix E2E/script wiring recorded in execution log |
| M3 Worker implementation | Completed | Fixture script, E2E script, package scripts, and contract test |
| M4 Independent reviewer gate | Completed | Main reviewer and independent tester verified logic, UI user path, no side effects, and E2E evidence |
| M5 Commit | Completed | Commit only this task's files after passing verification |

## BDD

BDD: readonly users cannot write -> Given a readonly test-tenant eDHR account, When it navigates through execution, tracking, signatures, field audit, domain trace, and archive status pages, Then no eDHR POST/PUT/PATCH/DELETE request is sent and readonly pages render or fail with explicit permission errors.

BDD: no-permission users fail visibly -> Given a test-tenant account without eDHR role menus, When it attempts to open eDHR pages, Then the UI or API exposes unauthorized/forbidden/no-menu evidence and never reports success.

BDD: role-specific access is separated -> Given executor, approver, and QA/archive accounts in test tenant `122`, When each account opens its allowed eDHR area, Then the allowed page renders and forbidden write areas are blocked by permissions.

BDD: formal admin is readonly only -> Given formal `芋道源码/admin`, When the matrix E2E runs formal coverage, Then it installs a write guard and performs only readonly smoke checks.

BDD: tenant fixture setup is explicit -> Given the fixture script is run without `--apply`, When it plans role/user/menu changes for tenant `122`, Then it performs no writes and reports the exact plan.

## Expected Verification

```powershell
node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs
pnpm e2e:edhr:permission-matrix:check
node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs
node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs --apply
pnpm e2e:edhr:permission-matrix
```

`pnpm e2e:edhr:permission-matrix` may only pass with real accounts, real frontend/backend, and test tenant data. Missing prerequisites must be recorded as `BLOCKED`, not skipped.

## Current Status

`completed`

Final verification result: PASS. `pnpm e2e:edhr:permission-matrix` passed against tenant `122`, execution `40` / `BRE202605280518101280040`, five test-tenant matrix users, and formal admin readonly smoke. All recorded write guards were clean and the denied user produced explicit permission-block evidence.

## Cleanup Keep

- `doc/tasks/20260528-edhr-role-tenant-e2e-gate/`
- `scripts/edhr-permission-tenant-matrix-contract.test.mjs`
- `tests/e2e/edhr-permission-tenant-matrix.e2e.js`
- `package.json`
