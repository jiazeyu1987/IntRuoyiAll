# Backend API Evidence - DCC NAS Control Audit Files Page, Recognition, And Import Execution

## Scope

Implemented backend slices for DCC NAS uncontrolled audit file details, deterministic pre-recognition, and import-selected:

- `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`
- `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`
- `POST /dcc/controlled-files/nas-control-audit/{taskId}/import-selected`
- `GET /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/content` is implemented and GREEN-verified in isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`.
- `POST /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/local-write-result`.
- Service-level import-selected persistence: `DccControlledFileNasTransferService#createUncontrolledImportTask(Long userId, Long auditTaskId, DccNasUncontrolledImportSelectedReqVO reqVO)`.
- Service-level content read: `DccControlledFileNasTransferService#readUncontrolledImportContent(Long userId, Long importTaskId, Long auditFileId, String sourceSignature, String localRelativePath)`.
- Service-level local write result: `DccControlledFileNasTransferService#recordUncontrolledImportLocalWriteResult(Long userId, Long importTaskId, Long auditFileId, DccNasUncontrolledImportLocalWriteResultReqVO reqVO)`.
- Controller contract, service method, mapper page query, request VO, response VO.
- Recognition reuses enabled DCC project codes, active file categories, active category match rules and active taxonomy paths.
- Import isolation prevents existing NAS transfer waiting processors from claiming or executing `NAS_UNCONTROLLED_IMPORT` tasks before content download and local-write-result.
- Import-selected idempotency is service-level protected by canonical request hash comparison and a transaction-time recheck using `FOR UPDATE` before task insertion.
- Scope does not complete archive execution, frontend behavior, or real E2E.

## Contract

- Files page endpoint: `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`.
- Recognition endpoint: `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`.
- Import-selected endpoint: `POST /dcc/controlled-files/nas-control-audit/{taskId}/import-selected`.
- Content endpoint: `GET /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/content`.
- Local-write-result endpoint: `POST /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/local-write-result`.
- Query permission: `@ss.hasPermission('dcc:controlled-file:query')`.
- Import-selected permission: `@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')`.
- Content permission: `@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')`.
- Local-write-result permission: `@ss.hasPermission('dcc:controlled-file:submit') and @ss.hasPermission('dcc:controlled-file:directory:manage') and @ss.hasPermission('dcc:controlled-file:category:manage')`.
- Content controller response: `ResponseEntity<byte[]>`, not `CommonResult`, JSON, or base64.
- Content controller request: path `importTaskId`, path `auditFileId`, query `sourceSignature`, and query `localRelativePath`.
- Content response headers include `Content-Disposition`, `Access-Control-Expose-Headers`, and `X-Source-Signature`.
- Local-write-result controller response: `CommonResult<DccControlledFileNasTransferRespVO>`.
- Local-write-result controller request: path `importTaskId`, path `auditFileId`, and `@Valid @RequestBody DccNasUncontrolledImportLocalWriteResultReqVO`.
- Local-write-result request body: `sourceSignature`, `localRelativePath`, `localWriteStatus`, optional `localWriteErrorCode`, and optional `localWriteError`.
- Import-selected controller response: `CommonResult<DccControlledFileNasTransferRespVO>`.
- Import-selected controller request: path `taskId` plus `@Valid @RequestBody DccNasUncontrolledImportSelectedReqVO`.
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
- The import-selected controller binds the current login user via `getLoginUserId()`, passes the path `taskId` unchanged as `auditTaskId`, and delegates to `DccControlledFileNasTransferService#createUncontrolledImportTask(...)`.
- The import-selected controller contract test verifies mapping, response type, write permission combination, and `@Valid @RequestBody` on the request VO.
- `readUncontrolledImportContent(...)` binds current user, import task, audit file, source signature and local relative path snapshot before reading NAS bytes.
- Content read rejects invalid user/task/source type, cross-task audit file, missing task item binding, stale source signature, stale local relative path, non-selected download status, non-`NOT_STARTED` local-write/archive state, or already-created controlled file before NAS read.
- Content read returns binary bytes and deliberately does not update audit file, task item, local-write state, archive state, controlled-file rows, workflow submission, or active NAS source mappings.
- Local-write-result binds current user, import task, audit file, selected task item, source signature, and local relative path snapshot before any state mutation.
- Local-write-result accepts only terminal `LOCAL_WRITTEN` or `LOCAL_WRITE_FAILED`; invalid status fails fast.
- `LOCAL_WRITTEN` updates audit download status and task item local-write status, clears local-write errors, and deliberately does not read NAS bytes, submit controlled files, archive, or insert ACTIVE NAS source mappings.
- Replaying the same `LOCAL_WRITTEN` returns current task state without audit/item updates or archive side effects; conflicting terminal replay is rejected before mutation.
- `LOCAL_WRITE_FAILED` records scoped local write error code/message on audit and task item snapshots without creating controlled files or archive records.

