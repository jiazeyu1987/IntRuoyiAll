# 任务：修复正式服 DCC NAS ACL 快照表缺失

## 目标

修复正式服务器 NAS 管理点击“转移”时报错 `Table 'ruoyi-vue-pro.dcc_nas_acl_snapshot' doesn't exist` 的问题，补齐 DCC NAS 权限快照/恢复所需的整组 `dcc_nas_acl_*` 表，并让测试服与正式服结构保持一致。

## 里程碑

- [x] M1：定位报错表对应的既有建表脚本和测试覆盖。
- [x] M2：只读查询测试服与正式服当前 `dcc_nas_acl_*` 表结构现状。
- [x] M3：先测试服、后正式服执行幂等建表脚本。
- [x] M4：验证两台服务器 8 张 NAS ACL 表、关键唯一键和索引存在。
- [x] M5：记录故障与数据库证据，收尾提交。

## BDD 场景

BDD: NAS 转移权限快照表存在 -> Given 正式服发起 NAS 转移 / When 后端查询最近一次 ACL snapshot / Then `dcc_nas_acl_snapshot` 表存在且查询不会因缺表失败。

BDD: NAS ACL 快照恢复整组表完整 -> Given NAS 转移需要采集与恢复权限快照 / When 迁移脚本执行 / Then 8 张 `dcc_nas_acl_*` 表及关键唯一键/索引全部存在。

## 预期验证

- RED：正式服只读 SQL 查询显示缺少 `dcc_nas_acl_snapshot` 或整组 ACL 表。
- GREEN：测试服执行 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql` 后 8 张表存在。
- GREEN：正式服执行 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql` 后 8 张表存在。
- GREEN：`python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q`
- GREEN：证据校验脚本通过。

## 当前状态

completed

## Current Status

completed

## 验证结果

- RED：正式服 `information_schema.tables` 查询显示 `dcc_nas_acl_*` 表数量为 0。
- GREEN：测试服重复执行 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql` 成功，表数量 8，唯一键数量 8。
- GREEN：正式服执行 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql` 成功，表数量 8，唯一键数量 8。
- GREEN：正式服原报错查询 `SELECT ... FROM dcc_nas_acl_snapshot WHERE transfer_task_id = ?` 可正常执行。
- GREEN：`python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> 2 passed。

## Cleanup Keep

- doc/tasks/20260528-prod-dcc-nas-acl-schema/task.md
- doc/tasks/20260528-prod-dcc-nas-acl-schema/execution-log.md
- doc/tasks/20260528-prod-dcc-nas-acl-schema/database-schema-evidence.md
- doc/tasks/20260528-prod-dcc-nas-acl-schema/bug-regression-evidence.md
