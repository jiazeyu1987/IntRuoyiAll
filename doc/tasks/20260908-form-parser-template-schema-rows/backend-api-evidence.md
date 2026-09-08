# Backend API Evidence

## Endpoint Service Handler Scope

- Endpoint: `POST /mes/pro/batch-record-report/production-batch-record/total-recognition-json`
- Controller: `MesProBatchRecordReportController#parseProductionBatchRecordTotalRecognitionJson`
- Service: `MesProBatchRecordReportService#parseProductionBatchRecordTotalRecognitionJson`
- Handler behavior: parse uploaded production batch record Word and return batch-record total recognition JSON without creating Jimu reports, batch-record versions, approvals, DCC project-code updates, or device sync records.

## API Contract And Data Contract

- Request: multipart form with required `file`, accepted extensions `.doc` and `.docx`.
- Response: `CommonResult<String>`, where `data` is a JSON string whose top-level object contains `product`, `schemaVersion`, and `processes`.
- Target JSON contract: semantically matches `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json` for the real IDI `.doc` fixture.
- Frontend download contract: `form-center/parser/index.vue` calls `BatchRecordReportApi.parseProductionBatchRecordTotalRecognitionJson(file)`, validates `product/schemaVersion/processes` with `JSON.parse`, and downloads the parsed object as `.json`.

## Auth Permissions Validation Error Behavior

- Permission: `@ss.hasPermission('form:parser:production-batch-record')`, matching the existing form parser production-batch-record button permission.
- Empty file and non-Word file validation reuse existing batch record upload errors.
- Parsed table count zero fails with `PRO_BATCH_RECORD_REPORT_TABLE_COUNT_INVALID`.
- Serialization failure fails with `PRO_BATCH_RECORD_REPORT_PARSE_FAILED`; no empty `{}` or default success is returned.

## Required Config Services Fixtures Migrations

- Required fixture: `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc`.
- Required expected JSON: `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json`.
- Required service dependency: `MesProBatchRecordDocParser`.
- No database migration, remote service, runtime restart, or configuration change is required.

## BDD Scenarios

- BDD: Production batch record downloads mapping JSON -> Given a user uploads the real IDI production batch record Word / When the form parser production batch record button parses it / Then the downloaded JSON is the batch-record mapping object with `product/schemaVersion/processes`, not Jimu schema JSON.
- BDD: Parse-only endpoint keeps no-write boundary -> Given the parse-only endpoint receives a Word file / When the service parses it / Then it must not write Jimu reports, batch-record versions, DCC project-code total recognition JSON, or device sync state.

## RED Command And Expected Failure

- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, `parseProductionBatchRecordTotalRecognitionJson` did not exist on the controller or service.
- RED: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> FAIL, frontend did not contain `parseProductionBatchRecordTotalRecognitionJson` and still targeted the old Jimu JSON parser path.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, service returned `{}` because Fastjson did not serialize the Java record total-recognition result.

## GREEN Command And Passing Result

- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 4 tests, failures 0, errors 0.
- GREEN: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> PASS, `form parser json download static contract passed`.
- GREEN: `pnpm ts:check` -> PASS, process exit 0.

## Contract Or Integration Verification

- Controller contract test verifies endpoint path, multipart parameter, and `form:parser:production-batch-record` permission.
- Service real-file test verifies the new parse-only service output semantically matches `批记录总对应.json`.
- Extractor real-file test verifies `.doc` parsing output matches the expected total-recognition mapping JSON.
- Static parse-only test verifies the new service method and serializer helper do not call report/version/DCC/Jimu/device-sync write paths.
- BPM regression verifies the earlier `.doc` visual schema fix still passes.

## Observability Touchpoints

- No new logging or metrics were added.
- Existing service exceptions remain explicit and fail fast.
- No raw Word content, source bytes, credentials, or tenant secrets are logged.

## Blockers And Downstream Skill Needs

- No current blocker for code/test verification.
- Real browser E2E and service restart were not run because the current turn did not explicitly authorize E2E or restarting `int_main`.
- Git commit/push was not run because this turn did not explicitly authorize Git operations.
