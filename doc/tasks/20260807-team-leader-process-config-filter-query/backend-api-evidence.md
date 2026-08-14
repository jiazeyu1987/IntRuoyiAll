# Backend API Evidence

## Endpoint Scope

- GET `/mes/pro/process-pool/team-leader/process-config/list`.
- Optional filters: route, process, loss reason description, mapped device, and parameter standard.

## API And Data Contract

- Response remains `List<MesTeamLeaderProcessConfigRowRespVO>`.
- Each keyword is optional and length-limited; populated keywords are AND-combined.
- Matching nested values never prune child loss-reason, device, or parameter collections from a matched row.

## Auth, Validation, Failure

- Permission remains `mes:pro-process-pool-team-leader:query`.
- Authorization remains sourced from `listLossReasonRows(leaderUserId)`.
- Null leader and validation failures remain explicit; no fallback, mock, or default success is added.

## Config, Services, Fixtures, Migrations

- No configuration, external service, fixture, database, or migration change.

## BDD And TDD Evidence

- BDD scenarios are recorded in `execution-log.md`.
- RED: the service signature source contract did not find the request VO parameter before implementation.
- Tests cover five individual fields, case-insensitive trimmed matching, AND intersection, blank/full-list behavior, unmatched empty results, full nested row retention, foreign-leader device exclusion, controller forwarding, endpoint reflection, and validation of all five maximum lengths.

## Contract Verification And Observability

- Controller forwarding, service field matching, intersection, empty query, and authorization are covered by focused JUnit tests.
- Existing HTTP error handling and service exceptions remain the observability surface.

## Blockers

- The required Maven command cannot currently reach the focused tests because concurrent Maven jobs keep rewriting the shared `target` tree; missing System module class outputs stop MES main compilation first.
- No shared Maven process was terminated and no `target` directory was deleted or cleaned.
