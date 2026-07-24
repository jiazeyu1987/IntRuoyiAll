# Execution Log：DCC 审阅矩阵 route node stage_type 运行时 schema 漂移修复

BDD: 旧版 route node 表缺少审阅矩阵元数据列时可正式升级 -> Given 历史 DCC 运行时库只具备旧版 dcc_category_approval_route_node 字段 / When 执行 runtime schema repair / Then stage_type、subject_label、marker、subject_type、subject_id、subject_name、subject_department_path、rule_remark 均被幂等补齐。
BDD: 审阅矩阵节点查询不再因缺列直接报错 -> Given route node 运行时表已通过 runtime schema repair 补齐规则元数据列 / When 后端按 routeId 查询 dcc_category_approval_route_node / Then SQL 可按完整字段集正常查询，不再抛出 Unknown column stage_type。

INFO: task-created -> 后端任务文档已创建，准备进入 DCC runtime schema repair RED 回归。
INFO: `mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，除本任务新增 `stage_type` 缺列断言外，还暴露与当前任务无关的 `dcc_category_view_matrix_rule` schema 覆盖缺口；已收敛为单方法定向回归，避免扩散范围。
RED: `mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest#mysqlRuntimeRepairSchemaShouldUpgradeLegacyDccTables "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `DCC runtime repair schema must add the route-node stage_type metadata column`
INFO: runtime-schema-repair -> 已将 `stage_type`、`subject_label`、`marker`、`subject_type`、`subject_id`、`subject_name`、`subject_department_path`、`rule_remark` 纳入 `20260515_dcc_runtime_schema_repair.sql` 幂等补列。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_route_node_rule_metadata_runtime_repair_sql.py -q` -> PASS
GREEN: `mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest#mysqlRuntimeRepairSchemaShouldUpgradeLegacyDccTables "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260625-dcc-route-node-stage-type-schema-drift\bug-regression-evidence.md` -> PASS
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260625-dcc-route-node-stage-type-schema-drift --mode preview` -> PASS
