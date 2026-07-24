# Execution Log: Report Management Six-Route Recognition Backend

BDD: each backend recognition route generates 15 business-template reports from the fixed pilot sample -> Given the backend can access `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`, When route `A-F` is triggered, Then the route should either generate 15 route-isolated reports or fail fast with an explicit prerequisite/error message.

BDD: route-specific results do not overwrite each other -> Given the same pilot sample is recognized by multiple routes, When metadata and generated reports are persisted, Then each route must keep its own report namespace and stored rows without overwriting other routes' output.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordReportControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `MesProBatchRecordReportController` does not yet expose `recognizeFixedRoute(String routeKey)` or the new fixed-route recognition contract.

BDD: route F uses a Word -> Excel intermediate boundary before table recognition -> Given the fixed pilot `.doc` sample exists, When route `F` is triggered, Then the route should materialize an Excel intermediate, re-read it into `MesProBatchRecordParsedTable` rows, and preserve the pilot sample's 15 business templates.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordRouteFRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, shared unfinished Route C sources in `MesProBatchRecordRouteCRecognizer.java` and `MesProBatchRecordRouteCRecognizerTest.java` still block the module-wide Maven test compile.

GREEN: isolated `javac` + manual runner for `MesProBatchRecordRouteFRecognizerTest` -> PASS, the route-F recognizer round-trips the pilot sample through an Excel intermediate and the failure path rejects an empty Excel intermediate with `PRO_BATCH_RECORD_REPORT_PARSE_FAILED`.
BDD: route D recognizes the pilot sample through a PDF-oriented reflow path -> Given route D converts the legacy `.doc` pilot sample into PDF and reopens the generated PDF as a reflowed `.docx`, When the PDF-oriented tables are normalized into `MesProBatchRecordParsedTable` rows, Then the recognizer should emit 15 business templates or fail fast with an explicit route-D prerequisite/output error.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteDRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the first route-D test attempt was blocked by an unrelated in-flight compile error in `MesProBatchRecordRouteFRecognizer` before the new recognizer could run.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\pom.xml clean test-compile -Dmaven.compiler.includes=**/MesProBatchRecordRouteDRecognizer.java,**/MesProBatchRecordParsedCell.java,**/MesProBatchRecordParsedTable.java,**/MesProBatchRecordWordParser.java,**/MesProBatchRecordParseRow.java,**/MesProBatchRecordParseCell.java -Dmaven.compiler.testIncludes=**/MesProBatchRecordRouteDRecognizerTest.java` -> FAIL, the module `testCompile` path remains blocked by unrelated missing Route B/E recognizer classes and missing imports in `MesProBatchRecordReportServiceImplDbTest`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\pom.xml compile -Dmaven.compiler.includes=**/MesProBatchRecordRouteDRecognizer.java,**/MesProBatchRecordParsedCell.java,**/MesProBatchRecordParsedTable.java,**/MesProBatchRecordWordParser.java,**/MesProBatchRecordParseRow.java,**/MesProBatchRecordParseCell.java` -> PASS, the MES module main sources compiled successfully with the new route-D recognizer in place.

GREEN: `manual isolated compile + RouteDTestRunner` -> PASS, `recognize_returnsFifteenTemplatesFromPdfReflowDocx` and `recognize_failsFastWhenReflowDocxIsMissing` both passed against the real `MesProBatchRecordRouteDRecognizerTest` class.

BDD: route C normalizes the fixed pilot `.doc` into a structured `.docx` representation before parsing -> Given route `C` receives the fixed pilot sample bytes, When the route-specific normalizer produces a valid normalized document, Then the recognizer must parse and split it into exactly 15 business-template tables; if the normalized output is missing or invalid, the route must fail fast with an explicit parse error.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteCRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, test compilation reported missing `MesProBatchRecordRouteCRecognizer`; the same compile pass also surfaced a parallel-worker blocker because `MesProBatchRecordRouteFRecognizerTest` references a still-missing `MesProBatchRecordRouteFRecognizer`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS, route-C recognizer source compiles in the MES module after adding the normalization-plus-docx parsing implementation.

GREEN: focused route-C compile/run via `javac` argfiles + `RouteCTestMethodRunner` -> PASS, `recognize_normalizedDocx_returnsFifteenBusinessTemplates`, `recognize_whenNormalizationOutputMissing_failFast`, and `recognize_whenNormalizationOutputIsInvalidDocx_failFast` all passed against the compiled route-C recognizer and test class.

