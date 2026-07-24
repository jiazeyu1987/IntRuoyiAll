# Execution Log: Electronic Batch Record Image Prompt Repair

BDD: screenshot recognition prompt must explicitly target system report tables -> Given the backend sends the electronic batch-record screenshot to Codex CLI, When the prompt is built, Then it must clearly instruct the model to convert the image into a system report table and return only schema-valid JSON.

BDD: prompt contract must preserve structure-first extraction rules -> Given the screenshot contains merged cells and uncertain OCR regions, When the prompt is built, Then it must require table ordering, row/cell ordering, explicit `rowSpan`/`colSpan`, no guessing for uncertain text, and confidence/issues output.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, before this repair there is no focused regression that protects the Codex CLI prompt contract, and the production prompt in `MesProBatchRecordCodexCliImageParser` is garbled.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, the repaired Chinese prompt contract and the existing Codex CLI parser process behavior all passed with `4` tests, `0` failures, and `0` errors.
