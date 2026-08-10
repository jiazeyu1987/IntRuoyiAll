# Backend API Evidence

## Scope

- Service: `MesTeamLeaderActiveOrderServiceImpl`
- Mapper: `MesProWorkOrderMapper`
- Tests: `MesTeamLeaderActiveOrderServiceTest`, `MesTeamLeaderActiveOrderErpPlannedStartTest`
- User-visible paths: production leader active-order candidate search and add active order.

## API Contract And Data Contract

- Candidate search keeps the same method/API shape and returns `workOrderId`, `workOrderCode`, `eligible`, and `ineligibleReason`.
- Add active order keeps the same request shape: `leaderUserId` and `workOrderId`.
- Candidate lookup now searches work orders by code/product keyword without requiring confirmed status.
- Active order route, route version, route process, and process identities are resolved from published QA/PQC regulations for the work order product.
- Duplicate detection remains keyed by work order + resolved QA route + resolved QA route version before insert.

## Auth, Permissions, Validation, And Errors

- Controller/auth shape is unchanged.
- Required request context remains: leader user id and work order id.
- Work order validation changed from confirmed-only to existence-only.
- QA is the formal remaining gate: missing/ambiguous/unpublished QA regulation, missing QA version, or missing QA items fails fast with `PRO_PQC_INSPECTION_TASK_GENERATION_BLOCKED`.
- Duplicate active order returns the existing active order id; concurrent duplicate insert is still handled through the existing unique-key path.

## Config, Services, Fixtures, And Migrations

- No config changes.
- No database migrations.
- Required fixtures for tests are mocked work order, QA regulation, QA version, QA items, active order mapper, process snapshot mapper, and PQC task mapper.

## BDD Scenarios

- BDD: QA 存在即可加入 -> Given a work order is not confirmed and has no effective schedule or product route binding but has published QA, When searching and adding, Then it is eligible and creates active order snapshots/tasks from QA.
- BDD: 缺少 QA 不可加入 -> Given a work order exists but has no published QA, When searching or adding, Then it is blocked and no active order is written.
- BDD: 重复活跃订单检测 -> Given the same work order and QA route version already has an active order, When adding again, Then the existing id is returned and no duplicate row is inserted.

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected compile failure because the test referenced the new `selectCandidatesByKeyword(...)` contract before implementation.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 21 tests / 0 failures / 0 errors / 0 skipped.

## Contract Or Integration Verification

- Verified candidate search no longer calls effective schedule, route product, or route active-version lookups in QA-only scenarios.
- Verified add active order no longer calls `validateWorkOrderConfirmed`.
- Verified active order snapshots and PQC tasks are generated from QA-derived process identity.

## Observability Touchpoints

- Existing audit insertion for `ADD_ACTIVE_ORDER` remains.
- Existing fail-fast service exceptions remain visible to the caller; no fallback success path was added.

## Blockers And Downstream Skill Needs

- No blockers.
- No downstream migration or frontend contract change required.
