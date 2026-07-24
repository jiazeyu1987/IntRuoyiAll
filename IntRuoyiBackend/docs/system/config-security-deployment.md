# Config, Security, and Deployment: Current-System-First Auto Scheduling

## Purpose and Scope

This document defines configuration, permissions, security controls, deployment notes, and observability for IntRuoyi automatic scheduling. The design must fail fast on missing prerequisites and must not silently downgrade scheduling behavior.

## Evidence Reviewed

- `docs/product/prd.md`
- `docs/product/acceptance-criteria.md`
- `docs/system/backend-api-design.md`
- Current controller permission style in `MesProTaskController`, using `@PreAuthorize("@ss.hasPermission('...')")`
- Existing production task permissions: `mes:pro-task:create`, `mes:pro-task:update`, `mes:pro-task:delete`, `mes:pro-task:query`, `mes:pro-task:export`

## Configuration

Recommended configuration keys:

- `mes.auto-schedule.enabled`
  - Enables auto scheduling endpoints and UI controls.
  - If disabled, endpoints must reject with a clear error.

- `mes.auto-schedule.default-capacity-mode`
  - Allowed values: `DEFAULT`, `PLANNED`, `ACTUAL`.
  - Must be validated at startup or first use.

- `mes.auto-schedule.material-shortage-policy`
  - Allowed values: `BLOCK_ON_SHORTAGE`, `ALLOW_RISK`.

- `mes.auto-schedule.require-preview-before-apply`
  - Open decision. If enabled, backend must require preview confirmation token or equivalent approved mechanism.

- `mes.auto-schedule.max-horizon-days`
  - Upper limit for schedule search horizon.
  - Missing or invalid value must fail fast if the scheduler requires it.

- `mes.auto-schedule.preserve-manual-tasks-by-default`
  - Controls default replan preservation behavior.

No fallback behavior should be added. Missing required config must be reported clearly.

## Secrets

No new external secret is required for first version if scheduling runs inside IntRuoyi and reads only IntRuoyi data.

If a future version calls an external scheduling service, that integration must be handled as a new scope change and must define endpoint, credentials, timeout, retry, and audit behavior.

## Permissions

Add permissions:

- `mes:pro-auto-schedule:preview`
- `mes:pro-auto-schedule:apply`
- `mes:pro-auto-schedule:replan`
- `mes:pro-auto-schedule:query`
- `mes:pro-auto-schedule:lock-task`
- `mes:pro-auto-schedule:capacity-manage`

Reuse existing permissions:

- `mes:pro-task:query` for viewing current production tasks and Gantt data.
- `mes:pro-task:create/update/delete` for manual task operations.

Permission behavior:

- Users without `apply` cannot write auto schedule results.
- Users without `replan` cannot trigger replan.
- Users without `capacity-manage` cannot edit capacity data.
- Users without `lock-task` cannot lock or unlock tasks.

## Security Controls

- Enforce permission checks at controller level and service level where write operations are sensitive.
- Validate all work order and task ids are within the current accessible tenant/org context according to existing framework behavior.
- Reject empty work order scopes.
- Reject task ids that do not belong to the requested work order scope.
- Reject apply/replan when validation issues are blocking.
- Do not swallow scheduler exceptions.
- Do not return mock success when scheduler prerequisites are missing.
- Keep generated change summaries for user review and audit if audit persistence is approved.

## Deployment

Deployment sequence:

1. Apply database migrations for capacity, dependency, metadata, and issue tables after final schema approval.
2. Deploy backend APIs with feature flag disabled if phased rollout is required.
3. Seed permissions and menu/action bindings.
4. Deploy frontend controls.
5. Enable feature flag in a controlled environment with real test data.
6. Run backend tests and Playwright E2E before production enablement.

Rollback:

- Disable `mes.auto-schedule.enabled`.
- Existing production tasks remain current schedule source.
- New scheduling extension tables can remain unused.
- Do not delete current production tasks during rollback.

## Observability

Log at service boundary:

- request id
- user id
- work order count
- capacity mode
- material policy
- replan policy
- generated/updated/deleted/preserved task counts
- issue counts by severity
- elapsed time

Metrics to add if current metric stack supports them:

- auto schedule request count
- apply success/failure count
- validation failure count by error code
- scheduling duration
- generated task count
- blocking issue count

Audit:

- Capacity changes require audit records.
- Apply/replan operations should have an operation log if approved.
- Finished or locked task preservation should be visible in result summaries.

## Open Questions

- Which configuration mechanism should hold MES auto scheduling settings in the existing deployment?
- Should auto scheduling be feature-flagged per tenant/org or globally?
- Is an operation log table required for apply/replan audit?
- Are scheduling metrics available in the current runtime stack?

## Design Blockers

- Feature flag rollout policy is not approved.
- Permission menu seed strategy is not approved.
- Operation audit persistence is not approved.
- Capacity management ownership is not approved.
