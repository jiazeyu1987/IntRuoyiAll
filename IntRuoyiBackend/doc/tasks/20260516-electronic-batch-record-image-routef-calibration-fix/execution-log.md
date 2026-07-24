# Execution Log: Electronic Batch Record Image Route F Calibration Fix

BDD: process-page screenshot should retain the left-side section labels as tall merged cells -> Given the "精洗工序生产记录" screenshot contains section labels such as "生产前检查记录" and "精洗生产操作及自检记录" spanning multiple rows, When the backend calibrates the parsed table for report rendering, Then those labels must remain vertically merged instead of collapsing into ordinary one-row cells.

BDD: process-page screenshot should keep the multi-level operation header grid -> Given the screenshot contains a dense operation table with grouped headers and subordinate reference/actual columns, When the calibrator normalizes the process template rows, Then the generated row/col spans must preserve the grouped header structure needed by the report table.

RED: `MesProBatchRecordReportLayoutCalibratorTest.calibrate_shouldKeepProcessTemplateMergedSectionsForScreenshotLayout` -> FAIL expected before the fix, because the current process-template normalization hardcodes a coarse row-span/col-span pattern that does not preserve the screenshot's large merged section blocks.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, focused calibrator verification completed with `3` tests, `0` failures, and `0` errors.
