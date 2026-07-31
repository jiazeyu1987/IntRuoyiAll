# Config Security Deployment

## Purpose and Scope

Define non-functional design for making batch void close workbench tasks without weakening security, permissions, deployment, or observability.

## Evidence Reviewed

- Existing void flows already require permissions, signature/password or BPM approval depending on policy.
- Existing work task cancellation revokes runtime task entitlement.
- Existing terminal task open behavior blocks voided batches.

## Configuration

No new configuration flag should be added. The behavior is a core lifecycle invariant, not an optional tenant switch.

## Secrets

No new secrets are required. Existing signature/password verification must remain unchanged and must not be logged.

## Permissions

- Void permission and golden-finger permission remain unchanged.
- Canceling active work tasks is a system side effect of an approved/effective void, not a user-facing separate permission.
- Runtime task entitlement must be revoked through the existing work task service path.

## Security Controls

- Do not permit old workbench URLs to process voided batches.
- Do not expose canceled tasks as actionable待办 items.
- Do not swallow cancellation failures because that would leave users with invalid active privileges.
- Do not log passwords, tokens, or raw signature challenges.

## Deployment

Implementation should be a backend behavior change plus optional frontend regression checks. No new service, port, environment variable, or deployment topology is required.

## Observability

Minimum evidence after implementation:

- Change event shows void became effective.
- Batch status is `VOIDED`.
- Active work tasks for the batch are `CANCELED` with reason.
- Runtime task entitlements are revoked.
- Personal console API and page no longer show target task.
- Old `openTask` path fails fast with terminal state.

## Open Questions

- Whether operation audit should include a batch-level summary field such as canceled work-task count. Not required for first implementation if work-task rows are already traceable.

## Design Blockers

- If runtime entitlement revocation fails or is not available in current test context, implementation must fail fast and record the blocker rather than silently skipping entitlement cleanup.