## BDD Scenarios

BDD: Uncontrolled audit file page is queryable -> Given a NAS audit task exists and contains uncontrolled file details When an authorized user opens the files page Then backend returns a paged `CommonResult<PageResult<DccNasControlAuditFileRespVO>>` filtered by task id and query parameters.

BDD: Files page keeps report output compatible -> Given the existing NAS audit start/get/download API exists When the files page API is added Then start/get/download mappings and permissions still pass controller contract regression.

BDD: Deterministic recognition persists candidates -> Given pending uncontrolled audit files and active project/category rules When recognition is requested Then backend writes `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS`, stable reason code, candidate summary and expected local relative path.

BDD: Recognition does not archive -> Given a pending audit file can be matched When recognition runs Then backend updates only audit snapshot fields and does not create import tasks, content downloads, local write results, archive records or controlled files.

BDD: Import-selected does not reuse legacy transfer defaults -> Given NAS_UNCONTROLLED_IMPORT reuses the transfer task table When import-selected creates a task Then the request and service contract must not require or fabricate legacy task-level template/effective-date/project defaults.

BDD: Legacy processor skips uncontrolled import tasks -> Given a NAS_UNCONTROLLED_IMPORT task exists When existing waiting processors run before content and LOCAL_WRITTEN Then they skip the task and do not read NAS content, submit DCC files, or write ACTIVE NAS source mappings.

BDD: Import-selected creates atomic task snapshots -> Given selected audit files are valid and matched to current task snapshots When the service creates an import task Then it writes task header, task items and audit bindings in one transaction without legacy task target defaults.

BDD: Import-selected idempotency is transaction protected -> Given an identical import request races with another request that inserts the same idempotent task When the backend reaches the transaction Then it rechecks the key/hash under lock and returns the existing task without duplicate writes.

BDD: Import-selected controller is write-permission protected -> Given an authorized user submits selected uncontrolled audit files through the NAS audit task API When `/dcc/controlled-files/nas-control-audit/{taskId}/import-selected` is called Then the controller requires NAS transfer write permissions, validates the request body, binds current login user and audit task id, and delegates to the import-selected service without exposing legacy transfer defaults.

BDD: Content binary download is snapshot-bound -> Given a selected `NAS_UNCONTROLLED_IMPORT` task item belongs to the current user and matches audit file/source signature/local path snapshots When content is requested Then backend returns binary `ResponseEntity<byte[]>` and does not mutate local-write or archive state.

BDD: Content binary download rejects stale snapshots -> Given the import task, audit file, source signature, or local relative path no longer matches the selected snapshot When content is requested Then backend fails fast before reading NAS bytes.

BDD: Local write result marks browser write success without archive side effects -> Given a selected import task item matches the current user, audit file, source signature and local path snapshots When local-write-result posts `LOCAL_WRITTEN` Then backend updates only audit/task item local-write state and does not read NAS, create controlled files, submit workflow, archive, or write ACTIVE NAS source mappings.

BDD: Local write result rejects conflicting terminal replay -> Given the same audit file is already terminal `LOCAL_WRITTEN` When a conflicting `LOCAL_WRITE_FAILED` result is posted Then backend fails fast before audit/item mutation and before archive side effects.

BDD: Archive metadata missing after local write is visible -> Given a matched uncontrolled import file reaches `LOCAL_WRITTEN` without a formal archive metadata source When backend evaluates archive eligibility Then it records `archiveStatus=FAILED` and `archiveErrorCode=ARCHIVE_METADATA_REQUIRED` instead of using current date, legacy task defaults or empty metadata.

BDD: Archive metadata blocker replay is idempotent -> Given `LOCAL_WRITTEN` already produced `ARCHIVE_METADATA_REQUIRED` When the same local-write-result is replayed Then backend returns the current task state without mutating rows or repeating archive side effects.

