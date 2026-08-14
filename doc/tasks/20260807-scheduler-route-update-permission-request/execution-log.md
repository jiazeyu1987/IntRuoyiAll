# 执行日志：给排产员增加工艺路线更新权限

## User Intent

用户要求给排产员增加 `mes:pro-route:update` 权限。

## BDD

BDD: 排产员拥有工艺路线更新权限 -> Given 目标环境存在有效 `排产员/mes_scheduler` 角色和启用的 `mes:pro-route:update` 菜单 / When 核对或执行正式授权 / Then 该角色对该菜单存在有效 `system_role_menu` 绑定，且不新增删除权限或跨租户授权。

## Command Intent

- 只读核对：确认本地库目标角色、菜单和现有绑定。
- 最小写入：仅当目标绑定缺失或逻辑删除时恢复 `mes:pro-route:update`。
- 验证：复核目标权限有效、删除权限未新增、任务证据完整。

## Evidence

- 只读 schema/权限核对：`system_role` 存在两个有效 `排产员/mes_scheduler` 角色，分别为 `tenant_id=1 role_id=910233`、`tenant_id=122 role_id=910287`。
- 只读菜单核对：`system_menu.permission='mes:pro-route:update'` 对应 `menu_id=5723`，启用且未删除；`system_menu.permission='mes:pro-route:delete'` 对应 `menu_id=5724`，启用且未删除。
- 只读绑定核对：`tenant_id=1 role_id=910233` 已存在 `menu_id=5723` 的有效 `system_role_menu` 绑定，未绑定删除权限 `menu_id=5724`。
- RED: 只读权限断言 -> FAIL, `tenant_id=122 role_id=910287` 缺少 `menu_id=5723 / mes:pro-route:update` 的有效 `system_role_menu` 绑定。
- 套餐边界核对：`tenant_id=122 / 测试租户 / package_id=113` 的 `menu_ids` JSON 有效，但未包含 `5723 / mes:pro-route:update`，因此不能只写 `system_role_menu` 冒充完整授权链路。
- 用户影响核对：`zhaojie` 同时存在于 `tenant_id=1` 和 `tenant_id=122`；`tenant_id=1` 账号已绑定 `mes_scheduler` 且具备更新权限，`tenant_id=122` 账号绑定 `mes_scheduler` 但缺少更新权限。
- 用户确认：用户回复“继续”，授权对 `tenant_id=122 / package_id=113 / role_id=910287` 执行正式套餐菜单扩展和角色菜单绑定。
- 写入事务第一次执行：`ERROR 1260 Row 79 was cut by GROUP_CONCAT()`，触发事务 rollback；随后 rollback check 证明 `package_has_update=0`、`active_update_bindings=0`，未产生部分写入。
- GREEN: 受控 MySQL 事务 -> PASS，`package_rows=1`、`inserted_role_menu_rows=1`、`restored_role_menu_rows=0`、新增有效 `system_role_menu.id=912265`。
- GREEN: 后置 SQL 复核 -> PASS，`package_id=113` 菜单数 `295`，`has_update_5723=1`，`has_delete_5724=0`，`menu_ids_sha256=0e2971b5fa8671f2d42261946cd93536e4806a1d97cb9336fefb983590795bd5`。
- GREEN: 角色权限边界复核 -> PASS，`tenant_id=122 role_id=910287` 对 `mes:pro-route:update / 5723` 有且仅有 1 条有效绑定，对 `mes:pro-route:delete / 5724` 有效绑定为 0；`tenant_id=1 role_id=910233` 保持 update=1/delete=0。
- GREEN: `zhaojie` 有效权限复核 -> PASS，`tenant_id=122 user_id=913324` 通过有效角色得到 `mes:pro-route:update=1`，未得到 `mes:pro-route:delete`。
- GREEN: `system_user_role` 边界复核 -> PASS，行数 `2452`，SHA-256 `e6b15b053587951d665bdc51acfb8b97615313fa7b5c4dba668af6bf9f9a1804`，写入前后一致。
- GREEN: Redis 精确缓存处理 -> PASS，DB0/DB1 扫描 `*5723*`、`*mes:pro-route:update*`、`*910287*`、`*913324*` 无命中；精确 `DEL permission_menu_ids:mes:pro-route:update menu_role_ids:5723 user_role_ids:913324` 在 DB0/DB1 返回 `0`，未清全库缓存。
- GREEN: database-schema-delivery evidence validator -> PASS，`Database schema evidence is valid.`；关键 RED/GREEN 已归档到 `execution-log.md` 和 `verification-report.md`。
- GREEN: task-closeout-cleanup preview/apply -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，删除中间 `database-schema-evidence.md`，无 blocked/warnings，当前为主 worktree 非 linked worktree。
- Project experience consolidation：未新增长期经验文档；本次“按租户套餐和角色菜单双链路核对、套餐缺目标菜单时先确认扩权范围”的经验已由 `docs/database-rules.md#跨环境角色权限差异同步门禁` 覆盖。

## Blockers

- 无。
