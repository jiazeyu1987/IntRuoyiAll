BDD: generic batch-record layout rules -> Given the generic visual rules are being tuned, When the report generator re-renders the affected工序 pages, Then gray/white cell semantics, repeated subheaders, border hierarchy, and page spacing should improve without template-name branching.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> FAIL, `yudao-server.jar` was still locked by the running local backend process during the first attempt.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportStyleEnhancerTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportShapeRulesTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS
GREEN: `GET http://127.0.0.1:48081/v3/api-docs` -> PASS
GREEN: live Route B screenshot capture for `精洗 / 清洗 / 清洁` -> PASS

Summary:
- `StyleEnhancer` now uses row/cell shape signals to shade headers more stably.
- `LayoutCalibrator` now relies on generic row-shape classification for multi-segment process pages, and the dead fixed-row overloads were removed.
- `JsonBuilder` continues to output border hierarchy and `/pcs` emphasis through generic style rules.
- `ShapeRules` now owns shared page width and narrative row-height floors as reusable rules.

Live recheck:
- `精洗` remains the closest of the three pages; the header/gray-row rhythm is more stable and the top metadata rows no longer depend on template-name branches.
- `清洗` benefits the most from the generic multi-segment logic; repeated subheaders and the inserted note scenario now stay on the process-page path instead of collapsing back to a plain table.
- `清洁` keeps the improved width usage and border hierarchy, with a more stable label-gray/value-white distribution than before.
