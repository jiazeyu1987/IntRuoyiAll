# 展厅产品附件表本机迁移执行日志

## BDD

- BDD: 产品管理附件表存在 -> Given 本机 MySQL 使用 `ruoyi-vue-pro` 数据库 / When 后端查询 `showroom_product_revision_attachment` / Then 数据库应返回表结构并允许按 `tenant_id`、`product_revision_id` 排序查询。
- BDD: 迁移可重复执行 -> Given 附件表可能已存在 / When 执行附件表 schema SQL / Then 不应破坏已有数据或重复建表失败。

## TDD / Migration

- RED: `docker exec int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro -e "SELECT ... FROM showroom_product_revision_attachment ... LIMIT 1;"` -> FAIL, MySQL 返回 `ERROR 1146 (42S02): Table 'ruoyi-vue-pro.showroom_product_revision_attachment' doesn't exist`。
- GREEN: `Get-Content -Encoding utf8 -Raw sql\showroom\20260605_showroom_product_revision_attachment_schema.sql | docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro` -> PASS。
- GREEN: `SHOW TABLES LIKE 'showroom_product_revision_attachment'; SHOW CREATE TABLE showroom_product_revision_attachment` -> PASS，表和索引存在。
- GREEN: 原附件查询 SQL -> PASS。
- GREEN: 重复执行 schema SQL -> PASS。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductAttachmentTest test` -> PASS，2 tests passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_showroom_product_revision_attachment_sql.py -q` -> PASS，1 passed。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260606-showroom-attachment-table-local-migration/database-schema-evidence.md` -> PASS。

## 变更记录

- 任务启动：上一任务 `20260605-showroom-product-attachments` 已 completed；本任务只处理本机 MySQL 附件表缺失导致产品管理页签打不开。
- 迁移完成：当前 Java 后端实际连接 `127.0.0.1:23306/ruoyi-vue-pro`，已在 `int-ruoyi-mysql` 容器对应库中创建附件表。
