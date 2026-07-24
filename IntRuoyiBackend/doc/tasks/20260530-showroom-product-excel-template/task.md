# 任务：展厅产品 Excel 模板对齐（后端）

## 任务目标

让展厅产品管理的导入模板、导入解析、导出 Excel 与参考文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 一致；以测试租户真实前端路径完成导入、导出和比对，明确覆盖规则与图片规则。

## 里程碑

- [x] M1：建立任务文档并确认前置任务完成。
- [x] M2：解析参考 workbook 的当前 15 列结构。
- [x] M3：补后端 RED 测试，锁定表头、中文名权威列、换行和产品图规则。
- [x] M4：实现导入模板、导入解析、导出回嵌与发布契约调整。
- [x] M5：补真实数据暴露出的空中文名、外部封面和前端列表兼容问题。
- [x] M6：运行后端回归、重建后端 jar，并用测试租户真实导入导出比对。
- [x] M7：按最新要求将中文名权威列表头从 `产品-中文` 统一为 `产品名-中文`，并快速失败旧表头。
- [x] M8：等待正式参考文件实际保存为 `产品名-中文` 后，重新运行测试租户真实导入、导出和严格比对。

## BDD 场景

- BDD: 当前参考 workbook 契约 -> Given 参考 workbook 为 Sheet `产品列表` / When 下载模板或导出 / Then 表头为 `展品编码 | 产品名-中文 | 产品名-英文 | 展柜名称 | 持证公司 | 在售/在研 | BU | 在售国家 | 适应症 | 型号规格 | 注册证信息 | 卖点文案 | 产品图 | 奖项 | 原材料表单`。
- BDD: 产品编码识别同一产品 -> Given 导入行包含 `展品编码` / When 系统导入 / Then 按产品编码匹配已有产品并更新当前版本字段。
- BDD: 中文名权威列 -> Given workbook 使用 `产品名-中文` / When 导入 / Then 只以 `产品名-中文` 写入中文名；如果补充 `产品` 列存在且冲突，则按行失败。
- BDD: 旧中文名表头快速失败 -> Given workbook 仍使用 `产品-中文` / When 导入 / Then 拒绝导入并提示必须使用 `产品名-中文`，避免静默清空中文名。
- BDD: 空中文名兼容 -> Given 参考 workbook 中部分 `产品名-中文` 为空 / When 导入并发布 / Then 保留空中文名，产品完整性仍可标记不完整，但只要英文名存在不阻塞发布。
- BDD: 卖点文案保留换行 -> Given `卖点文案` 包含多行文本 / When 导入再导出 / Then 文本换行保持，导出单元格启用自动换行。
- BDD: 产品图覆盖封面 -> Given `产品图` 列存在嵌入图片 / When 导入 / Then 图片替换 `cover_image`，即使只有图片变化也算变更；无图片时保留原封面。
- BDD: 产品图导出回嵌 -> Given 产品当前有内部或外部封面 URL / When 导出 / Then 将当前封面嵌入 `产品图`；缺失、空内容、非图片或非 http(s)/内部文件地址快速失败。

## 完成工作

- `ShowroomProductExcelVO` 调整为当前 15 列，移除旧 `产品` 导出列，`产品名-中文` 成为中文名唯一权威列。
- 导入按 `展品编码` 匹配产品；保留 `卖点文案` 换行；支持嵌入图片导入并替换封面；图片变化纳入发布字段。
- 允许参考 workbook 中空 `产品名-中文` 导入；发布必填调整为英文名，中文名仍参与完整性提示。
- 导入前严格校验表头：缺少 `产品名-中文` 或继续使用旧 `产品-中文` 时快速失败；可选 `产品` 列只用于与 `产品名-中文` 做冲突校验。
- 新增导出器，导出时自动换行 `卖点文案`，并把内部文件和外部 http(s) 封面嵌回 `产品图`。
- 导出仍只导出已上展柜产品，写入真实展柜名称，保证导出 workbook 可按导入契约回读。

## 验证结果

- RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现仍是旧表头、旧中文名列、无图片回嵌。
- RED: 将测试契约改为 `产品名-中文` 并新增旧表头拒绝用例后，`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现仍导出 `产品-中文` 且未拒绝旧表头。
- GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，并已重启当前 worktree 后端 `http://127.0.0.1:18083`。
- GREEN: 使用仅将 B1 改为 `产品名-中文` 的验收副本运行 `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入导出比对 `160/160` 行，文本差异 `0`，图片行按导入后的当前封面状态严格一致。
- BLOCKED: 使用正式文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 运行同一脚本 -> FAIL，导入响应 `SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 中文名权威列必须使用 \`产品名-中文\`，不能继续使用 \`产品-中文\``；当前磁盘文件 B1 仍为 `产品-中文`，正式验收需先保存为新表头。
- GREEN: 正式文件解锁后，将 B1 从 `产品-中文` 修正为 `产品名-中文`，校验仍为 `160` 行、`15` 列、`71` 张图片；运行 `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入正式文件并导出比对 `160/160` 行，表头一致、产品编码顺序一致、文本差异 `0`、图片行按当前封面状态严格一致。

## 当前状态

Completed: 后端契约、单测、回归构建和正式参考 workbook 测试租户导入导出严格比对均已通过。
