BDD: image recognition timeout applies to the full Codex CLI lifecycle -> Given the backend starts a Codex CLI image-recognition process, When the process does not complete within the configured timeout budget, Then the backend should stop the process and return the image-timeout error instead of waiting indefinitely on stdout reads.

BDD: recognition logs identify the slow phase -> Given an image-recognition request is processed, When the backend runs the Codex CLI command, Then logs should record request start, process start, process finish or timeout, stdout size, and structured output summary so operators can locate the bottleneck.

BDD: successful recognition logs the structured summary -> Given Codex CLI returns valid structured JSON, When parsing completes, Then logs should record confidence, issue count, table count, and elapsed milliseconds before returning the result.

RED: existing runtime behavior from the previous image-import task -> FAIL, the request could enter `/admin-api/mes/pro/batch-record-report/import-image` and then stall long enough for the client to time out, proving the old timeout/observability behavior was insufficient.
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.
RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> FAIL, unrelated pre-existing test compile error `MesProBatchRecordReportLayoutCalibratorTest` blocked the standard package path.
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS, rebuilt `yudao-server.jar` after targeted regressions had already passed.
GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS after the root-script fix, backend `48081` and frontend `8081` both returned `200`.
GREEN: direct shell control test with `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` and the same `output-schema` -> PASS in about 245 seconds, proving the image itself is recognizable.
GREEN: authenticated live import with `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` after rebuild and restart -> explicit timeout JSON returned after about 602 seconds instead of a silent hang.
GREEN: backend timeout logs -> PASS, logs now show import start, parser start, child process pid, timeout at about `600038 ms`, and empty stdout preview at timeout, which narrows the remaining issue to the long-running recognition workload.
