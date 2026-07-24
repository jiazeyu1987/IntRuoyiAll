BDD: local template parser should emit compact single-page constraints -> Given a Word table is recognized into local `sheetLayoutJson`, When the parser serializes the layout, Then the JSON carries constrained row height, effective width, and font size metadata for compact browser rendering.
BDD: local template parser should mark blank cells with a visible placeholder -> Given a recognized local template cell is empty, When the parser serializes `sheetLayoutJson`, Then the cell remains text-empty but carries visible placeholder metadata indicating the field is fillable.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordWordParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL before implementation, the parser test did not yet require `displayConstraints` or blank-cell placeholder metadata in `sheetLayoutJson`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProBatchRecordWordParserTest,MesProBatchRecordTemplateServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after adding `MesProBatchRecordTemplateLayoutRules`, extending local `sheetLayoutJson`, and keeping parse/commit service behavior green.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, rebuilt the backend jar consumed by the live electronic batch record import page.