## RED Evidence

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing endpoint mapping `/dcc/controlled-files/nas-control-audit/{taskId}/files`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognition snapshot fields such as `classification_candidates_json` were missing.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognize VO/service implementation was not present.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `DccNasUncontrolledImportSelectedReqVO` class missing.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: legacy processor claimed `sourceType=NAS_UNCONTROLLED_IMPORT`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service still threw `UnsupportedOperationException` from the fail-fast M17 stub.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service returned newly inserted task `8202` instead of existing idempotent task `8102`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsImportSelectedWithTransferWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing endpoint mapping `/dcc/controlled-files/nas-control-audit/{taskId}/import-selected`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing `readUncontrolledImportContent(...)` service contract.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing `DccNasUncontrolledImportController`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing local-write-result controller/service/VO contract.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: existing local-write-result implementation left matched files at `archiveStatus=NOT_STARTED` after `LOCAL_WRITTEN` instead of recording `ARCHIVE_METADATA_REQUIRED`.

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

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsImportSelectedWithTransferWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`; `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

REGRESSION: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`; `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dmaven.resources.skip=true" "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dmaven.resources.skip=true" "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 19, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

## Verification

- Controller contract now covers start/get/download, files page and recognize mappings with `dcc:controlled-file:query` permission, plus import-selected/content/local-write-result mappings with `submit + directory:manage + category:manage`.
- DCC module main and test sources compile under the targeted Maven reactor command.
- Existing audit scan detail persistence test still passes after adding files page and recognition APIs.
- Service tests cover unique match, pending when category missing, ambiguous when project duplicates, and no rewrite when no pending rows are selected.
- Transfer service tests cover import-selected request/service signature isolation from legacy transfer fields, legacy waiting processor skip behavior, service-level atomic task/item/audit binding creation, mixed invalid selection rejection, canonical idempotency reuse, request-hash conflict rejection, duplicate audit id rejection, transaction-time duplicate insert prevention, content binary snapshot binding, local-write-result terminal replay/conflict handling, and explicit `ARCHIVE_METADATA_REQUIRED` blocker handling for `NAS_UNCONTROLLED_IMPORT`.
- Content controller tests cover binary `ResponseEntity<byte[]>`, snapshot query params and write-permission combination.
- Local-write-result controller tests cover `CommonResult` response, `@Valid @RequestBody` snapshot body and write-permission combination.
- Backend API evidence validator passed after M23 update: `Backend API evidence is valid.`
- M23 contract keeps the existing local-write-result endpoint shape and permissions, changes matched `LOCAL_WRITTEN` service behavior to a visible archive metadata blocker, and does not require migrations or external services.
- BDD/TDD acceptance validator passed after M23 update: `BDD/TDD acceptance plan validation passed.`

## Blockers

- Full objective remains in progress: formal archive success metadata source, controlled-file creation, ACTIVE NAS source mapping, frontend, and real E2E are not yet implemented.
- Final closeout/commit/push remains blocked by pre-existing dirty worktree state, mixed concurrent commits, and concurrent unrelated changes; do not mix those with this task slice.

## M24 Backend Formal Archive Success Slice

- Scope: completed backend service path for `MATCHED + LOCAL_WRITTEN` uncontrolled import items with complete formal archive metadata snapshots.
- Contract: archive submit metadata must come from processing-item-level `archiveCategoryIdSnapshot`, `archiveDirectoryIdSnapshot`, `archiveDccProjectCodeIdSnapshot`, `archiveFileTypeTaxonomyIdSnapshot`, `archiveChangeTypeSnapshot`, `archiveFileNameSnapshot`, `archiveFileNumberSnapshot`, `archiveVersionNoSnapshot`, `archiveEffectiveDateSnapshot`, and `archiveRemarkSnapshot`.
- Service behavior: complete snapshot reads NAS, uploads the original, submits controlled file workflow without approval, inserts exact NAS source mapping, marks audit/item `ARCHIVED`, and leaves local write status `LOCAL_WRITTEN` for replay idempotency.
- Failure boundary: missing formal snapshot metadata still records `ARCHIVE_METADATA_REQUIRED`; the implementation does not use legacy task headers, candidate JSON, current date, empty templates, or taxonomy matches alone as fallback submit metadata.
- GREEN: M24 archive success targeted service test passed with Tests run 1, Failures 0, Errors 0, Skipped 0.
- REGRESSION: content/local-write/archive targeted set passed with Tests run 7, Failures 0, Errors 0, Skipped 0.