# 任务: 修复 DCC 文件查阅矩阵迁移依赖

## 任务目标

修复 `sql/mysql/20260613_dcc_file_view_matrix_seed.sql` 与 `sql/mysql/20260614_dcc_product_visibility_group.sql` 的 `release-migration` 依赖声明，确保正式发布时 `dcc_file_category_permission_rule.scope_type` 的 schema 迁移先于文件查阅矩阵 seed 执行。

本任务只修正迁移元数据与契约测试，不执行数据库写入，不改业务数据。

## 经验门禁

- 发布前必须通过迁移策略门禁，不能靠正式库手工补列掩盖顺序错误。
- `release-migration` 的 `dependsOn` 必须表达真实前置条件，避免 seed 先于 schema 执行。
- 正式发布阻塞已证明为真实 schema 顺序问题，本次必须从根因修复发布链，而不是手工改正式库。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过补齐迁移依赖元数据，让发布系统按真实前置关系排序。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 文件查阅矩阵 seed 必须等待 scope_type schema -> Given 20260613 DCC 文件查阅矩阵 seed 要写入 dcc_file_category_permission_rule.scope_type / When 发布迁移计划解析 SQL 依赖 / Then 20260613_dcc_file_view_matrix_seed 必须依赖 20260614_dcc_product_visibility_group，且后者必须依赖 20260513_dcc_base_schema。`

## 里程碑

- [x] 建立任务文档并记录正式发布阻塞根因。
- [x] 复核相关 SQL 的真实依赖链。
- [x] 先补契约测试再修正迁移元数据。
- [x] 运行迁移相关测试与策略门禁。
- [x] 更新证据与状态。

## 当前状态

已完成。

## 最终验证

- `GREEN: python -X utf8 -m pytest script\tests\test_dcc_product_visibility_group_sql.py -q -> PASS, 3 passed`
- `GREEN: python -X utf8 -m pytest script\tests\test_dcc_file_view_matrix_seed.py -q -> PASS, 8 passed`
- `GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS, status=passed, migrationCount=166`
