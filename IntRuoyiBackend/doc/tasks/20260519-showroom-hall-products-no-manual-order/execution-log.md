# 执行记录：展厅产品集合维护后端保存修复

BDD: 展厅产品映射替换必须允许原样保存 -> Given 某个展厅已存在正式产品映射集合 / When 后端收到同一 hall 的当前映射原样保存请求 / Then 替换逻辑必须成功，不得因为逻辑删除残留行与 `(hall_id, product_id)` 唯一键冲突而失败。

BDD: 展厅产品集合更新继续沿用真实替换语义 -> Given 前端提交新的 hall 产品集合和自动生成的 `displayOrder` / When 后端执行替换保存 / Then 旧映射应被清理并成功写入新集合，后续读取仍按 `displayOrder` 排序返回。

REPRO: 真实 API 直调 `PUT /admin-api/showroom/hall/update-product-mapping` 使用 hall 当前原样映射保存，也返回 `Duplicate entry '1-1' for key 'showroom_hall_product.uk_showroom_hall_product'`。
ROOT CAUSE: `replaceHallProductMappings` 使用 MyBatis Plus 默认 `delete(...)`，对继承 `BaseDO` 的 `showroom_hall_product` 实际执行的是逻辑删除；旧 `(hall_id, product_id)` 记录未物理移除，随后重插入同样组合时触发表唯一键冲突。
REGRESSION TEST: 在 `ShowroomPersistentContentServiceTest` 新增 `hallMappingsShouldAllowSavingTheSameProductSetTwice`，要求同一展厅同一套产品集合连续保存两次都成功。
RED: `mvn -pl yudao-module-showroom -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ShowroomPersistentContentServiceTest#hallMappingsShouldAllowSavingTheSameProductSetTwice" test` -> FAIL，第二次保存触发 `uk_showroom_hall_product` 唯一键冲突。
GREEN: `mvn -pl yudao-module-showroom -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ShowroomPersistentContentServiceTest#hallMappingsShouldAllowSavingTheSameProductSetTwice" test` -> PASS。
GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS，生成最新 `yudao-server.jar`。
GREEN: 本地 runtime 切换到 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260519-214110.jar` 后，认证调用 `PUT /admin-api/showroom/hall/update-product-mapping` 使用 hall 当前原样映射返回 `code=0`。
RISK: 本次修复只收口 hall 产品关系替换语义，未改动 showroom 其他逻辑删除表的行为。
BLOCKER: 无剩余 blocker。
