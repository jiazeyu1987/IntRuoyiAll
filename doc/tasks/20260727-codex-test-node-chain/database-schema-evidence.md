# Database Schema Evidence

## Data Change Goal

为 `system_codex_test_case` 增加节点串名称和串内序号字段，不修改现有测试项的默认归属。

## Rollback

迁移只新增可空字段和查询索引；回滚时删除新增索引和字段。正式回滚前必须确认没有测试项已使用节点串字段。

## Evidence

待执行。

