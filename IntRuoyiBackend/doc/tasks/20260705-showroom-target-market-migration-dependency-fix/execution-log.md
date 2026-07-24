# 20260705-showroom-target-market-migration-dependency-fix execution log

BDD: release migration dependency exists -> Given 发布前 migration policy gate 扫描 `sql/mysql`；When SQL 声明 `dependsOn`；Then 每个依赖必须能在同一发布扫描范围内找到真实 migrationId。

BDD: target market widening remains publishable -> Given `showroom_product_revision.target_market` 字段需要扩展为 `text`；When 执行发布 SQL 契约测试；Then SQL 元数据应引用真实存在的前置 migrationId，并保留非破坏性字段扩展语义。

## Initial State - 2026-07-05 00:12:18 +08:00

- Release task blocker: `dependsOn missing migration '20260519_showroom_v1_schema' for migrationId '20260704_showroom_product_target_market_text'`.
- Current unrelated dirty file preserved: `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordJingxiTableStructureVerificationTest.java`.

RED: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> FAIL, expected reason: `20260704_showroom_product_target_market_text` depends on missing migration `20260519_showroom_v1_schema`.

## Fix - 2026-07-05 00:14:00 +08:00

- Root cause: `sql/mysql/20260704_showroom_product_target_market_text.sql` is scanned under the `sql/mysql` release root, but its `dependsOn` referenced `20260519_showroom_v1_schema`, which exists only under `sql/showroom` and is not part of this root's migration id set.
- Change: set the `sql/mysql` migration `dependsOn=` to empty and update the focused contract test to assert the publishable metadata.

GREEN: `python -X utf8 -m pytest script\tests\test_showroom_product_target_market_release_sql.py` -> PASS, 1 passed.

GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS, 240 migrations scanned including `20260704_showroom_product_target_market_text` with `dependsOn=[]`.
