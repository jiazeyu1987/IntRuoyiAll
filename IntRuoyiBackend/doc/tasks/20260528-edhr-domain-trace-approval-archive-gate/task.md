# 20260528 EDHR DomainTrace Approval Archive Gate

## Goal

Implement fail-fast DomainTrace gates for eDHR approval, rejection, and archive generation. Approval snapshot submission already locks `domainTraceHash`; approval, rejection, and archive generation must re-check the current domain trace against that locked hash before any signature, BPM, renderer, file, or archive side effect.

## Milestones

- [completed] M0 Create backend task record before code changes.
- [completed] M1 Add RED backend tests for approval, rejection, and archive DomainTrace gates.
- [completed] M2 Implement minimal backend gate behavior without fallback.
- [completed] M3 Run focused Maven verification and record GREEN evidence.
- [completed] M4 Review changed paths, real E2E, DB/hash evidence, and residual risk before committing.

## BDD

BDD: Approval revalidates locked DomainTrace before side effects -> Given a submitted eDHR execution has an approval snapshot with a locked `domainTraceHash`, When an approver approves the BPM task, Then the backend calls DomainTrace approval verification with the execution id and locked hash before recording an approval signature or approving the BPM task.

BDD: Rejection revalidates locked DomainTrace before side effects -> Given a submitted eDHR execution has an approval snapshot with a locked `domainTraceHash`, When an approver rejects the BPM task, Then the backend calls DomainTrace approval verification with the execution id and locked hash before recording a reject signature or rejecting the BPM task.

BDD: Approval action fails fast when locked DomainTrace hash is absent -> Given a submitted eDHR execution has an approval snapshot whose `snapshotJson` omits or blanks `domainTraceHash`, When approve or reject is requested, Then the backend rejects the action before recording any signature or calling BPM.

BDD: Archive generation revalidates locked DomainTrace before archive side effects -> Given an approved and closed eDHR execution has an approved approval snapshot with a locked `domainTraceHash`, When archive generation is requested, Then the backend calls DomainTrace archive verification with the execution id and locked hash before creating archive rows, renderer output, files, or archive seal signatures.

BDD: Archive generation fails fast when locked DomainTrace hash is absent or stale -> Given an approved and closed eDHR execution has an approval snapshot with missing or stale `domainTraceHash`, When archive generation is requested, Then the backend rejects the action before renderer, file, archive row, event, or seal-signature side effects.

## Expected Verification

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm e2e:edhr:approval-tracking:check`
- `pnpm e2e:edhr:approval-tracking`
- `node doc/tasks/20260528-edhr-domain-trace-approval-archive-gate/scripts/verify-edhr-domain-trace-approval-archive-db-hash.cjs`

## Current Status

Completed. Focused backend TDD, adjacent DomainTrace regression, real test-tenant E2E, final DB/hash verification, and independent reviewer checks are green for this slice.

Release decision for this task: PASS for the DomainTrace approval/reject/archive gate. This does not mean the full eDHR product is globally production-ready; it closes this specific production-readiness gap.
