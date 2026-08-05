# Backend API Evidence

## Scope

- Endpoint: `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates`
- Endpoint: `POST /mes/pro/process-pool/team-leader/employee-profile/formal/link`
- Service: `MesTeamLeaderRuntimeConfigService`
- Integration boundary: `AdminUserApi`

## API And Data Contract

- Candidate request keeps the required `keyword` query parameter.
- Candidate response keeps `systemUserId` and `displayName`.
- Candidate source changes from current leader subordinates to all matching system users.
- Link request keeps `systemUserId` and optional `displayName`.
- Linking accepts any valid system user while retaining duplicate-user and active display-name uniqueness checks.

## Auth, Validation, And Errors

- Candidate and link endpoints retain `mes:pro-process-pool-team-leader:maintain`.
- Blank search text returns an empty list without an unbounded scan.
- Invalid system users fail through the existing system user validation.
- Duplicate formal users fail before database insert.
- No exception swallowing, default success, subordinate fallback, or client-side full-list filtering is introduced.

## Dependencies And Migrations

- Required service: system-module `AdminUserApi`.
- Required fixtures: unit-test system users.
- Database migration: none.
- Configuration change: none.

## BDD Scenarios

- Given a matching system user outside the leader's department scope, when searching by name or username, then the candidate is returned.
- Given a valid system user outside the leader's department scope, when linking, then a formal employee profile is created.
- Given blank text, when searching, then no unbounded query is performed.
- Given an invalid system user, when linking, then validation fails and no profile is created.
- Given an already linked user, when linking again, then the request fails before insert.

## TDD Evidence

- RED: `mvn -pl yudao-module-system,yudao-module-mes -am "-Dtest=AdminUserApiImplPostIdsTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL because `AdminUserApiImpl#getUserListByNickname` did not exist.
- GREEN system boundary: `mvn -pl yudao-module-system -am "-Dtest=AdminUserApiImplPostIdsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- Reactor contract discovery: BPM test stub did not implement the new abstract API method; it was updated with an explicit unsupported-operation failure because that test does not use user search.
- GREEN: pending.
- REGRESSION: pending.

## Observability

- Existing maintenance audit action `LINK_FORMAL_EMPLOYEE` remains unchanged.
- Existing controller and service exceptions remain visible to the API client.

## Blockers

- None currently identified.
