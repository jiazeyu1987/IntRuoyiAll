# Request Analysis

## User Goal

Continue the multi-agent eDHR production-readiness work in the current worktree. The main agent acts as reviewer and does not release a feature point unless subagent-developed work matches the documentation and every feature point has E2E coverage.

## Current System

- Existing DomainTrace frontend E2E script: `tests/e2e/edhr-domain-trace-real-flow.e2e.js`.
- Existing evidence proves the DomainTrace page, verify action, and UI/API cross-check can run on real test-tenant data.
- Prior evidence mainly proved a `BLOCKED` status and did not require the script to fail when the final release expectation is `VERIFIED` with zero blockers.
- A later approval/archive E2E created real test-tenant execution `40` and DB/hash verification confirmed `domainTraceStatus=VERIFIED`, `domainTraceSnapshotId=11`, and 8/8 verified items.

## Constraints

- Use only the test tenant for mutating E2E.
- No mock, fallback, API-only substitute, silent skip, or live tenant mutation.
- E2E entry remains `http://localhost:8081`.
- Any expected-status support must fail closed by default when configured.
- Keep production UI unchanged unless a real user-facing bug is discovered.

## Unknowns

- Whether execution `40` remains accessible from the current local test DB when the final E2E is run.
- Whether frontend 8081 still points to backend 48098 during final verification.

## Risks

- A script that accepts any visible status as PASS can create false release evidence.
- Reusing historical test data can be acceptable for idempotent verification, but the evidence must name the execution and status.
- Local runtime can accidentally point to old backend ports; trace or runtime config must confirm 48098 when used.

## Validation Surface

- Contract test for E2E script behavior.
- E2E syntax check.
- Real Playwright E2E against test tenant.
- Evidence markdown and `test-results/edhr-domain-trace/result.json`.

## Blocking Prerequisites

- Frontend server on `http://localhost:8081`.
- Healthy backend connected from frontend runtime.
- Test tenant login credentials.
- A real execution with expected `VERIFIED` DomainTrace evidence.
