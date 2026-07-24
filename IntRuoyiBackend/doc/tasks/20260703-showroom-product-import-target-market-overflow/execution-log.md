# 执行日志：修复展厅产品资源包导入 target_market 超长系统异常

- BDD: 长在售国家列表可导入 -> Given 产品资源包中某产品 `target_market` 超过 255 字符 / When 导入产品资源包 / Then 不应触发数据库截断导致系统异常，字段应按正式 schema 持久化。
- BDD: 导入异常可定位 -> Given 导入过程中出现数据问题 / When 后端处理导入 / Then 不应因单行数据库异常把事务标记为 rollback-only 后最终只返回系统异常。
- GREEN: experience-preflight -> PASS，已读取 PowerShell、经验索引、测试服访问、登录访问与缺陷/后端/数据库交付技能。
- RED: test-server-log-probe -> FAIL，测试服 2026-07-03 09:07 `/admin-api/showroom/product/import-excel?sameProductAction=SKIP` 在 rowNo=3/4 写入 `showroom_product_revision.target_market` 时报 `Data too long for column 'target_market'`，最终 `UnexpectedRollbackException` 显示为系统异常。
- RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldPersistLongSalesCountryList" test` -> FAIL，H2 报 `Value too long for column "target_market CHARACTER VARYING(255)"`，与测试服 MySQL 截断一致。
- GREEN: test-server-schema-fix -> PASS，测试服 `showroom_product_revision.target_market` 从 `varchar(255)` 扩为 `text`；修复前 `before_max_len=22`，修复后 `after_column=text`。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldPersistLongSalesCountryList" test` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py script/tests/test_showroom_release_sql_contract.py -q` -> PASS，`20 passed in 0.39s`。
- GREEN: login-preflight-test-server -> PASS，使用系统 Chrome 登录 `http://172.30.30.58:8081/showroom/product`，租户 `芋道源码`，用户 `admin`。
- GREEN: real-test-server-import -> PASS，Playwright 真实打开产品管理页并上传 `D:\ProjectPackage\Int\IntRuoyi\output\playwright\showroom-product-package-click-e2e\showroom-product-resource-package.zip`，接口返回 `totalRows=150`、`successCount=149`、`skippedCount=1`、`failureCount=0`、`awardTotalRows=46`、`awardSuccessCount=46`、`awardFailureCount=0`。
- NOTE: real-test-server-import-script -> 产品工具栏布局存在搜索框覆盖按钮坐标的问题，验证脚本使用真实 DOM button click 触发同一个 Vue 按钮事件；未改产品代码，未增加 fallback。