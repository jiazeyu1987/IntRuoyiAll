# Verification Report

## Result

PASS：测试服 `mes_scheduler/排产员` 已获得 `mes:pro-schedule-order:delete`，`zhaojie` fresh 登录后删除按钮可见；未执行实际删除。

## Implementation

- Forward migration: `IntRuoyiBackend/sql/mysql/20260911_mes_scheduler_schedule_order_delete_permission.sql`。
- Read-only target preflight: `IntRuoyiBackend/sql/mysql/target-preflight/20260911_mes_scheduler_schedule_order_delete_permission.preflight.sql`。
- Commit: `41467d6c2`，已推送 `origin/codex/20260911-scheduler-delete-permission`。

## Test Evidence

- RED：迁移和 preflight 不存在时静态合同 `5 failed`。
- GREEN：静态合同 + MySQL 8 隔离场景 `6 passed`。
- GREEN：迁移策略与 target-preflight 候选过滤 `10 passed`。
- GREEN：database schema evidence validator PASS；`git diff --check` PASS。

## Test Server Evidence

- Preflight: target roles 2、target menu 1、pre-active bindings 0。
- Apply: tenant 1 / role 910216 新增 binding 907132；tenant 122 / role 910235 恢复 binding 905577。
- Postcondition: target active bindings 2、duplicate groups 0。
- Isolation: non-target delete grants `count=11` 与 SHA256 未变；all user-role bindings `count=2370` 与 SHA256 未变。
- Playwright: `芋道源码/zhaojie` 权限响应包含目标 permission，排产工单页 20 行、删除按钮 20 个、页面错误 0。

## Rollback

- 精确 pre-state：`test-server-preapply-backup.json`。
- 回滚：`rollback.sql` 只软删除本迁移 creator/updater 标记的活动绑定。
- 当前无需回滚，所有验收项通过。

## Safety Boundary

- 未点击删除按钮，未修改排产工单。
- 未清空 Redis；精确缓存扫描未发现现存 menu-role 缓存键。
- 未访问或修改正式服、审查服、备份服。
