# Execution Log: 粗洗页眉比例贴近原 DOC

BDD: 粗洗页眉左侧标题区应约占半张表宽 -> Given 粗洗工序生产记录使用 20 列固定布局 When 生成文档页眉第一行 Then `球囊扩张压力泵生产记录` 应占 10 列，`记录编号` 占 4 列，`RE-PP-ID-01` 占 6 列，使页眉比例接近原 DOC 的左大右小结构。

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldUseDocLikeHeaderProportionsForRoughWash -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected rough-wash document header left title cell to span 10 columns, but the older layout used 9 columns.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldUseDocLikeHeaderProportionsForRoughWash -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 1 test, 0 failures.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 25 tests, 0 failures.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS.

GREEN: runtime probe -> PASS, `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.

GREEN: real rough-wash regeneration -> PASS, `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` returned `importedCount=15`, `updatedCount=15`, and preserved `reportId=1b1185fc32694fe1b24d2e83fdffddf5`.

GREEN: Playwright captured the live rough-wash report screenshot -> `doc/tasks/20260517-rough-wash-visual-fidelity-phase5/artifacts/rough-wash-title-centered-20260517-1240.png`.

BLOCKER: `verify_tdd_compliance.py` gate script is not present under either `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` or `D:\ProjectPackage\RagInt`, so the repository-specific TDD admission command from the local instructions could not be executed in this environment.
