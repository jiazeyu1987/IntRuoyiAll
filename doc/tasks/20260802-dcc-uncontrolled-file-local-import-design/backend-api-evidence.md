# Backend API Evidence - DCC NAS Control Audit Files Page, Recognition, And Import Isolation

## Scope

Implemented backend slices for DCC NAS uncontrolled audit file details and deterministic pre-recognition:

- `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`
- `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`
- Service-level import-selected persistence: `DccControlledFileNasTransferService#createUncontrolledImportTask(Long userId, Long auditTaskId, DccNasUncontrolledImportSelectedReqVO reqVO)`.
- Controller contract, service method, mapper page query, request VO, response VO.
- Recognition reuses enabled DCC project codes, active file categories, active category match rules and active taxonomy paths.
- Import isolation prevents existing NAS transfer waiting processors from claiming or executing `NAS_UNCONTROLLED_IMPORT` tasks before content download and local-write-result.
- Import-selected idempotency is service-level protected by canonical request hash comparison and a transaction-time recheck using `FOR UPDATE` before task insertion.
- Scope does not implement import-selected controller mapping, content, local-write-result, archive execution, frontend behavior, or real E2E.

## Contract

- Files page endpoint: `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`.
- Recognition endpoint: `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`.
- Permission: `@ss.hasPermission('dcc:controlled-file:query')`.
- Import-selected service request: `DccNasUncontrolledImportSelectedReqVO` contains `selectionScope`, `idempotencyKey`, and `selectedFiles[auditFileId, sourceSignature, localRelativePath]`; it intentionally does not expose legacy transfer target fields such as task-level template category, effective date, or project code.
- Import-selected service entry validates all selected audit files before writing; invalid mixed selections fail atomically without task/item inserts or audit row updates.
- Import-selected idempotency contract: same `idempotencyKey + requestHash` returns the existing task, same key with different request hash throws a conflict before audit reads or writes, and duplicate audit ids fail before hash calculation or persistence.
- Files page request: `taskId` path variable plus `pageNo/pageSize`, `keyword`, `classificationStatus`, `downloadStatus`, and `archiveStatus` query fields.
- Files page response: `CommonResult<PageResult<DccNasControlAuditFileRespVO>>`.
- Response rows expose audit metadata and state fields: `auditFileId`, task id, NAS share, normalized relative path, file metadata, source signature, classification/download/archive statuses, matched project/category ids, reasons, relative local path, error codes, and controlled file id.
- Query order is stable by `id ASC`.
- Recognition response: `CommonResult<DccNasControlAuditRecognizeRespVO>` with `matchedCount`, `unclassifiedPendingCount`, `ambiguousCount`, and `skippedCount`.
- Recognition status contract: unique project + unique category writes `MATCHED`; missing project/category writes `UNCLASSIFIED_PENDING`; multiple project/category candidates writes `AMBIGUOUS`.
- Recognition data contract: every processed pending row writes `classificationReason`, `classificationCandidatesJson`, and `expectedLocalRelativePath`; matched rows also write project code id and taxonomy levels.

## Validation

- The service calls `requireTask(taskId)` before querying details so missing or inaccessible tasks fail fast instead of returning a fake empty page.
- The mapper filters by exact `task_id`, optional classification/download/archive status, and optional keyword against normalized path or file name.
- The response mapper sets `auditFileId` from the DO primary key and does not expose local absolute paths, NAS credentials, or file content.
- Recognition only reads rows whose classification status is `PENDING_RECOGNITION`; already imported or archived snapshots are not rewritten by this slice.
- Recognition does not read NAS bytes, call content download, create import tasks, write local results, archive, or create `dcc_controlled_file` rows.
- Unknown or blank category match rule type/text fails fast instead of being treated as no-match or default success.
- `processWaitingTasks()` skips `sourceType=NAS_UNCONTROLLED_IMPORT` before `executeTask`, so legacy NAS transfer code cannot claim the task, read NAS content, call `submitControlledFileWithoutApproval`, or insert ACTIVE NAS source mappings.
- `createUncontrolledImportTask(...)` performs a non-mutating idempotency lookup before the transaction and repeats the same lookup with `LIMIT 1 FOR UPDATE` inside the transaction before any audit read, task insert, task item insert or audit binding update.

