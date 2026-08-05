# Backend API Evidence

## Scope

AC-M23 eDHR release transaction service and approval-center release adapter.

## Contract

- `submit`: requires precheck passed, current dossier config hash, route `RELEASE_APPROVE` owner, password validation, release signature, transaction event, operation audit.
- `approve`: requires pending approval, current approval task candidate, current-user approval signature evidence, transaction event, operation audit.
- `reject`: requires release owner for direct precheck-passed rejection or approval task candidate for pending approval rejection, reason, transaction event, operation audit.
- `withdraw`: remains lifecycle cancellation path and records operation audit.

## BDD

See `execution-log.md`.

- BDD: release owner submit audit -> Given precheck passed and current user is `RELEASE_APPROVE` owner When owner signs and submits release Then release transaction and operation audit are both recorded.
- BDD: release owner return audit -> Given precheck passed and current user is `RELEASE_APPROVE` owner When owner submits return reason Then transaction enters `REJECTED` and operation audit is recorded.
- BDD: forged signoff rejected -> Given approval request lacks matching approval-center signature record When approve is called Then service rejects without terminal transaction or audit.

## Acceptance

- `submit` terminal success writes transaction event and operation audit.
- `approve` requires a matching approval-center signature record for the current EDHR work task and user.
- `reject` supports owner return from `PRECHECK_PASSED` and keeps candidate validation for `PENDING_APPROVAL`.
- Unauthorized or unverifiable requests fail fast without fallback success.

## RED

- RED: backend regression tests added for submit audit, reject owner return, reject non-owner, fake signoff, and approval audit.
- Added focused regression coverage in `MesProEdhrReleaseServiceImplTest` for terminal operation audit, precheck-passed owner return, non-owner return rejection, unverifiable approval signoff rejection, and approval-center signature audit.
- RED command attempted: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Result: command timed out after 124s with no usable Surefire result; task-owned Maven process `MesProEdhrReleaseServiceImplTest` was identified and stopped. This is an environment/process blocker, not a product PASS.

## GREEN

- GREEN: backend implementation completed but backend Maven PASS is not claimed.
- Implemented service changes:
  - `submit`, `approve`, `reject`, and `withdraw` now record terminal `MesProEdhrOperationAuditCommand` entries after successful transaction events.
  - `approve` verifies `signoffEvidenceHash` against a current-user `bpm_approval_signature_record` generated for the EDHR approval work task.
  - `reject` now supports direct `PRECHECK_PASSED` return by the route `RELEASE_APPROVE` owner and keeps `PENDING_APPROVAL` rejection on approval-task candidate validation.
- Backend Maven GREEN is not claimed because same-module Maven processes from other work were still active in `E:\IntRuoyi\IntRuoyiBackend`.

## Verification

- Frontend integration contract verified separately in `frontend-feature-evidence.md`.
- Backend evidence validator PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260805-ac-m23-release-owner-compliance\backend-api-evidence.md`.
- Isolated detached worktree `D:\IntRuoyiWorktree\ac-m23-release-owner-verify-20260805-1` applied only the AC-M23 release service source/test diff and did not start services or reserve ports.
- Isolated backend Maven reached `yudao-module-mes` compile but did not reach Surefire because clean detached HEAD lacks the non-AC-M23 QA regulation `publish(MesQaInspectionRegulationSaveReqVO)` implementation; the temporary worktree was removed and `Test-Path` returned `False`.
- Backend Maven verification remains blocked in the main workspace by active same-module Maven processes; no backend PASS is claimed.

## Validation

- Evidence validator initially required explicit `BDD:`, `RED:`, `GREEN:`, and `Verification` markers; this file was updated with those markers.

## Blockers

- Backend targeted Maven verification remains blocked by concurrent `yudao-module-mes` Maven processes writing the same module target directory.
- Observed blockers included active Maven commands for `MesTeamLeaderSubmissionReviewServiceTest`, `MesTeamLeaderActiveOrderServiceTest`, `MesOrderReleaseCompletenessServiceTest/MesPqcProcessInspectionAggregationServiceTest/MesProEdhrReleaseServiceImplTest`, and `MesTeamLeaderBatchRecordBackfillServiceTest`.
- Current observed active blocker: `mvn -pl yudao-module-mes -am -DskipTests compile` in `E:\IntRuoyi\IntRuoyiBackend`.
