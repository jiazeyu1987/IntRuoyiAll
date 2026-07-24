# 任务：本地数据库应用SRM管理员角色迁移

## Goal

将已完成开发的 `SRM管理员` 角色与 `SRM` 页签可见性 migration 正式应用到本地数据库，并确认：

- `system_role` 中存在 `SRM管理员` / `srm_admin` 角色；
- `admin` 账号已被赋予 `SRM管理员` 角色；
- `SRM管理员` 已绑定完整 `SRM` 菜单树；
- 不引入 fallback、绕过或静默降级。

## Previous Task Check

- `doc/tasks/20260629-srm-admin-role-visibility/task.md`：状态 `COMPLETED`，代码、SQL 与测试已完成，但当前本地数据库尚未应用 migration。

## 经验门禁

- `docs/experience-index.md`：本任务命中任务文档门禁，必须先建任务记录再执行数据库写入。
- `docs/powershell-memory.md`：本任务在 Windows PowerShell 5.1 下执行容器和 SQL 命令，必须显式采用 UTF-8 路径并避免 `&&`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接应用已通过契约测试的正式 migration，而非手工改表或临时插数据。
- 是否存在临时补丁或绕过：否。

## Milestones

1. 建立任务文档并确认当前本地数据库未应用 `srm_admin` migration。状态：completed。
2. 执行 migration 到本地数据库。状态：completed。
3. 回查角色、用户角色绑定和菜单授权，记录 GREEN 证据。状态：completed。
4. 更新任务文档与 schema evidence，完成收尾。状态：completed。

## Expected Verification

- 本地数据库中 `system_role` 出现 `code='srm_admin'`、`name='SRM管理员'` 的启用角色。
- 本地数据库中 `admin` 与 `role_id=910240` 存在有效绑定。
- 本地数据库中 `system_role_menu` 为 `role_id=910240` 绑定了 `SRM` 菜单树。
- 执行记录写入 `execution-log.md`，schema 证据写入 `database-schema-evidence.md`。

## Current Status

COMPLETED

## Final Verification

- PASS: `docker cp D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_srm_admin_role_visibility.sql int-ruoyi-mysql:/tmp/20260629_srm_admin_role_visibility.sql`；`docker exec int-ruoyi-mysql sh -lc "mysql -uroot -p123456 -D ruoyi-vue-pro < /tmp/20260629_srm_admin_role_visibility.sql"`，本地 MySQL migration 执行成功。
- PASS: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -B -e "<verification SQL>"`，确认 `system_role.id=910240/code=srm_admin` 已存在、`admin` 已绑定 `role_id=910240`、`system_role_menu` 对应 SRM 菜单树授权数量与 SRM 菜单树数量一致，均为 `60`。
