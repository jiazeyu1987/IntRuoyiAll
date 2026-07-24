# Execution Log: 展厅产品与展厅 CRUD 后端契约

BDD: 产品管理 CRUD 后端契约 -> Given 后台产品管理页面需要新增、删除、查找、修改 When 调用展厅产品后台接口 Then 后端提供创建、保存、删除、关键字查询，并且列表最多返回 20 条。

BDD: 展厅管理 CRUD 后端契约 -> Given 后台展厅管理页面需要新增、删除、查找、修改 When 调用展厅后台接口 Then 后端提供创建、更新、删除、关键字查询，并且列表最多返回 20 条。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomHallContentTest" test` -> FAIL, `listProducts(keyword,pageNo,pageSize)`, `listHalls(keyword,pageNo,pageSize)`, `deleteProduct`, `deleteHall` 不存在。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomHallContentTest" test` -> PASS, 4 tests.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductContentTest,ShowroomHallContentTest,ShowroomHttpApiIntegrationTest" test` -> PASS, 9 tests.
