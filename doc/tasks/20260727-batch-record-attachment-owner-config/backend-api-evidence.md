# Backend API Evidence

## Scope

- Service scope: `MesProRouteServiceImpl#buildCurrentRouteSnapshotJson` and its internal complete route config snapshot builder.
- Runtime consumer: eDHR batch execution special-node filler resolution reads `configSnapshots.batchRecordAttachmentOwners` from the frozen route snapshot.

## Contract

- Data contract: if a route version snapshot contains `configSnapshots.batchRecordAttachmentOwners`, rebuilt current route snapshots must preserve the JSON array exactly as a route configuration snapshot field.
- Validation contract: invalid owner config remains fail-fast; missing or malformed owner arrays are not converted into defaults or empty success.
- Auth/permissions: unchanged.
- Migrations: none.

## BDD

- BDD: valid batch record attachment owners should not block batch execution confirm -> Given a route version snapshot has four saved batch record attachment owner entries, When the route current snapshot is rebuilt for versioning/opening flows, Then `configSnapshots.batchRecordAttachmentOwners` is preserved for eDHR runtime filler resolution.

## RED / GREEN

- RED: target JUnit failed before production change because the regenerated snapshot had no owner array.
- GREEN: target JUnit passed after preserving the existing owner array.
- REGRESSION: adjacent route snapshot and attachment-owner service tests passed.
- COMPILE: MES reactor compile passed with `-am`.

## Verification

- Target regression test passed after the fix.
- Adjacent route snapshot and attachment-owner service tests passed.
- MES reactor compile passed.

## Observability

- Existing service exception remains visible as `PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID` when route data truly lacks required owner config.
- Runtime health check confirmed local backend was UP before read-only data inspection.

## Blockers

- The screenshot route `922119` currently lacks the required owner snapshot in ACTIVE and DRAFT versions; resolving that specific path requires authorized configuration/publish or an approved migration.
