# Execution Log

BDD: DCC 矩阵注册主体映射匹配真实组织树 -> Given 正式服 `tenant_id=1` 中 `注册服务中心` 位于 `瑛泰医疗` 下 When 发布执行 `20260624_dcc_view_matrix_independent_seed.sql` Then `市场 / 注册` 主体应解析到 `瑛泰医疗/注册服务中心`，不得要求不存在的 `顶级部门/注册服务中心`。

GREEN: prod-readonly-root-cause -> PASS，正式服只读查询确认：`system_tenant` 中 `tenant_id=1` 为芋道源码且拥有 331 个部门；`tenant_id=122` 测试租户只有 3 个部门；`注册服务中心` 在 `tenant_id=1` 下 `parent_id=124`，父级 `瑛泰医疗`，`注册部` 在 `注册服务中心` 下。远端发布包原始 SQL 临时表预检显示唯一缺失主体为 `市场 / 注册 -> 顶级部门/注册服务中心`。

RED: python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py -q -> FAIL，预期原因：测试已要求 `市场 / 注册` 映射为 `瑛泰医疗/注册服务中心`，并要求测试租户 prerequisite 使用 `瑛泰医疗 -> 注册服务中心` 拓扑；当前 SQL 仍使用 `顶级部门/注册服务中心`。

GREEN: python -X utf8 -m pytest script\tests\test_dcc_view_matrix_independent_seed_sql.py -q -> PASS，结果：`4 passed`。修复后 `市场 / 注册` 指向 `瑛泰医疗/注册服务中心`，测试租户 prerequisite 同步使用 `瑛泰医疗 -> 注册服务中心 -> 注册部` 拓扑。

GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS，结果：`status=passed`，扫描 `migrationCount=218`，修复后的 `20260624_dcc_view_matrix_independent_seed.sql` 与 `20260624_dcc_view_matrix_test_tenant_prereq.sql` 均保留 release-migration 元数据并通过策略门禁。

GREEN: prod-readonly-fixed-subject-preflight -> PASS，正式服只读临时表预检使用修复后的 `tmp_dcc_view_matrix_seed_subject` 映射，仅校验 `system_dept` 解析，不调用 seed 过程、不写业务表；结果 `missing_or_duplicate=0`，`checked_dept_subjects=13`。

GREEN: git commit -> PASS，提交 `c17ef45d2c`，提交信息 `任务: 修复DCC矩阵正式服主体映射`。
