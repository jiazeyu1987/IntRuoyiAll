# DCC 产品目录改为数据库持久化

## 任务目标

- 新增全局共享的 `dcc_product_catalog` 表及 Excel 种子迁移。
- 将产品目录分页、CRUD 和注册证有效期比对改为数据库访问。
- 删除运行时桌面 Excel 路径依赖，不提供文件回退路径。

## 当前状态

completed

## 经验门禁

- 新增 SQL 必须补 `script/tests/` 发布契约测试。
- SQL 以当前真实 MySQL 8 结构为准，迁移需幂等并提供回滚说明。
- Excel 有效行按真实单元格内容识别，不使用异常的 `max_row=1048560` 作为数据行数。
- 产品目录为原文件全局共享数据，表加入租户忽略配置；权限仍由现有 Controller 权限控制。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以数据库作为唯一运行时数据源。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 查询产品目录只访问数据库 -> Given 数据已迁移 / When 调用分页接口 / Then Mapper 返回筛选分页结果且文件解析器不参与。`
- `BDD: CRUD 持久化产品目录 -> Given 合法请求 / When 创建、更新或删除 / Then 对应数据库记录持久化。`
- `BDD: 无效行键直接失败 -> Given 来源和原行号不存在 / When 更新、删除或比对 / Then 返回现有行键无效错误。`

## 里程碑

1. 数据模型与迁移契约。`COMPLETED`
2. RED 单测与 SQL 契约测试。`COMPLETED`
3. DO/Mapper/Service 最小实现。`COMPLETED`
4. 回归、迁移验证和证据校验。`COMPLETED`

## 预期验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=*ProductCatalog*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -m pytest script/tests/test_dcc_product_catalog_database_migration.py`
- 本机 MySQL 表结构、行数、来源分布和代表性记录只读核验。

## 最终验证

- SQL 契约测试 2 个通过，产品目录定向 Java 测试 17 个通过。
- 发布迁移策略、后端 API 证据和数据库 Schema 证据校验通过。
- 可执行包构建通过，内嵌 DCC 模块不再包含工作簿 Parser / Writer。
- 本机迁移和运行态接口均返回 213 条，来源分布 32 / 181。
- 本机后端健康检查和测试租户真实登录验证通过。

## Cleanup Keep

- `doc/tasks/20260710-dcc-product-catalog-database-import/task.md`
- `doc/tasks/20260710-dcc-product-catalog-database-import/execution-log.md`
- `doc/tasks/20260710-dcc-product-catalog-database-import/backend-api-evidence.md`
- `doc/tasks/20260710-dcc-product-catalog-database-import/database-schema-evidence.md`
