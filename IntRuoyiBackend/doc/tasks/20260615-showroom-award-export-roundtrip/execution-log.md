# Execution Log

BDD: 导出包含奖项页签 -> Given 当前租户存在产品和奖项 / When 用户在产品管理点击导出 / Then 下载的 workbook 包含 `产品列表` 与 `奖项` Sheet。

BDD: 导出奖项可回导 -> Given 奖项具备序号、中文名、日期/期限、颁发单位和封面 / When 导出 workbook 后重新导入 / Then 后端能读取奖项页签并生成相同 `AWARD-xxx` 编码。

BDD: 奖项封面缺失导出失败 -> Given 奖项缺少封面或封面文件不可读 / When 用户导出产品资料 / Then 导出失败并提示具体奖项，不生成不可回导文件。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> FAIL, 新增实现初版调用了不存在的 `normalizeText`。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> FAIL, 产品封面缺失用例需要补充有效奖项前置，奖项缺封面用例应使用草稿而非发布态。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS, 32 tests, 导出双 Sheet、奖项 E 列嵌图、模板奖项示例、导出后奖项解析回导和 fail fast 场景通过。