BDD: route E rasterizes the fixed pilot `.doc` into template PNGs and re-parses them through the Codex image recognizer -> Given route `E` can first split the pilot sample into 15 business templates, When each template is rendered into a standalone PNG and sent through the image parser, Then the route must return 15 ordered `MesProBatchRecordParsedTable` results or fail fast as soon as any rendered image yields zero or multiple tables.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, test compilation initially reported missing `MesProBatchRecordRouteERecognizer`; after the class was added, the same command failed with `route_e_grid_overflow_template_2_row_8`, proving the first fixed-width raster layout could not represent the real merged-cell structure from the pilot sample.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS, the route-E production class compiled cleanly inside `yudao-module-mes`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `MesProBatchRecordRouteERecognizerTest` verified three route-E cases: the fixed pilot sample is split into 15 templates and rasterized into PNG payloads, multi-table image-parser output fails fast, and empty Word bytes fail fast.

BDD: route B recognizes the fixed pilot sample through the Word COM object model -> Given the backend host exposes the fixed pilot `.doc` sample plus local `python` and `win32com.client`, When route `B` invokes the Word COM recognizer, Then it returns `15` `MesProBatchRecordParsedTable` templates with stable titles/row counts and fails fast if the COM/python prerequisite is missing.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteBRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `MesProBatchRecordRouteBRecognizer` did not exist yet; the same module-wide test compile also surfaced pre-existing unrelated blockers in `MesProBatchRecordReportServiceImplDbTest` and unfinished route-E test wiring.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS, the route-B production class compiles cleanly in `yudao-module-mes`.

GREEN: Java source probe against `MesProBatchRecordRouteBRecognizer` -> PASS, the real pilot sample returned `COUNT=15`, `FIRST=产品信息`, `LAST=大包装工序生产记录`, `ROWS=46,19,19,37,21,17,17,19,18,19,19,19,23,17,17`.

GREEN: Java source probe with missing python command -> PASS, route `B` failed fast with code `1040509004` and message fragment `route_b_python_process_start_failed`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteBRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `MesProBatchRecordRouteBRecognizerTest` completed with `2` tests, `0` failures, and `0` errors.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS, the shared controller/service wiring plus all six route recognizers compile together in the MES module.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, the fixed-route controller contract and route-isolated metadata/page filtering both passed.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteARecognizerTest,MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteCRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest,MesProBatchRecordRouteFRecognizerTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, the complete six-route recognizer bundle plus shared controller/service regression package finished with `26` tests, `0` failures, and `0` errors.

NOTE: the first real-page route-A smoke still failed even after backend code/tests were green because the local MySQL runtime table `mes_pro_batch_record_report` had not yet been upgraded with `route_key`. After applying the local schema upgrade and rebuilding/restarting the local server jar, the browser-side `recognize-fixed?routeKey=A` request returned HTTP `200` and the frontend rendered the route-A rows.

GREEN: authenticated live backend fetches from the browser context -> PASS for routes B, C, and F, each returning HTTP 200 with importedCount=15 against the rebuilt local runtime.

NOTE: route D remains a live-runtime blocker despite passing its focused backend tests; the real endpoint returns business code 500 with oute_d_pdf_reflow_converter_failed ... 'NoneType' object has no attribute 'SaveAs2', so the Word COM PDF reflow path is not stable on this workstation.

NOTE: route E remains a live-runtime blocker; backend logs show the first Codex CLI image-recognition subprocess timing out and the request terminates with Codex CLI ͼƬʶ��ʱ.

BDD: route D should complete from the exported PDF without reopening that PDF inside Word/WPS -> Given the backend can still export the fixed `.doc` sample to PDF, When route D parses the generated PDF text directly instead of saving a reflow `.docx`, Then it should still emit 15 business-template reports and avoid the workstation-specific `SaveAs2` failure.

BDD: route E should batch compact template images so the image recognizer is invoked a small number of times -> Given route E already has 15 source templates, When it renders compact summary images and stitches them into recognition batches, Then the image parser should be called in a few batched requests and still return 15 ordered reports.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, route D still required `route_d_reflow_docx_missing` even when a PDF already existed, and route E still rendered an oversized summary image with width `920`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, route D now parses exported PDF text directly and route E now renders compact summary images within the bounded dimensions.

NOTE: after the compact-image change, route E no longer stalled on the first template. Live logs showed it advancing through template 1 to template 4 with rendered image sizes around `13 KB` to `25 KB`, but the one-template-per-call design was still too slow for end-to-end completion.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the new batched route-E test expected 3 image-parser calls for 15 templates, but production route E still invoked the parser once per template and failed the batch contract.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, route E now stitches 5 compact templates per batch image, validates per-batch table counts, and still preserves 15 ordered template results.

GREEN: authenticated live backend `POST /admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=D` -> PASS, HTTP `200`, `importedCount=15`, elapsed about `5719 ms` before the final server rebuild and `5535 ms` after the final server rebuild.

GREEN: authenticated live backend `POST /admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=E` -> PASS, HTTP `200`, `importedCount=15`, elapsed about `934753 ms` after switching route E to compact batched image recognition.
