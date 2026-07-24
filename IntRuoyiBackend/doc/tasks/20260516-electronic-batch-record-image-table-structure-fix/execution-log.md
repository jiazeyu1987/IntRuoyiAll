# Execution Log: Electronic Batch Record Image Table Structure Fix

BDD: product information screenshot should remain one large structured table -> Given the provided electronic batch-record screenshot contains one large product-information region with obvious merged cells and section headers, When Route E renders and re-recognizes that region, Then the structured output should preserve the main row ordering and merged-cell boundaries instead of fragmenting it into flat single-cell rows.

BDD: compact rendered images must still preserve readable grid geometry -> Given Route E converts source tables into PNGs for image recognition, When a rendered table contains wide merged sections and tall multiline cells, Then the PNG layout should keep enough width and vertical spacing for the recognizer to recover the original grid.

RED: `MesProBatchRecordRouteERecognizerTest.recognizePilotSample_batchesFifteenTemplatesThroughImageParser` currently encodes one-template-per-call behavior -> FAIL for the intended structure-focused batching contract, because the current test fixture still accepts `15` image-parser calls and does not protect a more faithful batch layout.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteERecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, Route E now batches the 15 source templates into `3` image-parser calls, and the focused recognizer regression suite completed with `5` tests, `0` failures, and `0` errors.
