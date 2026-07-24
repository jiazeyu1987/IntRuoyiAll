# PRD

## Goal

Make the DomainTrace real E2E suitable for release gating by allowing the reviewer to require final `VERIFIED` status and zero blockers for a known real execution.

## Scope

- Update the DomainTrace E2E script and focused contract tests.
- Record RED/GREEN evidence in this task directory.
- Run real E2E when prerequisites are available.

## Non-Goals

- No production UI redesign.
- No backend DomainTrace algorithm change unless the E2E exposes a real defect.
- No live tenant mutation.
- No global eDHR production GO decision.

## User or System Scenarios

- A reviewer runs the DomainTrace E2E for a release candidate and requires `VERIFIED` plus zero blockers.
- A reviewer intentionally runs a BLOCKED fixture and requires blocker visibility without confusing it with release readiness.
- The script fails when configured expectations do not match backend/UI evidence.

## Functional Requirements

- FR-01: The script must accept `EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS`.
- FR-02: The script must accept `EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT`.
- FR-03: When configured, final API/UI evidence must match the expected status and blocker count.
- FR-04: Evidence markdown and JSON result must include expected and actual status/blocker count.
- FR-05: Existing canonical field and UI/API consistency checks must remain intact.

## Non-Functional Requirements

- NFR-01: Fail fast on missing or invalid prerequisites.
- NFR-02: No mock, fallback, API-only bypass, or silent downgrade.
- NFR-03: The script must remain runnable on Windows PowerShell with explicit UTF-8 handling by the caller.

## Dependencies and Constraints

- Existing Playwright runtime in frontend worktree.
- Real local frontend/backend runtime.
- Test tenant credentials and execution data.

## Acceptance Criteria

- AC-01: A contract test fails before implementation when the DomainTrace E2E script lacks expected-status and expected-blocker-count assertions.
- AC-02: `pnpm e2e:edhr:domain-trace:check` passes after implementation.
- AC-03: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` passes after implementation.
- AC-04: Real E2E configured with `EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS=VERIFIED` and `EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT=0` passes for a real test-tenant execution.
- AC-05: The script still rejects invalid frontend entry, live tenant, invalid execution id, and expectation mismatches.
