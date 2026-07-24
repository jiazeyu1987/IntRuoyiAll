# 执行日志：修复发布链路 showroom keyword runtime SQL 依赖顺序失真

BDD: required SQL 执行顺序必须服从 dependsOn 契约 -> Given release package 同时包含 showroom keyword schema runtime SQL 与 BU seed runtime SQL 且 BU seed dependsOn schema / When deploy-release 读取 preflight-plan 并执行 APPLY 项 / Then schema runtime SQL 必须先于 BU seed runtime SQL 执行，不能再被额外排序重排到后面。
BDD: 测试服真实 blocker 必须以发布链路正式修复收敛 -> Given 测试服 v9 发布日志显示 2026-06-27 真实执行时先跑了 20260626_showroom_keyword_bu_seed_runtime.sql 并报 Table 'ruoyi-vue-pro.showroom_keyword' doesn't exist / When 我们修复 deploy-release required SQL 排序逻辑 / Then 新发布包在测试服应先创建 showroom_keyword 表后再执行 BU seed，而不是依赖人工补表或改库绕过。

INFO: evidence -> 测试服真实失败 operation=op-2026-06-27T084525781193200Z-0aedd906-c321-4703-858f-f64a876df446；日志显示 package 已包含 20260626_showroom_keyword_schema_seed_runtime.sql 与 20260626_showroom_keyword_bu_seed_runtime.sql，但实际执行顺序先跑 bu_seed_runtime，随后报 ERROR 1146 Table 'ruoyi-vue-pro.showroom_keyword' doesn't exist。
INFO: root-cause-hypothesis -> deploy-release 的 Invoke-RequiredDatabaseSqlScripts 先读取 preflight-plan，再调用 Sort-RequiredDatabaseSqlApplyItems；当前排序函数只保留 DCC 特例 priorityMap，其他项按 migrationId 排序，破坏了 preflight 已计算出的 dependsOn 拓扑顺序。
INFO: experience-preflight -> 高风险真实发布动作尚未开始；当前仅做本机代码与只读服务器证据核对。
RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "preserves_preflight_dependency_order_for_non_priority_required_sql or executes_dcc_view_matrix_test_tenant_prereq_before_seed_on_test" -q -> FAIL, 新增契约测试断言 Sort-RequiredDatabaseSqlApplyItems 必须保留 preflight 原始依赖顺序时失败；函数中不存在 OriginalOrder 保序逻辑。
GREEN: apply_patch -> PASS, 将 Sort-RequiredDatabaseSqlApplyItems 调整为：测试环境仅对 20260624_dcc_view_matrix_test_tenant_prereq / 20260624_dcc_view_matrix_independent_seed 应用显式优先级，其他项按 preflight 传入顺序保序。
GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "preserves_preflight_dependency_order_for_non_priority_required_sql or executes_dcc_view_matrix_test_tenant_prereq_before_seed_on_test" -q -> PASS, 2 passed
GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q -> PASS, 94 passed
GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_release_preflight_plan.py -q -> PASS, 11 passed
GREEN: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> PASS, status=passed, migrationCount=218
