# 执行日志: 20260620-dcc-file-view-matrix-migration-dependency

- BDD: 文件查阅矩阵 seed 必须等待 scope_type schema -> Given 20260613 DCC 文件查阅矩阵 seed 要写入 dcc_file_category_permission_rule.scope_type / When 发布迁移计划解析 SQL 依赖 / Then 20260613_dcc_file_view_matrix_seed 必须依赖 20260614_dcc_product_visibility_group，且后者必须依赖 20260513_dcc_base_schema。

- GREEN: blocker-root-cause-confirmed -> PASS，正式发布日志已确认 `20260613_dcc_file_view_matrix_seed.sql` 在正式库执行时报 `Unknown column 'scope_type' in 'field list'`，根因不是 NAS、dry-run、包契约或状态表，而是迁移顺序错误。
- GREEN: dependency-chain-audit -> PASS，已核对 `20260513_dcc_base_schema.sql` 初始建表不含 `dcc_file_category_permission_rule.scope_type`；`20260614_dcc_product_visibility_group.sql` 才负责补列；`20260613_dcc_file_view_matrix_seed.sql` 已真实写入并关联 `scope_type`。
- GREEN: metadata-fix -> PASS，已将 `20260613_dcc_file_view_matrix_seed.sql` 的 `dependsOn` 修正为 `20260614_dcc_product_visibility_group`，并将 `20260614_dcc_product_visibility_group.sql` 的 `dependsOn` 修正为 `20260513_dcc_base_schema`。
- GREEN: pytest-dcc-product-visibility-group -> PASS，`python -X utf8 -m pytest script\tests\test_dcc_product_visibility_group_sql.py -q` 返回 `3 passed`。
- GREEN: pytest-dcc-file-view-matrix-seed -> PASS，`python -X utf8 -m pytest script\tests\test_dcc_file_view_matrix_seed.py -q` 返回 `8 passed`。
- GREEN: release-migration-policy-gate -> PASS，`python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` 返回 `status=passed, migrationCount=166`。
