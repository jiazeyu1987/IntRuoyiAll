# Flow4 Backend API Evidence

## Scope

`POST /mes/pro/process-pool/team-leader/active-order/complete` executes the active-order Tx-A and returns an immutable completion receipt handoff for Flow6. The endpoint is leader-owned and tenant-scoped.

## Contract

- The service locks the active order and authoritative production/PQC/source snapshots, then requires both progress values to be 100%.
- It writes batch-record, process-inspection, and loss/`NO_LOSS` results in one transaction. A writer failure aborts the transaction and no success receipt is inserted.
- Positive loss requires `hasActualLoss=true`, `lossQuantity>0`, and a formal loss record. No loss requires an explicit zero-loss snapshot, `hasActualLoss=false`, quantity `0`, and `lossReportStatus=NOT_REQUIRED`; no loss document is created.
- The receipt is immutable, has stable formal result IDs, `status=BACKFILL_SUCCEEDED`, source/signature snapshot data, completion version, idempotency evidence, and no `batchExecutionId`/`BATCH_*` state.
- Flow6 reads only the tenant-scoped receipt port. Missing, cross-tenant, tampered, incomplete, non-success, or loss-inconsistent receipts fail fast; Flow6 must not rebuild from raw production/PQC facts.

## BDD

- `BDD: dual 100 completion -> Given` a leader-owned active order has authoritative production and PQC progress at 100% and complete formal sources; `When` complete is submitted; `Then` Tx-A writes all three results and one immutable success receipt.
- `BDD: writer rollback -> Given` any one writer fails; `When` complete is submitted; `Then` all Tx-A writes and the receipt roll back and Flow6 is not invoked.
- `BDD: idempotent retry -> Given` a success receipt exists; `When` the same idempotency key and source hash are retried; `Then` the same receipt is returned without new writes; a changed payload/source hash conflicts.
- `BDD: loss branches -> Given` positive loss or explicit `NO_LOSS`; `When` complete is submitted; `Then` only the applicable loss result is written and the receipt records the matching status.

## TDD Evidence

- `RED: mvn -pl yudao-module-mes -Dtest=... test` -> FAIL, new receipt result-ID getters/contract were absent.
- `GREEN: mvn -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderCompletionServiceTest,MesTeamLeaderActiveOrderCompletionBackfillPortImplTest,MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest,MesProcessPoolTeamLeaderSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 37/37.
- `GREEN: mvn -pl yudao-module-mes -DskipTests -Dmaven.test.skip=true compile` -> PASS.

## Validation

The service contract test covers tenant/owner checks, dual-100 gating, formal result IDs, loss branches, idempotency/conflict, and Flow6 rejection of missing or tampered receipts.

## Verification

The targeted MES command produced 37 passing tests and the MES compile completed successfully. `git diff --check` passed. No runtime database or E2E write was attempted.

## Blockers

No database-backed transaction test or real-data E2E was run. The full reactor is independently blocked by `yudao-server` MDEP-98; an unrelated main-worktree test also has a pre-existing Java escape error.
