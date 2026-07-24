BDD: generated batch-record reports should fit on one browser page -> Given a recognized batch-record table is converted into a JimuReport sheet, When the shared layout pipeline calibrates and serializes the report, Then the final report width, row heights, and font sizes should be constrained so the report can render within a single browser page view without the legacy oversized blank padding.

BDD: recognized empty cells should show a visible text placeholder -> Given a recognized cell is empty and is intended to be filled by an operator, When the report JSON is built, Then the generated cell should contain a visible text placeholder and an editable Jimu fill-form config instead of an invisible blank.

BDD: generated reports remain visible in the Jimu category after the fillable conversion -> Given the report gateway persists a generated report into `jimu_report`, When the report is opened from the Jimu `电子批记录` category, Then the category query should still return the report and the view page should expose editable controls.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the pre-fix builder still exposed the old `build(table)` signature, kept `submitForm=0`, emitted plain blanks instead of `fillForm`, and did not yet guarantee the new single-page budgets.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, rebuilt the runnable backend jar while explicitly bypassing unrelated DCC test-compile failures outside this task’s scope.

GREEN: backend runtime restart -> PASS, switched the live backend to `output/runtime/backend-single-page-20260516-221618.jar`, and `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.

GREEN: real screenshot regeneration using the browser session token -> PASS, `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/import-image` returned HTTP `200` with `createdCount=1`, `reportId=3913be917ee8459fa169a03c1b61a789`, and `reportCode=EBR_IMG_c48cdb7020e2_T01`.

GREEN: real Jimu category and view-page validation -> PASS, `http://127.0.0.1:8081/report/jimu-report` showed the new card under the Jimu `电子批记录` category, and the direct view page for report `3913be917ee8459fa169a03c1b61a789` showed no viewport overflow while exposing live-editable `请填写` textboxes.
