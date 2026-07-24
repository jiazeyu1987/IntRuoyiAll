# Change Decision: Restore Data Without Rehearsal Gate

## Request

用户要求运行控制台恢复数据不再要求恢复演练，或默认恢复演练成功，并要求选择更容易实现的方案。

## Baseline

当前恢复候选会读取 `Backup/BackupPackage/<backupId>`，并把缺少 `manifest/rehearsal-report.json` 或 `manifest/现场快照.md` 作为 `BLOCKED` 原因；Backup 面板也把这两项纳入 `UNRECOVERABLE` 判定。

## Classification

Requirement change / operations gate change.

## Decision

Accepted with scope limit: remove `rehearsal-report.json` and `现场快照.md` as blocking requirements for restore candidate availability. Do not generate or assume a successful rehearsal report.

## Rationale

- Defaulting rehearsal to success would create false operational evidence.
- Removing the gate is explicit and auditable.
- Manifest, checksum and image tag checks remain mandatory, so missing core restore assets still fail fast.

## Impact

- Restore candidates with valid manifest, checksum and image tag become `AVAILABLE` even when no rehearsal report or snapshot exists.
- Backup point recoverability no longer depends on rehearsal report or snapshot.
- UI no longer shows the previous blocked reasons for otherwise valid backup points.

## Downstream

- Update backend restore candidate and backup point recoverability services.
- Update unit tests for the new gate contract.
- No database migration or frontend API shape change is required.

## Blockers

None. User explicitly requested this gate change.

## Verification

- Update restore candidate unit tests to expect availability without rehearsal evidence.
- Update backup point unit tests to expect recoverability without rehearsal evidence.
- Keep regression coverage for missing manifest/checksum/image tag blockers.
