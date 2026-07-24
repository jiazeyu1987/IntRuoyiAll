# Execution Log：DCC 产品目录改为数据库持久化

BDD: 查询产品目录只访问数据库 -> Given 数据已迁移 / When 调用分页接口 / Then Mapper 返回筛选分页结果且文件解析器不参与。
BDD: CRUD 持久化产品目录 -> Given 合法请求 / When 创建、更新或删除 / Then 对应数据库记录持久化。
BDD: 无效行键直接失败 -> Given 来源和原行号不存在 / When 更新、删除或比对 / Then 返回现有行键无效错误。

INFO: schema-preflight -> 当前本机 MySQL 8 数据库 `ruoyi-vue-pro` 不存在 `dcc_product_catalog` 表；`dcc_project_code` 使用标准审计字段和逻辑删除字段。
GREEN: experience-preflight -> PASS，仅允许对本机数据库执行本任务迁移与只读核验；禁止操作测试服和正式服。
RED: `python -X utf8 -m pytest script/tests/test_dcc_product_catalog_database_migration.py -q` -> FAIL，迁移和租户忽略配置不存在。
RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccProductCatalogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `DccProductCatalogDO` 和 `DccProductCatalogMapper`。
GREEN: `python -X utf8 -m pytest script/tests/test_dcc_product_catalog_database_migration.py -q` -> PASS，2 tests。
GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccProductCatalogServiceImplTest,DccProductCatalogRegistrationExpiryCompareServiceTest,DccProductCatalogControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，17 tests。
GREEN: local-migration -> PASS，首次导入 213 条，来源分布 32/181；重复执行后仍为 213 条。
GREEN: release-migration-policy -> PASS，`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` 校验 269 个迁移通过。
GREEN: evidence-validators -> PASS，Backend API 与 Database Schema evidence 均有效。
GREEN: backend-package -> PASS，`mvn.cmd -pl yudao-server -am -DskipTests package` 成功生成可执行包。
GREEN: packaged-dcc-contract -> PASS，包内存在数据库 Service / DO / Mapper，不存在 `DccProductCatalogWorkbookParser` 或 `DccProductCatalogWorkbookWriter`。
GREEN: local-runtime -> PASS，本机后端以新包重启并返回 `HEALTH=UP`。
GREEN: runtime-api -> PASS，测试租户认证后的分页接口返回 `total=213`，权限响应包含产品目录动态组件。
GREEN: final-result -> PASS，产品目录查询、维护和有效期比对已切换为数据库唯一数据源。
