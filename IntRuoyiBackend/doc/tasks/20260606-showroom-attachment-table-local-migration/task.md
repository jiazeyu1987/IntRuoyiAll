# 展厅产品附件表本机迁移

## 任务目标

修复打开产品管理页签时报错：`Table 'ruoyi-vue-pro.showroom_product_revision_attachment' doesn't exist`。本机 MySQL `ruoyi-vue-pro` 库必须存在展厅产品 revision 附件快照表，产品列表查询附件时不应因缺表失败。

## BDD 场景

- BDD: 产品管理附件表存在 -> Given 本机 MySQL 使用 `ruoyi-vue-pro` 数据库 / When 后端查询 `showroom_product_revision_attachment` / Then 数据库应返回表结构并允许按 `tenant_id`、`product_revision_id` 排序查询。
- BDD: 迁移可重复执行 -> Given 附件表可能已存在 / When 执行附件表 schema SQL / Then 不应破坏已有数据或重复建表失败。

## 里程碑

- [x] M1：确认上一附件任务已 completed，创建本任务文档。
- [x] M2：复现本机库缺表 RED。
- [x] M3：执行附件表 schema 迁移并保留正式 SQL。
- [x] M4：验证查询、更新证据并提交本任务相关文件。

## 预期验证

- 本机 MySQL 查询 `SHOW TABLES LIKE 'showroom_product_revision_attachment'`。
- 执行 `sql/showroom/20260605_showroom_product_revision_attachment_schema.sql`。
- 本机 MySQL 查询 `SHOW CREATE TABLE showroom_product_revision_attachment`。
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductAttachmentTest test`。
- `python -X utf8 -m pytest script/tests/test_showroom_product_revision_attachment_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260606-showroom-attachment-table-local-migration/database-schema-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺表按 schema 迁移处理，不在代码中吞掉附件查询错误。
- `是否从根因和长期维护角度解决`：是。使用正式附件快照表 SQL 修复数据库结构缺失。
- `是否存在临时补丁或绕过`：否。不跳过 Mapper 查询，不禁用附件功能。

## 当前状态

completed

## 完成记录

- 已确认当前 Java 后端连接 `127.0.0.1:23306/ruoyi-vue-pro`，对应 Docker 容器 `int-ruoyi-mysql`。
- 已复现缺表：附件查询返回 MySQL `ERROR 1146`。
- 已执行 `sql/showroom/20260605_showroom_product_revision_attachment_schema.sql` 创建 `showroom_product_revision_attachment` 表。
- 已验证表结构、原查询 SQL、重复执行 schema SQL 均通过。
- 已验证 `mvn -pl yudao-module-showroom -Dtest=ShowroomProductAttachmentTest test` 通过，2 tests passed。
- 已验证 `python -X utf8 -m pytest script/tests/test_showroom_product_revision_attachment_sql.py -q` 通过，1 passed。
- 已验证 `validate_database_schema.py` 通过。

## Cleanup Keep

- `doc/tasks/20260606-showroom-attachment-table-local-migration/database-schema-evidence.md`
