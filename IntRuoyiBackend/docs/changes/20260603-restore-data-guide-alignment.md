# Change Request: Restore Data Guide No Longer Requires Rehearsal Or Snapshot

## Request Summary And Source

- Source: user request on 2026-06-03.
- Request: “恢复数据改成不需要演练报告,不需要现场快照”。

## Current Baseline Reviewed

- `RuntimeOpsCandidateServiceImpl` already does not block restore candidates for missing `manifest/rehearsal-report.json` or `manifest/现场快照.md`.
- `RuntimeBackupDrillServiceImpl` already treats rehearsal report verified time as optional for backup point recoverability.
- `RuntimeOpsGuideServiceImpl` still lists `rehearsal-report` and `现场快照` as required evidence for the `data-exception` restore-data scenario, creating a contract mismatch.

## Classification

Operations requirement change and backend API contract alignment.

## Impact

- Product: restore-data guidance becomes consistent with the new rule.
- Design: wizard evidence chips and blocking text should not imply rehearsal report or site snapshot are required for restore data.
- Data: no database schema or stored-data change.
- API: `data-exception.requiredEvidence` removes `rehearsal-report` and `现场快照`; restore candidate fields remain available for optional display/legacy clients.
- Tests: update `RuntimeOpsGuideServiceImplTest` and retain restore candidate regression coverage.
- Release: low code risk, but operational policy risk is explicit because restore can proceed without rehearsal/snapshot evidence.
- Operations: manifest、checksum、imageTag remain required; this task must not generate fake rehearsal evidence.

## Decision

Accepted. Implement the official rule change by removing rehearsal report and site snapshot from restore-data required evidence and scenario blocking conditions. Do not fake successful rehearsal status.

## Required Approvals

User request is the explicit approval for this scope.

## Downstream Skill Reruns

- `backend-api-delivery` for service contract and tests.

## Blockers And Next Action

No blockers. Next action is RED test, minimal implementation, targeted verification.
