# 执行记录：展厅产品 Excel 模板对齐（后端）

## 2026-05-30 继续：统一 `产品名-中文`

BDD: 中文名权威列改名 -> Given workbook 使用 `产品名-中文` / When 导入或导出产品资料 / Then 系统只以 `产品名-中文` 写入和导出中文名，`产品` 仅作为可选冲突校验列。

BDD: 旧中文名表头快速失败 -> Given workbook 仍使用 `产品-中文` / When 导入 / Then 失败并提示必须使用 `产品名-中文`，避免静默清空中文名。

BDD: 当前参考 workbook 契约 -> Given 参考 workbook 为 Sheet `产品列表` / When 下载模板或导出 / Then 表头为当前 15 列，且不再导出旧 `产品` 列。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 `产品名-中文` 契约和旧 `产品-中文` 拒绝用例后，旧实现仍导出旧表头且不拒绝旧表头。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，后端 jar 重建成功并以 `http://127.0.0.1:18083` 启动。

GREEN: 使用仅将 B1 改为 `产品名-中文` 的验收副本运行 `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入导出比对 `160/160` 行，文本差异 `0`，产品图数量按导入后的当前封面状态严格一致。

BLOCKED: 使用正式文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 运行同一 E2E -> FAIL，导入响应 `SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 中文名权威列必须使用 \`产品名-中文\`，不能继续使用 \`产品-中文\``；当前磁盘文件仍为旧 `产品-中文` 表头，正式验收需先保存为新表头。

BLOCKED: 重新检查正式文件 -> Sheet `产品列表` 仍为 `160` 行、`71` 张图片，表头第 2 列仍是 `产品-中文`；目录中存在 `~$产品资料修改版-补充产品资料.xlsx` 锁文件。同一正式 E2E 再次失败于 `SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID`，需要先关闭/保存 Excel，让 B1 实际落盘为 `产品名-中文`。

BLOCKED: 第三次恢复检查正式文件 -> Sheet `产品列表` 仍为 `160` 行、`71` 张图片，表头第 2 列仍是 `产品-中文`；锁文件 `~$产品资料修改版-补充产品资料.xlsx` 仍存在。当前无法完成“正式参考 Excel 导入 -> 导出 -> 160 行严格比对”，需外部保存正式 workbook 为 `产品名-中文` 后恢复。

GREEN: 正式文件解锁后，将 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 的 B1 从 `产品-中文` 修正为 `产品名-中文`；校验 Sheet `产品列表` 仍为 `160` 行、`15` 列、`71` 张图片。

GREEN: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入正式参考文件后导出比对 `160/160` 行，表头一致，产品编码顺序一致，文本字段差异 `0`，导出图片行与导入后当前封面状态 `160/160` 一致。

BDD: 产品编码识别同一产品 -> Given 导入行包含 `展品编码` / When 系统导入 / Then 按产品编码匹配已有产品并更新当前版本字段。

BDD: 中文名权威列 -> Given workbook 使用 `产品-中文` / When 导入 / Then 只以 `产品-中文` 写入中文名；旧 `产品` 列冲突时按行失败。

BDD: 卖点文案保留换行 -> Given `卖点文案` 包含多行文本 / When 导入再导出 / Then 文本换行保持，导出单元格启用自动换行。

BDD: 产品图覆盖封面并回嵌导出 -> Given `产品图` 列存在嵌入图片 / When 导入产品资料 / Then 图片替换 `cover_image`；无图片时保留当前封面；导出时内部或外部封面均嵌回 `产品图`。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现仍为旧表头，`产品-中文` 未作为权威列，`卖点文案` 换行与产品图回嵌未满足。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`。

RED: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> FAIL，测试租户真实导入后前端产品列表因 `产品-中文` 为空的行抛出 `nameCn` 不能为空，页面 loading 遮罩未退出。

GREEN: 前端列表允许空中文名后，真实 E2E 继续执行到导出。

RED: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> FAIL，导出响应为 JSON 错误，后端快速失败于外部封面 URL：`product_077` 的 `https://int-medical.com/...png` 不支持。

GREEN: 导出器支持 http(s) 外部封面后，`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，后端 jar 重建成功并已重启本 worktree 后端。

GREEN: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入 `160` 行、失败 `0`；导出后比对 `160/160` 行，15 列表头一致，产品编码顺序一致，文本字段差异 `0`，参考 71 行产品图全部保留，89 行为空图行保留已有封面。
