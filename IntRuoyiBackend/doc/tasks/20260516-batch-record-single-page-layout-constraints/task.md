# Task: Batch Record Single-Page Layout Constraints

## Goal

Constrain the currently generated electronic batch-record reports so each
recognized form fits within a single browser page view, while preserving the
ability to fill the report. The generated layout must therefore:

- cap effective row width and column width
- cap row height
- cap font size
- remove oversized footer padding
- turn recognized empty cells into visible fillable inputs with placeholders

## Scope

- Apply the layout constraint in the shared report-generation chain so it
  affects all currently generated routes.
- Keep the report editable in JimuReport after generation.
- Do not change unrelated repositories or unrelated dirty files.
- Do not implement filled-value persistence in this task.

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-six-route-report-doc-consistency-review/task.md`
- Status before this task: blocked by Route `E` external Codex image-recognition
  instability after the shared runtime, preview, and screenshot chain were repaired.
- Impact: the shared report-generation chain was already stable enough for
  routes `A/B/C/D/F`, so this task could focus on the shared layout rules.

## Milestones

- [x] M1: Create this task package and record the previous-task blocker status.
- [x] M2: Add RED tests for single-page constraints and fillable placeholders.
- [x] M3: Implement shared layout and placeholder constraints in the batch-record report pipeline.
- [x] M4: Run focused verification and live screenshot/report validation.
- [x] M5: Update task evidence and leave the repository ready for task-scoped commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- Real regeneration of the target screenshot file and browser validation of the final Jimu view page

## Current Status

Completed. The shared batch-record report pipeline now enforces compact
single-page dimensions and converts recognized empty cells into fillable Jimu
inputs with the placeholder `请填写`.

## Final Verification Result

- Focused backend tests -> PASS, `19` tests passed across JSON builder,
  layout calibrator, Jimu gateway, and style enhancer coverage.
- Server packaging -> PASS, `mvn ... -Dmaven.test.skip=true package` produced
  an updated `yudao-server.jar` despite unrelated DCC test-compile failures in
  full-package mode.
- Runtime restart -> PASS, backend switched to
  `output/runtime/backend-single-page-20260516-221618.jar`, and
  `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.
- Real screenshot regeneration -> PASS, with the browser session’s real auth
  token, `POST /admin-api/mes/pro/batch-record-report/import-image` returned
  `{"createdCount":1,"reportId":"3913be917ee8459fa169a03c1b61a789" ... }`.
- Real Jimu view validation -> PASS, the final page
  `/jmreport/view/3913be917ee8459fa169a03c1b61a789?...` showed no horizontal or
  vertical overflow at the captured viewport, rendered `156` fillable textboxes,
  and allowed at least three `请填写` inputs to be edited live.
