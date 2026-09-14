# Backend API Evidence

## Endpoint And Scope

- Endpoint: `POST /mes/pro/process-pool/team-leader/submission/allocation/confirm`.
- Scope: allocation confirmation request validation boundary for signature fields.

## API And Data Contract

- Required allocation fields remain `eventId`, `leaderType`, `allocationMode`, and `allocations`.
- `reviewSignatureId`, `reviewSignatureEmployeeUserId`, and `reviewSignatureSnapshotJson` are optional for allocation-only confirmation.
- If any review signature field is supplied, the service still validates the complete signature payload and leader/user match.

## Auth Permissions Validation Errors

- Permission remains `mes:pro-process-pool-team-leader:review`.
- Missing event, leader, leader type, invalid mode, invalid allocation lines, duplicate allocation, or unauthorized scope still fail fast through existing service errors.
- Partial or malformed signature payload still fails if any signature field is supplied.

## Config Services Fixtures Migrations

- No schema migration or new external service is required.
- Existing nullable review signature columns are preserved.

## BDD

- BDD: 分配弹窗隐藏内部字段 -> Given allocation-only confirmation from the production leader UI When the request contains formal allocation lines and no client-visible signature IDs Then the backend accepts the allocation contract without requiring hidden signature fields.

## RED And GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before implementation because allocation confirm signature fields were still `@NotNull`.
- GREEN: Source-level implementation and frontend contract passed; target Maven GREEN is blocked by unrelated test-compile failures before Surefire.

## Contract Verification

- Updated `MesP0TeamLeaderReviewSignatureSchemaTest` to assert submission review still requires signature fields while allocation confirm request does not require client-visible signature fields.

## Observability

- Existing allocation confirmation service errors remain explicit; no exception swallowing or default success path was added.

## Blockers

- Backend target Maven cannot complete in the current shared workspace because unrelated test sources fail to compile and concurrent same-module Maven processes are active.
