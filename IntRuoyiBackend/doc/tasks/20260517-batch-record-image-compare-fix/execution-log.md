BDD: local parser should preserve merged table structure -> Given the source Word table contains merged header or section cells, When the local parser serializes `sheetLayoutJson`, Then the JSON should preserve the corresponding `rowSpan` and `colSpan`.
BDD: local parser output should better match the target rough-wash image -> Given the rough-wash source document is parsed, When the preview renders the local layout JSON, Then major merged regions from the image are preserved instead of being flattened into repeated 1x1 cells.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordWordParserTest,MesProBatchRecordTemplateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL before implementation, the local parser still flattened merged Word cells to `rowSpan=1,colSpan=1`, so the rough-wash structure could not match the target image.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordWordParserTest,MesProBatchRecordTemplateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after local merge-span extraction was added.
