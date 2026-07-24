# 执行日志

BDD: 产品管理按旧产品编号显示 -> Given 产品存在 `product_003`、`product_010`、`product_020` 等旧编号且数据库 id 顺序与旧编号顺序不一致 / When 用户打开展厅产品管理列表 / Then 列表必须按旧编号自然顺序显示，缺旧编号产品排在最后。
RED: mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentContentServiceTest#productPageShouldOrderRowsByLegacyProductCodeNaturally" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 当前按 id 返回 product_020/product_003/product_010，未按旧编号顺序。
GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentContentServiceTest#productPageShouldOrderRowsByLegacyProductCodeNaturally" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，1 test，0 failures，0 errors。
GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentContentServiceTest#productPageShouldOrderRowsByLegacyProductCodeNaturally+hallMappingsShouldPersistInDisplayOrder,ShowroomProductExcelImportExportIntegrationTest#legacyProductCodeShouldBeImportedAndVisibleOnProductPage" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，实际执行 2 tests，0 failures，0 errors；legacyProductCodeShouldBeImportedAndVisibleOnProductPage 未被当前 Surefire 方法模式匹配到。
