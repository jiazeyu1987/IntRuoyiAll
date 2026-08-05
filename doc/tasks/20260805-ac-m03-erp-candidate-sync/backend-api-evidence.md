# Backend API Evidence

## Scope

- Service/job scope: MES ERP candidate synchronization for AC-M03, including Kingdee production order sync and active-order transfer/batch trace projection.

## API Contract And Data Contract

- ERP production order facts must be keyed by formal ERP identity such as source FID, bill number, material number, and work order code where available.
- Transfer and batch trace facts must be keyed by formal transfer, transfer line/detail, material stock, batch, item, and active order identifiers.
- Duplicate, reordered, or conflicting source payloads must not create duplicate MES facts.

## Auth, Permissions, Validation, And Error Behavior

- This slice is service/job behavior; no frontend permission change is expected.
- Missing formal identifiers or conflicting identifiers must fail fast or be recorded as explicit conflict evidence; no default-success path is allowed.

## Required Config, Services, Fixtures, And Migrations

- Existing MES module test fixtures and mocked Kingdee client data.
- Existing mapper schema evidence for `mes_kingdee_production_order_sync_record`, MES work orders, and transfer trace rows.
- No migration is planned unless tests prove a formal unique key or source identity field is missing.

## BDD Scenarios

- BDD: Duplicate ERP order idempotency -> Given duplicate source payloads and repeated sync runs When production order sync runs Then one formal MES work order fact is created or updated.
- BDD: Out-of-order ERP snapshot -> Given an existing source-linked work order When the same ERP formal source key arrives with a changed bill number Then the source-linked work order is reused instead of creating a duplicate fact.
- BDD: Conflicting ERP source identity -> Given the same formal source identity points to one work order but the incoming bill number already belongs to another work order When sync runs Then the service fails fast and does not update facts.
- BDD: Transfer and batch trace idempotency -> Given repeated trace projection for the same active order and formal transfer/batch IDs When trace recording runs Then one trace set is created.

## RED Command And Expected Failure

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest#syncWorkOrders_usesSourceRecordWorkOrderWhenBillNoChanges" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `createdCount=0` but current implementation returned `createdCount=1`.
- Expected failure: before the fix, the same ERP formal source key with a changed `billNo` created a second work-order fact and returned `createdCount=1`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 20 tests run with 2 AC-M03 failures.
- Expected failure: before the fix, source-linked work orders were not reused and `billNo` conflicts did not fail fast.

## GREEN Command And Passing Result

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 20 tests run, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderTransferTraceSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests run, 0 failures, 0 errors.

## Contract Or Integration Verification

- Production order sync now resolves existing facts by formal source record before `billNo`.
- Transfer and batch trace contract remains covered by idempotency-key tests and schema checks.

## Observability Touchpoints

- Sync summary counts, sync records, transfer trace rows, and explicit failure/conflict assertions in tests.

## Blockers And Downstream Skill Needs

- Full AC-M03 acceptance still requires M6 real E2E/coverage ledger evidence after backend/source contracts are proven.
