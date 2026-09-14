# Backend API Evidence

## Scope

`MesTeamLeaderOrderProcessCompletionService` and frontline submit source handling. Review/allocation target reach writes only source/progress facts; formal batch-record creation is deferred to flow4 completion.

## Contract

Flow2 publishes only `ProductionSubmissionFactRecorded`, `ProductionSubmissionReviewed`, `ProductionSubmissionRejected`, and `ProductionAllocationConfirmed`, carrying the frozen source IDs and version/hash/signature fields in the task development plan. Its recordbook input is an explicit `MesProFrontlineRecordbookSourceSnapshot`, not a formal recordbook entry. It must not call `completeAndBackfill`, create a backfill receipt, create a batch execution, upload any material, or release.

## Auth and Validation

Existing submit/review/allocation authorization and context validation remain in their owning services. Invalid context, version, idempotency, or source binding remains a fail-fast structured service error. No fallback or swallowed exception was added.

## BDD

BDD: process target reached during review/allocation -> Given valid formal source facts and target quantity reached When review/allocation is accepted Then only the stage fact and progress projection are written and no downstream backfill/batch write occurs.

## TDD Evidence

RED: Historical runs reproduced the ERP `assertContains` arity error, missing MES `EquipmentOption`, and two MES test-contract errors; all were fixed in the current worktree.
GREEN: ERP `test-compile`, MES reactor main/test compilation (2783/488 sources), and the target flow2/QA test run all pass.
REGRESSION: 108 flow2 plus adjacent review/rejection, confirmation, allocation version/idempotency/concurrency/overage, initial-allocation boundary, projection, QA save, and Word-import tests pass.

## Data and Migration

No schema or migration changes. Existing `rawPayload` retains a `recordbookSourceSnapshot`; formal records are not written by the submit transaction.

## Observability and Blockers

Existing process-pool event IDs, signatures, source trace, and completion projection remain the audit points. The full reactor test still has unrelated infra runtime failures; runtime, database, and cross-thread PQC contract verification remain outside this task's completed evidence.

## Verification

Static source and task-document checks pass; reactor compilation plus task-scope production tests and adjacent regression pass; full-reactor runtime verification remains red on unrelated infra tests and service/database/write-E2E verification was not run.