## BDD Scenarios

BDD: Uncontrolled audit file page is queryable -> Given a NAS audit task exists and contains uncontrolled file details When an authorized user opens the files page Then backend returns a paged `CommonResult<PageResult<DccNasControlAuditFileRespVO>>` filtered by task id and query parameters.

BDD: Files page keeps report output compatible -> Given the existing NAS audit start/get/download API exists When the files page API is added Then start/get/download mappings and permissions still pass controller contract regression.

BDD: Deterministic recognition persists candidates -> Given pending uncontrolled audit files and active project/category rules When recognition is requested Then backend writes `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS`, stable reason code, candidate summary and expected local relative path.

BDD: Recognition does not archive -> Given a pending audit file can be matched When recognition runs Then backend updates only audit snapshot fields and does not create import tasks, content downloads, local write results, archive records or controlled files.

BDD: Import-selected does not reuse legacy transfer defaults -> Given NAS_UNCONTROLLED_IMPORT reuses the transfer task table When import-selected creates a task Then the request and service contract must not require or fabricate legacy task-level template/effective-date/project defaults.

BDD: Legacy processor skips uncontrolled import tasks -> Given a NAS_UNCONTROLLED_IMPORT task exists When existing waiting processors run before content and LOCAL_WRITTEN Then they skip the task and do not read NAS content, submit DCC files, or write ACTIVE NAS source mappings.

BDD: Import-selected creates atomic task snapshots -> Given selected audit files are valid and matched to current task snapshots When the service creates an import task Then it writes task header, task items and audit bindings in one transaction without legacy task target defaults.

BDD: Import-selected idempotency is transaction protected -> Given an identical import request races with another request that inserts the same idempotent task When the backend reaches the transaction Then it rechecks the key/hash under lock and returns the existing task without duplicate writes.

## RED Evidence

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing endpoint mapping `/dcc/controlled-files/nas-control-audit/{taskId}/files`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognition snapshot fields such as `classification_candidates_json` were missing.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognize VO/service implementation was not present.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `DccNasUncontrolledImportSelectedReqVO` class missing.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: legacy processor claimed `sourceType=NAS_UNCONTROLLED_IMPORT`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service still threw `UnsupportedOperationException` from the fail-fast M17 stub.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service returned newly inserted task `8202` instead of existing idempotent task `8102`.

## GREEN Evidence

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksPendingWhenProjectOrCategoryMissing,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksAmbiguousWhenProjectOrCategoryHasMultipleCandidates,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_doesNotRewriteImportedOrArchivedSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails,DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS, 2 passed in 0.17s.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

## Verification

- Controller contract now covers start/get/download, files page and recognize mappings with `dcc:controlled-file:query` permission.
- DCC module main and test sources compile under the targeted Maven reactor command.
- Existing audit scan detail persistence test still passes after adding files page and recognition APIs.
- Service tests cover unique match, pending when category missing, ambiguous when project duplicates, and no rewrite when no pending rows are selected.
- Transfer service tests cover import-selected request/service signature isolation from legacy transfer fields, legacy waiting processor skip behavior, service-level atomic task/item/audit binding creation, mixed invalid selection rejection, canonical idempotency reuse, request-hash conflict rejection, duplicate audit id rejection and transaction-time duplicate insert prevention for `NAS_UNCONTROLLED_IMPORT`.
- Backend API evidence validator passed after M19 update: `Backend API evidence is valid.`

## Blockers

- Full objective remains in progress: import-selected controller mapping, content binary download, local-write-result, archive execution, frontend, and real E2E are not yet implemented.
- Final closeout/commit/push remains blocked by the pre-existing dirty worktree and concurrent unrelated changes; do not mix those with this task slice.
