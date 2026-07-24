# 20260528 EDHR DomainTrace VERIFIED E2E

## Goal

Strengthen the eDHR DomainTrace real Playwright path so a release reviewer can prove the positive `VERIFIED` path, not only the visible `BLOCKED` path. The E2E must fail fast when the final DomainTrace status or blocker count does not match the expected release condition.

## Milestones

- [completed] M0 Create task record before code changes.
- [completed] M1 Add RED contract coverage for expected DomainTrace status and blocker-count assertions in the E2E script.
- [completed] M2 Implement minimal E2E support for `EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS` and `EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT`.
- [completed] M3 Run syntax and contract checks.
- [completed] M4 Run real test-tenant E2E against a known `VERIFIED` execution and record evidence.
- [completed] M5 Independent reviewer gate.

## BDD

BDD: DomainTrace VERIFIED path is asserted -> Given a real test-tenant eDHR execution has a complete DomainTrace with `status=VERIFIED`, When the user opens the DomainTrace detail page and triggers verification through the real frontend, Then the E2E must assert final `status=VERIFIED`, `blockerCount=0`, non-empty items, and UI/API consistency.

BDD: DomainTrace expected status mismatch fails closed -> Given the E2E is configured with an expected status or blocker count, When the backend returns different evidence, Then the script must fail and write RED evidence instead of treating any visible status as pass.

BDD: DomainTrace BLOCKED path remains supported -> Given a real test-tenant execution intentionally has blockers, When the E2E is configured for `status=BLOCKED`, Then the script can still prove blocker visibility without weakening the `VERIFIED` release path.

## Expected Verification

- `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs`
- `pnpm e2e:edhr:domain-trace:check`
- `pnpm e2e:edhr:domain-trace` with `EDHR_E2E_DOMAIN_TRACE_EXPECTED_STATUS=VERIFIED` and `EDHR_E2E_DOMAIN_TRACE_EXPECTED_BLOCKER_COUNT=0`

## Current Status

Completed. Main reviewer verification passed on 2026-05-28 05:40 Asia/Shanghai:

- `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs` -> PASS, 5 tests.
- `pnpm e2e:edhr:domain-trace:check` -> PASS.
- `pnpm e2e:edhr:domain-trace` -> PASS against test tenant execution `BRE202605280518101280040`.

Real E2E evidence is recorded in `doc/tasks/20260528-edhr-domain-trace-verified-e2e/real-e2e-evidence.md`; the final DomainTrace summary is `status=VERIFIED`, `blockerCount=0`, `itemCount=8`, `hash=2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`.

Independent reviewer `019e6b62-d89a-7fb2-8a2f-55c75fca7f91` returned `final_decision: pass` with no blocking issues and no required changes.

## Cleanup Keep

- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/task.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/execution-log.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/request-analysis.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/prd.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/dev-plan.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/test-plan.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/test-report.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/task-state.json`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/verification-report.md`
- `doc/tasks/20260528-edhr-domain-trace-verified-e2e/real-e2e-evidence.md`
- `scripts/edhr-domain-trace-e2e-contract.test.mjs`
- `tests/e2e/edhr-domain-trace-real-flow.e2e.js`
