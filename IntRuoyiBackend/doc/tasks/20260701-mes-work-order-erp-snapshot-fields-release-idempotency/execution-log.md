# 20260701 生产工单 ERP 快照字段发布 SQL 幂等修复执行日志

BDD: 已存在 ERP 快照字段时 required SQL 仍可重复执行 -> Given mes_pro_work_order 已存在 workshop_name 等全部或部分 ERP 快照字段 When 运行 20260630_mes_pro_work_order_erp_snapshot_fields.sql Then SQL 只补缺口且不再报 Duplicate column。

BDD: 全新库缺少 ERP 快照字段时 required SQL 仍完整补齐 -> Given mes_pro_work_order 尚未包含 ERP 快照字段 When 运行 20260630_mes_pro_work_order_erp_snapshot_fields.sql Then 9 个 ERP 快照字段以既定类型、注释与列顺序被补齐。

BDD: 迁移只补列不破坏本地扩展 -> Given 本地 mes_pro_work_order 已含现有业务字段 When ERP 快照字段迁移执行或重跑 Then 不执行 drop/delete/change/rename，也不覆盖其他本地扩展字段。

INFO: task-created -> 已创建生产工单 ERP 快照字段发布 SQL 幂等修复任务文档与执行日志。
GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md` 与 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`。
GREEN: publish-log-root-cause -> PASS，已确认测试服 `publish-test` 失败根因为 required SQL `20260630_mes_pro_work_order_erp_snapshot_fields.sql` 在目标库已存在 `workshop_name` 时重复 `ADD COLUMN`。
RED: python -X utf8 -c "import subprocess; repo=r'D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro'; sql=subprocess.run(['git','show','HEAD:sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql'], cwd=repo, check=True, capture_output=True, text=True, encoding='utf-8').stdout; required=['FROM information_schema.COLUMNS','PREPARE mes_pro_work_order_erp_snapshot_workshop_name_stmt','COLUMN_NAME = \\'planned_end_time\\'']; missing=[item for item in required if item not in sql]; assert not missing, 'missing guard fragments: ' + ', '.join(missing)" -> FAIL，旧版 SQL 缺少 information_schema 防重与逐列 PREPARE 语句，不满足重复发布契约。
GREEN: sql-idempotency-implementation -> PASS，已将 `20260630_mes_pro_work_order_erp_snapshot_fields.sql` 改为逐列 `information_schema.COLUMNS + PREPARE/EXECUTE` 正式幂等写法，保持只补缺口、不删除本地扩展。
GREEN: python -X utf8 -m pytest script/tests/test_mes_work_order_erp_snapshot_fields_sql.py script/tests/test_release_migration_metadata_sql_20260630.py -q -> PASS
GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS
GREEN: python -X utf8 tool/verify_tdd_compliance.py --task-dir doc/tasks/20260701-mes-work-order-erp-snapshot-fields-release-idempotency --paths sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql script/tests/test_mes_work_order_erp_snapshot_fields_sql.py -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-mes-work-order-erp-snapshot-fields-release-idempotency\bug-regression-evidence.md -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-mes-work-order-erp-snapshot-fields-release-idempotency\database-schema-evidence.md -> PASS
GREEN: ready-for-commit -> PASS，本次 required SQL 幂等修复、脚本测试与证据文件均已收口，待最小提交并交回维护仓重跑 committed-only 测试服发布。
