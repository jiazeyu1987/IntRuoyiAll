# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: Uploading the real legacy `.doc` production batch record through the form parser could fail with `Template schema rows are missing` because the `.doc` branch returned recognized fields without visual schema rows.
- Related regression caught during the output-format fix: the batch-record total-recognition result was a Java record, and Fastjson serialization produced `{}` instead of the expected `product/schemaVersion/processes` JSON.
- Expected behavior: The real `.doc` fixture parses successfully, retains schema rows for the form-center Jimu path, and the production batch record parser button downloads a batch-record total-recognition JSON matching `批记录总对应.json`.

## Reproduction Command Or Path

- User path: Admin opens `表单解析`, clicks `生产批记录`, uploads `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc`, and expects a `.json` download.
- Reproduction command for the original bug: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest#recognizeLegacyDocProductionRecordBuildsVisualSchemaRows' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- Reproduction command for the mapping JSON serialization issue: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`.

## Root Cause

- Legacy `.doc` recognition originally extracted field labels but did not build the Jimu visual schema JSON required by `requireRecognizedVisualSchema`.
- The production batch record business mapping JSON must use the MES batch-record parser and `MesProBatchRecordTotalRecognitionExtractor`; the form-center Jimu schema response is a different contract.
- Fastjson did not serialize the Java record returned by `MesProBatchRecordTotalRecognitionExtractor`, so service-level total-recognition JSON could become `{}` even when extractor data was correct.

## Regression Test Added Or Updated

- Added/updated BPM tests for legacy `.doc` visual schema rows and parse-only service schema output.
- Added MES real-file tests comparing `.doc` output to `批记录总对应.json`.
- Added MES parse-only service test covering the new endpoint service path.
- Added static parse-only contract test to guard against report/version/DCC/Jimu write calls.
- Updated frontend static contract to require the production batch record button to call the total-recognition JSON API and reject Jimu schema fields.

## RED Command And Expected Failure

- RED: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest#recognizeLegacyDocProductionRecordBuildsVisualSchemaRows' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, legacy `.doc` recognition returned no `jimuSchemaJson`.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, production batch record total-recognition parse-only API was missing.
- RED: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> FAIL, frontend still targeted the old Jimu JSON parser.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, service output was `{}` before switching total-recognition serialization to Jackson.

## GREEN Command And Passing Result

- GREEN: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterRuntimeServiceImplParseJsonTest,FormCenterRuntimeImportRecognitionFlowContractTest,FormCenterRuntimeContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 25 tests, failures 0, errors 0.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 4 tests, failures 0, errors 0.
- GREEN: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> PASS, `form parser json download static contract passed`.
- GREEN: `pnpm ts:check` -> PASS, process exit 0.

## Verification

- Verified the real `.doc` fixture no longer fails the BPM visual schema path.
- Verified the real `.doc` fixture parses through the new MES parse-only service to a JSON object semantically equal to `批记录总对应.json`.
- Verified the frontend button no longer uses Jimu schema JSON for the production batch record download.
- Verified parse-only backend code avoids report/version/DCC/Jimu/device-sync write paths.

## Risk And Regression Scope

- Regression scope: form parser production batch record button, BPM legacy `.doc` visual schema recognition, MES batch-record total-recognition JSON extraction, and total-recognition JSON serialization in existing batch-record import flows.
- Risk controlled by real `.doc` fixture semantic comparison to `批记录总对应.json`, parse-only no-write static contract, frontend download contract, and BPM schema rows regression.

## Blockers And Follow-up Actions

- No current blocker for local code/test verification.
- Real browser E2E, backend service restart, and Git commit/push remain not run because they require explicit authorization under project rules.
