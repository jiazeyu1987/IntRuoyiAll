# 执行日志：20260605-required-sql-product-name-idempotent

BDD: 列不存在时添加 product_name -> Given 目标库 `dcc_controlled_file` 表不存在 `product_name` 列 / When 执行 `20260604_dcc_controlled_file_product_name.sql` / Then SQL 成功并添加该列。

BDD: 列已存在时可重复执行 -> Given 目标库 `dcc_controlled_file` 表已存在 `product_name` 列 / When 再次执行同一 required SQL / Then SQL 成功结束，不报重复列错误。

RED: `deploy-release 26-06-05_14-45-host-config-runtime-base to test` -> FAIL，测试服执行 `20260604_dcc_controlled_file_product_name.sql` 报 `ERROR 1060 (42S21): Duplicate column name 'product_name'`。

RED: `python -m pytest script/tests/test_dcc_sql_scripts.py -q` -> FAIL，`20260604_dcc_controlled_file_product_name.sql` 未查询 `information_schema.COLUMNS`，当前为裸 `ALTER TABLE`。

GREEN: `python -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS，8 tests passed。

GREEN: `mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportControlledFileProductName test` -> PASS。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_packages_and_applies_required_dcc_sql_for_all_deploys -q` -> PASS。
