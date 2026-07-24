# PRD

## Goal

Provide executable runtime evidence for the eDHR role/tenant matrix by adding a real Playwright E2E gate and fixture preparation script.

## Scope

- Frontend E2E for eDHR role/tenant matrix.
- Test-tenant fixture preparation for role, user, and menu bindings.
- Static contract test for script wiring and fail-fast protections.
- Task evidence and reviewer gate records.

## Non-Goals

- Full eDHR approval/archive business E2E replacement.
- Formal production write validation.
- CSV, DR, alerting, or object storage retention closure.

## User or System Scenarios

- A readonly user can inspect eDHR evidence but cannot write.
- A no-permission user receives explicit unauthorized evidence.
- Executor, approver, and QA/archive accounts do not collapse into a single admin account.
- Formal admin readonly smoke never sends mutating requests.
- Fixture setup is explicit and tenant-scoped.

## Functional Requirements

- AC-01: The E2E script must fail fast when required matrix accounts, URLs, tenant names, passwords, or execution IDs are missing.
- AC-02: The E2E script must reject mutating runs against `芋道源码`, `yudao`, `prod`, or `production` tenant names.
- AC-03: Readonly and formal admin coverage must install a write guard that fails on eDHR `POST`, `PUT`, `PATCH`, or `DELETE`.
- AC-04: The no-permission path must require explicit unauthorized, forbidden, route-blocked, or no-menu evidence.
- AC-05: Executor, approver, and QA/archive users must each render at least one role-appropriate eDHR page with their own account.
- AC-06: The fixture script must be dry-run by default and write only tenant `122` when `--apply` is provided.
- AC-07: Static contract tests must prove the package scripts and critical guards exist.

## Non-Functional Requirements

- No mock data or fake API success.
- No frontend debug controls.
- No fallback account switching.
- Evidence files must avoid secrets.
- Temporary E2E artifacts must not be committed.

## Dependencies and Constraints

- Test tenant id is `122`.
- Default local frontend is `http://localhost:8081`.
- The eDHR menu and permission SQL already exists in the backend worktree.
- Existing execution IDs may be supplied for read-only matrix evidence.

## Acceptance Criteria

- AC-01 through AC-07 all have passing automated evidence.
- E2E result records account role, tenant, route, write guard status, and observed permission outcome.
- Reviewer confirms no formal tenant writes occurred.
