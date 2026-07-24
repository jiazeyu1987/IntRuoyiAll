# 执行日志：展厅产品导入空单元格保留当前值（后端）

BDD: 空单元格保留产品字段 -> Given Excel 行只填写 `展品编码` 且其它可导入字段为空 / When 导入产品 / Then 当前产品中英文名、生命周期、BU、在售国家、适应症、型号规格、注册证信息、卖点文案和封面均保持不变，且无变化时跳过发布。

BDD: 局部空单元格保留当前值 -> Given Excel 行仅填写部分字段 / When 导入产品 / Then 非空字段覆盖当前值，空字段保留当前值，并只在确有变化时发布新版本。

BDD: 空展柜名称保留映射 -> Given 产品已有展柜映射且 Excel `展柜名称` 为空 / When 导入产品 / Then 不移动、不删除该产品现有展柜映射。

BDD: 非空非法值仍失败可见 -> Given Excel `持证公司` 与当前所属公司不一致或 `在售/在研` 为未知值 / When 导入产品 / Then 按行失败并返回清晰原因，不静默跳过或降级。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现清空空 `产品名-中文`，空 `产品名-英文` 和空 `展柜名称` 按行失败，空 `在售国家` 未保留当前在售国家。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，生成当前源码后端运行 jar。

GREEN: Playwright real frontend E2E at `http://127.0.0.1:8081/showroom/product` with test tenant `测试租户/aoteman` -> PASS，导入只填写 `展品编码=product_001` 的 xlsx，请求命中当前源码后端 `48082`，结果 `跳过无变化：1`，中文名、版本号和封面保持不变。

GREEN: `task_closeout.py --task-id 20260531-showroom-product-import-blank-keep-current --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json` -> PASS，`delete=[]`，`blocked=[]`。
