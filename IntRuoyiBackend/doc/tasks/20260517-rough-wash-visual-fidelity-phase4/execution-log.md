# Execution Log: 粗洗生产批量汇总行左侧留白修正

BDD: 生产批量汇总行必须避开左侧竖排分区栏 -> Given 粗洗工序生产记录已经进入 `生产批量汇总` 行 When 固定布局生成该行单元格 Then 第一个单元格必须是 1 列宽的视觉空白格，第二个单元格才是灰底居中的 `生产批量汇总`，右侧继续保留多个白底填写格。

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldKeepProductionBatchSummaryBehindRoughWashSideColumn -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected the rough-wash production batch summary row to have the left blank side-column cell, but the old layout started the gray summary title from the far-left edge.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldKeepProductionBatchSummaryBehindRoughWashSideColumn -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 1 test, 0 failures.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 24 tests, 0 failures.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS.

GREEN: backend runtime restarted on `http://127.0.0.1:48081`; `GET /v3/api-docs` -> HTTP 200.

GREEN: `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` -> PASS, `importedCount=15`, `createdCount=0`, `updatedCount=15`, `reportId=1b1185fc32694fe1b24d2e83fdffddf5`.

GREEN: Playwright captured the live rough-wash report screenshot -> `doc/tasks/20260517-rough-wash-visual-fidelity-phase4/artifacts/rough-wash-batch-summary-left-gap-20260517-1205.png`.

BLOCKER: `verify_tdd_compliance.py` gate script is not present under either `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` or `D:\ProjectPackage\RagInt`, so the repository-specific TDD admission command from the local instructions could not be executed in this environment.
