# Backend API Evidence - DCC NAS Control Audit Files Page And Recognition

## Scope

Implemented backend slices for DCC NAS uncontrolled audit file details and deterministic pre-recognition:

- `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`
- `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`
- Controller contract, service method, mapper page query, request VO, response VO.
- Recognition reuses enabled DCC project codes, active file categories, active category match rules and active taxonomy paths.
- Scope does not implement import-selected, content, local-write-result, archive, frontend behavior, or real E2E.

## Contract

- Files page endpoint: `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`.
- Recognition endpoint: `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`.
- Permission: `@ss.hasPermission('dcc:controlled-file:query')`.
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

## BDD Scenarios

BDD: Uncontrolled audit file page is queryable -> Given a NAS audit task exists and contains uncontrolled file details When an authorized user opens the files page Then backend returns a paged `CommonResult<PageResult<DccNasControlAuditFileRespVO>>` filtered by task id and query parameters.

BDD: Files page keeps report output compatible -> Given the existing NAS audit start/get/download API exists When the files page API is added Then start/get/download mappings and permissions still pass controller contract regression.

BDD: Deterministic recognition persists candidates -> Given pending uncontrolled audit files and active project/category rules When recognition is requested Then backend writes `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS`, stable reason code, candidate summary and expected local relative path.

BDD: Recognition does not archive -> Given a pending audit file can be matched When recognition runs Then backend updates only audit snapshot fields and does not create import tasks, content downloads, local write results, archive records or controlled files.

## RED Evidence

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing endpoint mapping `/dcc/controlled-files/nas-control-audit/{taskId}/files`.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognition snapshot fields such as `classification_candidates_json` were missing.

RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognize VO/service implementation was not present.

## GREEN Evidence

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksPendingWhenProjectOrCategoryMissing,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksAmbiguousWhenProjectOrCategoryHasMultipleCandidates,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_doesNotRewriteImportedOrArchivedSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails,DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS, 2 passed in 0.17s.

## Verification

- Controller contract now covers start/get/download, files page and recognize mappings with `dcc:controlled-file:query` permission.
- DCC module main and test sources compile under the targeted Maven reactor command.
- Existing audit scan detail persistence test still passes after adding files page and recognition APIs.
- Service tests cover unique match, pending when category missing, ambiguous when project duplicates, and no rewrite when no pending rows are selected.
- Backend API evidence validator passed: `Backend API evidence is valid.`

## Blockers

- Full objective remains in progress: import-selected, content binary download, local-write-result, frontend, and real E2E are not yet implemented.
- Final closeout/commit/push remains blocked by the pre-existing dirty worktree and concurrent unrelated changes; do not mix those with this task slice.
