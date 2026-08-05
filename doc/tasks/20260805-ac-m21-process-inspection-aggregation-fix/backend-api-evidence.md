# Backend API Evidence - AC-M21 Process Inspection Aggregation

## Scope

- Service scope: `MesPqcProcessInspectionAggregationServiceImpl#aggregateApprovedPqcSubmission`.
- Trigger path: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission` calls aggregation only for approved `PQC_INSPECTION` events.
- Data contract: approved PQC review must create structured process-inspection aggregate rows from formal PQC task and piece-detail records, not from raw payload.

## Contract

- Approved PQC review requires a pending `mes_pro_process_pool_pqc_record`.
- Source event must be `PQC_INSPECTION`, tenant-matched, and bound to `MES_PQC_INSPECTION_TASK`.
- Source task must be `SUBMITTED`, tenant-matched, and carry task identity: work order, route, route process, process, regulation version, inspection type, business date, shift, round, and actual quantity.
- Source piece details must exist and carry item, method, standard, equipment snapshot, measured value, and judgement.
- Aggregation atomically marks the PQC record `AGGREGATED`, confirms the PQC task `SUBMITTED -> CONFIRMED`, and inserts aggregate detail rows.

## BDD

- BDD: Approved PQC review creates structured process inspection aggregation -> Given a submitted PQC inspection event with structured item results and a team leader approval, When the approval is completed, Then the system persists process inspection aggregate detail rows traceable to tenant, event, review, task, round, regulation version, item, piece and revision.
- BDD: Non-final or unapproved PQC submissions are excluded -> Given pending, rejected, self-review-blocked, old revision, duplicate, or cross-tenant PQC data, When aggregation runs, Then only the final approved revision is aggregated and all other data is excluded without default success.

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL.
- Expected reason: AC-M21 structured aggregate model and mapper were missing; Maven also exposed pre-existing unrelated test compile blockers in the module.

## GREEN

- GREEN: selected production `javac` over AC-M21 changed classes -> PASS.
- GREEN: JUnit Console `MesPqcProcessInspectionAggregationServiceTest` -> PASS, 7 tests successful.
- GREEN: JUnit Console `MesProcessPoolSchemaTest` + `MesQaPqcSchemaTest` -> PASS, 5 tests successful.

## Verification

- Verified approved PQC aggregation inserts structured detail rows with tenant/event/review/task/item/piece traceability.
- Verified missing PQC record, duplicate aggregation, cross-tenant mismatch, missing piece detail, and task-confirmation CAS failure all fail fast.
- Verified schema contracts for process-pool and PQC task/status metadata.

## Validation

- Backend evidence validator is part of the task gate.
- Full Maven verification remains blocked by unrelated compile errors and Windows native memory exhaustion; targeted JUnit Console verification passed.

## Error Behavior

- Missing PQC record fails fast with `PRO_PROCESS_POOL_PQC_RECORD_REQUIRED`.
- Duplicate or concurrent aggregation fails fast with `PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED`.
- Cross-tenant event/task/detail mismatch fails fast before aggregate insert.
- Missing formal piece details fails fast; raw payload is not used as a fallback source.

## Observability

- Aggregate rows preserve `eventId`, `reviewId`, `productionSubmitEventId`, `pqcTaskId`, `regulationVersionId`, `inspectionType`, `roundNo`, `sourcePieceDetailId`, `aggregatedAt`, and tenant.
- PQC record preserves aggregation status, triggering review ID, and aggregated timestamp.

## Blockers

- Full Maven module verification is blocked by current environment/workspace state:
  - `mvn -pl yudao-module-mes -am ... test` failed at test compilation due unrelated existing missing symbols in other test classes.
  - `mvn -pl yudao-module-mes -am "-DskipTests" compile` hit Windows native memory/page-file exhaustion during MES full javac.
  - Low-memory Maven retry timed out and its task-owned Maven process was stopped.
