# Database Schema Evidence

## Change

- 不新增业务表或备份状态表。
- 在现有备份计划菜单幂等 SQL 中增加 `system:backup-plan:evidence-export` 按钮权限和菜单 ID `901103`。
- 新增 `20260908_system_backup_evidence_export_permission.sql`，为已执行过 7 月迁移的环境提供独立幂等升级路径。
- 同步租户套餐和管理员角色权限集合，冲突 ID、非法 JSON 或权限缺失时 SQL 直接失败。

## Safety

- SQL 仍使用既有幂等 procedure 模式，不包含业务数据迁移。
- 本轮未连接数据库、未执行 migration、未写入任何租户数据。

## Verification

- `test_system_backup_plan_menu_sql.py` 纳入 160 项相关 Python 回归并通过。
- `run-release-migration-policy-gate.py` 对旧菜单迁移和新增权限迁移的完整依赖闭包验证通过。
- 真实数据库应用与回滚验证需要后续测试环境写入授权。
