BDD: 本地数据库应用SRM管理员角色迁移 -> Given 代码与 SQL migration 已完成但本地数据库尚未应用 / When 执行 `20260629_srm_admin_role_visibility.sql` / Then `srm_admin` 角色、`admin` 角色绑定和 SRM 菜单授权必须在本地数据库中实际存在

GREEN: experience-preflight -> PASS
- 已读取 `docs/experience-index.md`
- 已读取 `docs/powershell-memory.md`
- 本任务当前高风险动作仅为本机 Docker MySQL 容器内数据库写入，不涉及服务器、发布、备份或真实 E2E

GREEN: previous-task-check -> PASS
- `doc/tasks/20260629-srm-admin-role-visibility/task.md` 状态为 `COMPLETED`
- 已确认本次仅补执行本地数据库 migration，不重复修改生产代码

RED: local-db-state-check -> FAIL，当前本地数据库尚未存在 `srm_admin` 角色，也没有 `admin` -> `SRM管理员` 绑定

GREEN: `docker cp D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_srm_admin_role_visibility.sql int-ruoyi-mysql:/tmp/20260629_srm_admin_role_visibility.sql`；`docker exec int-ruoyi-mysql sh -lc "mysql -uroot -p123456 -D ruoyi-vue-pro < /tmp/20260629_srm_admin_role_visibility.sql"` -> PASS，已将正式 migration 应用到本地 MySQL 容器数据库。

GREEN: local-db-state-check -> PASS
- `system_role` 中存在 `id=910240`, `code=srm_admin`, `tenant_id=1`, `deleted=0`
- `admin` 与 `role_id=910240` 存在有效绑定，结果为 `user_id=1`, `tenant_id=1`, `deleted=0`
- `system_role_menu` 中 `role_id=910240` 的有效 SRM 菜单授权数量为 `60`
- 当前 SRM 菜单树数量为 `60`，与角色授权数量一致
