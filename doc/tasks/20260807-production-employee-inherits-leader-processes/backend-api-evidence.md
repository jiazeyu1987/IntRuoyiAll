# Backend API Evidence

## Scope

- Remove production employee-to-process write contracts.
- Resolve frontline production employee process visibility through the employee profile's production leader.
- Keep runtime employee options and submit validation on the same leader personnel source.

## API And Data Contract

- `GET /mes/pro/feedback/frontline/device-account/processes` returns the responsible production leader's formal route processes for an enabled production employee account.
- Employee-process add/save/disable endpoints are removed.
- Formal source: `mes_pro_process_pool_team_employee_profile.system_user_id -> leader_user_id`, followed by active route-start production leader configuration.

## Auth, Validation, And Failure

- Existing frontline query/create permissions remain unchanged.
- Disabled employee profiles, multiple distinct leader ownership, or no formal responsible route fail explicitly.
- No post, device, historical employee binding, or default-success fallback is allowed for a recognized production employee account.

## Required Prerequisites

- Existing employee profile schema and active route version snapshots.
- Existing Maven/JUnit test fixtures.

## BDD Scenarios

- BDD: Enabled production employee inherits all formal route processes of the unique production leader.
- BDD: Employee-process write contracts are absent.
- BDD: Runtime employee options and submit authorization use all enabled profiles under the responsible leader.
- BDD: Invalid or ambiguous employee ownership fails fast.

## RED And GREEN

- RED: targeted Maven test failed because the employee account was treated as a device account and reached missing route-binding source error `1040760100` instead of inheriting its leader's formal routes.
- GREEN: production source compiles; focused behavior test and regression suite await a stable shared Maven output directory after concurrent test compilation completes.

## Contract Verification

- Verification: source contract scan passes and confirms that employee-process write endpoints and runtime binding reads are absent.

## Observability

- Existing business exceptions remain visible through the standard API error contract.

## Blockers And Downstream Needs

- None.
