BDD: legacy .doc parsing uses logical table conversion -> Given the pilot pressure-pump `.doc` file is uploaded, When the parser handles a legacy `.doc`, Then it converts the file into a docx-style table model before splitting candidate templates.

BDD: pilot .doc expands into per-operation template candidates -> Given the pilot pressure-pump `.doc` contains product information and multiple operation records, When the parser returns template candidates, Then the result covers each logical operation table instead of collapsing them into only a few coarse candidates.

BDD: docx parsing remains stable -> Given a normal `.docx` input with multiple top-level tables, When the parser runs, Then it still returns each top-level table in order.

RED: `mvn -f D:\wt\rbt-be-clean\yudao-module-mes\pom.xml "-Dtest=MesProBatchRecordWordParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the parser constructor did not yet accept an injected legacy `.doc` converter, so the regression test for 10 logical tables could not compile.

GREEN: `mvn -f D:\wt\rbt-be-clean\yudao-module-mes\pom.xml "-Dtest=MesProBatchRecordWordParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: direct HTTP verification -> PASS, posting the real pilot `.doc` to `/admin-api/mes/pro/batch-record-template/import/parse` returned `tableCount = 10` and the expected per-operation candidate titles.
