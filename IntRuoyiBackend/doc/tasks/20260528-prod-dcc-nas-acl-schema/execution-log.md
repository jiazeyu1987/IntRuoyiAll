# 执行日志：修复正式服 DCC NAS ACL 快照表缺失

BDD: NAS 转移权限快照表存在 -> Given 正式服发起 NAS 转移 / When 后端查询最近一次 ACL snapshot / Then `dcc_nas_acl_snapshot` 表存在且查询不会因缺表失败。

BDD: NAS ACL 快照恢复整组表完整 -> Given NAS 转移需要采集与恢复权限快照 / When 迁移脚本执行 / Then 8 张 `dcc_nas_acl_*` 表及关键唯一键/索引全部存在。

- 2026-05-28：定位报错为正式服缺少 `dcc_nas_acl_snapshot` 表，调用点为 `DccNasAclSnapshotMapper` 查询最近一次 transfer task 的 ACL snapshot。
- 2026-05-28：定位既有幂等建表脚本为 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql`，会创建 8 张 DCC NAS ACL 快照/恢复表。
- 2026-05-28：RED: 正式服只读 SQL 查询 -> FAIL，`dcc_nas_acl_*` 表数量为 0，符合用户报错。
- 2026-05-28：GREEN: 测试服只读 SQL 查询 -> PASS，`dcc_nas_acl_*` 表数量为 8；随后重复执行迁移脚本成功，验证幂等。
- 2026-05-28：GREEN: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> PASS，2 passed。
- 2026-05-28：GREEN: 正式服执行 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql` -> PASS。
- 2026-05-28：GREEN: 正式服验证 SQL -> PASS，`dcc_nas_acl_*` 表数量 8，唯一键数量 8，原 `dcc_nas_acl_snapshot` 查询可正常返回 0 行。
