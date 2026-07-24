# Task: Report Management Six-Route Recognition Backend

## Goal

Add six backend recognition routes under the existing batch-record-report
surface so the global report-management page can trigger six isolated
recognition strategies against the fixed pilot `.doc` sample and generate
route-separated report sets with a target count of 15 business templates.

## Scope

- Check the latest backend task and explicitly block it before switching scope.
- Create this task package before production code changes.
- Keep the existing batch-record-report APIs working for the current flows.
- Add route-specific recognition orchestration for routes `A-F`.
- Read the pilot sample from the fixed backend-local path instead of frontend upload.
- Persist route-isolated generated report metadata so different routes never overwrite one another.
- Fail fast when a route prerequisite or confidence gate is not met; no silent fallback.
- Add focused backend tests for each route and for route isolation behavior.

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-pro-workorder-freeze-page-order/task.md`
- Status before this task: blocked due to explicit user reprioritization.
- Impact: the old work-order ordering task remains paused and does not block
  this report-management recognition feature.

## Milestones

- [x] M1: Block the unfinished previous backend task and create this task package.
- [x] M2: Record BDD scenarios and RED verification for the missing six-route backend surface.
- [x] M3: Implement shared route orchestration, route-isolated persistence, and the six route parsers.
- [x] M4: Run focused backend verification, update evidence, and mark the task completed.
- [x] M5: Commit only the backend files produced by this task.
- [x] M6: Harden the live-runtime blockers for route D and route E, then rerun focused verification.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteAParserTest,MesProBatchRecordRouteBParserTest,MesProBatchRecordRouteCParserTest,MesProBatchRecordRouteDParserTest,MesProBatchRecordRouteEParserTest,MesProBatchRecordRouteFParserTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Implementation, focused backend verification, and the runtime-hardening
pass are complete. The `batch-record-report` module now supports:

- `LEGACY` route isolation for the existing upload-based flows
- fixed-sample route dispatch for `A-F`
- route-isolated metadata persistence and page filtering
- six dedicated recognizers:
  - route A direct `.doc`
  - route B Word COM object model
  - route C `.doc -> .docx` normalization
  - route D `doc -> PDF -> PDF text extraction`
  - route E compact template rasterization plus batched image recognizer
  - route F Word-to-Excel intermediate round trip

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteBRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest,MesProBatchRecordRouteFRecognizerTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- authenticated live backend `recognize-fixed?routeKey=D` -> PASS, HTTP `200`, `importedCount=15`, elapsed about `5.5s`
- authenticated live backend `recognize-fixed?routeKey=E` -> PASS, HTTP `200`, `importedCount=15`, elapsed about `934.8s`

## Residual Risk

- Route E is now live-verified, but its end-to-end runtime remains high on this
  workstation at roughly `15.6` minutes for the full fixed sample.
- Impact: the feature is functionally complete, but route E should still be
  treated as a heavy operation for operator expectations and future performance
  tuning.
