BDD: print-view fidelity follow-up -> Given the batch-record reports are already structurally closer to the source images, When we separately improve preview purity, placeholder density, dense right-column width, and paper-like line/gray styling, Then the latest live Route B screenshots should move closer to the paper source without introducing template-name-specific branches.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `MesProBatchRecordReportLayoutCalibratorTest.calibrate_shouldReserveMoreWidthForDenseTailColumns` first exposed that `/pcs` quantity columns still were not consistently widened enough.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after tightening the semantic dense-tail width-floor rule.
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordJimuReportGatewayImplTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS
GREEN: `GET http://127.0.0.1:48082/v3/api-docs` -> PASS
GREEN: `POST http://127.0.0.1:48082/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` -> PASS
GREEN: `GET http://127.0.0.1:48082/admin-api/mes/pro/batch-record-report/designer-path?...` -> PASS, returned `/jmreport/view/{reportId}?tenantId=1`
GREEN: `GET http://127.0.0.1:48082/jmreport/show?...` -> PASS, report JSON confirmed `rpbar.show = false` and `fillFormToolbar.show = false`

Review summary:
- Worker 1 changed the backend preview path generation from `/jmreport/index/...` to `/jmreport/view/...`.
- Worker 2 removed visible `请填写` body text while keeping placeholder hints in `fillForm.placeholder`.
- Worker 3 widened dense right-tail columns through semantic width floors and a larger dense-page budget.
- Worker 4 split gray fills into stronger section bands, stronger group headers, and lighter label cells while preserving heavier outer borders.

Live evidence:
- Direct isolated `/jmreport/view/...` screenshots show the designer sidebars are gone and blank fillable cells no longer paint visible `请填写` text throughout the page.
- The remaining top toolbar (`首页 / 上一页 / 打印 / 导出`) belongs to JMReport’s built-in viewer itself, not the report JSON toolbar flags.
- This means the final blocker is narrowed to viewer-level chrome suppression or frontend-side same-origin wrapper/cropping rather than report-generation logic.
