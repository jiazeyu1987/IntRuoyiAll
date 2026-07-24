# 20260627 发布链路 DCC 查阅矩阵角色用户前置缺失

BDD: 测试服发布先准备 DCC 测试租户 prerequisite -> Given tenant_id=122 的真实部门存在但关键 leader_user_id 和 dccmatrix* prerequisite 用户缺失 / When deploy-release(test) 执行 DCC 查阅矩阵 required SQL / Then 发布链路必须先执行正式的测试租户 prerequisite SQL，再执行 independent seed，避免角色用户预检查失败。

BDD: prerequisite SQL 不得进入非测试环境写入 -> Given 同一发布包会继续用于 backup/prod / When preflight plan 针对 backup/prod 生成 required SQL 执行列表 / Then 测试租户 prerequisite SQL 必须只允许 test 环境执行，不得在 backup/prod 误写 tenant_id=122。

GREEN: experience-preflight -> PASS，本次仅在本机修改 SQL、契约测试与任务文档；测试服真实 blocker 已通过只读查询收集证据，不以人工服务器写入作为修复方案。

GREEN: test-server-role-user-readonly-preflight -> PASS，只读核对测试服 `172.30.30.58` / tenant `122` 真实数据后确认：
- `研发创新中心`、`质量体系中心`、`注册服务中心`、`瑛泰医疗`、`供应链中心` 及其关键子部门都已存在；
- `QC`、`新品开发部`、`生产制造中心`、`生产采购`、`包装设计组` 的 `leader_user_id` 均为空；
- `system_users` 中不存在任何 `username LIKE 'dccmatrix%'` 的 prerequisite 用户；
- 基于真实部门数据模拟 `dcc_matrix_qc_lead`、`dcc_matrix_packaging_design_lead`、`dcc_matrix_new_product_lead`、`dcc_matrix_production_line_lead`、`dcc_matrix_production_purchase_lead` 的 role-user 解析结果均为 `user_count=0`。

RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，新增“正式 `sql/mysql` 必须存在 test-only prerequisite 迁移、independent seed 契约必须与之协同、deploy-release(test) 必须在 seed 前执行 prerequisite”断言后，复现：
- `sql/mysql` 下还不存在 `20260624_dcc_view_matrix_test_tenant_prereq.sql`
- `20260624_dcc_view_matrix_independent_seed.sql` 仍只依赖 `20260623_dcc_view_matrix_independent_source`
- 发布脚本还没有针对 test 环境对 prerequisite / seed 做执行排序

GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_independent_seed_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_view_matrix_excel_seed_sql.py -q` -> PASS，`98 passed`

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS，`status=passed`、`migrationCount=218`

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_independent_seed_sql_test.py` -> PASS

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\dcc_view_matrix_test_tenant_prereq_sql_test.py` -> PASS

修复说明：
- 已把原 `script/dcc_view_matrix_test_tenant_prereq_20260624.sql` 正式化为发布迁移 `sql/mysql/20260624_dcc_view_matrix_test_tenant_prereq.sql`，并限制 `allowedEnvironments=test`，确保只对测试服租户 `122` 生效。
- `20260624_dcc_view_matrix_independent_seed.sql` 保持 `test,backup,prod` 共享契约，不直接依赖 test-only migration，避免破坏 release migration policy gate 的环境子集规则。
- 发布脚本新增 `Sort-RequiredDatabaseSqlApplyItems`：仅在 `deploy-release(test)` 时，把 `20260624_dcc_view_matrix_test_tenant_prereq` 排在 `20260624_dcc_view_matrix_independent_seed` 之前执行，从而在真实测试服先补齐 prerequisite 用户与 `leader_user_id`，再运行 independent seed。
