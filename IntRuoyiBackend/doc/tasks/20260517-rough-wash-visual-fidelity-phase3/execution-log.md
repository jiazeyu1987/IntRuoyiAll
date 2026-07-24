# Execution Log: 粗洗工序合并标题区视觉修正

## BDD

BDD: 合并后的粗洗工序标题区必须保持报表表头居中 -> Given Route B 粗洗模板的标题与 `关键/特殊工序` 勾选说明被合并到同一个多行表头单元格, When 报表 JSON 生成器为该单元格生成样式, Then 即使文本包含换行且长度较长, 显式 `horizontalAlign=center` 也必须保留为水平居中, `verticalAlign=middle` 必须保留为垂直居中。

BDD: 默认左对齐单元格不得被本次修正误改 -> Given 普通解析单元格没有显式声明 `center/right`, When 文本不满足自动居中规则或已有用例依赖默认左对齐, Then 本次修正不得把默认 `left` 当成强制样式覆盖既有空白格、短文本和叙述文本规则。

## RED

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldRespectExplicitCenterAlignForMultilineProcessHeader -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected `center` but generated style was `left` at `MesProBatchRecordReportJsonBuilderTest.java:197`.

## GREEN

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest#build_shouldRespectExplicitCenterAlignForMultilineProcessHeader -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 1 test, 0 failures.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 23 tests, 0 failures.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS.

GREEN: backend runtime restarted on `http://127.0.0.1:48081`; `GET /v3/api-docs` -> HTTP 200.

GREEN: `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` -> PASS, `importedCount=15`, `createdCount=0`, `updatedCount=15`, `reportId=1b1185fc32694fe1b24d2e83fdffddf5`, `reportName=电子批记录[B]-表2-粗洗工序生产记录`.

GREEN: Playwright captured the live rough-wash report screenshot -> `doc/tasks/20260517-rough-wash-visual-fidelity-phase3/artifacts/rough-wash-title-centered-20260517-1138.png`.

## Implementation Notes

- `MesProBatchRecordReportShapeRules.resolveHorizontalAlign` now honors explicit `center/right` before applying narrative long-text left alignment.
- The rule intentionally does not treat default `left` as an explicit override, because parsed cells default to `left`; honoring that default globally would break existing short-cell and blank-cell alignment behavior.
- The new regression test locks the exact merged rough-wash title/checklist case that visually regressed.

## Closeout

- task-closeout-cleanup preview: first preview marked the screenshot artifact for deletion under the default rules, so the screenshot was explicitly added to `Cleanup Keep`.
- task-closeout-cleanup preview: second preview completed with the task markdown, execution log, and screenshot artifact all in `keep`; `delete=<none>`, `blocked=<none>`, `warnings=<none>`.
